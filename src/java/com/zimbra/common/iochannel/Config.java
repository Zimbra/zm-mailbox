/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
