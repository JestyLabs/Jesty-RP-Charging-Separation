package android.service.quicksettings;
import android.content.*;
public abstract class TileService extends Context {
 public Tile tile=new Tile(); public boolean locked; public Runnable unlock;
 public Tile getQsTile(){return tile;} public boolean isLocked(){return locked;}
 public void unlockAndRun(Runnable r){unlock=r;}
 public void onStartListening(){} public void onStopListening(){} public void onDestroy(){} public void onClick(){}
 public SharedPreferences getSharedPreferences(String n,int m){return FixtureState.prefs;}
 public Object startForegroundService(Intent i){return FixtureState.start(i);}
}
