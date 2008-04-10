/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import static com.zimbra.cs.account.Provisioning.*;
import static com.zimbra.cs.util.Config.*;

public class Pop3Config extends ServerConfig {
    private String mBanner;
    private String mGoodbye;
    private boolean mSaslGssapiEnabled;

    private static final int DEFAULT_NUM_THREADS = 10;
    private static final int DEFAULT_MAX_IDLE_SECONDS = 600;
    private static final int DEFAULT_BIND_PORT = D_POP3_BIND_PORT;
    private static final int DEFAULT_SSL_BIND_PORT = D_POP3_SSL_BIND_PORT;

    public Pop3Config() {
        setNumThreads(DEFAULT_NUM_THREADS);
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
    }

    public Pop3Config(Provisioning prov, boolean ssl) throws ServiceException {
        Server server = prov.getLocalServer();
        String name = server.getAttr(A_zimbraPop3AdvertisedName);
        if (name != null && name.length() > 0) setName(name);
        // TODO actually get this from configuration
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
        setNumThreads(server.getIntAttr(A_zimbraPop3NumThreads,
                                        DEFAULT_NUM_THREADS));
        if (ssl) {
            setSSLEnabled(ssl);
            com.zimbra.cs.account.Config config = prov.getConfig();
            setSSLExcludeCiphers(config.getMultiAttr(A_zimbraSSLExcludeCipherSuites));
            setBindAddress(server.getAttr(A_zimbraPop3SSLBindAddress));
            setBindPort(server.getIntAttr(A_zimbraPop3SSLBindPort,
                                          DEFAULT_SSL_BIND_PORT));
        } else {
            setBindAddress(server.getAttr(A_zimbraPop3BindAddress));
            setBindPort(server.getIntAttr(A_zimbraPop3BindPort,
                                          DEFAULT_BIND_PORT));
        }
        mSaslGssapiEnabled = server.getBooleanAttr(
            A_zimbraPop3SaslGssapiEnabled, false);
        validate();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        mBanner = name + " Zimbra POP3 server ready";
        mGoodbye = name + " closing connection";
    }

    // TODO Can this result be cached?
    public boolean allowCleartextLogins() {
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            return server.getBooleanAttr(A_zimbraPop3CleartextLoginEnabled, false);
        } catch (ServiceException e) {
            ZimbraLog.pop.warn("Unable to determine state of %s",
                               A_zimbraPop3CleartextLoginEnabled, e);
            return false;
        }
    }

    public String getGoodbye() { return mGoodbye; }

    public String getBanner() { return mBanner; }

    public boolean isSaslGssapiEnabled() { return mSaslGssapiEnabled; }
}
