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

package com.zimbra.cs.pop3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.server.NioConnection;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioServer;
import com.zimbra.cs.server.ServerThrottle;
import com.zimbra.cs.stats.ZimbraPerf;

public final class NioPop3Server extends NioServer implements Pop3Server, RealtimeStatsCallback {
    private static final ProtocolDecoder DECODER = new TextLineDecoder(Charsets.ISO_8859_1, LineDelimiter.AUTO);

    public NioPop3Server(Pop3Config config) throws ServiceException {
        super(config);
        registerMBean(getName());
        ZimbraPerf.addStatsCallback(this);
        Set<String> safeHosts = new HashSet<String>();
        for (Server server : Provisioning.getInstance().getAllServers()) {
            safeHosts.add(server.getServiceHostname());
        }
        for (String ignoredHost : config.getIgnoredHosts()) {
            safeHosts.add(ignoredHost);
        }
        ServerThrottle.configureThrottle(config.getProtocol(), LC.pop3_throttle_ip_limit.intValue(), LC.pop3_throttle_acct_limit.intValue(), safeHosts);
    }

    @Override
    public String getName() {
        return config.isSslEnabled() ? "Pop3SSLServer" : "Pop3Server";
    }

    @Override
    public NioHandler createHandler(NioConnection conn) {
        return new NioPop3Handler(this, conn);
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
                return DECODER;
            }
        };
    }

    @Override
    public Pop3Config getConfig() {
        return (Pop3Config) super.getConfig();
    }

    @Override
    public Map<String, Object> getStatData() {
        String connStatName = getConfig().isSslEnabled() ? ZimbraPerf.RTS_POP_SSL_CONN : ZimbraPerf.RTS_POP_CONN;
        String threadStatName = getConfig().isSslEnabled() ? ZimbraPerf.RTS_POP_SSL_THREADS : ZimbraPerf.RTS_POP_THREADS;
        return ImmutableMap.of(connStatName, (Object) getNumConnections(), threadStatName, getNumThreads());
    }
}
