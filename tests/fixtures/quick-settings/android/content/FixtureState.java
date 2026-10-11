package android.content;
import java.util.*;
public class FixtureState {
 public static Prefs prefs; public static Intent last; public static int starts; public static boolean failStart;
 public static void reset(){prefs=new Prefs();last=null;starts=0;failStart=false;}
 public static Object start(Intent i){starts++;last=i;if(failStart)throw new IllegalStateException("blocked");return null;}
 public static class Prefs implements SharedPreferences {
  public final Map<String,Object> values=new HashMap<>(); public boolean commitOk=true;
  public final List<OnSharedPreferenceChangeListener> listeners=new ArrayList<>();
  public boolean getBoolean(String k,boolean f){return values.containsKey(k)?(Boolean)values.get(k):f;}
  public Map<String,?> getAll(){return new HashMap<>(values);}
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.add(l);}
  public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.remove(l);}
  public Editor edit(){return new Editor(){
   Map<String,Object> next=new HashMap<>(); Set<String> removed=new HashSet<>();
   public Editor putBoolean(String k,boolean v){next.put(k,v);return this;}
   public Editor putString(String k,String v){next.put(k,v);return this;}
   public Editor remove(String k){removed.add(k);return this;}
   public boolean commit(){values.keySet().removeAll(removed);values.putAll(next);
    for(OnSharedPreferenceChangeListener l:new ArrayList<>(listeners))l.onSharedPreferenceChanged(Prefs.this,"changed");return commitOk;}
   public void apply(){commit();}
  };}
 }
}
