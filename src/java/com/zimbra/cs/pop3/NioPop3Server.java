/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.pop3;

import java.util.Map;

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
        ServerThrottle.configureThrottle(config.getProtocol(), LC.pop3_throttle_ip_limit.intValue(), LC.pop3_throttle_acct_limit.intValue(), getThrottleSafeHosts(), getThrottleWhitelist());
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
