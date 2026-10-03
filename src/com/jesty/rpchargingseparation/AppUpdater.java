package com.jesty.rpchargingseparation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks this repository's latest stable GitHub release, offers it to the user, downloads the
 * APK, verifies it, and hands it to Android's PackageInstaller. Android still shows its own
 * confirmation and refuses an APK that is not signed with the installed app's certificate.
 */
final class AppUpdater {
    interface Listener {
        void onUpdateAvailable(String version);
    }

    static final String REPOSITORY = "JestyLabs/Jesty-RP-Charging-Separation";
    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
    private static final String TAG = "JestyRPUpdate";
    private static final String PREFS = "updates";
    private static final String PREF_LAST_CHECK = "last_check";
    private static final String PREF_RELEASE_JSON = "release_json";
    private static final String PREF_PROMPTED_VERSION = "prompted_version";
    private static final long CHECK_INTERVAL_MS = 60L * 60L * 1000L;
    private static final int MAX_RELEASE_JSON_BYTES = 1024 * 1024;
    private static final long MAX_APK_BYTES = 64L * 1024L * 1024L;

    private static final class Release {
        final String tag;
        final String apkUrl;
        final long apkSize;
        final String sha256;

        Release(String tag, String apkUrl, long apkSize, String sha256) {
            this.tag = tag;
            this.apkUrl = apkUrl;
            this.apkSize = apkSize;
            this.sha256 = sha256;
        }

        String version() {
            return tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        }
    }

    private final Activity activity;
    private final Listener listener;
    private final SharedPreferences prefs;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private Release available;
    private Release awaitingInstallPermission;

    AppUpdater(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Shows a previously found update immediately, then refreshes at most once an hour. */
    void start() {
        Release cached = parseRelease(prefs.getString(PREF_RELEASE_JSON, null));
        if (cached != null && UpdateVersion.isNewer(cached.tag, installedVersion())) {
            publish(cached, false);
        }
        long last = prefs.getLong(PREF_LAST_CHECK, 0L);
        long now = System.currentTimeMillis();
        if (now - last < CHECK_INTERVAL_MS && now >= last) return;
        worker.execute(() -> {
            try {
                String json = fetchLatestReleaseJson();
                Release release = parseRelease(json);
                prefs.edit()
                        .putLong(PREF_LAST_CHECK, System.currentTimeMillis())
                        .putString(PREF_RELEASE_JSON, release == null ? null : json)
                        .apply();
                if (release != null && UpdateVersion.isNewer(release.tag, installedVersion())) {
                    onUi(() -> publish(release, true));
                }
            } catch (Throwable error) {
                Log.w(TAG, "Update check failed", error);
            }
        });
    }

    /** Called from the top-bar UPDATE button. */
    void promptUpdate() {
        if (available != null) showUpdateDialog(available);
    }

    void onResume() {
        Release release = awaitingInstallPermission;
        if (release == null) return;
        awaitingInstallPermission = null;
        if (activity.getPackageManager().canRequestPackageInstalls()) {
            download(release);
        } else {
            Toast.makeText(activity, "Update needs permission to install apps",
                    Toast.LENGTH_LONG).show();
        }
    }

    void shutdown() {
        cancelled.set(true);
        worker.shutdownNow();
    }

    private void publish(Release release, boolean promptIfNew) {
        available = release;
        listener.onUpdateAvailable(release.version());
        if (promptIfNew && !release.tag.equals(prefs.getString(PREF_PROMPTED_VERSION, null))) {
            prefs.edit().putString(PREF_PROMPTED_VERSION, release.tag).apply();
            showUpdateDialog(release);
        }
    }

    private void showUpdateDialog(Release release) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        String message = String.format(Locale.US,
                "v%s is ready. You're on v%s.\n\nYour settings will be kept.",
                release.version(), installedVersion());
        new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Update available")
                .setMessage(message)
                .setPositiveButton("Update", (dialog, which) -> startUpdate(release))
                .setNegativeButton("Later", null)
                .show();
    }

