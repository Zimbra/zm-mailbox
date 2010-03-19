/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import static com.zimbra.cs.account.Provisioning.*;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.Config;

import java.util.HashSet;
import java.util.Set;

public class ImapConfig extends ServerConfig {
    private int mUnauthMaxIdleSeconds = 0;
    private String mBanner;
    private String mGoodbye;
    private Set<String> mDisabledExtensions;
    private boolean mSaslGssapiEnabled;
    private int mMaxRequestSize;
    private int mMaxMessageSize;

    private static final int DEFAULT_NUM_THREADS = 10;
    private static final int DEFAULT_BIND_PORT = Config.D_IMAP_BIND_PORT;
    private static final int DEFAULT_SSL_BIND_PORT = Config.D_IMAP_SSL_BIND_PORT;
    private static final int DEFAULT_MAX_IDLE_SECONDS = ImapFolder.IMAP_IDLE_TIMEOUT_SEC;
    private static final int DEFAULT_UNAUTH_MAX_IDLE_SECONDS = 60;
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 100 * 1024 * 1024;
    
    public ImapConfig(boolean ssl) throws ServiceException {
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
        setUnauthMaxIdleSeconds(DEFAULT_UNAUTH_MAX_IDLE_SECONDS);
        setNumThreads(DEFAULT_NUM_THREADS);
        setBindPort(ssl ? DEFAULT_SSL_BIND_PORT : DEFAULT_BIND_PORT);
        mDisabledExtensions = new HashSet<String>();
        validate();
    }

    public ImapConfig(Provisioning prov, boolean ssl) throws ServiceException {
        Server server = prov.getLocalServer();
        setSSLEnabled(ssl);
        
        // set excluded ciphers for SSL and StartTLS
        com.zimbra.cs.account.Config config = prov.getConfig();
        setSSLExcludeCiphers(config.getMultiAttr(A_zimbraSSLExcludeCipherSuites));
        
        String name = server.getAttr(A_zimbraImapAdvertisedName);
        if (name != null && name.length() > 0)
            setName(name);
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
        setUnauthMaxIdleSeconds(DEFAULT_UNAUTH_MAX_IDLE_SECONDS);
        setNumThreads(server.getIntAttr(A_zimbraImapNumThreads, DEFAULT_NUM_THREADS));
        if (ssl) {
            setBindAddress(server.getAttr(A_zimbraImapSSLBindAddress));
            setBindPort(server.getIntAttr(A_zimbraImapSSLBindPort, DEFAULT_SSL_BIND_PORT));
            mDisabledExtensions = getDisabledExtensions(server.getMultiAttr(A_zimbraImapSSLDisabledCapability));
        } else {
            setBindAddress(server.getAttr(A_zimbraImapBindAddress));
            setBindPort(server.getIntAttr(A_zimbraImapBindPort, DEFAULT_BIND_PORT));
            mDisabledExtensions = getDisabledExtensions(server.getMultiAttr(A_zimbraImapDisabledCapability));
        }
        mSaslGssapiEnabled = server.getBooleanAttr(A_zimbraImapSaslGssapiEnabled, false);
        mMaxRequestSize = LC.imap_max_request_size.intValue();
        mMaxMessageSize = config.getIntAttr(A_zimbraMtaMaxMessageSize, DEFAULT_MAX_MESSAGE_SIZE);
        validate();
    }

    @Override public void validate() throws ServiceException {
        if (getUnauthMaxIdleSeconds() < 0) {
            failure("Invalid unauth max idle seconds: " + getUnauthMaxIdleSeconds());
        }
        super.validate();
    }
    
    private Set<String> getDisabledExtensions(String[] names) {
        HashSet<String> disabled = new HashSet<String>(names.length);
        for (String name : names)
            disabled.add(name.toUpperCase());
        return disabled;
    }
    
    @Override public void setName(String name) {
        super.setName(name);

        String version = "";
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            if (server.getBooleanAttr(Provisioning.A_zimbraImapExposeVersionOnBanner, false))
                version = " " + BuildInfo.VERSION;
        } catch (ServiceException e) { }

        mBanner = "OK " + name + " Zimbra" + version + " IMAP4rev1 service ready";
        mGoodbye = name + " IMAP4rev1 server terminating connection";
    }

    public String getBanner()   { return mBanner; }
    public String getGoodbye()  { return mGoodbye; }

    public int getUnauthMaxIdleSeconds() {
        return mUnauthMaxIdleSeconds;
    }

    public void setUnauthMaxIdleSeconds(int secs) {
        mUnauthMaxIdleSeconds = secs;
    }
    
    public boolean isExtensionDisabled(String name) {
        return mDisabledExtensions.contains(name.toUpperCase());
    }

    public boolean isSaslGssapiEnabled() {
        return mSaslGssapiEnabled;
    }
    
    public int getMaxRequestSize() {
        return mMaxRequestSize;
    }

    public int getMaxMessageSize() {
        return mMaxMessageSize;
    }

    public int getNioMaxScheduledWriteBytes() {
        return LC.nio_imap_max_scheduled_write_bytes.intValue();
    }

    public int getNioWriteTimeout() {
        return LC.nio_imap_write_timeout.intValue();
    }

    public int getNioWriteChunkSize() {
        return LC.nio_imap_write_chunk_size.intValue();
    }

    public boolean allowCleartextLogins() {
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            return server.getBooleanAttr(A_zimbraImapCleartextLoginEnabled, false);
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("Unable to determine state of " + A_zimbraImapCleartextLoginEnabled, e);
            return false;
        }
    }

}
