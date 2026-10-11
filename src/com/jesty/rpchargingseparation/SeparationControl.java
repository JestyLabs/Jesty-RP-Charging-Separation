package com.jesty.rpchargingseparation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Map;

/** Shared user-choice path for the dashboard and Quick Settings. */
final class SeparationControl {
    static final String PREF_START_ERROR = "control_start_error";

    static boolean setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences("state", Context.MODE_PRIVATE);
        Map<String, ?> previous = prefs.getAll();
        SharedPreferences.Editor edit = prefs.edit()
                .putBoolean(BypassService.PREF_DESIRED, enabled)
                .remove(BypassService.PREF_LAST_SAFETY_STOP)
                .remove(PREF_START_ERROR);
        if (!enabled) edit.putBoolean("auto_on_boot", false);
        if (!edit.commit()) {
            // Android updates in-memory preferences even when the disk commit fails.
            // Roll back the in-memory choice and do not dispatch a service request.
            SharedPreferences.Editor rollback = prefs.edit();
            for (String key : new String[]{BypassService.PREF_DESIRED, "auto_on_boot",
                    BypassService.PREF_LAST_SAFETY_STOP, PREF_START_ERROR}) {
                Object value = previous.get(key);
                if (value instanceof Boolean) rollback.putBoolean(key, (Boolean) value);
                else if (value instanceof String) rollback.putString(key, (String) value);
                else rollback.remove(key);
            }
            rollback.apply();
            return false;
        }
        Intent service = new Intent(context, BypassService.class).setAction(enabled
                ? BypassService.ACTION_ENABLE : BypassService.ACTION_DISABLE);
        try {
            // OFF may be requested from a background tile too. The existing service
            // enters foreground before restoring, so both actions use this entry point.
            context.startForegroundService(service);
            return true;
        } catch (RuntimeException unavailable) {
            // Retain the user's choice, but never report hardware success.
            prefs.edit().putBoolean(PREF_START_ERROR, true).apply();
            return false;
        }
    }
}
