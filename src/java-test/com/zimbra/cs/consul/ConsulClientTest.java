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
package com.zimbra.cs.consul;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link ConsulClient}.
 */
public final class ConsulClientTest {
    ConsulClient consulClient = new ConsulClient();

    @Before
    public void setUp() throws Exception {
        try {
            consulClient.ping();
        } catch(IOException e) {
            Assume.assumeNoException(e);
        }
    }

    @Test
    public void testCatalogRegisterCRUD() throws IOException {

        // Prepare test data
        CatalogRegistration.Service service = new CatalogRegistration.Service();
        service.name = RandomStringUtils.randomAlphanumeric(10);
        service.id = service.name + new Random().nextInt(1000);
        service.port = 8000 + new Random().nextInt(1000);

        // Register
        consulClient.agentRegister(service);

        // Deregister
        consulClient.agentDeregister(service.id);
    }
}
