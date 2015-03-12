/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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
package com.zimbra.cs.consul;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;


/**
 * A convenient facade for service locator related operations.
 */
public class ConsulServiceLocator implements ServiceLocator {
    @Autowired protected ConsulClient consulClient;
    protected ObjectMapper objectMapper = new ObjectMapper();

    public ConsulServiceLocator() {}

    public ConsulServiceLocator(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    /** De-register a service. */
    @Override
    public void deregister(String serviceID) throws IOException, ServiceException {
        consulClient.agentDeregister(serviceID);
        ZimbraLog.misc.info("Deregistered %s with service locator", serviceID);
    }

    @Override
    public void deregisterSilent(String serviceID) {
        try {
            deregister(serviceID);
        } catch (IOException | ServiceException e) {
            ZimbraLog.misc.error("Failed deregistering %s with service locator", serviceID, e);
        }
    }

    /** Returns matching service instances. */
    @Override
    public List<Entry> find(String serviceName, boolean healthyOnly) throws IOException, ServiceException {
        List<ServiceHealthResponse> list = consulClient.health(serviceName, healthyOnly);

        List<Entry> result = new ArrayList<>();
        for (ServiceHealthResponse health: list) {
            result.add(new Entry(health.node.name, health.node.address, new Integer(health.service.port), health.service.tags));
        }
        return result;
    }

    /** Determines whether a given service instance is healthy. */
    @Override
    public boolean isHealthy(String serviceName, String hostName) throws IOException, ServiceException {

//        The following code would perform better, but isn't currently convenient to use due to
//        ZCS internals lowercasing hostnames, and Consul being node name case-sensitive.
//
//        List<NodeHealthResponse> list = consulClient.health(hostName);
//        for (NodeHealthResponse health: list) {
//            if (!Objects.equal(serviceID, health.serviceID)) {
//                continue;
//            }
//            return "passing".equals(health.status);
//        }

        List<ServiceHealthResponse> list = consulClient.health(serviceName, false);
        for (ServiceHealthResponse health: list) {
            if (!hostName.equalsIgnoreCase(health.node.name)) {
                continue;
            }
            if (health.checks.isEmpty()) {
                throw ServiceException.NOT_FOUND("Service has never been health checked");
            }
            ServiceHealthResponse.Check lastCheck = health.checks.get(health.checks.size() - 1);
            return "passing".equals(lastCheck.status);
        }
        throw ServiceException.NOT_FOUND("No such service in node health response");
    }

    /** Contact the service locator to determine whether it is reachable and responsive. */
    @Override
    public void ping() throws IOException {
        consulClient.ping();
    }

    /** Register a service */
    @Override
    public void register(CatalogRegistration.Service service) throws IOException, ServiceException {
        consulClient.agentRegister(service);
        ZimbraLog.misc.info("Registered %s with service locator", service.id);
    }

    @Override
    public void registerSilent(CatalogRegistration.Service service) {
        try {
            register(service);
        } catch (IOException | ServiceException e) {
            ZimbraLog.misc.error("Failed registering %s with service locator", service.id, e);
        }
    }
}
