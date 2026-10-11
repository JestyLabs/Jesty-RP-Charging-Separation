package android.service.quicksettings;
public class Tile {
 public static final int STATE_ACTIVE=2,STATE_INACTIVE=1; public int state,updates; public String subtitle,label;
 public void setState(int s){state=s;} public void setLabel(CharSequence s){label=s.toString();}
 public void setSubtitle(CharSequence s){subtitle=s.toString();} public void updateTile(){updates++;}
}
