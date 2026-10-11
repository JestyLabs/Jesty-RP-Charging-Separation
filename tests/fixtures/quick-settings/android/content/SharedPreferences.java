package android.content;
public interface SharedPreferences {
 boolean getBoolean(String key, boolean fallback); Editor edit(); java.util.Map<String,?> getAll();
 void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
 void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
 interface Editor { Editor putBoolean(String k,boolean v); Editor putString(String k,String v); Editor remove(String k); boolean commit(); void apply(); }
 interface OnSharedPreferenceChangeListener { void onSharedPreferenceChanged(SharedPreferences p,String key); }
}
