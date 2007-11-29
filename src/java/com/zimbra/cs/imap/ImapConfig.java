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

package com.zimbra.cs.imap;

import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Config;

import static com.zimbra.cs.account.Provisioning.*;

import java.util.Set;
import java.util.HashSet;

public class ImapConfig extends ServerConfig {
    private int mUnauthMaxIdleSeconds = 0;
    private String mBanner;
    private String mGoodbye;
    private Set<String> mDisabledExtensions;
    private boolean mSaslGssapiEnabled;
    private int mMaxRequestSize;

    private static final int DEFAULT_NUM_THREADS = 10;
    private static final int DEFAULT_BIND_PORT = Config.D_IMAP_BIND_PORT;
    private static final int DEFAULT_SSL_BIND_PORT = Config.D_IMAP_SSL_BIND_PORT;
    private static final int DEFAULT_MAX_IDLE_SECONDS = ImapFolder.IMAP_IDLE_TIMEOUT_SEC;
    private static final int DEFAULT_UNAUTH_MAX_IDLE_SECONDS = 60;
    private static final int DEFAULT_MAX_REQUEST_SIZE = 10000000;
    
    public ImapConfig(boolean ssl) throws ServiceException {
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
        setUnauthMaxIdleSeconds(DEFAULT_UNAUTH_MAX_IDLE_SECONDS);
        setNumThreads(DEFAULT_NUM_THREADS);
        if (ssl) {
            setBindPort(DEFAULT_SSL_BIND_PORT);
        } else {
            setBindPort(DEFAULT_BIND_PORT);
        }
        mDisabledExtensions = new HashSet<String>();
        validate();
    }

    public ImapConfig(Provisioning prov, boolean ssl) throws ServiceException {
        Server server = prov.getLocalServer();
        setSSLEnabled(ssl);
        String name = server.getAttr(A_zimbraImapAdvertisedName);
        if (name != null && name.length() > 0) setName(name);
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
        setUnauthMaxIdleSeconds(DEFAULT_UNAUTH_MAX_IDLE_SECONDS);
        setNumThreads(server.getIntAttr(A_zimbraImapNumThreads, DEFAULT_NUM_THREADS));
        if (ssl) {
            setBindAddress(server.getAttr(A_zimbraImapSSLBindAddress));
            setBindPort(server.getIntAttr(A_zimbraImapSSLBindPort, DEFAULT_SSL_BIND_PORT));
            mDisabledExtensions = getDisabledExtensions(
                server.getMultiAttr(A_zimbraImapSSLDisabledCapability));
        } else {
            setBindAddress(server.getAttr(A_zimbraImapBindAddress));
            setBindPort(server.getIntAttr(A_zimbraImapBindPort, DEFAULT_BIND_PORT));
            mDisabledExtensions = getDisabledExtensions(
                server.getMultiAttr(A_zimbraImapDisabledCapability));
        }
        mSaslGssapiEnabled = server.getBooleanAttr(
            A_zimbraImapSaslGssapiEnabled, false);
        // enough space to hold the largest possible message, plus a bit extra to cover IMAP protocol chatter
        mMaxRequestSize = server.getIntAttr(A_zimbraFileUploadMaxSize, -1);
        if (mMaxRequestSize <= 0) {
            mMaxRequestSize = DEFAULT_MAX_REQUEST_SIZE;
        } else {
            mMaxRequestSize += 1024;
        }
        validate();
    }

    @Override
    public void validate() throws ServiceException {
        if (getUnauthMaxIdleSeconds() < 0) {
            failure("Invalid unauth max idle seconds: " +
                    getUnauthMaxIdleSeconds());
        }
        super.validate();
    }
    
    private Set<String> getDisabledExtensions(String[] names) {
        HashSet<String> disabled = new HashSet<String>(names.length);
        for (String name : names) disabled.add(name.toUpperCase());
        return disabled;
    }
    
    @Override
    public void setName(String name) {
        super.setName(name);
        mBanner = "OK " + name + " Zimbra IMAP4rev1 service ready";
        mGoodbye = "BYE " + name + " IMAP4rev1 server terminating connection";
    }

    public String getBanner() { return mBanner; }
    public String getGoodbye() { return mGoodbye; }
    public int getUnauthMaxIdleSeconds() { return mUnauthMaxIdleSeconds; }

    public void setUnauthMaxIdleSeconds(int secs) {
        mUnauthMaxIdleSeconds = secs;
    }
    
    public boolean isExtensionDisabled(String name) {
        return mDisabledExtensions.contains(name.toUpperCase());
    }

    public boolean isSaslGssapiEnabled() {
        return mSaslGssapiEnabled;
    }
    
    /**
     * Returns the size of the largest request (total size of all non-literals
     * and literals) this IMAP server will handle.
     */
    public int getMaxRequestSize() {
        return mMaxRequestSize;
    }
    
    // TODO can this value be cached?
    public boolean allowCleartextLogins() {
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            return server.getBooleanAttr(A_zimbraImapCleartextLoginEnabled, false);
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("Unable to determine state of %s",
                                A_zimbraImapCleartextLoginEnabled, e);
            return false;
        }
    }

}
