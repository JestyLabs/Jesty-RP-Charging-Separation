package com.jesty.rpchargingseparation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

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
        switchPanel.addView(separationToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        autoBootToggle = new Switch(this);
        autoBootToggle.setText("Restore after a normal reboot");
        autoBootToggle.setTextColor(MUTED);
        autoBootToggle.setTextSize(13f);
        switchPanel.addView(autoBootToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        content.addView(switchPanel);

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

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView wordmark = new ImageView(this);
        wordmark.setImageResource(R.drawable.jesty_rp_wordmark);
        wordmark.setScaleType(ImageView.ScaleType.FIT_START);
        wordmark.setAdjustViewBounds(true);
        wordmark.setContentDescription("Jesty");
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(126), dp(62));
        logoParams.setMargins(0, 0, dp(12), 0);
        header.addView(wordmark, logoParams);

        LinearLayout name = new LinearLayout(this);
        name.setOrientation(LinearLayout.VERTICAL);
        TextView rp = text("RP CHARGING", 16f, YELLOW, true);
        rp.setLetterSpacing(0.07f);
        name.addView(rp);
        TextView separation = text("SEPARATION", 16f, PURPLE, true);
        separation.setLetterSpacing(0.08f);
        name.addView(separation);
        header.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

        header.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(64)));
        return header;
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
                "%s%s | %s | limit %d/%d | counter %d uAh",
                telemetry.usbType, telemetry.pdActive ? " / PD" : "",
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
