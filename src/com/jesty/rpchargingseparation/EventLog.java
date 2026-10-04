package com.jesty.rpchargingseparation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Small persistent event log in the app's private storage. It contains only app state
 * transitions and power readings, so users can copy it into a bug report without adb.
 */
final class EventLog {
    static final String FILE_NAME = "events.txt";
    static final int MAX_LINES = 400;

    private final File file;
    private final int maxLines;

    EventLog(File directory) {
        this(new File(directory, FILE_NAME), MAX_LINES);
    }

    EventLog(File file, int maxLines) {
        this.file = file;
        this.maxLines = Math.max(1, maxLines);
    }

    synchronized void add(long wallClockMillis, String message) {
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date(wallClockMillis));
        List<String> lines = readLines();
        lines.add(stamp + " " + message.replace('\n', ' ').replace('\r', ' '));
        int from = Math.max(0, lines.size() - maxLines);
        StringBuilder out = new StringBuilder();
        for (int i = from; i < lines.size(); i++) out.append(lines.get(i)).append('\n');
        try (FileOutputStream stream = new FileOutputStream(file, false)) {
            stream.write(out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Diagnostics must never affect charging control.
        }
    }

    synchronized List<String> recent(int count) {
        List<String> lines = readLines();
        int from = Math.max(0, lines.size() - Math.max(0, count));
        return new ArrayList<>(lines.subList(from, lines.size()));
    }

    private List<String> readLines() {
        List<String> lines = new ArrayList<>();
        if (!file.isFile()) return lines;
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] data = new byte[(int) Math.min(file.length(), 1_000_000L)];
            int total = 0;
            while (total < data.length) {
                int read = stream.read(data, total, data.length - total);
                if (read < 0) break;
                total += read;
            }
            for (String line : new String(data, 0, total, StandardCharsets.UTF_8).split("\n")) {
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (IOException ignored) {
            // Start a fresh log if the old one cannot be read.
        }
        return lines;
    }
}
