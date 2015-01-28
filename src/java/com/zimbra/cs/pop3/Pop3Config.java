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

package com.zimbra.cs.pop3;

import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3AdvertisedName;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3BindAddress;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3BindPort;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3CleartextLoginEnabled;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3ExposeVersionOnBanner;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3MaxConnections;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3MaxIdleTime;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3NumThreads;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3SSLBindAddress;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3SSLBindPort;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3SaslGssapiEnabled;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3ShutdownGraceSeconds;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3ThreadKeepAliveTime;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraPop3WriteTimeout;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.ProvisioningUtil;

public class Pop3Config extends ServerConfig {
    private static final String PROTOCOL = "POP3";

    public Pop3Config(boolean ssl) {
        super(PROTOCOL, ssl);
    }

    @Override
    public String getServerName() {
        return getAttr(A_zimbraPop3AdvertisedName, LC.zimbra_server_hostname.value());
    }

    @Override
    public String getServerVersion() {
        return getBooleanAttr(A_zimbraPop3ExposeVersionOnBanner, false) ?
            BuildInfo.VERSION : null;
    }

    @Override
    public String getBindAddress() {
        return getAttr(isSslEnabled() ?
            A_zimbraPop3SSLBindAddress : A_zimbraPop3BindAddress, null);
    }

    @Override
    public int getBindPort() {
        return isSslEnabled() ?
            getIntAttr(A_zimbraPop3SSLBindPort, Config.D_POP3_SSL_BIND_PORT) :
            getIntAttr(A_zimbraPop3BindPort, Config.D_POP3_BIND_PORT);
    }

    @Override
    public int getShutdownTimeout() {
       return getIntAttr(A_zimbraPop3ShutdownGraceSeconds, super.getShutdownTimeout());
    }

    @Override
    public int getMaxIdleTime() {
    	return (int) (ProvisioningUtil.getTimeIntervalServerAttribute(A_zimbraPop3MaxIdleTime, 60000L)/1000);
    }

    @Override
    public int getMaxThreads() {
        return getIntAttr(A_zimbraPop3NumThreads, super.getMaxThreads());
    }

    @Override
    public int getThreadKeepAliveTime() {
    	return (int) (ProvisioningUtil.getTimeIntervalServerAttribute(A_zimbraPop3ThreadKeepAliveTime, 60000L)/1000);
    }
    
    @Override
    public int getMaxConnections() {
        return getIntAttr(A_zimbraPop3MaxConnections, super.getMaxConnections());
    }

    @Override
    public int getWriteTimeout() {
        return (int) (ProvisioningUtil.getTimeIntervalServerAttribute(A_zimbraPop3WriteTimeout, 10000L)/1000);
    }

    @Override
    public Log getLog() {
        return ZimbraLog.pop;
    }

    @Override
    public String getConnectionRejected() {
        return "-ERR " + getDescription() + " closing connection; service busy";
    }

    public boolean isCleartextLoginsEnabled() {
        return getBooleanAttr(A_zimbraPop3CleartextLoginEnabled, false);
    }

    public boolean isSaslGssapiEnabled() {
        return getBooleanAttr(A_zimbraPop3SaslGssapiEnabled, false);
    }

    @Override
    protected String getUrlScheme() {
        return isSslEnabled() ? "pops" : "pop";
    }
    
}
