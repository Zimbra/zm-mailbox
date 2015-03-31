package com.zimbra.cs.redolog;

/**
 * Listener which fires when redolog leader changes
 *
 */
public interface LeaderChangeListener {
    public void onLeaderChange(String newLeaderSessionId);
}
