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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.qless.JSON;

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
    public void catalogRegisterCRUD() throws IOException {

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

    @Test
    public void parseHealthResponse() throws IOException {
        String json = IOUtils.toString(new FileInputStream("./data/unittest/consulHealthResponse.json"));
        JavaType javaType = new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, HealthResponse.class);
        List<HealthResponse> result = JSON.parse(json, javaType);
        Assert.assertEquals(1, result.size());
        HealthResponse hr = result.get(0);
        Assert.assertEquals("Davids-MacBook-Pro.local", hr.node.name);
        Assert.assertEquals("10.0.1.7", hr.node.address);
        Assert.assertEquals("zimbra:LmtpServer:7025", hr.service.id);
        Assert.assertEquals("zimbra:LmtpServer", hr.service.name);
        Assert.assertEquals("7025", hr.service.port);
        Assert.assertEquals(2, hr.checks.size());
        HealthResponse.Check check = hr.checks.get(0);
        Assert.assertEquals("Davids-MacBook-Pro.local", check.node);
        Assert.assertEquals("service:zimbra:LmtpServer:7025", check.id);
        Assert.assertEquals("Service 'zimbra:LmtpServer' check", check.name);
        Assert.assertEquals("passing", check.status);
        Assert.assertEquals("", check.notes);
        Assert.assertEquals("smtp://10.0.1.7:7025... OK\n", check.output);
        Assert.assertEquals("zimbra:LmtpServer:7025", check.serviceID);
        Assert.assertEquals("zimbra:LmtpServer", check.serviceName);
        check = hr.checks.get(1);
        Assert.assertEquals("Davids-MacBook-Pro.local", check.node);
        Assert.assertEquals("serfHealth", check.id);
        Assert.assertEquals("Serf Health Status", check.name);
        Assert.assertEquals("passing", check.status);
        Assert.assertEquals("", check.notes);
        Assert.assertEquals("Agent alive and reachable", check.output);
        Assert.assertEquals("", check.serviceID);
        Assert.assertEquals("", check.serviceName);
    }
}
