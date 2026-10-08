package com.jesty.rpchargingseparation;

public final class RetroidProcessProtectionTest {
    private static final String PACKAGE = "com.jesty.rpchargingseparation";

    public static void main(String[] args) {
        readOnlyProbeUsesOneFixedCommand();
        exactWhitelistMembership();
        similarPackageDoesNotMatch();
        unknownKeyContainingPackageIsSurfaced();
        unrelatedSettingsAreFiltered();
        bridgeFailureStaysUnknown();
        System.out.println("RetroidProcessProtectionTest passed");
    }

    private static void readOnlyProbeUsesOneFixedCommand() {
        RecordingShell shell = new RecordingShell("app_whiteList=" + PACKAGE + "\n");
        RetroidProcessProtection.Snapshot snapshot =
                RetroidProcessProtection.inspect(shell, PACKAGE);

        require(snapshot.readSucceeded, "probe should succeed");
        require(shell.calls == 1, "probe must issue exactly one command");
        require(RetroidProcessProtection.SYSTEM_SETTINGS_COMMAND.equals(shell.lastCommand),
                "probe must only list system settings");
        require(!shell.lastCommand.contains(" put "),
                "read-only probe must never write settings");
    }

    private static void exactWhitelistMembership() {
        String settings = "app_whiteList=com.example.one," + PACKAGE + ",com.example.two\n";
        RetroidProcessProtection.Snapshot snapshot =
                RetroidProcessProtection.inspect(new RecordingShell(settings), PACKAGE);

        require(snapshot.knownWhitelistKeyPresent, "candidate key should be detected");
        require(snapshot.knownWhitelistMembership
                        == RetroidProcessProtection.KnownWhitelistMembership.PRESENT,
                "exact package token should be present");
    }

    private static void similarPackageDoesNotMatch() {
        String settings = "app_whiteList=" + PACKAGE + ".debug,com.example.other\n";
        RetroidProcessProtection.Snapshot snapshot =
                RetroidProcessProtection.inspect(new RecordingShell(settings), PACKAGE);

        require(snapshot.knownWhitelistMembership
                        == RetroidProcessProtection.KnownWhitelistMembership.ABSENT,
                "similar package name must not count as membership");
        require(!RetroidProcessProtection.valueContainsPackage(
                        PACKAGE + ".debug", PACKAGE),
                "package-boundary matching must reject package suffixes");
    }

    private static void unknownKeyContainingPackageIsSurfaced() {
        String settings = "opaque_vendor_bucket=foo;" + PACKAGE + ";bar\n"
                + "screen_brightness=100\n";
        RetroidProcessProtection.Snapshot snapshot =
                RetroidProcessProtection.inspect(new RecordingShell(settings), PACKAGE);

        require(snapshot.relevantSettings.size() == 1,
                "unknown key containing our exact package should be surfaced");
        require("opaque_vendor_bucket".equals(snapshot.relevantSettings.get(0).key),
                "unexpected relevant key");
        require(!snapshot.knownWhitelistKeyPresent,
                "Odin candidate must stay unknown when key is absent");
    }

    private static void unrelatedSettingsAreFiltered() {
        String settings = "screen_brightness=100\n"
                + "volume_music=7\n"
                + "clean_process_on_standby=1\n";
        RetroidProcessProtection.Snapshot snapshot =
                RetroidProcessProtection.inspect(new RecordingShell(settings), PACKAGE);

        require(snapshot.relevantSettings.size() == 1,
                "only vendor-protection-looking settings should remain");
        require("clean_process_on_standby".equals(snapshot.relevantSettings.get(0).key),
                "clean-process candidate should be retained");
    }

    private static void bridgeFailureStaysUnknown() {
        RetroidProcessProtection.Snapshot snapshot =
                RetroidProcessProtection.inspect(command -> {
                    throw new IllegalStateException("PServerBinder unavailable");
                }, PACKAGE);

        require(!snapshot.readSucceeded, "bridge failure should be recorded");
        require(snapshot.knownWhitelistMembership
                        == RetroidProcessProtection.KnownWhitelistMembership.UNKNOWN,
                "bridge failure must not be reported as unprotected");
        require(snapshot.relevantSettings.isEmpty(),
                "failed read must not manufacture settings");
    }

    private static final class RecordingShell implements RetroidProcessProtection.Shell {
        final String result;
        int calls;
        String lastCommand;

        RecordingShell(String result) {
            this.result = result;
        }

        @Override
        public String exec(String command) {
            calls++;
            lastCommand = command;
            return result;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
