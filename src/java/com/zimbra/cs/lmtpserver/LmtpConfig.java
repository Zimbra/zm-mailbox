/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import static com.zimbra.cs.account.Provisioning.*;

public class LmtpConfig extends ServerConfig {
    private String mRecipientDelimiter;
    private LmtpBackend mBackend;

    private static final int DEFAULT_MAX_IDLE_SECONDS = 300;
    
    public LmtpConfig() {}

    public LmtpConfig(Provisioning provisioning) throws ServiceException {
	setName(getServerName());
	Server server = provisioning.getLocalServer();
        // TODO get this from configuration
        setMaxIdleSeconds(DEFAULT_MAX_IDLE_SECONDS);
        setNumThreads(server.getIntAttr(A_zimbraLmtpNumThreads, -1));
        setBindPort(server.getIntAttr(A_zimbraLmtpBindPort, -1));
        setBindAddress(server.getAttr(A_zimbraLmtpBindAddress));
        setRecipientDelimiter(provisioning.getConfig().getAttr(A_zimbraMtaRecipientDelimiter));
        setLmtpBackend(new ZimbraLmtpBackend(this));
        validate();
    }

    @Override
    public void validate() throws ServiceException {
        if (getNumThreads() < 0) {
            failure("invalid value " + getNumThreads() + " for " + A_zimbraLmtpNumThreads);
        }
        if (getBindPort() < 0) {
            failure("invalid value " + getBindPort() + " for " + A_zimbraLmtpBindPort);
        }
//        if (getRecipientDelimiter() == null) {
//            failure("missing recipient delimiter");
//        }
        if (getLmtpBackend() == null) {
            failure("missing lmtp backend");
        }
        super.validate();
    }

    public static String getServerName() {
	try {
	    Server server = Provisioning.getInstance().getLocalServer();
	    String name = server.getAttr(A_zimbraLmtpAdvertisedName);
	    if (name != null && name.length() > 0) {
		return name;
	    }
	    
	    name = LC.zimbra_server_hostname.value();
	    if (name != null) {
		return name;
	    }
	} catch (ServiceException se) {}
        return "";
    }

    public static String getServerVersion() {
        String version = "";
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            if (server.getBooleanAttr(Provisioning.A_zimbraLmtpExposeVersionOnBanner, false))
                version = " " + BuildInfo.VERSION;
        } catch (ServiceException se) {}
        return version;
    }
    
    public String getRecipientDelimiter() {
        return mRecipientDelimiter;
    }

    public void setRecipientDelimiter(String delimiter) {
        mRecipientDelimiter = delimiter;
    }
    
    public LmtpBackend getLmtpBackend() { return mBackend; }
    
    public void setLmtpBackend(LmtpBackend backend) {
        mBackend = backend;
    }
    
    public boolean permanentFailureWhenOverQuota() {
        boolean isPermanent = false;
        try {
            isPermanent = Provisioning.getInstance().getLocalServer().getBooleanAttr(
                Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, false);
        } catch (ServiceException e) {
            ZimbraLog.lmtp.warn("Unable to determine value of %s.  Defaulting to false.",
                Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota);
        }
        return isPermanent;
    }
    
}
