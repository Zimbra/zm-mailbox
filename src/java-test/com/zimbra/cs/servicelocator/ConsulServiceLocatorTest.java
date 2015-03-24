/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servicelocator;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.consul.CatalogRegistration;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link ConsulServiceLocator}.
 */
public final class ConsulServiceLocatorTest {
    ServiceLocator serviceLocator;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class);
        Zimbra.startupMinimal();
        serviceLocator = Zimbra.getAppContext().getBean(ServiceLocator.class);
        try {
            serviceLocator.ping();
        } catch(IOException e) {
            Assume.assumeNoException(e);
        }
    }

    @Test
    public void testCRUD() throws IOException, ServiceException {

        // Prepare test data
        CatalogRegistration.Service service = new CatalogRegistration.Service();
        service.name = RandomStringUtils.randomAlphanumeric(10);
        service.id = service.name + ":" + new Random().nextInt(1000);
        service.port = 9000 + new Random().nextInt(1000);

        // Register
        serviceLocator.register(service);

        // Verify expected health check result when no health check has ever run
        try {
            serviceLocator.isHealthy(service.id, InetAddress.getLocalHost().getHostName());
            Assert.fail("Expected a ServiceException with code=NOT_FOUND");
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.NOT_FOUND, e.getCode());
        } catch (IOException e) {}

        // Deregister
        serviceLocator.deregister(service.id);
    }
}
