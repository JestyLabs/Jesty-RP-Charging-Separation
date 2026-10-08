package android.system;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Syscall boundary fixture, backed by real host files and kernel file locks. */
public class Os {
    public static Path root;
    public static final Map<String, StructStat> metadata = new HashMap<>();
    public static final Set<String> links = new HashSet<>();
    private static final Map<FileDescriptor, RandomAccessFile> handles = new IdentityHashMap<>();
    private static final Map<FileDescriptor, String> names = new IdentityHashMap<>();
    public static void reset() throws Exception {
        root = Files.createTempDirectory("rp-research-syscalls-");
        metadata.clear(); links.clear(); names.clear(); handles.clear();
        metadata.put("/data", new StructStat(0040771, 1000));
    }
    private static Path path(String name) { return root.resolve(name.substring(1)); }
    public static StructStat lstat(String name) throws ErrnoException {
        StructStat st = metadata.get(name);
        if (st == null) throw new ErrnoException("missing", OsConstants.ENOENT);
        return links.contains(name) ? new StructStat(0120777, 0) : st;
    }
    public static void mkdir(String name, int mode) throws Exception {
        if (metadata.containsKey(name)) throw new ErrnoException("exists", OsConstants.EEXIST);
        Files.createDirectories(path(name)); metadata.put(name, new StructStat(0040000 | mode, 0));
    }
    public static FileDescriptor open(String name, int flags, int mode) throws Exception {
        if ((flags & OsConstants.O_NOFOLLOW)==0 || (flags & OsConstants.O_NONBLOCK)==0)
            throw new AssertionError("open must not follow links or block on a FIFO");
        if (links.contains(name)) throw new IOException("symlink refused");
        if (!Files.exists(path(name))) {
            if ((flags & OsConstants.O_CREAT)==0) throw new ErrnoException("missing", OsConstants.ENOENT);
            Files.createFile(path(name)); metadata.put(name, new StructStat(0100000 | mode, 0));
        }
        RandomAccessFile file = new RandomAccessFile(path(name).toFile(), (flags & 3)==0 ? "r" : "rw");
        if ((flags & OsConstants.O_APPEND)!=0) file.seek(file.length());
        FileDescriptor fd=file.getFD(); handles.put(fd,file); names.put(fd,name); return fd;
    }
    public static StructStat fstat(FileDescriptor fd) { return metadata.get(names.get(fd)); }
    public static void close(FileDescriptor fd) throws Exception { handles.get(fd).close(); }
    public static void remove(String name) throws Exception {
        // Windows cannot unlink an open fixture handle. Android permits unlink after validation.
        for (FileDescriptor fd : new ArrayList<>(names.keySet()))
            if (name.equals(names.get(fd)) && fd.valid()) handles.get(fd).close();
        Files.delete(path(name)); metadata.remove(name);
    }
}
