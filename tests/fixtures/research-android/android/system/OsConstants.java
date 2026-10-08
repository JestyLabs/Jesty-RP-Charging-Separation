package android.system;
public class OsConstants {
    public static final int O_RDONLY=0, O_WRONLY=1, O_RDWR=2, O_CREAT=64, O_APPEND=1024,
        O_NOFOLLOW=131072, O_CLOEXEC=524288, O_NONBLOCK=2048, EEXIST=17, ENOENT=2;
    public static boolean S_ISDIR(int m) { return (m & 0170000) == 0040000; }
    public static boolean S_ISREG(int m) { return (m & 0170000) == 0100000; }
}
