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
package com.zimbra.cs.server;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoServiceStatistics;

/**
 * A wrapper of {@link IoServiceStatistics} to expose it as a MBean.
 *
 * @author ysasaki
 */
final class NioServerStats implements NioServerStatsMBean {
    private final IoAcceptor acceptor;
    private final IoServiceStatistics stats;

    NioServerStats(NioServer server) {
        acceptor = server.acceptor;
        stats = acceptor.getStatistics();
    }

    @Override
    public long getTotalSessions() {
        return stats.getCumulativeManagedSessionCount();
    }

    @Override
    public long getActiveSessions() {
        return acceptor.getManagedSessionCount();
    }

    @Override
    public long getReadBytes() {
        return stats.getReadBytes();
    }

    @Override
    public long getReadMessages() {
        return stats.getReadMessages();
    }

    @Override
    public long getWrittenBytes() {
        return stats.getWrittenBytes();
    }

    @Override
    public long getWrittenMessages() {
        return stats.getWrittenMessages();
    }

    @Override
    public long getScheduledWriteBytes() {
        return stats.getScheduledWriteBytes();
    }

    @Override
    public long getScheduledWriteMessages() {
        return stats.getScheduledWriteMessages();
    }
}
