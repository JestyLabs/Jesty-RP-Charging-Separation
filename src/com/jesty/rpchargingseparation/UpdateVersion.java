package com.jesty.rpchargingseparation;

import java.util.Locale;

/** Pure release-version and download checks, kept free of Android APIs for unit testing. */
final class UpdateVersion {
    private UpdateVersion() { }

    /** Returns the numeric parts of "v1.5.4" / "1.5.4-rc1", or null when not a version. */
    static int[] parse(String version) {
        if (version == null) return null;
        String core = version.trim();
        if (core.startsWith("v") || core.startsWith("V")) core = core.substring(1);
        int suffix = indexOfAny(core, '-', '+');
        if (suffix >= 0) core = core.substring(0, suffix);
        if (core.isEmpty()) return null;
        String[] parts = core.split("\\.", -1);
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty() || parts[i].length() > 6) return null;
            for (int c = 0; c < parts[i].length(); c++) {
                if (!Character.isDigit(parts[i].charAt(c))) return null;
            }
            numbers[i] = Integer.parseInt(parts[i]);
        }
        return numbers;
    }

    static boolean isPrerelease(String version) {
        return version != null && version.indexOf('-') >= 0;
    }

    /** Negative, zero, or positive like Comparator; unparseable versions sort lowest. */
    static int compare(String left, String right) {
        int[] a = parse(left);
        int[] b = parse(right);
        if (a == null || b == null) return (a == null ? 0 : 1) - (b == null ? 0 : 1);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return x < y ? -1 : 1;
        }
        boolean preA = isPrerelease(left);
        boolean preB = isPrerelease(right);
        if (preA == preB) return 0;
        return preA ? -1 : 1;
    }

    static boolean isNewer(String candidate, String installed) {
        return parse(candidate) != null && compare(candidate, installed) > 0;
    }

    /** Converts GitHub's "sha256:&lt;hex&gt;" asset digest to lowercase hex, or null. */
    static String sha256FromDigest(String digest) {
        if (digest == null || !digest.regionMatches(true, 0, "sha256:", 0, 7)) return null;
        String hex = digest.substring(7).trim().toLowerCase(Locale.US);
        if (hex.length() != 64) return null;
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return null;
        }
        return hex;
    }

    /** Only release assets from this repository's own tag are downloaded. */
    static boolean isTrustedDownloadUrl(String url, String repository, String tag) {
        if (url == null || tag == null || tag.isEmpty()) return false;
        String prefix = "https://github.com/" + repository + "/releases/download/" + tag + "/";
        if (!url.startsWith(prefix)) return false;
        String file = url.substring(prefix.length());
        return !file.isEmpty() && file.indexOf('/') < 0 && file.indexOf('?') < 0
                && file.indexOf('#') < 0 && !file.contains("..");
    }

    static boolean isInstallableApkName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.US);
        return lower.endsWith(".apk") && !lower.contains("unsigned");
    }

    private static int indexOfAny(String value, char first, char second) {
        int a = value.indexOf(first);
        int b = value.indexOf(second);
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }
}
