/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioServer;
import com.zimbra.cs.server.ServerThrottle;
import com.zimbra.cs.stats.ZimbraPerf;

public final class NioImapServer extends NioServer implements ImapServer, RealtimeStatsCallback {
    private final NioImapDecoder decoder;

    public NioImapServer(ImapConfig config) throws ServiceException {
        super(config);
        decoder = new NioImapDecoder();
        decoder.setMaxChunkSize(config.getWriteChunkSize());
        decoder.setMaxLineLength(config.getMaxRequestSize());
        decoder.setMaxLiteralSize(config.getMaxMessageSize());
        registerMBean(getName());
        ZimbraPerf.addStatsCallback(this);
        Set<String> safeHosts = new HashSet<String>();
        for (Server server : Provisioning.getInstance().getAllServers()) {
            safeHosts.add(server.getServiceHostname());
        }
        for (String ignoredHost : config.getIgnoredHosts()) {
            safeHosts.add(ignoredHost);
        }
        ServerThrottle.configureThrottle(config.getProtocol(), LC.imap_throttle_ip_limit.intValue(), LC.imap_throttle_acct_limit.intValue(), safeHosts);
    }

    @Override
    public String getName() {
        return config.isSslEnabled() ? "ImapSSLServer" : "ImapServer";
    }

    @Override
    public NioHandler createHandler(NioConnection conn) {
        return new NioImapHandler(this, conn);
    }

    @Override
    protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new ProtocolCodecFactory() {
            @Override
            public ProtocolEncoder getEncoder(IoSession session) throws Exception {
                return DEFAULT_ENCODER;
            }

            @Override
            public ProtocolDecoder getDecoder(IoSession session) {
                return decoder;
            }
        };
    }

    @Override
    public ImapConfig getConfig() {
        return (ImapConfig) super.getConfig();
    }

    @Override
    public Log getLog() {
        return ZimbraLog.imap;
    }

    @Override
    public Map<String, Object> getStatData() {
        String connStatName = getConfig().isSslEnabled() ? ZimbraPerf.RTS_IMAP_SSL_CONN : ZimbraPerf.RTS_IMAP_CONN;
        String threadStatName = getConfig().isSslEnabled() ? ZimbraPerf.RTS_IMAP_SSL_THREADS : ZimbraPerf.RTS_IMAP_THREADS;
        return ImmutableMap.of(connStatName, (Object) getNumConnections(), threadStatName, getNumThreads());
    }
}
