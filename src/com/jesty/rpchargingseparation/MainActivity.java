package com.jesty.rpchargingseparation;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
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

    private ScheduledExecutorService telemetryWorker;
    private ImageView backgroundImage;
    private boolean visualSeparated;
    private boolean suppressToggle;
    private Switch separationToggle;
    private Switch autoBootToggle;
    private RadioButton immediateMode;
    private RadioButton autoLimitMode;
    private LinearLayout autoLimitSettings;
    private TextView limitLabel;
    private TextView marginLabel;
    private SeekBar limitSeek;
    private SeekBar marginSeek;
    private TextView stateText;
    private TextView stateDetail;
    private TextView batteryValue;
    private TextView usbValue;
    private TextView deviceValue;
    private TextView tempValue;
    private TextView diagnostic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());

        boolean auto = getSharedPreferences("state", MODE_PRIVATE)
                .getBoolean("auto_on_boot", false);
        autoBootToggle.setChecked(auto);
        autoBootToggle.setOnCheckedChangeListener((button, checked) ->
                getSharedPreferences("state", MODE_PRIVATE).edit()
                        .putBoolean("auto_on_boot", checked).apply());
        bindModeSettings();

        separationToggle.setOnCheckedChangeListener((button, checked) -> {
            if (suppressToggle) return;
            getSharedPreferences("state", MODE_PRIVATE).edit()
                    .putBoolean("desired_enabled", checked).apply();
            Intent service = new Intent(this, BypassService.class)
                    .setAction(checked
                            ? BypassService.ACTION_ENABLE
                            : BypassService.ACTION_DISABLE);
            if (checked && Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            stateText.setText(checked ? "VALIDATING..." : "RESTORING...");
        });

        boolean desired = getSharedPreferences("state", MODE_PRIVATE)
                .getBoolean("desired_enabled", false);
        if (desired) {
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

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        int panelWidth = Math.round(getResources().getDisplayMetrics().widthPixels * 0.56f);
        root.addView(scroll, new FrameLayout.LayoutParams(
                panelWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));

        LinearLayout topActions = buildTopActions();
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42), Gravity.TOP | Gravity.END);
        actionParams.setMargins(0, dp(16), dp(18), 0);
        root.addView(topActions, actionParams);

        LinearLayout openSourceBadge = buildOpenSourceBadge();
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(50), Gravity.BOTTOM | Gravity.END);
        badgeParams.setMargins(0, 0, dp(18), dp(16));
        root.addView(openSourceBadge, badgeParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(24);
        content.setPadding(horizontal, dp(16), horizontal, dp(16));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(buildHeader());

        TextView subtitle = text("Retroid native charging separation", 13f, MUTED, false);
        subtitle.setPadding(0, dp(3), 0, dp(10));
        content.addView(subtitle);

        stateText = text("READING HARDWARE...", 19f, YELLOW, true);
        content.addView(stateText);
        stateDetail = text("", 13f, MUTED, false);
        stateDetail.setPadding(0, dp(2), 0, dp(10));
        content.addView(stateDetail);

        LinearLayout switchPanel = panel();
        separationToggle = new Switch(this);
        separationToggle.setText("Charging separation");
        separationToggle.setTextColor(Color.WHITE);
        separationToggle.setTextSize(18f);
        separationToggle.setTypeface(Typeface.DEFAULT_BOLD);
        applyBrandToggleColors(separationToggle);
        switchPanel.addView(separationToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        autoBootToggle = new Switch(this);
        autoBootToggle.setText("Restore after a normal reboot");
        autoBootToggle.setTextColor(MUTED);
        autoBootToggle.setTextSize(13f);
        applyBrandToggleColors(autoBootToggle);
        switchPanel.addView(autoBootToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        content.addView(switchPanel);
        content.addView(buildModePanel());

        LinearLayout row1 = row();
        batteryValue = addCard(row1, "BATTERY FLOW", "-");
        usbValue = addCard(row1, "USB INPUT", "-");
        content.addView(row1);

        LinearLayout row2 = row();
        deviceValue = addCard(row2, "DIRECT TO DEVICE", "-");
        tempValue = addCard(row2, "TEMPERATURE", "-");
        content.addView(row2);

        diagnostic = text("", 11f, MUTED, false);
        diagnostic.setPadding(dp(4), dp(8), 0, 0);
        content.addView(diagnostic);

        TextView legend = text(
                "V voltage  |  A current  |  W power  |  battery current: + charge / - discharge",
                10f, Color.rgb(151, 143, 170), false);
        legend.setPadding(dp(4), dp(4), 0, 0);
        content.addView(legend);
        return root;
    }

    private View buildModePanel() {
        LinearLayout modePanel = panel();
        modePanel.setPadding(dp(14), dp(8), dp(14), dp(8));

        TextView heading = text("SEPARATION MODE", 11f, MUTED, true);
        heading.setLetterSpacing(0.06f);
        modePanel.addView(heading);

        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.HORIZONTAL);
        immediateMode = modeOption("Immediate");
        autoLimitMode = modeOption("Automatic limit");
        modes.addView(immediateMode, new RadioGroup.LayoutParams(
                0, dp(40), 1f));
        modes.addView(autoLimitMode, new RadioGroup.LayoutParams(
                0, dp(40), 1f));
        modePanel.addView(modes);

        autoLimitSettings = new LinearLayout(this);
        autoLimitSettings.setOrientation(LinearLayout.VERTICAL);
        limitLabel = text("", 13f, Color.WHITE, false);
        limitLabel.setPadding(0, dp(4), 0, 0);
        autoLimitSettings.addView(limitLabel);
        limitSeek = new SeekBar(this);
        limitSeek.setMin(BypassService.MIN_LIMIT_PERCENT);
        limitSeek.setMax(BypassService.MAX_LIMIT_PERCENT);
        applyBrandSeekColors(limitSeek);
        autoLimitSettings.addView(limitSeek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        marginLabel = text("", 13f, Color.WHITE, false);
        autoLimitSettings.addView(marginLabel);
        marginSeek = new SeekBar(this);
        marginSeek.setMin(BypassService.MIN_RESUME_MARGIN);
        marginSeek.setMax(BypassService.MAX_RESUME_MARGIN);
        applyBrandSeekColors(marginSeek);
        autoLimitSettings.addView(marginSeek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));
        modePanel.addView(autoLimitSettings);
        return modePanel;
    }

    private RadioButton modeOption(String label) {
        RadioButton option = new RadioButton(this);
        option.setId(View.generateViewId());
        option.setText(label);
        option.setTextColor(Color.WHITE);
        option.setTextSize(14f);
        option.setButtonTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { YELLOW, Color.rgb(218, 211, 228) }));
        return option;
    }

    private void applyBrandSeekColors(SeekBar seek) {
        seek.setThumbTintList(ColorStateList.valueOf(YELLOW));
        seek.setProgressTintList(ColorStateList.valueOf(YELLOW));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(86, 78, 101)));
    }

    private void bindModeSettings() {
        SharedPreferences prefs = getSharedPreferences("state", MODE_PRIVATE);
        boolean auto = BypassService.autoLimitMode(prefs);
        immediateMode.setChecked(!auto);
        autoLimitMode.setChecked(auto);
        limitSeek.setProgress(BypassService.limitPercent(prefs));
        marginSeek.setProgress(BypassService.resumeMargin(prefs));
        renderModeSettings();

        immediateMode.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) return;
            prefs.edit().putString(BypassService.PREF_MODE,
                    BypassService.MODE_IMMEDIATE).apply();
            renderModeSettings();
        });
        autoLimitMode.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) return;
            prefs.edit().putString(BypassService.PREF_MODE,
                    BypassService.MODE_AUTO_LIMIT).apply();
            renderModeSettings();
        });
        limitSeek.setOnSeekBarChangeListener(seekListener(value ->
                prefs.edit().putInt(BypassService.PREF_LIMIT_PERCENT, value).apply()));
        marginSeek.setOnSeekBarChangeListener(seekListener(value ->
                prefs.edit().putInt(BypassService.PREF_RESUME_MARGIN, value).apply()));
    }

    private interface IntConsumer { void accept(int value); }

    private SeekBar.OnSeekBarChangeListener seekListener(IntConsumer save) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
                if (fromUser) save.accept(progress);
                renderModeSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seek) { }

            @Override
            public void onStopTrackingTouch(SeekBar seek) { }
        };
    }

    private void renderModeSettings() {
        boolean auto = autoLimitMode.isChecked();
        autoLimitSettings.setVisibility(auto ? View.VISIBLE : View.GONE);
        int limit = limitSeek.getProgress();
        int margin = marginSeek.getProgress();
        limitLabel.setText(String.format(Locale.US, "Separate at %d%%", limit));
        marginLabel.setText(String.format(Locale.US,
                "Resume charging at %d%% (-%d pts)", Math.max(0, limit - margin), margin));
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        ImageView lockup = new ImageView(this);
        lockup.setImageResource(R.drawable.jesty_rp_header_lockup);
        lockup.setScaleType(ImageView.ScaleType.FIT_START);
        lockup.setAdjustViewBounds(true);
        lockup.setContentDescription("Jesty RP Charging Separation");
        header.addView(lockup, new LinearLayout.LayoutParams(dp(300), dp(100)));

        header.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(102)));
        return header;
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

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(5), dp(14), dp(5));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xCF151021);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), 0x88B15BFF);
        panel.setBackground(background);
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

    private TextView addCard(LinearLayout row, String label, String initial) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(9), dp(13), dp(9));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xC0161024);
        background.setCornerRadius(dp(13));
        card.setBackground(background);

        TextView heading = text(label, 11f, MUTED, true);
        heading.setLetterSpacing(0.06f);
        card.addView(heading);
        TextView value = text(initial, 20f, Color.WHITE, true);
        value.setPadding(0, dp(3), 0, 0);
        card.addView(value);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(80), 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(card, params);
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
                stateText.setText("TELEMETRY UNAVAILABLE");
                stateText.setTextColor(Color.rgb(255, 106, 106));
                stateDetail.setText(error.getMessage());
            });
        }
    }

    private void render(PowerTelemetry telemetry) {
        BypassService.State serviceState = BypassService.state();
        boolean active = serviceState == BypassService.State.ACTIVE
                && telemetry.separationConfirmed();
        setSeparationVisual(active);
        boolean desired = getSharedPreferences("state", MODE_PRIVATE)
                .getBoolean("desired_enabled", false);
        suppressToggle = true;
        separationToggle.setChecked(desired);
        suppressToggle = false;

        if (active) {
            stateText.setText("CHARGING SEPARATED");
            stateText.setTextColor(YELLOW);
        } else if (serviceState == BypassService.State.CHARGING_TO_LIMIT) {
            stateText.setText(String.format(Locale.US, "CHARGING TO %d%%",
                    BypassService.limitPercent(getSharedPreferences("state", MODE_PRIVATE))));
            stateText.setTextColor(Color.WHITE);
        } else if (serviceState == BypassService.State.ENABLING) {
            stateText.setText("VALIDATING...");
            stateText.setTextColor(YELLOW);
        } else if (serviceState == BypassService.State.ARMED
                || (desired && !telemetry.usbPresent)) {
            stateText.setText("SEPARATION ARMED");
            stateText.setTextColor(YELLOW);
        } else if (serviceState == BypassService.State.FAILED) {
            stateText.setText("SEPARATION FAILED");
            stateText.setTextColor(Color.rgb(255, 106, 106));
        } else if (!telemetry.usbPresent) {
            stateText.setText("NO EXTERNAL POWER");
            stateText.setTextColor(MUTED);
        } else {
            stateText.setText("NORMAL CHARGING");
            stateText.setTextColor(Color.WHITE);
        }
        stateDetail.setText(BypassService.detail());

        batteryValue.setText(String.format(Locale.US, "%+.2f A | %+.1f W\nBattery %d%%",
                telemetry.displayedBatteryCurrentUa() / 1_000_000d,
                telemetry.displayedBatteryWatts(), telemetry.batteryPercent));
        usbValue.setText(String.format(Locale.US, "%.2f V | %.2f A\n%.1f W",
                telemetry.usbVoltageUv / 1_000_000d,
                telemetry.usbCurrentUa / 1_000_000d, telemetry.usbWatts()));
        deviceValue.setText(String.format(Locale.US, "~ %.1f W",
                telemetry.estimatedDeviceWatts()));
        tempValue.setText(String.format(Locale.US, "%.1f C", telemetry.temperatureC));
        diagnostic.setText(String.format(Locale.US,
                "%s | %s%s | %s | limit %d/%d | counter %d uAh",
                Build.MODEL, telemetry.usbType, telemetry.pdActive ? " / PD" : "",
                telemetry.batteryStatus, telemetry.limit, telemetry.limitMax,
                telemetry.chargeCounterUah));
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
