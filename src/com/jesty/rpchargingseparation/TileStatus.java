package com.jesty.rpchargingseparation;

/** Tile highlight means user-enabled; subtitle describes the service, not a fresh readback. */
final class TileStatus {
    static String subtitle(boolean enabled, boolean startFailed, BypassController.State state,
            boolean published) {
        if (startFailed) return enabled ? "Start failed · open app" : "OFF failed · open app";
        if (!enabled) {
            if (state == BypassController.State.FAILED) return "Open app to check";
            return state == BypassController.State.OFF
                    ? (published ? "Off · restored" : "Off · unverified") : "Turning off";
        }
        switch (state) {
            case ACTIVE: return "Separation active";
            case ARMED: return "Waiting for USB";
            case CHARGING_TO_LIMIT: return "Charging to limit";
            case ENABLING: return "Starting";
            case RETRYING: return "Retrying";
            case FAILED: return "Open app to check";
            default: return "Enabled · open app";
        }
    }
}
