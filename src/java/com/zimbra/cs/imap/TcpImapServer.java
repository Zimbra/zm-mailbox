/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.server.ProtocolHandler;
import com.zimbra.cs.server.TcpServer;

import java.util.HashMap;
import java.util.Map;

public final class TcpImapServer extends TcpServer implements ImapServer, RealtimeStatsCallback {
    public TcpImapServer(ImapConfig config) throws ServiceException {
        super(config);
        ZimbraPerf.addStatsCallback(this);
    }

    @Override
    public String getName() {
        return getConfig().isSslEnabled() ? "ImapSSLServer" : "ImapServer";
    }

    @Override
    protected ProtocolHandler newProtocolHandler() {
        return new TcpImapHandler(this);
    }

    @Override
    public ImapConfig getConfig() {
        return (ImapConfig) super.getConfig();
    }

    /**
     * Implementation of {@link RealtimeStatsCallback} that returns the number
     * of active handlers and number of threads for this server.
     */
    @Override
    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        if (getConfig().isSslEnabled()) {
            data.put(ZimbraPerf.RTS_IMAP_SSL_CONN, numActiveHandlers());
            data.put(ZimbraPerf.RTS_IMAP_SSL_THREADS, numThreads());
        } else {
            data.put(ZimbraPerf.RTS_IMAP_CONN, numActiveHandlers());
            data.put(ZimbraPerf.RTS_IMAP_THREADS, numThreads());
        }
        return data;
    }

}
