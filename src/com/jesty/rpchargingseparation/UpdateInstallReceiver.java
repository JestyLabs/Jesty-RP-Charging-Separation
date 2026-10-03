package com.jesty.rpchargingseparation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;
import android.widget.Toast;

/** Receives PackageInstaller results for in-app updates. */
public final class UpdateInstallReceiver extends BroadcastReceiver {
    static final String ACTION_STATUS = "com.jesty.rpchargingseparation.UPDATE_INSTALL_STATUS";
    private static final String TAG = "JestyRPUpdate";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_STATUS.equals(intent.getAction())) return;
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm == null) break;
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirm);
                } catch (Throwable error) {
                    Log.w(TAG, "Could not show the install confirmation", error);
                    AppUpdater.clearDownloads(context);
                    Toast.makeText(context, "Update failed: could not ask for confirmation",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PackageInstaller.STATUS_SUCCESS:
                AppUpdater.clearDownloads(context);
                break;
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                AppUpdater.clearDownloads(context);
                Toast.makeText(context, "Update cancelled", Toast.LENGTH_SHORT).show();
                break;
            default: {
                AppUpdater.clearDownloads(context);
                String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Log.w(TAG, "Update install failed: " + status + " " + message);
                Toast.makeText(context, "Update failed"
                        + (message == null ? "" : ": " + message), Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
}
