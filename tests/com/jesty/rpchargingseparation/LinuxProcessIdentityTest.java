package com.jesty.rpchargingseparation;

public final class LinuxProcessIdentityTest {
    private static int passed;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static String stat(int pid, String comm, int ppid, int pgrp, int session, long start) {
        // Fields 3..22. Values between session (field 6) and starttime (field 22) are fillers.
        return pid + " (" + comm + ") S " + ppid + " " + pgrp + " " + session
                + " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " + start;
    }

    private static void parsesIdentityFields() {
        LinuxProcessIdentity id = LinuxProcessIdentity.parse(stat(321, "app_process", 1, 321, 321, 987654L));
        check(id.pid == 321, "pid");
        check(id.ppid == 1, "ppid");
        check(id.processGroup == 321, "pgrp");
        check(id.session == 321, "session");
        check(id.startTimeTicks == 987654L, "starttime");
        passed++;
    }

    private static void commMayContainSpacesAndParentheses() {
        LinuxProcessIdentity id = LinuxProcessIdentity.parse(
                stat(77, "odd name) with (parens", 12, 13, 14, 444L));
        check(id.pid == 77 && id.startTimeTicks == 444L, "comm parsing");
        passed++;
    }

    private static void pidReuseIsRejected() {
        LinuxProcessIdentity first = LinuxProcessIdentity.parse(stat(42, "owner", 1, 42, 42, 100L));
        LinuxProcessIdentity reused = LinuxProcessIdentity.parse(stat(42, "other", 1, 42, 42, 900L));
        check(first.sameProcess(42, 100L), "original identity accepted");
        check(!reused.sameProcess(42, 100L), "reused pid must not match owner identity");
        passed++;
    }

    private static void malformedInputFailsClosed() {
        try {
            LinuxProcessIdentity.parse("42 broken");
            throw new AssertionError("malformed stat accepted");
        } catch (IllegalArgumentException expected) {}
        passed++;
    }

    public static void main(String[] args) {
        parsesIdentityFields();
        commMayContainSpacesAndParentheses();
        pidReuseIsRejected();
        malformedInputFailsClosed();
        System.out.println("Linux process identity tests passed: " + passed);
    }
}
