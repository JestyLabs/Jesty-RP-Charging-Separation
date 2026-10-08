package com.jesty.rpchargingseparation;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Root-private research output. The persistent lock inode is never removed. */
final class ResearchRuntime implements AutoCloseable {
    final String directory;
    private FileOutputStream lockStream;
    private FileLock lock;

    ResearchRuntime(String name) throws Exception {
        if (!name.equals("probe") && !name.equals("sentinel")) throw new IOException("namespace");
        StructStat parent = Os.lstat("/data");
        if (!OsConstants.S_ISDIR(parent.st_mode) || (parent.st_uid != 0 && parent.st_uid != 1000)
                || (parent.st_mode & 0002) != 0
                || ((parent.st_mode & 0020) != 0 && parent.st_gid != 0 && parent.st_gid != 1000)) throw new IOException("unsafe parent");
        directory = "/data/jesty-rp-research-" + name;
        try { Os.mkdir(directory, 0700); }
        catch (ErrnoException e) { if (e.errno != OsConstants.EEXIST) throw e; }
        StructStat dir = Os.lstat(directory);
        if (!OsConstants.S_ISDIR(dir.st_mode) || dir.st_uid != 0
                || (dir.st_mode & 07777) != 0700) throw new IOException("unsafe directory");
    }

    private FileDescriptor open(String name, int flags) throws Exception {
        if (!name.matches("lock|log|stop")) throw new IOException("file name");
        FileDescriptor fd = Os.open(directory + "/" + name,
                flags | OsConstants.O_NOFOLLOW | OsConstants.O_CLOEXEC | OsConstants.O_NONBLOCK, 0600);
        try {
            StructStat stat = Os.fstat(fd);
            if (!OsConstants.S_ISREG(stat.st_mode) || stat.st_uid != 0
                    || (stat.st_mode & 07777) != 0600 || stat.st_nlink != 1)
                throw new IOException("unsafe research file");
            return fd;
        } catch (Exception e) { Os.close(fd); throw e; }
    }

    void acquire() throws Exception {
        lockStream = new FileOutputStream(open("lock", OsConstants.O_RDWR | OsConstants.O_CREAT));
        try {
            lock = lockStream.getChannel().tryLock();
            if (lock == null) throw new IOException("research worker busy");
        } catch (Exception e) { lockStream.close(); lockStream = null; throw e; }
    }

    void resetLog() throws Exception {
        // Validate the opened inode before truncating it.
        try (FileOutputStream out = new FileOutputStream(open("log",
                OsConstants.O_WRONLY | OsConstants.O_CREAT))) { out.getChannel().truncate(0); }
        remove("stop");
    }

    void append(String line) throws Exception {
        try (FileOutputStream out = new FileOutputStream(open("log",
                OsConstants.O_WRONLY | OsConstants.O_CREAT | OsConstants.O_APPEND))) {
            out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    boolean stopped() throws Exception {
        try (FileInputStream ignored = new FileInputStream(open("stop", OsConstants.O_RDONLY))) {
            return true;
        } catch (ErrnoException e) { if (e.errno == OsConstants.ENOENT) return false; throw e; }
    }

    private void remove(String name) throws Exception {
        try (FileInputStream ignored = new FileInputStream(open(name, OsConstants.O_RDONLY))) {
            Os.remove(directory + "/" + name);
        } catch (ErrnoException e) { if (e.errno != OsConstants.ENOENT) throw e; }
    }

    void control(String action) throws Exception {
        if (action.equals("status")) {
            try (FileInputStream in = new FileInputStream(open("log", OsConstants.O_RDONLY));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = in.read(buffer)) != -1) {
                    if (out.size() + count > 1024 * 1024) throw new IOException("log bound");
                    out.write(buffer, 0, count);
                }
                System.out.println(Base64.getEncoder().encodeToString(out.toByteArray()));
            }
            return;
        }
        if (action.equals("stop")) {
            try (FileOutputStream ignored = new FileOutputStream(open("stop",
                    OsConstants.O_WRONLY | OsConstants.O_CREAT))) { }
        } else if (action.equals("clean")) {
            acquire(); // Fails while the exact worker holds this persistent inode.
            remove("stop"); remove("log");
        } else throw new IOException("unknown action");
        System.out.println("OK");
    }

    public void close() throws Exception {
        try { if (lock != null) lock.release(); }
        finally {
            lock = null;
            if (lockStream != null) { lockStream.close(); lockStream = null; }
        }
    }
}
