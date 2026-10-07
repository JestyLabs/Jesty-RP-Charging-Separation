package com.jesty.rpchargingseparation;

/** Builds a harmless shell watchdog that writes only a research sentinel marker. */
final class RestoreWatchdogSentinelScript {
    static final String PREFIX = "/data/local/tmp/jesty-rp-watchdog-sentinel";
    static final String SCRIPT_PATH = PREFIX + ".sh";
    static final String OUTPUT_PATH = PREFIX + ".out";
    static final int MIN_LEASE_SECONDS = 5;
    static final int MAX_LEASE_SECONDS = 120;

    private RestoreWatchdogSentinelScript() {}

    static int clampLeaseSeconds(int seconds) {
        return Math.max(MIN_LEASE_SECONDS, Math.min(MAX_LEASE_SECONDS, seconds));
    }

    /** PServer gets an operator-free command; the script performs its own bounded detach. */
    static String launchCommand() {
        return "sh " + SCRIPT_PATH;
    }

    static String statusCommand() {
        return "cat " + OUTPUT_PATH + " 2>/dev/null";
    }

    static String cleanCommand() {
        return "rm -f " + SCRIPT_PATH + " " + OUTPUT_PATH;
    }

    static String build(int ownerPid, long ownerStartTicks, int requestedLeaseSeconds) {
        if (ownerPid <= 1) throw new IllegalArgumentException("invalid owner pid");
        if (ownerStartTicks <= 0L) throw new IllegalArgumentException("invalid owner starttime");
        int lease = clampLeaseSeconds(requestedLeaseSeconds);
        return "#!/system/bin/sh\n"
                + "PID=" + ownerPid + "\n"
                + "START=" + ownerStartTicks + "\n"
                + "OUT=" + OUTPUT_PATH + "\n"
                + "if [ \"$1\" != worker ]; then\n"
                + "  rm -f \"$OUT\"\n"
                + "  nohup sh \"$0\" worker </dev/null >/dev/null 2>&1 &\n"
                + "  P=$!\n"
                + "  sleep 1\n"
                + "  kill -0 \"$P\" 2>/dev/null || exit 21\n"
                + "  exit 0\n"
                + "fi\n"
                + "END=$(( $(date +%s) + " + lease + " ))\n"
                + "while [ $(date +%s) -lt $END ]; do\n"
                + "  S=$(cat /proc/$PID/stat 2>/dev/null) || { echo 'RESTORE_REQUIRED owner_missing' > \"$OUT\"; exit 0; }\n"
                + "  R=${S##*) }\n"
                + "  set -- $R\n"
                + "  CUR=${20}\n"
                + "  [ \"$CUR\" = \"$START\" ] || { echo 'RESTORE_REQUIRED owner_mismatch' > \"$OUT\"; exit 0; }\n"
                + "  sleep 1\n"
                + "done\n"
                + "echo 'RESTORE_REQUIRED lease_expired' > \"$OUT\"\n"
                + "exit 0\n";
    }
}
