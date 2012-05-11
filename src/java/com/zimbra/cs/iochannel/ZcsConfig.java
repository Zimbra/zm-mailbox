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
package com.zimbra.cs.iochannel;

import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.iochannel.Config;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class ZcsConfig extends Config {

    public ZcsConfig() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        peerServers = new HashSet<ServerConfig>();
        // null on hostname in order to bind to all the addresses
        localConfig = new ServerConfig(prov.getLocalServer().getServiceHostname(), null, servicePort);
        for (Server s : prov.getAllServers()) {
            if (s.hasMailboxService()) {
                peerServers.add(new ZcsServerConfig(s));
            }
        }
    }

    @Override
    public ServerConfig getLocalConfig() {
        return localConfig;
    }

    @Override
    public Collection<ServerConfig> getPeerServers() {
        return ImmutableSet.copyOf(peerServers);
    }

    private final ServerConfig localConfig;
    private final HashSet<ServerConfig> peerServers;
    private final static int servicePort = 7285;  // temporary

    private static final class ZcsServerConfig extends Config.ServerConfig {
        public ZcsServerConfig(Server s) {
            super(s.getServiceHostname(), s.getServiceHostname(), servicePort);
        }
    }
}
