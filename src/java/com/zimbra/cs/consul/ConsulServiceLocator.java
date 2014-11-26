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

    /** Contact the service locator to determine whether it is reachable and responsive */
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
