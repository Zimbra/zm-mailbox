/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
