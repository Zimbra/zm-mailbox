/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
    private static final int MAX_IDLE_TIME = 300; // seconds

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
    public int getMaxIdleTime() {
        return MAX_IDLE_TIME;
    }

    @Override
    public int getShutdownTimeout() {
       return getIntAttr(A_zimbraLmtpShutdownGraceSeconds, super.getShutdownTimeout());
    }

    @Override
    public int getMaxThreads() {
        return getIntAttr(A_zimbraLmtpNumThreads, super.getMaxThreads());
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

    @Override
    public String getConnectionRejected() {
        return "421 " + getDescription() + " closing connection; service busy";
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
