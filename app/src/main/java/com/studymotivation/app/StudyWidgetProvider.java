package com.studymotivation.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

/**
 * Minimal, launcher-safe home-screen widget.
 * Uses only standard RemoteViews widgets and the normal AppWidgetProvider lifecycle.
 */
public class StudyWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_UPDATE_NOW = "com.studymotivation.app.ACTION_UPDATE_NOW";

    private static int dayNumber() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.set(2026, Calendar.AUGUST, 31, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        long days = Math.max(0L, (now.getTimeInMillis() - start.getTimeInMillis()) / 86400000L);
        return (int) days + 1;
    }

    private static int quoteIndex() {
        return (dayNumber() - 1) % MainActivity.QUOTES.length;
    }

    private static String countdown() {
        Calendar target = Calendar.getInstance();
        target.set(2027, Calendar.JULY, 1, 0, 0, 0);
        target.set(Calendar.MILLISECOND, 0);

        long remaining = Math.max(0L, target.getTimeInMillis() - System.currentTimeMillis()) / 1000L;
        long days = remaining / 86400L;
        remaining %= 86400L;
        long hours = remaining / 3600L;
        remaining %= 3600L;
        long minutes = remaining / 60L;
        long seconds = remaining % 60L;
        return String.format(Locale.US, "%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.study_widget);
        int quote = quoteIndex();
        views.setTextViewText(R.id.widget_day, "DAY " + dayNumber());
        views.setTextViewText(R.id.widget_countdown, countdown());
        views.setTextViewText(R.id.widget_quote, "\u201c" + MainActivity.QUOTES[quote] + "\u201d");
        views.setTextViewText(R.id.widget_author, "\u2014 " + MainActivity.AUTHORS[quote]);

        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                widgetId,
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pi);
        manager.updateAppWidget(widgetId, views);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, StudyWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_UPDATE_NOW.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            updateAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        updateAll(context);
    }
}
