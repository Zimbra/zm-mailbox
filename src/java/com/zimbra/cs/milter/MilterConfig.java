/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.milter;

import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.ProvisioningUtil;

public class MilterConfig extends ServerConfig {
    private static final String PROTOCOL = "MILTER";

    public MilterConfig() {
        super(PROTOCOL, false);
    }

    @Override
    public int getBindPort() {
        int port = LC.milter_bind_port.intValue();
        return port != 0 ? port : getIntAttr(Provisioning.A_zimbraMilterBindPort, Config.D_MILTER_BIND_PORT);
    }

    @Override
    public String getBindAddress() {
        String addr = LC.milter_bind_address.value();
        return addr != null ? addr : getAttr(Provisioning.A_zimbraMilterBindAddress, "127.0.0.1");
    }

    @Override
    public int getMaxThreads() {
        return getIntAttr(Provisioning.A_zimbraMilterNumThreads, super.getMaxThreads());
    }

    @Override
    public Log getLog() {
        return ZimbraLog.milter;
    }

    @Override
    public int getMaxConnections() {
        return getIntAttr(Provisioning.A_zimbraMilterMaxConnections, super.getMaxConnections());
    }

    @Override
    public int getMaxIdleTime() {
        return (int) (ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMilterMaxIdleTime, 3630*1000L)/1000);
    }

    @Override
    public int getWriteTimeout() {
        return (int)  (ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMilterWriteTimeout, 10*1000L)/1000);
    }

    @Override
    public int getWriteChunkSize() {
        return ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMilterWriteChunkSize, 1024);
    }

    @Override
    public int getThreadKeepAliveTime() {
        return (int) ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMilterThreadKeepAliveTime, 60*1000L)/1000;
    }

    @Override
    protected String getUrlScheme() {
        return isSslEnabled() ? "milters" : "milter";
    }
}
