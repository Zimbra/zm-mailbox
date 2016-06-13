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
        Server localServer = prov.getLocalServer();
        peerServers = new HashSet<ServerConfig>();
        // null on hostname in order to bind to all the addresses
        localConfig = new ServerConfig(localServer.getServiceHostname(), null, localServer.getMessageChannelPort());
        for (Server s : prov.getAllServers()) {
            if (!s.isLocalServer() && s.hasMailboxService() && s.isMessageChannelEnabled()) {
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

    private static final class ZcsServerConfig extends Config.ServerConfig {
        public ZcsServerConfig(Server s) {
            super(s.getServiceHostname(), s.getServiceHostname(), s.getMessageChannelPort());
        }
    }
}
