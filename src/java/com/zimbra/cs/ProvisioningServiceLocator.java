/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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
package com.zimbra.cs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.consul.CatalogRegistration;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.Selector;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.servicelocator.ZimbraServiceNames;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.IPUtil;


/**
 * Service locator operations that are implemented with Provisioning calls. This is useful as a fallback,
 * if a realtime service locator is off-line.
 */
public class ProvisioningServiceLocator implements ServiceLocator {
    protected Provisioning prov;

    public ProvisioningServiceLocator(Provisioning prov) {
        this.prov = prov;
    }

    /** De-register a service. */
    public void deregister(String serviceID) throws IOException, ServiceException {};

    public void deregisterSilent(String serviceID) {};

    /**
     * Find service instances.
     *
     * @return the Host Name, Host Address, and Service Port of all the instances of a service.
     */
    public List<ServiceLocator.Entry> find(String serviceName, String tag, boolean healthyOnly) throws IOException, ServiceException {
        List<ServiceLocator.Entry> result = new ArrayList<>();
        if (ZimbraServiceNames.MAILSTOREADMIN.equals(serviceName)) {
            try {
                List<Server> servers = prov.getAllServers(Provisioning.SERVICE_MAILBOX);
                for (Server server: servers) {
                    int port = server.getAdminPort();
                    String hostName = server.getName();
                    String hostAddress = IPUtil.getIPByIPMode(prov, hostName).getHostAddress();
                    ServiceLocator.Entry entry = new ServiceLocator.Entry(hostName, hostAddress, port);
                    if (server.getAdminServiceScheme().startsWith("https")) {
                        entry.tags.add("ssl");
                    }
                    result.add(entry);
                }
            } catch (ServiceException e) {
                // TODO detect transport error & rethrow as IOException
                throw e;
            }
        }
        return result;
    }

    /**
     * Find a service instance.
     */
    public ServiceLocator.Entry findOne(String serviceName, Selector<ServiceLocator.Entry> selector, String tag, boolean healthyOnly) throws IOException, ServiceException {
        List<ServiceLocator.Entry> list = find(serviceName, tag, healthyOnly);
        if (list.isEmpty()) {
            list = find(serviceName, tag, false);
        }
        if (list.isEmpty()) {
            throw ServiceException.NOT_FOUND("Failed locating an instance of " + serviceName);
        }
        return selector.selectOne(list);
    }

    /**
     * Determines whether a given service instance is healthy.
     *
     * @throws IOException when unable to determine a service status due to a middleware I/O failure.
     * @throws ServiceException NOT_FOUND if the service could not be found or if no health checks have been performed.
     */
    public boolean isHealthy(String serviceID, String hostName) throws IOException, ServiceException {
        throw new IOException("Unable to determine service health");
    }

    /** Contact the service locator to determine whether it is reachable and responsive */
    public void ping() throws IOException {
        try {
            prov.healthCheck();
        } catch (ServiceException e) {
            throw new IOException("Provisioning failed health check", e);
        } catch (UnsupportedOperationException e) {}
    }

    /** Register a service */
    public void register(CatalogRegistration.Service service) throws IOException, ServiceException {};

    public void registerSilent(CatalogRegistration.Service service) {}
}

