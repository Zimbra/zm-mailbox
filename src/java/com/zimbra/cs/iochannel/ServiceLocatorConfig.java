/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.iochannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.iochannel.Config;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.servicelocator.ServiceLocator.Entry;
import com.zimbra.common.servicelocator.ZimbraServiceNames;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * iochannel configuration that sources the list of peer servers from a service locator.
 *
 * This is the more real-time version of ProvisioningConfig that merely uses an ldap snapshot of mailstores
 * that were installed, but may or may not currently be running & healthy.
 **/
public class ServiceLocatorConfig extends Config {
    protected ServiceLocator serviceLocator;
    protected ServerConfig localConfig;

    public ServiceLocatorConfig(ServiceLocator serviceLocator) throws ServiceException {
        this.serviceLocator = serviceLocator;
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String bindAddress = null;
        localConfig = new ServerConfig(localServer.getServiceHostname(), bindAddress, localServer.getMessageChannelPort());
    }

    @Override
    public ServerConfig getLocalConfig() {
        return localConfig;
    }

    @Override
    public Collection<ServerConfig> getPeerServers() throws IOException, ServiceException {
        List<Entry> entries = serviceLocator.find(ZimbraServiceNames.IOCHANNEL, null, true);
        List<ServerConfig> result = new ArrayList<>();
        Provisioning prov = Provisioning.getInstance();
        for (Entry entry: entries) {
            Server server = prov.getServerByName(entry.hostName);
            if (server.isLocalServer()) {
                continue;
            }
            result.add(new ZcsServerConfig(server));
        }
        return result;
    }

    private static final class ZcsServerConfig extends Config.ServerConfig {
        public ZcsServerConfig(Server s) {
            super(s.getServiceHostname(), s.getServiceHostname(), s.getMessageChannelPort());
        }
    }
}
