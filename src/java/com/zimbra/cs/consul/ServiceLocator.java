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
import java.util.Random;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;


/**
 * A convenient facade for service locator related operations.
 */
public interface ServiceLocator {
    public static final Selector SELECT_FIRST = new SelectFirst();
    public static final Selector SELECT_RANDOM = new SelectRandom();

    /** De-register a service. */
    public void deregister(String serviceID) throws IOException, ServiceException;

    public void deregisterSilent(String serviceID);

    /**
     * Find service instances.
     *
     * @return the Host Name, Host Address, and Service Port of all the instances of a service.
     */
    public List<Entry> find(String serviceName, boolean healthyOnly) throws IOException, ServiceException;

    /**
     * Find a healthy service instance.
     */
    public Entry findOne(String serviceName, Selector selector, boolean healthyOnly) throws IOException, ServiceException;

    /**
     * Determines whether a given service instance is healthy.
     *
     * @throws IOException when unable to determine a service status due to a middleware I/O failure.
     * @throws ServiceException NOT_FOUND if the service could not be found or if no health checks have been performed.
     */
    public boolean isHealthy(String serviceID, String hostName) throws IOException, ServiceException;

    /** Contact the service locator to determine whether it is reachable and responsive */
    public void ping() throws IOException;

    /** Register a service */
    public void register(CatalogRegistration.Service service) throws IOException, ServiceException;

    public void registerSilent(CatalogRegistration.Service service);


    public static class Entry {
        public String hostName, hostAddress;
        public Integer servicePort;
        public List<String> tags;

        public Entry(String hostName, String hostAddress, Integer servicePort, List<String> tags) {
            this.hostName = hostName;
            this.hostAddress = hostAddress;
            this.servicePort = servicePort;
            this.tags = tags;
        }

        public Entry(String hostName, String hostAddress, Integer servicePort) {
            this(hostName, hostAddress, servicePort, new ArrayList<>());
        }

        public String toString() {
            return Objects.toStringHelper(this)
                    .add("hostName", hostName)
                    .add("hostAddress", hostAddress)
                    .add("servicePort", servicePort)
                    .add("tags", tags)
                    .toString();
        }
    }


    public interface Selector {
        public Entry selectOne(List<Entry> list) throws IOException, ServiceException;
    }

    public static class SelectFirst implements Selector {
        public Entry selectOne(List<Entry> list) throws IOException, ServiceException {
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    public static class SelectRandom implements Selector {
        public Entry selectOne(List<Entry> list) throws IOException, ServiceException {
            if (list.isEmpty()) {
                return null;
            } else if (list.size() == 1) {
                return list.get(0);
            } else {
                int index = new Random().nextInt(list.size() - 1);
                return list.get(index);
            }
        }
    }
}

