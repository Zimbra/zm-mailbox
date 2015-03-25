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
package com.zimbra.cs.servicelocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.consul.CatalogRegistration;
import com.zimbra.cs.util.Zimbra;


/**
 * Service locator operations that are implememted with Provisioning calls. This is useful as a fallback,
 * if a realtime service locator is off-line.
 */
public class ChainedServiceLocator implements ServiceLocator {
    protected List<ServiceLocator> delegates = new ArrayList<>();

    public ChainedServiceLocator(ServiceLocator... serviceLocators) {
        for (ServiceLocator serviceLocator: serviceLocators) {
            delegates.add(serviceLocator);
        }
    }

    @PostConstruct
    public void init() {
        for (ServiceLocator delegate: delegates) {
            Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(delegate);
        }
    }

    /** De-register a service. */
    public void deregister(String serviceID) throws IOException, ServiceException {
        for (ServiceLocator delegate: delegates) {
            try {
                delegate.deregister(serviceID);
            } catch (IOException | ServiceException e) {}
        }
    };

    public void deregisterSilent(String serviceID) {
        for (ServiceLocator delegate: delegates) {
            delegate.deregisterSilent(serviceID);
        }
    };

    /**
     * Find service instances.
     *
     * @return the Host Name, Host Address, and Service Port of all the instances of a service.
     */
    public List<ServiceLocator.Entry> find(String serviceName, boolean healthyOnly) throws IOException, ServiceException {
        IOException ioe = null;
        ServiceException se = null;
        for (ServiceLocator delegate: delegates) {
            try {
                List<ServiceLocator.Entry> list = delegate.find(serviceName, healthyOnly);
                if (!list.isEmpty()) {
                    return list;
                }
            } catch (IOException e) {
                ioe = e;
            } catch (ServiceException e) {
                se = e;
            }
        }
        if (ioe != null) {
            throw ioe;
        }
        if (se != null) {
            throw se;
        }
        throw new IOException("No service locator available to handle the request");
    }

    /**
     * Find a service instance.
     */
    public ServiceLocator.Entry findOne(String serviceName, Selector selector, boolean healthyOnly) throws IOException, ServiceException {
        IOException ioe = null;
        ServiceException se = null;
        for (ServiceLocator delegate: delegates) {
            try {
                ServiceLocator.Entry entry = delegate.findOne(serviceName, selector, healthyOnly);
                if (entry != null) {
                    return entry;
                }
            } catch (IOException e) {
                ioe = e;
            } catch (ServiceException e) {
                se = e;
            }
        }
        if (ioe != null) {
            throw ioe;
        }
        if (se != null) {
            throw se;
        }
        throw new IOException("No service locator available to handle the request");
    }

    /**
     * Determines whether a given service instance is healthy.
     *
     * @throws IOException when unable to determine a service status due to a middleware I/O failure.
     * @throws ServiceException NOT_FOUND if the service could not be found or if no health checks have been performed.
     */
    public boolean isHealthy(String serviceID, String hostName) throws IOException, ServiceException {
        IOException ioe = null;
        ServiceException se = null;
        for (ServiceLocator delegate: delegates) {
            try {
                return delegate.isHealthy(serviceID, hostName);
            } catch (IOException e) {
                ioe = e;
            } catch (ServiceException e) {
                se = e;
            }
        }
        if (ioe != null) {
            throw ioe;
        }
        if (se != null) {
            throw se;
        }
        throw new IOException("No service locator available to handle the request");
    }

    /** Contact the service locator to determine whether it is reachable and responsive */
    public void ping() throws IOException {
        IOException ioe = null;
        for (ServiceLocator delegate: delegates) {
            try {
                delegate.ping();
                return;
            } catch (IOException e) {
                ioe = e;
            }
        }
        if (ioe != null) {
            throw ioe;
        }
        throw new IOException("No service locator available to handle the request");
    }

    /** Register a service */
    public void register(CatalogRegistration.Service service) throws IOException, ServiceException {
        for (ServiceLocator delegate: delegates) {
            try {
                delegate.register(service);
            } catch (IOException | ServiceException e) {}
        }
    }

    public void registerSilent(CatalogRegistration.Service service) {
        for (ServiceLocator delegate: delegates) {
            delegate.registerSilent(service);
        }
    }
}
