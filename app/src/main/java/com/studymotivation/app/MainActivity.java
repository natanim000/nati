package com.studymotivation.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String PREFS = "study_motivation";
    static final String KEY_MCQ = "physics_mcqs";
    static final String KEY_QUOTE_DATE = "quote_date";
    static final String KEY_QUOTE_OFFSET = "quote_offset";
    static final String KEY_FOCUS_SECONDS = "focus_seconds";
    static final String KEY_FOCUS_RUNS = "focus_runs";

    static final int BG = Color.rgb(11, 9, 21);
    static final int SURFACE = Color.rgb(20, 17, 34);
    static final int SURFACE2 = Color.rgb(33, 27, 54);
    static final int WHITE = Color.WHITE;
    static final int MUTED = Color.rgb(175, 168, 198);
    static final int PURPLE = Color.rgb(167, 139, 250);
    static final int CYAN = Color.rgb(125, 211, 252);
    static final int GREEN = Color.rgb(105, 212, 163);
    static final int GOLD = Color.rgb(246, 200, 109);

    static final Calendar START = makeCalendar(2026, Calendar.AUGUST, 31);
    static final Calendar TARGET = makeCalendar(2027, Calendar.JULY, 1);

    static final String[] QUOTES = {
            "The important thing is to keep questioning.",
            "It always seems impossible until it is done.",
            "The people who are crazy enough to think they can change the world are the ones who do.",
            "Success is the sum of small efforts, repeated day in and day out.",
            "It does not matter how slowly you go as long as you do not stop.",
            "The future depends on what you do today.",
            "Great things are done by a series of small things brought together."
    };
    static final String[] AUTHORS = {
            "Albert Einstein", "Nelson Mandela", "Steve Jobs", "Robert Collier",
            "Confucius", "Mahatma Gandhi", "Vincent van Gogh"
    };

    SharedPreferences prefs;
    LinearLayout content;
    TextView navToday, navJourney, navTargets;
    TextView todayProgressText, focusTimerText;
    CountDownTimer focusTimer;
    boolean focusRunning = false;
    long focusRemainingMs = 25 * 60 * 1000L;
    final Handler handler = new Handler();

    static Calendar makeCalendar(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.set(y, m, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    int dp(float x) {
        return (int) (x * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        content = findViewById(R.id.content);
        navToday = findViewById(R.id.nav_today);
        navJourney = findViewById(R.id.nav_journey);
        navTargets = findViewById(R.id.nav_targets);

        navToday.setOnClickListener(v -> showToday());
        navJourney.setOnClickListener(v -> showJourney());
        navTargets.setOnClickListener(v -> showTargets());
        showToday();
        handler.post(updateRunnable);
    }

    final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (focusTimerText != null && !focusRunning) {
                focusTimerText.setText(formatDuration(focusRemainingMs));
            }
            handler.postDelayed(this, 1000L);
        }
    };

    String dateText() {
        Calendar c = Calendar.getInstance();
        String[] days = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return String.format(Locale.US, "%s, %s %d", days[c.get(Calendar.DAY_OF_WEEK)], months[c.get(Calendar.MONTH)], c.get(Calendar.DAY_OF_MONTH));
    }

    int dayNumber() {
        long diff = System.currentTimeMillis() - START.getTimeInMillis();
        return (int) Math.max(0L, diff / 86400000L) + 1;
    }

    long countdownMs() {
        return Math.max(0L, TARGET.getTimeInMillis() - System.currentTimeMillis());
    }

    String formatCountdown() {
        long total = countdownMs() / 1000L;
        long d = total / 86400L;
        total %= 86400L;
        long h = total / 3600L;
        total %= 3600L;
        long m = total / 60L;
        long s = total % 60L;
        return String.format(Locale.US, "%dd %02dh %02dm %02ds", d, h, m, s);
    }

    String formatDuration(long ms) {
        long total = Math.max(0L, ms / 1000L);
        long m = total / 60L;
        long s = total % 60L;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    int quoteIndex() {
        Calendar c = Calendar.getInstance();
        String today = String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        if (!today.equals(prefs.getString(KEY_QUOTE_DATE, ""))) {
            prefs.edit().putString(KEY_QUOTE_DATE, today).putInt(KEY_QUOTE_OFFSET, 0).apply();
        }
        int offset = prefs.getInt(KEY_QUOTE_OFFSET, 0);
        return ((dayNumber() - 1) + offset) % QUOTES.length;
    }

    void nextQuote() {
        prefs.edit().putInt(KEY_QUOTE_OFFSET, prefs.getInt(KEY_QUOTE_OFFSET, 0) + 1).apply();
        showToday();
        updateWidgetNow();
    }

    TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    void margin(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams base = v.getLayoutParams();
        ViewGroup.MarginLayoutParams p = base instanceof ViewGroup.MarginLayoutParams
                ? (ViewGroup.MarginLayoutParams) base
                : (base == null ? new LinearLayout.LayoutParams(-1, -2) : new ViewGroup.MarginLayoutParams(base));
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        v.setLayoutParams(p);
    }

    LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackgroundResource(R.drawable.card_bg);
        return c;
    }

    Button btn(String label, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(primary ? BG : WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
        return b;
    }

    ProgressBar progress(int value) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(100);
        p.setProgress(Math.max(0, Math.min(100, value)));
        p.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        p.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(7)));
        return p;
    }

    void setNav(String active) {
        navToday.setTextColor("today".equals(active) ? PURPLE : MUTED);
        navJourney.setTextColor("journey".equals(active) ? PURPLE : MUTED);
        navTargets.setTextColor("targets".equals(active) ? PURPLE : MUTED);
    }

    void gap(int h) {
        content.addView(new View(this), new LinearLayout.LayoutParams(1, dp(h)));
    }

    String taskKey(String id) {
        Calendar c = Calendar.getInstance();
        return "done_" + id + "_" + String.format(Locale.US, "%04d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    boolean done(String id) {
        return prefs.getBoolean(taskKey(id), false);
    }

    int completed() {
        String[] ids = {"recall", "physics", "math", "english", "review"};
        int count = 0;
        for (String id : ids) if (done(id)) count++;
        return count;
    }

    void toggleTask(String id) {
        prefs.edit().putBoolean(taskKey(id), !done(id)).apply();
        showToday();
        updateWidgetNow();
    }

    void resetToday() {
        String[] ids = {"recall", "physics", "math", "english", "review"};
        SharedPreferences.Editor e = prefs.edit();
        for (String id : ids) e.remove(taskKey(id));
        e.apply();
        Toast.makeText(this, "Today's plan reset", Toast.LENGTH_SHORT).show();
        showToday();
    }

    void addTask(LinearLayout parent, String id, String title, String detail, String time) {
        boolean complete = done(id);
        LinearLayout r = row();
        r.setPadding(0, dp(10), 0, dp(10));
        TextView check = text(complete ? "✓" : "○", 19, complete ? GREEN : MUTED, true);
        check.setGravity(Gravity.CENTER);
        r.addView(check, new LinearLayout.LayoutParams(dp(26), dp(26)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        TextView titleText = text(title, 13, complete ? MUTED : WHITE, true);
        if (complete) titleText.setPaintFlags(titleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        copy.addView(titleText);
        TextView detailText = text(detail, 10, MUTED, false);
        margin(detailText, 0, 2, 0, 0);
        copy.addView(detailText);
        r.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(text(time, 10, MUTED, true));
        r.setOnClickListener(v -> toggleTask(id));
        parent.addView(r);
    }

    void showToday() {
        stopFocusTimer();
        setNav("today");
        focusTimerText = null;
        content.removeAllViews();

        LinearLayout header = row();
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(dateText(), 12, MUTED, true));
        TextView title = text("Let's make it count.", 27, WHITE, true);
        margin(title, 0, 3, 0, 0);
        left.addView(title);
        header.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        TextView day = text("DAY " + dayNumber(), 10, GOLD, true);
        day.setGravity(Gravity.CENTER);
        day.setPadding(dp(10), dp(8), dp(10), dp(8));
        day.setBackgroundResource(R.drawable.pill_bg);
        header.addView(day);
        content.addView(header);
        gap(16);

        LinearLayout progressCard = card();
        TextView pt = text("TODAY'S PROGRESS", 10, MUTED, true);
        progressCard.addView(pt);
        todayProgressText = text(completed() + " / 5 blocks completed", 20, WHITE, true);
        margin(todayProgressText, 0, 6, 0, 0);
        progressCard.addView(todayProgressText);
        ProgressBar pb = progress(completed() * 20);
        margin(pb, 0, 7, 0, 0);
        progressCard.addView(pb);
        TextView small = text("Tap a study block when you finish it.", 10, MUTED, false);
        margin(small, 0, 8, 0, 0);
        progressCard.addView(small);
        Button reset = btn("Reset today's plan", false);
        reset.setOnClickListener(v -> resetToday());
        progressCard.addView(reset);
        content.addView(progressCard);
        gap(16);

        LinearLayout focus = card();
        focus.setBackgroundResource(R.drawable.card_gradient);
        focus.addView(text("FOCUS SESSION", 10, PURPLE, true));
        focusTimerText = text(formatDuration(focusRemainingMs), 34, WHITE, true);
        focusTimerText.setGravity(Gravity.CENTER);
        margin(focusTimerText, 0, 8, 0, 4);
        focus.addView(focusTimerText);
        TextView focusHint = text("25-minute focus • 5-minute break", 10, MUTED, false);
        focusHint.setGravity(Gravity.CENTER);
        focus.addView(focusHint);
        LinearLayout focusButtons = row();
        Button start = btn("Start", true);
        start.setOnClickListener(v -> startFocus(start));
        Button resetFocus = btn("Reset", false);
        resetFocus.setOnClickListener(v -> { stopFocusTimer(); focusRemainingMs = 25 * 60 * 1000L; focusTimerText.setText(formatDuration(focusRemainingMs)); start.setText("Start"); });
        focusButtons.addView(start, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, dp(44), 1);
        rp.setMargins(dp(8), 0, 0, 0);
        focusButtons.addView(resetFocus, rp);
        margin(focusButtons, 0, 12, 0, 0);
        focus.addView(focusButtons);
        content.addView(focus);
        gap(16);

        LinearLayout blocks = card();
        LinearLayout bh = row();
        bh.addView(text("TODAY'S INTERACTIVE PLAN", 10, MUTED, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView doneLabel = text(completed() * 20 + "%", 11, GREEN, true);
        bh.addView(doneLabel);
        blocks.addView(bh);
        addTask(blocks, "recall", "Morning recall", "Flashcards + quick formulas", "20m");
        addTask(blocks, "physics", "Physics priority", "Main topic + worked examples", "90m");
        addTask(blocks, "math", "Math / problem solving", "Timed practice + corrections", "75m");
        addTask(blocks, "english", "English / aptitude", "Grammar, vocabulary, speed", "30m");
        addTask(blocks, "review", "Review + error log", "Write the 3 most useful mistakes", "30m");
        content.addView(blocks);
        gap(16);

        LinearLayout quick = card();
        quick.addView(text("QUICK ACTIONS", 10, MUTED, true));
        LinearLayout qrow = row();
        Button journey = btn("Open Journey", false);
        journey.setOnClickListener(v -> showJourney());
        Button quote = btn("New quote", false);
        quote.setOnClickListener(v -> nextQuote());
        qrow.addView(journey, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(0, dp(44), 1);
        qp.setMargins(dp(8), 0, 0, 0);
        qrow.addView(quote, qp);
        quick.addView(qrow);
        content.addView(quick);
    }

    void startFocus(Button startButton) {
        if (focusRunning) {
            stopFocusTimer();
            startButton.setText("Resume");
            return;
        }
        focusRunning = true;
        startButton.setText("Pause");
        focusTimer = new CountDownTimer(focusRemainingMs, 1000L) {
            @Override public void onTick(long ms) { focusRemainingMs = ms; if (focusTimerText != null) focusTimerText.setText(formatDuration(ms)); }
            @Override public void onFinish() {
                focusRunning = false;
                focusRemainingMs = 25 * 60 * 1000L;
                prefs.edit().putInt(KEY_FOCUS_RUNS, prefs.getInt(KEY_FOCUS_RUNS, 0) + 1).apply();
                if (focusTimerText != null) focusTimerText.setText("DONE ✓");
                Toast.makeText(MainActivity.this, "Focus session complete!", Toast.LENGTH_LONG).show();
                updateWidgetNow();
            }
        }.start();
    }

    void stopFocusTimer() {
        if (focusTimer != null) { focusTimer.cancel(); focusTimer = null; }
        focusRunning = false;
    }

    void showJourney() {
        stopFocusTimer();
        setNav("journey");
        focusTimerText = null;
        content.removeAllViews();

        content.addView(text("THE LONG GAME", 10, PURPLE, true));
        TextView h = text("Your journey", 30, WHITE, true);
        margin(h, 0, 3, 0, 0);
        content.addView(h);
        content.addView(text("See the bigger picture, then come back to today's work.", 12, MUTED, false));
        gap(16);

        LinearLayout arc = card();
        arc.setBackgroundResource(R.drawable.card_gradient);
        arc.addView(text("COUNTDOWN", 10, CYAN, true));
        TextView cd = text(formatCountdown(), 27, WHITE, true);
        margin(cd, 0, 5, 0, 0);
        arc.addView(cd);
        arc.addView(text("until July 1, 2027", 11, MUTED, false));
        content.addView(arc);
        gap(16);

        LinearLayout mcq = card();
        mcq.addView(text("PHYSICS MCQ TRACKER", 10, PURPLE, true));
        int count = prefs.getInt(KEY_MCQ, 0);
        TextView n = text(count + " / 500", 30, WHITE, true);
        margin(n, 0, 4, 0, 0);
        mcq.addView(n);
        ProgressBar mp = progress((int) Math.round(count / 5.0));
        margin(mp, 0, 8, 0, 0);
        mcq.addView(mp);
        mcq.addView(text("Log questions as you complete them.", 10, MUTED, false));
        LinearLayout mBtns = row();
        Button b5 = btn("+5", false); b5.setOnClickListener(v -> addMcq(5));
        Button b10 = btn("+10", true); b10.setOnClickListener(v -> addMcq(10));
        mBtns.addView(b5, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams b10p = new LinearLayout.LayoutParams(0, dp(44), 1); b10p.setMargins(dp(8),0,0,0); mBtns.addView(b10,b10p);
        margin(mBtns, 0, 12, 0, 0);
        mcq.addView(mBtns);
        content.addView(mcq);
        gap(16);

        LinearLayout quote = card();
        quote.addView(text("TODAY'S MOTIVATION", 10, GOLD, true));
        int qi = quoteIndex();
        TextView qt = text("\u201c" + QUOTES[qi] + "\u201d", 17, WHITE, true);
        margin(qt,0,9,0,0);
        quote.addView(qt);
        quote.addView(text("— " + AUTHORS[qi], 11, MUTED, true));
        Button nq = btn("New quote", false); nq.setOnClickListener(v -> nextQuote()); margin(nq,0,10,0,0); quote.addView(nq);
        content.addView(quote);
        gap(16);

        LinearLayout rhythm = card();
        rhythm.addView(text("TODAY'S RHYTHM", 10, MUTED, true));
        TextView rpText = text(completed() * 20 + "% completed", 22, WHITE, true);
        margin(rpText,0,6,0,0); rhythm.addView(rpText);
        ProgressBar rp = progress(completed()*20); margin(rp,0,8,0,0); rhythm.addView(rp);
        TextView runs = text(prefs.getInt(KEY_FOCUS_RUNS,0) + " focus sessions completed", 10, MUTED, false); rhythm.addView(runs);
        content.addView(rhythm);
        gap(16);

        LinearLayout phases = card();
        phases.addView(text("THE ROADMAP", 10, MUTED, true));
        addPhase(phases,"TERM 1 · FOUNDATION","Sep–Dec 2026","Coverage, fundamentals, and a consistent study rhythm.",PURPLE);
        addPhase(phases,"TERM 2 · INTENSIVE COVERAGE","Jan–Mar 2027","Harder questions, deeper Physics, and full mock practice.",CYAN);
        addPhase(phases,"TERM 3 · EXAM SPRINT","Apr–May 2027","Mixed review, timed papers, and final confidence building.",GREEN);
        content.addView(phases);
    }

    void addMcq(int amount) {
        int next = Math.min(500, prefs.getInt(KEY_MCQ,0) + amount);
        prefs.edit().putInt(KEY_MCQ,next).apply();
        Toast.makeText(this, "+"+amount+" Physics MCQs", Toast.LENGTH_SHORT).show();
        showJourney(); updateWidgetNow();
    }

    void addPhase(LinearLayout parent,String title,String dates,String desc,int color){
        LinearLayout r=row();
        View dot=new View(this); dot.setBackgroundColor(color); LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(dp(8),dp(8)); dp1.setMargins(0,dp(13),dp(12),0); r.addView(dot,dp1);
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title,13,WHITE,true));
        TextView d=text(dates,10,MUTED,true); margin(d,0,3,0,0); copy.addView(d);
        copy.addView(text(desc,10,MUTED,false));
        r.addView(copy,new LinearLayout.LayoutParams(0,-2,1)); margin(r,0,10,0,0); parent.addView(r);
    }

    void showTargets() {
        stopFocusTimer(); setNav("targets"); focusTimerText=null; content.removeAllViews();
        content.addView(text("THE SCOREBOARD",10,PURPLE,true));
        TextView h=text("Targets",30,WHITE,true); margin(h,0,3,0,0); content.addView(h);
        content.addView(text("Know the standard. Then win the next block.",12,MUTED,false)); gap(16);
        addTargetCard("Physics",89,PURPLE); addTargetCard("Chemistry",96,CYAN); addTargetCard("Biology",95,GREEN); addTargetCard("Math",94,PURPLE); addTargetCard("English",91,GOLD); addTargetCard("Aptitude",90,PURPLE);
    }

    void addTargetCard(String subject,int target,int color){
        LinearLayout c=card(); LinearLayout r=row(); r.addView(text(subject,14,WHITE,true),new LinearLayout.LayoutParams(0,-2,1)); r.addView(text(target+"%",19,color,true)); c.addView(r);
        ProgressBar p=progress(target); p.setProgressTintList(android.content.res.ColorStateList.valueOf(color)); margin(p,0,10,0,0); c.addView(p);
        c.addView(text(target+"% target",10,MUTED,false)); content.addView(c); gap(10);
    }

    void updateWidgetNow(){
        try {
            Intent intent = new Intent(this, StudyWidgetProvider.class);
            intent.setAction("com.studymotivation.app.ACTION_UPDATE_NOW");
            sendBroadcast(intent);
        } catch (Exception ignored) { }
    }

    @Override protected void onDestroy(){
        stopFocusTimer(); handler.removeCallbacks(updateRunnable); super.onDestroy();
    }
}