    private void startUpdate(Release release) {
        if (activity.getPackageManager().canRequestPackageInstalls()) {
            download(release);
            return;
        }
        new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Allow updates")
                .setMessage("To install the update, allow this app to install apps:"
                        + " turn on \"Allow from this source\", then come back.")
                .setPositiveButton("Open settings", (dialog, which) -> {
                    awaitingInstallPermission = release;
                    try {
                        activity.startActivity(new Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName())));
                    } catch (Throwable error) {
                        awaitingInstallPermission = null;
                        Toast.makeText(activity, "Could not open settings",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void download(Release release) {
        if (!busy.compareAndSet(false, true)) return;
        cancelled.set(false);

        int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(padding, padding / 2, padding, 0);
        TextView label = new TextView(activity);
        label.setText("Downloading v" + release.version() + "...");
        box.addView(label);
        ProgressBar bar = new ProgressBar(activity, null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setIndeterminate(release.apkSize <= 0);
        box.addView(bar);
        AlertDialog progress = new AlertDialog.Builder(activity,
                android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Updating")
                .setView(box)
                .setCancelable(false)
                .setNegativeButton("Cancel", (dialog, which) -> cancelled.set(true))
                .show();

        worker.execute(() -> {
            try {
                File apk = downloadVerified(release, percent -> onUi(() -> bar.setProgress(percent)));
                onUi(() -> label.setText("Checking the update..."));
                verifyArchive(apk);
                install(apk);
                onUi(() -> {
                    dismiss(progress);
                    Toast.makeText(activity, "Confirm the update to finish",
                            Toast.LENGTH_LONG).show();
                });
            } catch (Throwable error) {
                Log.w(TAG, "Update failed", error);
                clearDownloads(activity);
                String reason = cancelled.get() ? "Update cancelled"
                        : "Update failed: " + (error.getMessage() == null
                                ? error.getClass().getSimpleName() : error.getMessage());
                onUi(() -> {
                    dismiss(progress);
                    Toast.makeText(activity, reason, Toast.LENGTH_LONG).show();
                });
            } finally {
                busy.set(false);
            }
        });
    }

    private interface Progress {
        void update(int percent);
    }

    private File downloadVerified(Release release, Progress progress) throws Exception {
        if (!UpdateVersion.isTrustedDownloadUrl(release.apkUrl, REPOSITORY, release.tag)) {
            throw new IOException("unexpected download address");
        }
        clearDownloads(activity);
        File directory = downloadDirectory(activity);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("cannot create download folder");
        }
        File apk = new File(directory, "update.apk");

        HttpURLConnection connection = open(release.apkUrl, "application/octet-stream");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        long total = 0L;
        int lastPercent = -1;
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            if (!connection.getURL().getProtocol().equals("https")) {
                throw new IOException("download was redirected away from HTTPS");
            }
            long length = connection.getContentLengthLong();
            if (length > MAX_APK_BYTES) throw new IOException("download is too large");
            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(apk)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                        throw new IOException("cancelled");
                    }
                    total += read;
                    if (total > MAX_APK_BYTES) throw new IOException("download is too large");
                    sha256.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                    if (release.apkSize > 0) {
                        int percent = (int) Math.min(100L, total * 100L / release.apkSize);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            progress.update(percent);
                        }
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        if (release.apkSize > 0 && total != release.apkSize) {
            throw new IOException("download is incomplete");
        }
        if (!release.sha256.equals(hex(sha256.digest()))) {
            throw new IOException("download checksum does not match the release");
        }
        return apk;
    }

    private void verifyArchive(File apk) throws Exception {
        PackageManager packages = activity.getPackageManager();
        String packageName = activity.getPackageName();
        PackageInfo archive = packages.getPackageArchiveInfo(apk.getAbsolutePath(),
                PackageManager.GET_SIGNING_CERTIFICATES);
        if (archive == null) throw new IOException("downloaded file is not a valid app");
        if (!packageName.equals(archive.packageName)) {
            throw new IOException("downloaded app has a different package name");
        }
        PackageInfo installed = packages.getPackageInfo(packageName, 0);
        if (archive.getLongVersionCode() <= installed.getLongVersionCode()) {
            throw new IOException("downloaded app is not newer than the installed one");
        }
        // PackageInstaller enforces the signing certificate anyway; this gives a clearer error.
        SigningInfo signing = archive.signingInfo;
        if (signing == null) throw new IOException("downloaded app has no signing information");
        Signature[] signers = signing.hasMultipleSigners()
                ? signing.getApkContentsSigners()
                : signing.getSigningCertificateHistory();
        boolean trusted = false;
        if (signers != null) {
            for (Signature signer : signers) {
                if (packages.hasSigningCertificate(packageName, signer.toByteArray(),
                        PackageManager.CERT_INPUT_RAW_X509)) {
                    trusted = true;
                    break;
                }
            }
        }
        if (!trusted) throw new IOException("downloaded app is not signed by Jesty");
    }

    private void install(File apk) throws Exception {
        PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(activity.getPackageName());
        params.setSize(apk.length());
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
        }
        int sessionId = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (InputStream input = new FileInputStream(apk);
                 OutputStream output = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
                session.fsync(output);
            }
            Intent status = new Intent(activity, UpdateInstallReceiver.class)
                    .setAction(UpdateInstallReceiver.ACTION_STATUS);
            // PackageInstaller adds status extras to this callback.
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(activity, sessionId, status,
                    flags);
            session.commit(pending.getIntentSender());
        } catch (Exception error) {
            installer.abandonSession(sessionId);
            throw error;
        }
    }

    private String fetchLatestReleaseJson() throws Exception {
        HttpURLConnection connection = open(LATEST_RELEASE_URL, "application/vnd.github+json");
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code);
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    body.write(buffer, 0, read);
                    if (body.size() > MAX_RELEASE_JSON_BYTES) {
                        throw new IOException("release response is too large");
                    }
                }
                return new String(body.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static Release parseRelease(String json) {
        if (json == null) return null;
        try {
            JSONObject release = new JSONObject(json);
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null;
            String tag = release.optString("tag_name", "");
            if (UpdateVersion.parse(tag) == null) return null;
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) return null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null
                        || !UpdateVersion.isInstallableApkName(asset.optString("name"))) {
                    continue;
                }
                String url = asset.optString("browser_download_url", "");
                String sha256 = UpdateVersion.sha256FromDigest(
                        asset.isNull("digest") ? null : asset.optString("digest"));
                long size = asset.optLong("size", 0L);
                if (sha256 == null
                        || size <= 0 || size > MAX_APK_BYTES
                        || !UpdateVersion.isTrustedDownloadUrl(url, REPOSITORY, tag)) {
                    continue;
                }
                return new Release(tag, url, size, sha256);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Unreadable release information", error);
        }
        return null;
    }

    private static HttpURLConnection open(String url, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", accept);
        connection.setRequestProperty("User-Agent", "Jesty-RP-Charging-Separation-Updater");
        return connection;
    }

    private String installedVersion() {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Throwable error) {
            return "0";
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) value.append(String.format(Locale.US, "%02x", b & 0xff));
        return value.toString();
    }

    private static File downloadDirectory(Context context) {
        return new File(context.getCacheDir(), "updates");
    }

    static void clearDownloads(Context context) {
        File[] files = downloadDirectory(context).listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.delete()) Log.w(TAG, "Could not delete " + file);
        }
    }

    private void onUi(Runnable action) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) action.run();
        });
    }

    private static void dismiss(AlertDialog dialog) {
        try {
            if (dialog.isShowing()) dialog.dismiss();
        } catch (Throwable ignored) {
            // The Activity may already be gone.
        }
    }
}
