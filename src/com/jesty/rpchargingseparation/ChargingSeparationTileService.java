package com.jesty.rpchargingseparation;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** Quick Settings control; all hardware work stays in the existing charging service. */
public final class ChargingSeparationTileService extends TileService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean listening;
    private SharedPreferences prefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener changes =
            (settings, key) -> handler.post(this::render);
    private final Runnable refresh = new Runnable() {
        public void run() {
            if (!listening) return;
            render();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onStartListening() {
        super.onStartListening();
        if (listening) return;
        prefs = getSharedPreferences("state", MODE_PRIVATE);
        listening = true;
        prefs.registerOnSharedPreferenceChangeListener(changes);
        refresh.run();
    }

    @Override public void onStopListening() {
        listening = false;
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(changes);
        handler.removeCallbacksAndMessages(null);
        super.onStopListening();
    }

    @Override public void onDestroy() {
        onStopListening();
        super.onDestroy();
    }

    @Override public void onClick() {
        super.onClick();
        if (isLocked()) unlockAndRun(this::toggle);
        else toggle();
    }

    private void toggle() {
        SharedPreferences settings = getSharedPreferences("state", MODE_PRIVATE);
        boolean enabled = settings.getBoolean(BypassService.PREF_DESIRED, false);
        if (!SeparationControl.setEnabled(this, !enabled)) {
            Toast.makeText(this, "Could not apply setting. Open the app to check charging.",
                    Toast.LENGTH_LONG).show();
        }
        render();
    }

    private void render() {
        Tile tile = getQsTile();
        if (tile == null) return;
        SharedPreferences settings = getSharedPreferences("state", MODE_PRIVATE);
        boolean enabled = settings.getBoolean(BypassService.PREF_DESIRED, false);
        boolean failed = settings.getBoolean(SeparationControl.PREF_START_ERROR, false);
        tile.setLabel("Charging Separation");
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= 29) tile.setSubtitle(TileStatus.subtitle(enabled, failed,
                BypassService.state()));
        tile.updateTile();
    }
}
