/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
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
