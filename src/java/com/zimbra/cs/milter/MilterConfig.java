/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.milter;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.server.ServerConfig;
import com.zimbra.cs.util.Config;

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
        return LC.milter_max_idle_time.intValue();
    }

    @Override
    public int getWriteTimeout() {
        return LC.milter_write_timeout.intValue();
    }

    @Override
    public int getWriteChunkSize() {
        return LC.milter_write_chunk_size.intValue();
    }

    @Override
    public int getThreadKeepAliveTime() {
        return LC.milter_thread_keep_alive_time.intValue();
    }
}
