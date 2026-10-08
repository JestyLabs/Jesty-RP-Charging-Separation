package android.system;
public class ErrnoException extends Exception {
    public final int errno;
    public ErrnoException(String message, int errno) { super(message); this.errno = errno; }
}
