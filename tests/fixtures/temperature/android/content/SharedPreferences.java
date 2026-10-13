package android.content;

/** Minimal Android preferences contract for battery-history host tests. */
public interface SharedPreferences {
    int getInt(String key, int defaultValue);
    Editor edit();
    interface Editor {
        Editor putInt(String key, int value);
        Editor remove(String key);
        void apply();
    }
}
