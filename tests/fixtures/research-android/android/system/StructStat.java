package android.system;
public class StructStat {
    public int st_mode, st_uid, st_gid = 1000; public long st_nlink = 1;
    public StructStat(int mode, int uid) { st_mode = mode; st_uid = uid; }
}
