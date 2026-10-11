package android.content;
public class Intent {
 public final Class<?> target; public String action;
 public Intent(Context c,Class<?> cls) {target=cls;}
 public Intent setAction(String a) {action=a;return this;}
}
