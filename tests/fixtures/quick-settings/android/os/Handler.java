package android.os;
public class Handler {
 public static Runnable next;
 public Handler(Looper l){} public void post(Runnable r){r.run();}
 public void postDelayed(Runnable r,long ms){next=r;} public void removeCallbacksAndMessages(Object token){next=null;}
}
