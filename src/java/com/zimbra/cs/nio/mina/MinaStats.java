/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.nio.mina;

import com.zimbra.cs.nio.NioStatsMBean;
import org.apache.mina.common.IoSession;

import java.util.concurrent.atomic.AtomicLong;

class MinaStats implements NioStatsMBean {
    private final MinaServer server;
    private final AtomicLong totalSessions = new AtomicLong();
    private final AtomicLong activeSessions = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();

    public MinaStats(MinaServer server) {
        this.server = server;
    }
    
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

    public void sessionOpened() {
        activeSessions.incrementAndGet();
        totalSessions.incrementAndGet();
    }

    public void sessionClosed() {
        activeSessions.decrementAndGet();
    }

    public void bytesSent(int count) {
        sentBytes.addAndGet(count);
    }

    public void bytesReceived(int count) {
        receivedBytes.addAndGet(count);
    }

    public long getScheduledWriteBytes() {
        long total = 0;
        for (IoSession session : server.getSessions()) {
            total += session.getScheduledWriteBytes();
        }
        return total;
    }
}
