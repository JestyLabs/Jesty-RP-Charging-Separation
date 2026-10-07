package com.jesty.rpchargingseparation;

import android.os.Process;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

/**
 * Research-only detached process probe.
 *
 * It never reads or writes charging controls. It writes a bounded heartbeat under
 * /data/local/tmp so device-side tests can prove whether a PServer-launched root
 * app_process survives Retroid process cleaning independently of the Android app.
 */
public final class ProcessSurvivalProbe {
    private static final String LOCK_PATH = "/data/local/tmp/jesty-rp-process-survival.lock";
    private static final long HEARTBEAT_MS = 2_000L;

    private ProcessSurvivalProbe() {}

    public static void main(String[] args) {
        int durationSeconds = parseDuration(args);
        try (RandomAccessFile lockFile = new RandomAccessFile(LOCK_PATH, "rw");
             FileChannel channel = lockFile.getChannel();
             FileLock lock = channel.tryLock()) {
            if (lock == null) return;
            runProbe(durationSeconds);
        } catch (Throwable error) {
            append("FATAL type=" + error.getClass().getSimpleName()
                    + " message=" + safe(error.getMessage()));
        }
    }

    private static void runProbe(int durationSeconds) throws Exception {
        File stop = new File(ProcessSurvivalProbeCommand.STOP_PATH);
        if (stop.exists()) stop.delete();

        truncateLog();
        long startedWall = System.currentTimeMillis();
        long startedElapsed = SystemClock.elapsedRealtime();
        long deadline = startedElapsed + durationSeconds * 1000L;
        long previousElapsed = startedElapsed;

        LinuxProcessIdentity process = processIdentity();

        append("START probe_version=2"
                + " pid=" + Process.myPid()
                + " uid=" + Process.myUid()
                + " duration_s=" + durationSeconds
                + " wall_ms=" + startedWall
                + " elapsed_ms=" + startedElapsed
                + " boot_id=" + compact(readFirstLine("/proc/sys/kernel/random/boot_id"), 80));
        append("IDENTITY selinux=" + compact(readFirstLine("/proc/self/attr/current"), 160)
                + " oom_score_adj=" + compact(readFirstLine("/proc/self/oom_score_adj"), 32)
                + " classpath=" + compact(System.getenv("CLASSPATH"), 320));
        append("PROCESS ppid=" + field(process, 0)
                + " pgrp=" + field(process, 1)
                + " session=" + field(process, 2)
                + " start_ticks=" + field(process, 3));
        append("CGROUP " + compact(readText("/proc/self/cgroup", 1024), 700));

        int sequence = 0;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (stop.exists()) {
                append("END reason=stop_requested seq=" + sequence
                        + " elapsed_ms=" + SystemClock.elapsedRealtime());
                stop.delete();
                return;
            }

            long nowElapsed = SystemClock.elapsedRealtime();
            long gap = nowElapsed - previousElapsed;
            previousElapsed = nowElapsed;
            append("HEARTBEAT seq=" + sequence
                    + " pid=" + Process.myPid()
                    + " uid=" + Process.myUid()
                    + " wall_ms=" + System.currentTimeMillis()
                    + " elapsed_ms=" + nowElapsed
                    + " gap_ms=" + gap);
            sequence++;
            Thread.sleep(HEARTBEAT_MS);
        }

        append("END reason=lease_expired seq=" + sequence
                + " elapsed_ms=" + SystemClock.elapsedRealtime());
    }

    private static LinuxProcessIdentity processIdentity() {
        try {
            return LinuxProcessIdentity.parse(readFirstLine("/proc/self/stat"));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String field(LinuxProcessIdentity identity, int which) {
        if (identity == null) return "?";
        switch (which) {
            case 0: return Integer.toString(identity.ppid);
            case 1: return Integer.toString(identity.processGroup);
            case 2: return Integer.toString(identity.session);
            case 3: return Long.toString(identity.startTimeTicks);
            default: return "?";
        }
    }

    private static int parseDuration(String[] args) {
        if (args == null || args.length == 0) {
            return ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS;
        }
        try {
            return ProcessSurvivalProbeCommand.clampDurationSeconds(
                    Integer.parseInt(args[0]));
        } catch (NumberFormatException ignored) {
            return ProcessSurvivalProbeCommand.DEFAULT_DURATION_SECONDS;
        }
    }

    private static void truncateLog() throws Exception {
        try (FileOutputStream output =
                     new FileOutputStream(ProcessSurvivalProbeCommand.LOG_PATH, false)) {
            output.write(new byte[0]);
        }
    }

    private static synchronized void append(String line) {
        try (FileOutputStream output =
                     new FileOutputStream(ProcessSurvivalProbeCommand.LOG_PATH, true)) {
            output.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (Throwable ignored) {
            // The probe must never start changing system state in response to log failure.
        }
    }

    private static String readFirstLine(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            return line == null ? "?" : line.trim();
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private static String readText(String path, int maxChars) {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null && out.length() < maxChars) {
                if (out.length() > 0) out.append('|');
                out.append(line.trim());
            }
        } catch (Throwable ignored) {
            return "?";
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    private static String compact(String value, int maxChars) {
        if (value == null) return "?";
        String clean = value.replace('\n', '|').replace('\r', '|').trim();
        return clean.length() <= maxChars ? clean : clean.substring(0, maxChars) + "...";
    }

    private static String safe(String value) {
        return compact(value == null ? "?" : value, 200);
    }
}
