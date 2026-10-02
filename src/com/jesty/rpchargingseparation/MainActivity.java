package com.jesty.rpchargingseparation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
    private static final int PURPLE = Color.rgb(177, 91, 255);
    private static final int YELLOW = Color.rgb(255, 214, 58);
    private static final int MUTED = Color.rgb(190, 184, 207);
    private static final int HINT = Color.rgb(151, 143, 170);
    private static final int ERROR = Color.rgb(255, 106, 106);
    private static final int CONTENT_INSET_DP = 14;
    private static final int STEP_PERCENT = 5;

    private ScheduledExecutorService telemetryWorker;
    private SharedPreferences prefs;
    private ImageView backgroundImage;
    private boolean visualSeparated;
    private boolean suppressToggle;
    private Switch separationToggle;
    private Switch autoBootToggle;
    private TextView stateText;
    private TextView stateDetail;
    private TextView batteryValue;
    private TextView usbValue;
    private TextView deviceValue;
    private TextView tempValue;
    private TextView diagnostic;
    private RadioButton immediateMode;
    private RadioButton autoLimitMode;
    private LinearLayout autoLimitSettings;
    private EditText limitInput;
    private EditText resumeInput;
    private TextView limitNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("state", MODE_PRIVATE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());

        autoBootToggle.setChecked(prefs.getBoolean("auto_on_boot", false));
        autoBootToggle.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("auto_on_boot", checked).apply());
        bindModeSettings();

        separationToggle.setOnCheckedChangeListener((button, checked) -> {
            if (suppressToggle) return;
            prefs.edit().putBoolean("desired_enabled", checked).apply();
            Intent service = new Intent(this, BypassService.class)
                    .setAction(checked
                            ? BypassService.ACTION_ENABLE
                            : BypassService.ACTION_DISABLE);
            if (checked && Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            stateText.setText(checked ? "STARTING..." : "TURNING OFF...");
        });

        if (prefs.getBoolean("desired_enabled", false)) {
            Intent reconcile = new Intent(this, BypassService.class)
                    .setAction(BypassService.ACTION_ENABLE);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(reconcile);
            else startService(reconcile);
        }

        telemetryWorker = Executors.newSingleThreadScheduledExecutor();
        telemetryWorker.scheduleAtFixedRate(this::readAndRender, 0L, 1L, TimeUnit.SECONDS);
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        backgroundImage = new ImageView(this);
        backgroundImage.setImageResource(R.drawable.jesty_rp_background_charging);
        backgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(backgroundImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View shade = new View(this);
        GradientDrawable shadeDrawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFC060810, 0xEB0A0A18, 0x8A110B20, 0x00110B20});
        shade.setBackground(shadeDrawable);
        root.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        // The live dashboard gets its own right-hand column on wide landscape screens;
        // narrower screens keep it stacked under the controls.
        boolean wide = metrics.widthPixels / metrics.density >= 640f
                && metrics.widthPixels >= metrics.heightPixels * 1.5f;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        int panelWidth = wide ? Math.round(metrics.widthPixels * 0.50f)
                : metrics.widthPixels;
        root.addView(scroll, new FrameLayout.LayoutParams(
                panelWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        LinearLayout topActions = buildTopActions();
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42), Gravity.TOP | Gravity.END);
        actionParams.setMargins(0, dp(16), dp(18), 0);
        root.addView(topActions, actionParams);

        LinearLayout openSourceBadge = buildOpenSourceBadge();
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40), Gravity.BOTTOM | Gravity.END);
        badgeParams.setMargins(0, 0, dp(18), dp(16));
        root.addView(openSourceBadge, badgeParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(24);
        content.setPadding(horizontal, dp(12), horizontal, dp(16));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildHeader());

        stateText = text("READING HARDWARE...", 19f, YELLOW, true);
        stateText.setPadding(dp(CONTENT_INSET_DP), 0, dp(CONTENT_INSET_DP), 0);
        content.addView(stateText);
        stateDetail = text("", 13f, MUTED, false);
        stateDetail.setPadding(dp(CONTENT_INSET_DP), dp(2), dp(CONTENT_INSET_DP), dp(10));
        content.addView(stateDetail);

        content.addView(buildSwitchPanel());
        content.addView(buildModePanel());

        LinearLayout dashboard = buildDashboard();
        if (wide) {
            dashboard.setBackground(roundedPanel(0xB3100B19, 0x88B15BFF));
            dashboard.setPadding(dp(12), dp(10), dp(12), dp(10));
            int width = Math.round(metrics.widthPixels * 0.42f);
            FrameLayout.LayoutParams dashboardParams = new FrameLayout.LayoutParams(
                    width, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END | Gravity.CENTER_VERTICAL);
            dashboardParams.setMargins(0, dp(66), dp(18), dp(66));
            root.addView(dashboard, dashboardParams);
        } else {
            content.addView(dashboard);
        }
        return root;
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView lockup = new ImageView(this);
        lockup.setImageResource(R.drawable.jesty_rp_header_lockup);
        lockup.setScaleType(ImageView.ScaleType.FIT_CENTER);
        lockup.setAdjustViewBounds(true);
        lockup.setContentDescription("Jesty RP Charging Separation");
        header.addView(lockup, new LinearLayout.LayoutParams(dp(300), dp(100)));

        TextView version = text(versionLabel(), 11f, HINT, true);
        version.setGravity(Gravity.CENTER);
        version.setLetterSpacing(0.08f);
        version.setPadding(0, 0, 0, dp(8));
        header.addView(version);

        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return header;
    }

    private String versionLabel() {
        try {
            return "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable error) {
            return "";
        }
    }

    private LinearLayout buildSwitchPanel() {
        LinearLayout switchPanel = panel();
        separationToggle = new Switch(this);
        separationToggle.setText("Bypass charging");
        separationToggle.setTextColor(Color.WHITE);
        separationToggle.setTextSize(18f);
        separationToggle.setTypeface(Typeface.DEFAULT_BOLD);
        applyBrandToggleColors(separationToggle);
        switchPanel.addView(separationToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        TextView explanation = text(
                "USB powers the handheld while active battery charging stops."
                        + " Small battery currents can remain.",
                12f, MUTED, false);
        explanation.setPadding(0, 0, dp(56), dp(6));
        switchPanel.addView(explanation);

        autoBootToggle = new Switch(this);
        autoBootToggle.setText("Turn on again after restart");
        autoBootToggle.setTextColor(MUTED);
        autoBootToggle.setTextSize(13f);
        applyBrandToggleColors(autoBootToggle);
        switchPanel.addView(autoBootToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        return switchPanel;
    }

    private View buildModePanel() {
        LinearLayout modePanel = panel();
        modePanel.setPadding(dp(CONTENT_INSET_DP), dp(10), dp(CONTENT_INSET_DP), dp(10));

        TextView heading = text("WHEN TO STOP CHARGING", 11f, MUTED, true);
        heading.setLetterSpacing(0.06f);
        modePanel.addView(heading);

        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.VERTICAL);
        immediateMode = modeOption("Right away");
        modes.addView(immediateMode);
        modes.addView(optionHelp("Stops active charging while USB is connected."));
        autoLimitMode = modeOption("At a battery level");
        modes.addView(autoLimitMode);
        modes.addView(optionHelp("Charges normally up to a level you choose, then stops."));
        modePanel.addView(modes);

        autoLimitSettings = new LinearLayout(this);
        autoLimitSettings.setOrientation(LinearLayout.VERTICAL);
        autoLimitSettings.setPadding(0, dp(8), 0, 0);

        limitInput = percentInput();
        autoLimitSettings.addView(stepperRow("Stop charging at", limitInput,
                () -> commitLimit(readPercent(limitInput, BypassService.limitPercent(prefs))
                        - STEP_PERCENT),
                () -> commitLimit(readPercent(limitInput, BypassService.limitPercent(prefs))
                        + STEP_PERCENT)));
        autoLimitSettings.addView(fieldHelp(
                "Charges normally until this level, then runs from USB only."));

        resumeInput = percentInput();
        autoLimitSettings.addView(stepperRow("Charge again at", resumeInput,
                () -> commitResume(readPercent(resumeInput, BypassService.resumePercent(prefs))
                        - STEP_PERCENT),
                () -> commitResume(readPercent(resumeInput, BypassService.resumePercent(prefs))
                        + STEP_PERCENT)));
        autoLimitSettings.addView(fieldHelp(
                "If the battery drops to this level while plugged in,"
                        + " charging turns back on until it reaches the stop level again."));

        limitNote = text("", 11f, YELLOW, false);
        limitNote.setPadding(0, dp(4), 0, 0);
        limitNote.setVisibility(View.GONE);
        autoLimitSettings.addView(limitNote);

        modePanel.addView(autoLimitSettings);
        return modePanel;
    }

    private RadioButton modeOption(String label) {
        RadioButton option = new RadioButton(this);
        option.setId(View.generateViewId());
        option.setText(label);
        option.setTextColor(Color.WHITE);
        option.setTextSize(15f);
        option.setTypeface(Typeface.DEFAULT_BOLD);
        option.setButtonTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { YELLOW, Color.rgb(218, 211, 228) }));
        option.setLayoutParams(new RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        return option;
    }

    private TextView optionHelp(String value) {
        TextView help = text(value, 12f, MUTED, false);
        help.setPadding(dp(32), 0, 0, dp(4));
        return help;
    }

    private TextView fieldHelp(String value) {
        TextView help = text(value, 11f, HINT, false);
        help.setPadding(0, dp(2), 0, dp(8));
        return help;
    }

    private LinearLayout stepperRow(String label, EditText input,
                                    Runnable decrement, Runnable increment) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(label, 14f, Color.WHITE, true);
        row.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(stepButton("\u2212", decrement));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(dp(64), dp(38));
        inputParams.setMargins(dp(6), 0, dp(2), 0);
        row.addView(input, inputParams);
        TextView percent = text("%", 14f, MUTED, true);
        percent.setPadding(0, 0, dp(6), 0);
        row.addView(percent);
        row.addView(stepButton("+", increment));
        return row;
    }

    private TextView stepButton(String label, Runnable action) {
        TextView button = text(label, 18f, YELLOW, true);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setClickable(true);
        GradientDrawable background = roundedPanel(0xE0241338, 0xCCB15BFF);
        background.setCornerRadius(dp(19));
        button.setBackground(background);
        button.setOnFocusChangeListener((view, focused) ->
                background.setStroke(dp(focused ? 2 : 1), focused ? YELLOW : 0xCCB15BFF));
        button.setOnClickListener(view -> action.run());
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));
        return button;
    }

    private EditText percentInput() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(3) });
        input.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER);
        input.setTextColor(Color.WHITE);
        input.setTextSize(16f);
        input.setTypeface(Typeface.DEFAULT_BOLD);
        input.setPadding(dp(4), 0, dp(4), 0);
        GradientDrawable background = roundedPanel(0xE0100B19, 0x88B15BFF);
        background.setCornerRadius(dp(10));
        input.setBackground(background);
        input.setOnFocusChangeListener((view, focused) -> {
            background.setStroke(dp(focused ? 2 : 1), focused ? YELLOW : 0x88B15BFF);
            if (!focused) commitInput(input);
        });
        input.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            commitInput(input);
            input.clearFocus();
            InputMethodManager keyboard =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            return true;
        });
        return input;
    }

    private void commitInput(EditText input) {
        if (input == limitInput) {
            commitLimit(readPercent(limitInput, BypassService.limitPercent(prefs)));
        } else {
            commitResume(readPercent(resumeInput, BypassService.resumePercent(prefs)));
        }
    }

    private int readPercent(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private void commitLimit(int requested) {
        int limit = BypassService.clamp(requested,
                BypassService.MIN_LIMIT_PERCENT, BypassService.MAX_LIMIT_PERCENT);
        int previousResume = BypassService.resumePercent(prefs);
        int resume = BypassService.clampResumePercent(previousResume, limit);
        prefs.edit()
                .putInt(BypassService.PREF_LIMIT_PERCENT, limit)
                .putInt(BypassService.PREF_RESUME_PERCENT, resume)
                .apply();
        String note = null;
        if (limit != requested) {
            note = String.format(Locale.US, "Stop level must be between %d%% and %d%%.",
                    BypassService.MIN_LIMIT_PERCENT, BypassService.MAX_LIMIT_PERCENT);
        } else if (resume != previousResume) {
            note = String.format(Locale.US,
                    "Charge-again level moved to %d%% to stay %d%% below the stop level.",
                    resume, BypassService.MIN_RESUME_GAP);
        }
        renderLimitSettings(note);
    }

    private void commitResume(int requested) {
        int limit = BypassService.limitPercent(prefs);
        int resume = BypassService.clampResumePercent(requested, limit);
        prefs.edit().putInt(BypassService.PREF_RESUME_PERCENT, resume).apply();
        renderLimitSettings(resume == requested ? null : String.format(Locale.US,
                "Charge-again level must be between %d%% and %d%% (at least %d%% below stop).",
                BypassService.MIN_RESUME_PERCENT, limit - BypassService.MIN_RESUME_GAP,
                BypassService.MIN_RESUME_GAP));
    }

    private void bindModeSettings() {
        boolean auto = BypassService.autoLimitMode(prefs);
        immediateMode.setChecked(!auto);
        autoLimitMode.setChecked(auto);
        renderLimitSettings(null);

        immediateMode.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) return;
            prefs.edit().putString(BypassService.PREF_MODE,
                    BypassService.MODE_IMMEDIATE).apply();
            renderLimitSettings(null);
        });
        autoLimitMode.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) return;
            prefs.edit().putString(BypassService.PREF_MODE,
                    BypassService.MODE_AUTO_LIMIT).apply();
            renderLimitSettings(null);
        });
    }

    private void renderLimitSettings(String note) {
        autoLimitSettings.setVisibility(autoLimitMode.isChecked() ? View.VISIBLE : View.GONE);
        limitInput.setText(String.valueOf(BypassService.limitPercent(prefs)));
        resumeInput.setText(String.valueOf(BypassService.resumePercent(prefs)));
        limitNote.setText(note == null ? "" : note);
        limitNote.setVisibility(note == null ? View.GONE : View.VISIBLE);
    }

    private LinearLayout buildDashboard() {
        LinearLayout dashboard = new LinearLayout(this);
        dashboard.setOrientation(LinearLayout.VERTICAL);

        TextView heading = text("LIVE", 11f, YELLOW, true);
        heading.setLetterSpacing(0.12f);
        heading.setPadding(dp(4), 0, 0, dp(4));
        dashboard.addView(heading);

        LinearLayout row1 = row();
        batteryValue = addCard(row1, "BATTERY");
        usbValue = addCard(row1, "FROM CHARGER");
        dashboard.addView(row1);

        LinearLayout row2 = row();
        deviceValue = addCard(row2, "USED BY HANDHELD");
        tempValue = addCard(row2, "TEMPERATURE");
        dashboard.addView(row2);

        diagnostic = text("", 10f, HINT, false);
        diagnostic.setPadding(dp(4), dp(6), 0, 0);
        dashboard.addView(diagnostic);

        TextView legend = text("Battery: + charging  \u00b7  - using battery",
                10f, HINT, false);
        legend.setPadding(dp(4), dp(2), 0, 0);
        dashboard.addView(legend);
        return dashboard;
    }

    private LinearLayout buildTopActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView support = topAction("\u2615  SUPPORT");
        support.setOnClickListener(v -> openExternal("https://buymeacoffee.com/jesty"));
        actions.addView(support);

        TextView github = topAction("\u2605  GITHUB");
        github.setOnClickListener(v -> openExternal(
                "https://github.com/JestyLabs/Jesty-RP-Charging-Separation"));
        LinearLayout.LayoutParams githubParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        githubParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(github, githubParams);
        return actions;
    }

    private TextView topAction(String label) {
        TextView action = text(label, 10f, YELLOW, true);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(14), 0, dp(14), 0);
        GradientDrawable bubble = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xE0241338, 0xE0441B55});
        bubble.setCornerRadius(dp(21));
        bubble.setStroke(dp(1), 0xCCB15BFF);
        action.setBackground(bubble);
        action.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)));
        return action;
    }

    private LinearLayout buildOpenSourceBadge() {
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(16), dp(5), dp(16), dp(5));
        TextView title = text("JESTY APPS ARE FREE & OPEN SOURCE", 9f, YELLOW, true);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.05f);
        badge.addView(title);
        GradientDrawable bubble = new GradientDrawable();
        bubble.setColor(0xB5100B19);
        bubble.setCornerRadius(dp(25));
        bubble.setStroke(dp(1), 0x667B4AE2);
        badge.setBackground(bubble);
        return badge;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable error) {
            Toast.makeText(this, "No browser is available", Toast.LENGTH_SHORT).show();
        }
    }

    private GradientDrawable roundedPanel(int fill, int stroke) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), stroke);
        return background;
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(CONTENT_INSET_DP), dp(5), dp(CONTENT_INSET_DP), dp(5));
        panel.setBackground(roundedPanel(0xCF151021, 0x88B15BFF));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(9));
        panel.setLayoutParams(params);
        return panel;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2f);
        return row;
    }

    private TextView addCard(LinearLayout row, String label) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(8), dp(13), dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xC0161024);
        background.setCornerRadius(dp(13));
        card.setBackground(background);

        TextView heading = text(label, 11f, MUTED, true);
        heading.setLetterSpacing(0.06f);
        card.addView(heading);
        TextView value = text("-", 22f, Color.WHITE, true);
        value.setPadding(0, dp(2), 0, 0);
        card.addView(value);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(78), 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(card, params);
        return value;
    }

    private CharSequence twoLine(String main, String secondary) {
        SpannableString value = new SpannableString(main + "\n" + secondary);
        int start = main.length() + 1;
        value.setSpan(new RelativeSizeSpan(0.55f), start, value.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        value.setSpan(new ForegroundColorSpan(MUTED), start, value.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return value;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT_BOLD);
        return text;
    }

    private void applyBrandToggleColors(Switch toggle) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
        };
        toggle.setThumbTintList(new ColorStateList(states,
                new int[] { YELLOW, Color.rgb(218, 211, 228) }));
        toggle.setTrackTintList(new ColorStateList(states,
                new int[] { Color.rgb(154, 108, 18), Color.rgb(86, 78, 101) }));
    }

    private void readAndRender() {
        try {
            PowerTelemetry telemetry = PowerTelemetry.read();
            runOnUiThread(() -> render(telemetry));
        } catch (Throwable error) {
            runOnUiThread(() -> {
                stateText.setText("NO POWER READINGS");
                stateText.setTextColor(ERROR);
                stateDetail.setText(error.getMessage());
            });
        }
    }

    private void render(PowerTelemetry telemetry) {
        BypassService.State serviceState = BypassService.state();
        boolean active = serviceState == BypassService.State.ACTIVE
                && telemetry.separationConfirmed();
        setSeparationVisual(active);
        boolean desired = prefs.getBoolean("desired_enabled", false);
        suppressToggle = true;
        separationToggle.setChecked(desired);
        suppressToggle = false;

        String detail = BypassService.detail();
        if (active) {
            setState("RUNNING FROM USB", YELLOW);
        } else if (serviceState == BypassService.State.CHARGING_TO_LIMIT) {
            setState(String.format(Locale.US, "CHARGING TO %d%%",
                    BypassService.limitPercent(prefs)), Color.WHITE);
        } else if (serviceState == BypassService.State.ENABLING) {
            setState("STARTING...", YELLOW);
        } else if (serviceState == BypassService.State.ARMED
                || (desired && !telemetry.usbPresent)) {
            setState("READY", YELLOW);
            detail = "Plug in USB to start";
        } else if (serviceState == BypassService.State.FAILED) {
            setState("COULD NOT START", ERROR);
        } else if (!telemetry.usbPresent) {
            setState("ON BATTERY", MUTED);
        } else {
            setState("CHARGING NORMALLY", Color.WHITE);
        }
        stateDetail.setText(detail);

        batteryValue.setText(twoLine(
                String.format(Locale.US, "%d%%", telemetry.batteryPercent),
                String.format(Locale.US, "%+.2f A \u00b7 %+.1f W",
                        telemetry.displayedBatteryCurrentUa() / 1_000_000d,
                        telemetry.displayedBatteryWatts())));
        usbValue.setText(twoLine(
                String.format(Locale.US, "%.1f W", telemetry.usbWatts()),
                String.format(Locale.US, "%.2f V \u00b7 %.2f A",
                        telemetry.usbVoltageUv / 1_000_000d,
                        telemetry.usbCurrentUa / 1_000_000d)));
        deviceValue.setText(twoLine(
                String.format(Locale.US, "~ %.1f W", telemetry.estimatedDeviceWatts()),
                "estimated"));
        tempValue.setText(twoLine(
                String.format(Locale.US, "%.1f \u00b0C", telemetry.temperatureC),
                "battery"));
        diagnostic.setText(String.format(Locale.US,
                "%s | %s%s | %s | limit %d/%d | counter %d uAh",
                Build.MODEL, telemetry.usbType, telemetry.pdActive ? " / PD" : "",
                telemetry.batteryStatus, telemetry.limit, telemetry.limitMax,
                telemetry.chargeCounterUah));
    }

    private void setState(String label, int color) {
        stateText.setText(label);
        stateText.setTextColor(color);
    }

    private void setSeparationVisual(boolean separated) {
        if (visualSeparated == separated) return;
        visualSeparated = separated;
        backgroundImage.setImageResource(separated
                ? R.drawable.jesty_rp_background
                : R.drawable.jesty_rp_background_charging);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (telemetryWorker != null) telemetryWorker.shutdownNow();
        super.onDestroy();
    }
}
