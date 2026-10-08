package com.jesty.rpchargingseparation;

/** Pure parser for the Linux /proc/<pid>/stat identity fields used by watchdog research. */
final class LinuxProcessIdentity {
    final int pid;
    final int ppid;
    final int processGroup;
    final int session;
    final long startTimeTicks;

    LinuxProcessIdentity(int pid, int ppid, int processGroup, int session, long startTimeTicks) {
        this.pid = pid;
        this.ppid = ppid;
        this.processGroup = processGroup;
        this.session = session;
        this.startTimeTicks = startTimeTicks;
    }

    static LinuxProcessIdentity parse(String stat) {
        if (stat == null) throw new IllegalArgumentException("stat is null");
        int open = stat.indexOf('(');
        int close = stat.lastIndexOf(')');
        if (open <= 0 || close <= open || close + 2 >= stat.length()) {
            throw new IllegalArgumentException("invalid /proc stat");
        }
        int pid;
        try {
            pid = Integer.parseInt(stat.substring(0, open).trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid pid", error);
        }

        // After comm, token 0 is field 3 (state). starttime is field 22 => token 19.
        String[] fields = stat.substring(close + 1).trim().split("\\s+");
        if (fields.length <= 19) throw new IllegalArgumentException("short /proc stat");
        try {
            int ppid = Integer.parseInt(fields[1]);
            int pgrp = Integer.parseInt(fields[2]);
            int session = Integer.parseInt(fields[3]);
            long start = Long.parseLong(fields[19]);
            return new LinuxProcessIdentity(pid, ppid, pgrp, session, start);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid /proc stat field", error);
        }
    }

    boolean sameProcess(int expectedPid, long expectedStartTimeTicks) {
        return pid == expectedPid && startTimeTicks == expectedStartTimeTicks;
    }
}
