package com.jesty.rpchargingseparation;
final class BypassService {
 static final String PREF_DESIRED="desired_enabled",PREF_LAST_SAFETY_STOP="last_safety_stop",
 ACTION_ENABLE="com.jesty.rpchargingseparation.ENABLE",ACTION_DISABLE="com.jesty.rpchargingseparation.DISABLE";
 static BypassController.State current=BypassController.State.OFF;
 static BypassController.State state(){return current;}
}
