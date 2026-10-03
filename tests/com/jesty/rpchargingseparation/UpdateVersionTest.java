package com.jesty.rpchargingseparation;

public final class UpdateVersionTest {
    private static final String REPO = "JestyLabs/Jesty-RP-Charging-Separation";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(UpdateVersion.isNewer("v1.5.5", "1.5.4"), "patch bump");
        check(UpdateVersion.isNewer("v1.6.0", "1.5.10"), "minor bump");
        check(UpdateVersion.isNewer("v1.10.0", "1.9.9"), "numeric, not lexical");
        check(UpdateVersion.isNewer("v2", "1.9.9"), "short version");
        check(!UpdateVersion.isNewer("v1.5.4", "1.5.4"), "same version");
        check(!UpdateVersion.isNewer("v1.5.4.0", "1.5.4"), "trailing zero");
        check(!UpdateVersion.isNewer("v1.5.3", "1.5.4"), "older version");
        check(!UpdateVersion.isNewer("v1.5.4-rc1", "1.5.4"), "rc before stable");
        check(UpdateVersion.isNewer("v1.5.4", "1.5.4-rc1"), "stable after rc");
        check(UpdateVersion.isNewer("v1.5.5-rc1", "1.5.4"), "rc of next version");
        check(!UpdateVersion.isNewer("latest", "1.5.4"), "non-version tag");
        check(!UpdateVersion.isNewer("v1..5", "1.5.4"), "empty part");
        check(!UpdateVersion.isNewer(null, "1.5.4"), "null tag");

        String hex = "00006beb9c938ce3aadb77bcebc1c15b3c6b977dc5f9f974e668f11a258b8f45";
        check(hex.equals(UpdateVersion.sha256FromDigest("sha256:" + hex)), "digest");
        check(hex.equals(UpdateVersion.sha256FromDigest("SHA256:" + hex.toUpperCase())),
                "digest case");
        check(UpdateVersion.sha256FromDigest("sha1:" + hex) == null, "wrong algorithm");
        check(UpdateVersion.sha256FromDigest("sha256:abc") == null, "short digest");
        check(UpdateVersion.sha256FromDigest(null) == null, "missing digest");
        check(UpdateVersion.sha256FromDigest("null") == null, "JSON null digest");

        String base = "https://github.com/" + REPO + "/releases/download/v1.5.5/";
        check(UpdateVersion.isTrustedDownloadUrl(base + "App-1.5.5.apk", REPO, "v1.5.5"),
                "own release asset");
        check(!UpdateVersion.isTrustedDownloadUrl(base + "App.apk", REPO, "v1.5.4"),
                "other tag");
        check(!UpdateVersion.isTrustedDownloadUrl(base + "../x/App.apk", REPO, "v1.5.5"),
                "path traversal");
        check(!UpdateVersion.isTrustedDownloadUrl(
                "https://github.com/evil/repo/releases/download/v1.5.5/App.apk",
                REPO, "v1.5.5"), "other repository");
        check(!UpdateVersion.isTrustedDownloadUrl(
                "http://github.com/" + REPO + "/releases/download/v1.5.5/App.apk",
                REPO, "v1.5.5"), "plain http");

        check(UpdateVersion.isInstallableApkName("Jesty-RP-Charging-Separation-1.5.5.apk"),
                "signed apk");
        check(!UpdateVersion.isInstallableApkName("App-1.5.5-unsigned.apk"), "unsigned apk");
        check(!UpdateVersion.isInstallableApkName("SHA256SUMS.txt"), "checksum file");

        System.out.println("UpdateVersion tests passed");
    }
}
