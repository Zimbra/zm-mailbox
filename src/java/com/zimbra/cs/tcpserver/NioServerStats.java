/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.tcpserver;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.session.IoSession;

public class NioServerStats implements NioServerStatsMBean {
    private final NioServer server;

    public final AtomicLong totalSessions = new AtomicLong();
    public final AtomicLong activeSessions = new AtomicLong();
    public final AtomicLong receivedBytes = new AtomicLong();
    public final AtomicLong sentBytes = new AtomicLong();

    public NioServerStats(NioServer server) {
        this.server = server;
    }

    @Override
    public long getTotalSessions() {
        return totalSessions.get();
    }

    @Override
    public long getActiveSessions() {
        return activeSessions.get();
    }

    @Override
    public long getReceivedBytes() {
        return receivedBytes.get();
    }

    @Override
    public long getSentBytes() {
        return sentBytes.get();
    }

    @Override
    public long getScheduledWriteBytes() {
        long total = 0;
        for (IoSession session : server.getSessions().values()) {
            total += session.getScheduledWriteBytes();
        }
        return total;
    }
}
