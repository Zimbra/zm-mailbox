package com.zimbra.cs.mina;

public interface MinaStatsMBean {
    long getTotalSessions();
    long getActiveSessions();
    long getReceivedBytes();
    long getSentBytes();
    long getScheduledWriteBytes();
}
