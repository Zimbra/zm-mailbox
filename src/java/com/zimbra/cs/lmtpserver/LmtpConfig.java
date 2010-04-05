/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.util.Config;

import static com.zimbra.cs.account.Provisioning.*;

public class LmtpConfig extends ServerConfig {
    private final LmtpBackend lmtpBackend;

    private static final String PROTOCOL = "LMTP";
    private static final int MAX_IDLE_SECONDS = 300;

    public static final LmtpConfig INSTANCE = new LmtpConfig();

    public static LmtpConfig getInstance() {
        return INSTANCE;
    }
    
    private LmtpConfig() {
        super(PROTOCOL, false);
        lmtpBackend = new ZimbraLmtpBackend(this);
    }

    @Override
    public String getServerName() {
        return getAttr(A_zimbraLmtpAdvertisedName, LC.zimbra_server_hostname.value());
    }

    @Override
    public String getServerVersion() {
        return getBooleanAttr(A_zimbraLmtpExposeVersionOnBanner, false) ?
            BuildInfo.VERSION : null;
    }

    @Override
    public int getMaxIdleSeconds() {
        return MAX_IDLE_SECONDS;
    }

    @Override
    public int getShutdownGraceSeconds() {
       return getIntAttr(A_zimbraLmtpShutdownGraceSeconds, super.getShutdownGraceSeconds());
    }
    
    @Override
    public int getNumThreads() {
        return getIntAttr(A_zimbraLmtpNumThreads, super.getNumThreads());
    }

    @Override
    public int getBindPort() {
        return getIntAttr(A_zimbraLmtpBindPort, Config.D_LMTP_BIND_PORT);
    }

    @Override
    public String getBindAddress() {
        return getAttr(A_zimbraLmtpBindAddress, null);
    }

    @Override
    public Log getLog() {
        return ZimbraLog.lmtp;
    }

    public String getMtaRecipientDelimiter() {
        try {
            return getGlobalConfig().getAttr(A_zimbraMtaRecipientDelimiter);
        } catch (ServiceException e) {
            getLog().warn("Unable to get global attribute: " + A_zimbraMtaRecipientDelimiter, e);
            return null;
        }
    }

    public LmtpBackend getLmtpBackend() {
        return lmtpBackend;
    }

    public boolean isPermanentFailureWhenOverQuota() {
        return getBooleanAttr(A_zimbraLmtpPermanentFailureWhenOverQuota, false);
    }
}
