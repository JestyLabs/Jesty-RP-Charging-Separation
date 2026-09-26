package com.jesty.rpchargingseparation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences("state", Context.MODE_PRIVATE);
        String action = intent == null ? "" : intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);
        if (boot && !prefs.getBoolean("auto_on_boot", false)) {
            prefs.edit().putBoolean("desired_enabled", false).apply();
            return;
        }
        if (!prefs.getBoolean("desired_enabled", false)) return;

        try {
            if (PowerTelemetry.read().usbPresent) {
                Intent service = new Intent(context, BypassService.class)
                        .setAction(BypassService.ACTION_ENABLE);
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
                else context.startService(service);
            }
        } catch (Throwable ignored) {
            // Fail closed: normal charging remains enabled.
        }
    }
}
