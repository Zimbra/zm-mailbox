package com.zimbra.cs.redolog;

/**
 * Listener which fires when redolog leader changes
 *
 */
public interface LeaderChangeListener {
    public void onLeaderChange(String newLeaderSessionId, LeaderStateChange stateChange);

    public enum LeaderStateChange {
        LOST_LEADERSHIP,
        GAINED_LEADERSHIP,
        NO_CHANGE
    }
}
