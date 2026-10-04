package com.jesty.rpchargingseparation;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public final class EventLogTest {
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("jesty-events").toFile();
        File file = new File(directory, EventLog.FILE_NAME);
        EventLog log = new EventLog(file, 3);
        for (int i = 1; i <= 5; i++) log.add(0L, "event " + i + "\nsplit");
        List<String> recent = new EventLog(file, 3).recent(10);
        if (recent.size() != 3) throw new AssertionError("keeps the newest lines: " + recent);
        if (!recent.get(0).endsWith("event 3 split") || !recent.get(2).endsWith("event 5 split")) {
            throw new AssertionError("unexpected order or newline handling: " + recent);
        }
        if (log.recent(1).size() != 1) throw new AssertionError("recent(count) limit");
        file.delete();
        directory.delete();
        System.out.println("Event log tests passed");
    }
}
