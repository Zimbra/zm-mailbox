/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.cs.imap;

import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapAdvertisedName;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapBindAddress;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapBindPort;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapCleartextLoginEnabled;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapDisabledCapability;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapExposeVersionOnBanner;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapMaxConnections;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapMaxRequestSize;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapNumThreads;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapSSLBindAddress;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapSSLBindPort;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapSSLDisabledCapability;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapSaslGssapiEnabled;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraImapShutdownGraceSeconds;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraMtaMaxMessageSize;

import java.util.Arrays;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.Config;

public class ImapConfig extends ServerConfig {
    private static final String PROTOCOL = "IMAP4rev1";
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 100 * 1024 * 1024;

    public ImapConfig(boolean ssl) {
        super(PROTOCOL, ssl);
    }

    @Override
    public String getServerName() {
        return getAttr(A_zimbraImapAdvertisedName, LC.zimbra_server_hostname.value());
    }

    @Override
    public String getServerVersion() {
        return getBooleanAttr(A_zimbraImapExposeVersionOnBanner, false) ? BuildInfo.VERSION : null;
    }

    @Override
    public String getBindAddress() {
        return getAttr(isSslEnabled() ? A_zimbraImapSSLBindAddress : A_zimbraImapBindAddress, null);
    }

    @Override
    public int getBindPort() {
        return isSslEnabled() ?
            getIntAttr(A_zimbraImapSSLBindPort, Config.D_IMAP_SSL_BIND_PORT) :
            getIntAttr(A_zimbraImapBindPort, Config.D_IMAP_BIND_PORT);
    }

    @Override
    public int getWriteTimeout() {
        int time;
        try {
            time = Provisioning.getInstance().getLocalServer().getImapWriteTimeout();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Error while fetching attribute ImapWriteTimeout", e);
            time  = 10;
        }
        return time;
    }

    @Override
    public int getWriteChunkSize() {
        int chunkSize;
        try {
            chunkSize = Provisioning.getInstance().getLocalServer().getImapWriteChunkSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Error while fetching attribute ImapWriteChunkSize", e);
            chunkSize  = 10;
        }
        return chunkSize;
    }

    /**
     * Returns the max idle timeout for unauthenticated connections.
     *
     * @return max idle timeout in seconds
     */
    @Override
    public int getMaxIdleTime() {
        try {
            return Provisioning.getInstance().getLocalServer().getImapMaxIdleTime();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Error while fetching attribute ImapMaxIdleTime", e);
        }
        return 60;
    }

    /**
     * Returns the max idle timeout for authenticated connections.
     *
     * @return max idle timeout in seconds
     */
    public int getAuthenticatedMaxIdleTime() {
        int maxIdleTime;
        try {
            maxIdleTime = Provisioning.getInstance().getLocalServer().getImapAuthenticatedMaxIdleTime();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Error while fetching attribute imap authentication max idle time", e);
            maxIdleTime = 1800;
        }
        return maxIdleTime;
    }

    @Override
    public int getMaxThreads() {
        return getIntAttr(A_zimbraImapNumThreads, super.getMaxThreads());
    }

    @Override
    public int getMaxConnections() {
        return getIntAttr(A_zimbraImapMaxConnections, super.getMaxConnections());
    }

    @Override
    public Log getLog() {
        return ZimbraLog.imap;
    }

    @Override
    public String getConnectionRejected() {
        return "* BYE " + getDescription() + " closing connection; service busy";
    }

    @Override
    public int getShutdownTimeout() {
       return getIntAttr(A_zimbraImapShutdownGraceSeconds, super.getShutdownTimeout());
    }

    @Override
    public int getThreadKeepAliveTime() {
        int keepAliveTime;
        try {
            keepAliveTime = Provisioning.getInstance().getLocalServer().getImapThreadKeepAliveTime();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Error while fetching attribute imap authentication max idle time", e);
            keepAliveTime = 60;
        }
        return keepAliveTime;
    }

    public boolean isCleartextLoginEnabled() {
        return getBooleanAttr(A_zimbraImapCleartextLoginEnabled, false);
    }

    public boolean isSaslGssapiEnabled() {
        return getBooleanAttr(A_zimbraImapSaslGssapiEnabled, false);
    }

    public boolean isCapabilityDisabled(String name) {
        String key = isSslEnabled() ? A_zimbraImapSSLDisabledCapability : A_zimbraImapDisabledCapability;
        try {
            return Arrays.asList(getLocalServer().getMultiAttr(key)).contains(name);
        } catch (ServiceException e) {
            getLog().warn("Unable to get server attribute: " + key, e);
            return false;
        }
    }

    public int getMaxRequestSize() {
        return getIntAttr(A_zimbraImapMaxRequestSize, LC.imap_max_request_size.intValue());
    }

    /**
     * @return maximum message size where 0 means "no limit"
     */
    public long getMaxMessageSize() throws ServiceException {
        return Provisioning.getInstance().getConfig().getLongAttr(A_zimbraMtaMaxMessageSize, DEFAULT_MAX_MESSAGE_SIZE);
    }

    @Override
    protected String getUrlScheme() {
        return isSslEnabled() ? "imaps" : "imap";
    }
}
