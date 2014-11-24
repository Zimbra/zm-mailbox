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

import com.zimbra.common.service.ServiceException;


/**
 * A convenient facade for service locator related operations.
 */
public interface ServiceLocator {

    /** De-register a service. */
    public void deregister(String serviceID) throws IOException, ServiceException;

    public void deregisterSilent(String serviceID);

    /** Contact the service locator to determine whether it is reachable and responsive */
    public void ping() throws IOException;

    /** Register a service */
    public void register(CatalogRegistration.Service service) throws IOException, ServiceException;

    public void registerSilent(CatalogRegistration.Service service);
}

