package com.jesty.rpchargingseparation;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
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

import java.util.List;
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
    private AppUpdater updater;
    private TextView updateAction;
    private SharedPreferences prefs;
    private ImageView backgroundImage;
    private boolean visualSeparated;
    private boolean suppressToggle;
    private boolean offReconcileStarted;
    private Boolean controlsEnabled;
    private Switch separationToggle;
    private Switch autoBootToggle;
    private LinearLayout modePanel;
    private TextView stateText;
    private TextView stateDetail;
    private TextView batteryValue;
    private TextView usbValue;
    private TextView deviceValue;
    private TextView tempValue;
    private TextView diagnostic;
    private TextView restrictionWarning;
    private RadioButton immediateMode;
    private RadioButton autoLimitMode;
    private RadioGroup modeGroup;
    private LinearLayout autoLimitSettings;
    private EditText limitInput;
    private EditText resumeInput;
    private TextView limitNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("state", MODE_PRIVATE);
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

        boolean desiredAtLaunch = prefs.getBoolean("desired_enabled", false);
        if (!desiredAtLaunch) {
            prefs.edit().putBoolean("auto_on_boot", false).apply();
        }
        autoBootToggle.setChecked(desiredAtLaunch && prefs.getBoolean("auto_on_boot", false));
        autoBootToggle.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("auto_on_boot", checked).apply());
        bindModeSettings();
        setControlsEnabled(desiredAtLaunch);

        separationToggle.setOnCheckedChangeListener((button, checked) -> {
            if (suppressToggle) return;
            SharedPreferences.Editor settings = prefs.edit()
                    .putBoolean("desired_enabled", checked)
                    .remove(BypassService.PREF_LAST_SAFETY_STOP);
            if (!checked) {
                settings.putBoolean("auto_on_boot", false);
                autoBootToggle.setChecked(false);
            }
            if (!settings.commit()) {
                Toast.makeText(this, "Could not save bypass setting", Toast.LENGTH_LONG).show();
                suppressToggle = true;
                separationToggle.setChecked(!checked);
                suppressToggle = false;
                return;
            }
            offReconcileStarted = !checked;
            setControlsEnabled(checked);
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

        updater = new AppUpdater(this, version -> {
            updateAction.setText("\u2191  UPDATE v" + version);
            updateAction.setVisibility(View.VISIBLE);
        });
        updater.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updater != null) updater.onResume();
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
        // The live panel gets its own right-hand column on wide landscape screens;
        // narrower screens keep it stacked under the controls.
        boolean wide = metrics.widthPixels / metrics.density >= 640f
                && metrics.widthPixels >= metrics.heightPixels * 1.5f;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setHorizontalScrollBarEnabled(false);
        int panelWidth = wide ? Math.round(metrics.widthPixels * 0.50f)
                : metrics.widthPixels;
        root.addView(scroll, new FrameLayout.LayoutParams(
                panelWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        LinearLayout topActions = buildTopActions();
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42), Gravity.TOP | Gravity.END);
        actionParams.setMargins(0, dp(16), dp(18), 0);
        root.addView(topActions, actionParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(24);
        content.setPadding(horizontal, dp(12), horizontal, dp(16));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildHeader());

        stateText = text("READING HARDWARE...", 19f, YELLOW, true);
        stateDetail = text("", 13f, MUTED, false);
        if (!wide) {
            stateText.setPadding(dp(CONTENT_INSET_DP), 0, dp(CONTENT_INSET_DP), 0);
            stateDetail.setPadding(dp(CONTENT_INSET_DP), dp(2),
                    dp(CONTENT_INSET_DP), dp(10));
            content.addView(stateText);
            content.addView(stateDetail);
        }

        LinearLayout controls = panel();
        controls.addView(buildSwitchPanel());
        modePanel = buildModePanel();
        controls.addView(modePanel);
        autoBootToggle = new Switch(this);
        autoBootToggle.setText("Maintain bypass charging after reboot");
        autoBootToggle.setTextColor(Color.WHITE);
        autoBootToggle.setTextSize(13f);
        applyBrandToggleColors(autoBootToggle);
        LinearLayout.LayoutParams bootParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        bootParams.setMargins(0, dp(8), 0, 0);
        controls.addView(autoBootToggle, bootParams);
        content.addView(controls);

        LinearLayout dashboard = buildDashboard(wide);
        if (wide) {
            dashboard.setBackground(roundedPanel(0xB3100B19, 0x88B15BFF));
            dashboard.setPadding(dp(12), dp(14), dp(12), dp(12));
            int width = Math.round(metrics.widthPixels * 0.46f);
            FrameLayout.LayoutParams dashboardParams = new FrameLayout.LayoutParams(
                    width, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END | Gravity.BOTTOM);
            dashboardParams.setMargins(0, 0, dp(18), dp(18));
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
        LinearLayout switchPanel = new LinearLayout(this);
        switchPanel.setOrientation(LinearLayout.VERTICAL);
        separationToggle = new Switch(this);
        separationToggle.setText("BYPASS CHARGING");
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

        return switchPanel;
    }

    private LinearLayout buildModePanel() {
        LinearLayout modePanel = new LinearLayout(this);
        modePanel.setOrientation(LinearLayout.VERTICAL);
        modePanel.setPadding(0, dp(10), 0, 0);

        TextView heading = text("WHEN TO STOP CHARGING", 11f, MUTED, true);
        heading.setLetterSpacing(0.06f);
        modePanel.addView(heading);

        modeGroup = new RadioGroup(this);
        modeGroup.setOrientation(RadioGroup.VERTICAL);
        immediateMode = modeOption("RIGHT AWAY");
        modeGroup.addView(immediateMode);
        modeGroup.addView(optionHelp("Stops active charging to the battery."));
        autoLimitMode = modeOption("AT A BATTERY LEVEL");
        modeGroup.addView(autoLimitMode);
        modeGroup.addView(optionHelp("Charges normally up to a level you choose, then stops."));
        modePanel.addView(modeGroup);

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
        modeGroup.check(auto ? autoLimitMode.getId() : immediateMode.getId());
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

    private void setControlsEnabled(boolean enabled) {
        if (controlsEnabled != null && controlsEnabled == enabled) return;
        controlsEnabled = enabled;
        setEnabledRecursively(modePanel, enabled);
        modePanel.setAlpha(enabled ? 1f : 0.42f);
        autoBootToggle.setEnabled(enabled);
        autoBootToggle.setAlpha(enabled ? 1f : 0.42f);
        if (enabled) {
            boolean auto = BypassService.autoLimitMode(prefs);
            modeGroup.check(auto ? autoLimitMode.getId() : immediateMode.getId());
        } else {
            prefs.edit().putBoolean("auto_on_boot", false).apply();
            autoBootToggle.setChecked(false);
        }
        renderLimitSettings(null);
    }

    private void setEnabledRecursively(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setEnabledRecursively(group.getChildAt(index), enabled);
            }
        }
    }

    private void renderLimitSettings(String note) {
        autoLimitSettings.setVisibility(autoLimitMode.isChecked()
                ? View.VISIBLE : View.GONE);
        limitInput.setText(String.valueOf(BypassService.limitPercent(prefs)));
        resumeInput.setText(String.valueOf(BypassService.resumePercent(prefs)));
        limitNote.setText(note == null ? "" : note);
        limitNote.setVisibility(note == null ? View.GONE : View.VISIBLE);
    }

    private LinearLayout buildDashboard(boolean wide) {
        LinearLayout dashboard = new LinearLayout(this);
        dashboard.setOrientation(LinearLayout.VERTICAL);

        diagnostic = text("", 11f, MUTED, false);
        diagnostic.setPadding(dp(4), 0, 0, 0);

        if (wide) {
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.VERTICAL);

            stateText.setTextSize(20f);
            stateText.setGravity(Gravity.START);
            stateText.setPadding(dp(4), 0, dp(4), 0);
            LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            header.addView(stateText, stateParams);

            LinearLayout.LayoutParams deviceParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            deviceParams.setMargins(0, dp(5), 0, 0);
            header.addView(diagnostic, deviceParams);

            stateDetail.setTextSize(11f);
            stateDetail.setGravity(Gravity.START);
            stateDetail.setPadding(dp(4), 0, dp(4), 0);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            detailParams.setMargins(0, dp(2), 0, 0);
            header.addView(stateDetail, detailParams);

            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            headerParams.setMargins(0, 0, 0, dp(10));
            dashboard.addView(header, headerParams);
        } else {
            LinearLayout.LayoutParams deviceParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            deviceParams.setMargins(0, dp(4), 0, dp(7));
            dashboard.addView(diagnostic, deviceParams);
        }

        LinearLayout row1 = row();
        batteryValue = addCard(row1, "BATTERY");
        usbValue = addCard(row1, "FROM CHARGER");
        dashboard.addView(row1);

        LinearLayout row2 = row();
        deviceValue = addCard(row2, "USED BY HANDHELD");
        tempValue = addCard(row2, "TEMPERATURE");
        dashboard.addView(row2);

        // Battery restriction can stop the service while the screen is off (issue #2).
        restrictionWarning = text("Android is restricting this app in the background, so "
                + "bypass may stop while the screen is off. Open App info \u2192 Battery "
                + "and allow background use.", 11f, ERROR, false);
        restrictionWarning.setPadding(dp(4), dp(6), dp(4), 0);
        restrictionWarning.setVisibility(View.GONE);
        restrictionWarning.setOnClickListener(v -> openAppInfo());
        dashboard.addView(restrictionWarning);

        TextView copyDiagnostics = text("COPY DIAGNOSTICS", 10f, YELLOW, true);
        copyDiagnostics.setLetterSpacing(0.08f);
        copyDiagnostics.setPadding(dp(4), dp(8), dp(4), dp(4));
        copyDiagnostics.setFocusable(true);
        copyDiagnostics.setOnClickListener(v -> copyDiagnostics());
        dashboard.addView(copyDiagnostics, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return dashboard;
    }

    private LinearLayout buildTopActions() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        // Only shown once AppUpdater finds a newer stable release.
        updateAction = topAction("\u2191  UPDATE");
        updateAction.setTextColor(Color.rgb(36, 19, 56));
        GradientDrawable highlight = new GradientDrawable();
        highlight.setColor(YELLOW);
        highlight.setCornerRadius(dp(21));
        updateAction.setBackground(highlight);
        updateAction.setFocusable(true);
        updateAction.setVisibility(View.GONE);
        updateAction.setOnClickListener(v -> {
            if (updater != null) updater.promptUpdate();
        });
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        updateParams.setMargins(0, 0, dp(8), 0);
        actions.addView(updateAction, updateParams);

        TextView github = topAction("\u2605  GITHUB");
        github.setOnClickListener(v -> openExternal(
                "https://github.com/JestyLabs/Jesty-RP-Charging-Separation"));
        LinearLayout.LayoutParams githubParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        actions.addView(github, githubParams);

        TextView support = topAction("\u2615  SUPPORT");
        support.setOnClickListener(v -> openExternal("https://buymeacoffee.com/jesty"));
        LinearLayout.LayoutParams supportParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        supportParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(support, supportParams);
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

    private boolean backgroundRestricted() {
        ActivityManager activities = getSystemService(ActivityManager.class);
        return Build.VERSION.SDK_INT >= 28 && activities != null
                && activities.isBackgroundRestricted();
    }

    private void openAppInfo() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable error) {
            Toast.makeText(this, "Could not open App info", Toast.LENGTH_SHORT).show();
        }
    }

    /** Copies a privacy-safe report (no account or location data) for bug reports. */
    private void copyDiagnostics() {
        telemetryWorker.execute(() -> {
            String report = diagnosticsReport();
            runOnUiThread(() -> {
                ClipboardManager clipboard = getSystemService(ClipboardManager.class);
                if (clipboard == null) return;
                clipboard.setPrimaryClip(ClipData.newPlainText("Jesty RP diagnostics", report));
                Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private String diagnosticsReport() {
        StringBuilder out = new StringBuilder();
        out.append("Jesty RP Charging Separation ").append(versionLabel()).append('\n');
        out.append("Device: ").append(Build.MODEL).append('\n');
        out.append("Firmware: ").append(Build.DISPLAY).append('\n');
        out.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        out.append("Background restricted: ").append(backgroundRestricted()).append('\n');
        boolean auto = BypassService.autoLimitMode(prefs);
        out.append("Bypass on: ").append(prefs.getBoolean("desired_enabled", false))
                .append(", start on boot: ").append(prefs.getBoolean("auto_on_boot", false))
                .append('\n');
        out.append("When to stop: ").append(auto
                ? String.format(Locale.US, "at %d%%, charge again at %d%%",
                        BypassService.limitPercent(prefs), BypassService.resumePercent(prefs))
                : "right away").append('\n');
        out.append("Service: ").append(BypassService.state()).append(" - ")
                .append(BypassService.detail()).append('\n');
        try {
            PowerTelemetry t = PowerTelemetry.read();
            out.append(String.format(Locale.US,
                    "Battery: %d%% %s, %d uA, %.1f C; USB present %s, %s, %.1f W; "
                            + "limit %d/%d%n",
                    t.batteryPercent, t.batteryStatus, t.batteryCurrentUa, t.temperatureC,
                    t.usbPresent, t.usbType, t.usbWatts(), t.limit, t.limitMax));
        } catch (Throwable error) {
            out.append("Telemetry error: ").append(error.getMessage()).append('\n');
        }
        out.append("Recent events:\n");
        List<String> recent = BypassService.events(this).recent(80);
        if (recent.isEmpty()) out.append("(none)\n");
        for (String line : recent) out.append(line).append('\n');
        return out.toString();
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
            runOnUiThread(() -> {
                // If a previous process died after native separation was enabled,
                // an OFF preference must still restore normal charging on reopen.
                if (!prefs.getBoolean("desired_enabled", false) && telemetry.limit == 0) {
                    offReconcileStarted = false;
                }
                if (!prefs.getBoolean("desired_enabled", false)
                        && telemetry.limit != 0 && !offReconcileStarted
                        && (BypassService.state() == BypassController.State.OFF
                        || BypassService.state() == BypassController.State.FAILED)) {
                    offReconcileStarted = true;
                    Intent restore = new Intent(this, BypassService.class)
                            .setAction(BypassService.ACTION_DISABLE);
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(restore);
                    else startService(restore);
                }
                render(telemetry);
            });
        } catch (Throwable error) {
            runOnUiThread(() -> {
                stateText.setText("NO POWER READINGS");
                stateText.setTextColor(ERROR);
                stateDetail.setText(error.getMessage());
            });
        }
    }

    private void render(PowerTelemetry telemetry) {
        BypassController.State serviceState = BypassService.state();
        boolean desired = prefs.getBoolean("desired_enabled", false);
        boolean nativeSeparated = telemetry.separationConfirmed();
        boolean active = desired && serviceState == BypassController.State.ACTIVE
                && nativeSeparated;
        setSeparationVisual(active);
        suppressToggle = true;
        separationToggle.setChecked(desired);
        suppressToggle = false;
        setControlsEnabled(desired);

        String detail = BypassService.detail();
        String safetyStop = prefs.getString(BypassService.PREF_LAST_SAFETY_STOP, null);
        if (!desired && telemetry.limit != 0) {
            if (serviceState == BypassController.State.FAILED) {
                setState("COULD NOT START", ERROR);
            } else {
                setState("TURNING OFF...", YELLOW);
                detail = "Restoring normal charging";
            }
        } else if (!desired && safetyStop != null) {
            setState("COULD NOT START", ERROR);
            detail = safetyStop + ". Bypass was turned off for safety.";
        } else if (active) {
            setState("RUNNING FROM CHARGER", YELLOW);
        } else if (desired && nativeSeparated) {
            setState("STARTING...", YELLOW);
            detail = "Native bypass active; restoring monitoring";
        } else if (desired && serviceState == BypassController.State.CHARGING_TO_LIMIT) {
            setState(String.format(Locale.US, "CHARGING TO %d%%",
                    BypassService.limitPercent(prefs)), Color.WHITE);
        } else if (desired && serviceState == BypassController.State.ENABLING) {
            setState("STARTING...", YELLOW);
        } else if (desired && serviceState == BypassController.State.RETRYING
                && telemetry.usbPresent) {
            setState("TRYING AGAIN", YELLOW);
        } else if (desired && (serviceState == BypassController.State.ARMED
                || !telemetry.usbPresent)) {
            setState("READY", YELLOW);
            detail = "Plug in USB to start";
        } else if (desired && serviceState == BypassController.State.FAILED) {
            setState("COULD NOT START", ERROR);
        } else if (desired) {
            setState("STARTING...", YELLOW);
            detail = "Restoring bypass monitoring";
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
        restrictionWarning.setVisibility(desired && backgroundRestricted()
                ? View.VISIBLE : View.GONE);
        diagnostic.setText(Build.MODEL + "  ·  "
                + (!telemetry.usbPresent ? "On battery"
                : active ? "Not charging"
                : telemetry.batteryStatus));
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
        if (updater != null) updater.shutdown();
        super.onDestroy();
    }
}
