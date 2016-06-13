/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.iochannel;

import java.util.Collection;

/**
 * Configuration data used in iochannel.
 *
 * @author jylee
 *
 */
public abstract class Config {

    public static class ServerConfig {
        public ServerConfig(String id, String host, int port) { this.id = id; this.host = host; this.port = port; }
        public final String id;
        public final String host;
        public final int port;
    }

    public abstract ServerConfig getLocalConfig();
    public abstract Collection<ServerConfig> getPeerServers();
}
