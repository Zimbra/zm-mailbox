package com.zimbra.cs.mina;

import org.apache.mina.common.IoSession;

import java.util.concurrent.atomic.AtomicLong;

public class MinaStats implements MinaStatsMBean {
    private final MinaServer server;

    public MinaStats(MinaServer server) {
        this.server = server;
    }

    public final AtomicLong totalSessions = new AtomicLong();
    public final AtomicLong activeSessions = new AtomicLong();
    public final AtomicLong receivedBytes = new AtomicLong();
    public final AtomicLong sentBytes = new AtomicLong();

    public long getTotalSessions() {
        return totalSessions.get();
    }

    public long getActiveSessions() {
        return activeSessions.get();
    }

    public long getReceivedBytes() {
        return receivedBytes.get();
    }

    public long getSentBytes() {
        return sentBytes.get();
    }

    public long getScheduledWriteBytes() {
        long total = 0;
        for (IoSession session : server.getSessions()) {
            total += session.getScheduledWriteBytes();
        }
        return total;
    }
}
