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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.google.common.net.HostAndPort;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.consul.NodeInfoResponse.NodeInfo;
import com.zimbra.cs.consul.ServiceListResponse.Service;
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

    private String TEST_SERVICE_PREFIX = "zimbra-consulclienttest-";

    @After
    public void tearDown() throws Exception {
        ServiceListResponse list = consulClient.listAgentServices();
        if (list != null && list.getServicesById() != null) {
            for (String key : list.getServicesById().keySet()) {
                if (key.startsWith(TEST_SERVICE_PREFIX)) {
                    consulClient.agentDeregister(key);
                }
            }
        }
    }

    private CatalogRegistration.Service createService() {
        CatalogRegistration.Service service = new CatalogRegistration.Service();
        service.name = TEST_SERVICE_PREFIX + RandomStringUtils.randomAlphanumeric(10);
        service.id = service.name + new Random().nextInt(1000);
        service.port = 8000 + new Random().nextInt(1000);
        return service;
    }

    @Test
    public void catalogRegisterCRUD() throws IOException {

        // Prepare test data
        CatalogRegistration.Service service = createService();

        // Register
        consulClient.agentRegister(service);

        // Deregister
        consulClient.agentDeregister(service.id);
    }

    @Test
    public void parseHealthResponse() throws IOException {
        String json = IOUtils.toString(new FileInputStream("./data/unittest/consulHealthResponse.json"));
        JavaType javaType = new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, ServiceHealthResponse.class);
        List<ServiceHealthResponse> result = JSON.parse(json, javaType);
        Assert.assertEquals(1, result.size());
        ServiceHealthResponse hr = result.get(0);
        Assert.assertEquals("Davids-MacBook-Pro.local", hr.node.name);
        Assert.assertEquals("10.0.1.7", hr.node.address);
        Assert.assertEquals("zimbra:LmtpServer:7025", hr.service.id);
        Assert.assertEquals("zimbra:LmtpServer", hr.service.name);
        Assert.assertEquals("7025", hr.service.port);
        Assert.assertEquals(2, hr.checks.size());
        ServiceHealthResponse.Check check = hr.checks.get(0);
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

    @Test
    public void listServices() throws IOException {
        CatalogRegistration.Service service = createService();
        consulClient.agentRegister(service);

        ServiceListResponse list = consulClient.listAgentServices();
        Assert.assertNotNull(list.getServicesById());
        Assert.assertTrue(list.getServicesById().size() > 0);

        Assert.assertTrue(list.getServicesById().containsKey(service.id));
        ServiceListResponse.Service fromResp = list.getServicesById().get(service.id);
        Assert.assertNotNull(fromResp);
        Assert.assertEquals(service.id, fromResp.id);
        Assert.assertEquals(service.name, fromResp.service);
    }

    @Test
    public void electLeader() throws IOException {
        CatalogRegistration.Service service = createService();

        LeaderResponse leader = consulClient.findLeader(service);
        Assert.assertNull(leader);

        SessionResponse session = consulClient.createSession(service.id, 120, null);
        Assert.assertNotNull(session);
        Assert.assertNotNull(session.id);

        String someData = RandomStringUtils.randomAlphanumeric(100);

        boolean isLeader = consulClient.acquireLeadership(service.name, session.id, someData);
        Assert.assertTrue(isLeader);

        //try again; can't reacquire
        isLeader = consulClient.acquireLeadership(service.name, session.id, someData);
        Assert.assertFalse(isLeader);

        leader = consulClient.findLeader(service);
        Assert.assertNotNull(leader);
        Assert.assertEquals(session.id, leader.sessionId);

        Assert.assertEquals(someData, new String(Base64.decodeBase64(leader.value), "UTF-8"));

        //now release
        consulClient.releaseLeadership(service.name, session.id);
        //and get again; should be non-null, but no session
        leader = consulClient.findLeader(service);
        Assert.assertNotNull(leader);
        Assert.assertNull(leader.sessionId);
    }

    @Test
    public void getSessionNodeInfo() throws IOException {
        CatalogRegistration.Service service = createService();
        consulClient.agentRegister(service);

        SessionResponse session = consulClient.createSession(service.id, 120, null);
        Assert.assertNotNull(session);
        Assert.assertNotNull(session.id);

        SessionResponse info = consulClient.getSessionInfo(session.id);
        Assert.assertNotNull(info);
        Assert.assertNotNull(info.id);
        Assert.assertEquals(session.id, info.id);
        Assert.assertNotNull(info.nodeName);

        NodeInfoResponse nodeInfoResponse = consulClient.getNodeInfo(info.nodeName);
        NodeInfo nodeInfo = nodeInfoResponse.node;
        Assert.assertNotNull(nodeInfo);
        Assert.assertNotNull(nodeInfo.address);
        Assert.assertNotNull(nodeInfo.nodeName);

        HostAndPort hp = HostAndPort.fromString(nodeInfo.address);
        Assert.assertNotNull(hp);
        Assert.assertNotNull(hp.getHostText());

        ServiceListResponse services = nodeInfoResponse.services;
        Assert.assertNotNull(services);
        Assert.assertNotNull(services.getServicesById());
        Service nodeResponseService = services.getServicesById().get(service.id);
        Assert.assertNotNull(nodeResponseService);
        Assert.assertNotNull(nodeResponseService.service);
        Assert.assertNotNull(nodeResponseService.id);
    }

    @Test
    public void createCheckedSession() throws IOException {
        CatalogRegistration.Service service = createService();

        LeaderResponse leader = consulClient.findLeader(service);
        Assert.assertNull(leader);

        List<String> checks = new ArrayList<String>();
        checks.add("serfHealth");
        SessionResponse session = consulClient.createSession(service.id, 0, checks);
        Assert.assertNotNull(session);
        Assert.assertNotNull(session.id);
        consulClient.deleteSession(session.id);
    }

    @Test
    public void ttlSession() throws IOException {
        CatalogRegistration.Service service = createService();

        SessionResponse session = consulClient.createSession(service.id, 120, null);
        consulClient.renewSession(session.id);
    }

    private class FinalStringWrapper {
        private String value;
        public FinalStringWrapper(String value) {
            super();
            this.value = value;
        }
    }

    @Test
    public void blockForLeaderChange() throws IOException, InterruptedException {
        CatalogRegistration.Service service = createService();

        SessionResponse session = consulClient.createSession(service.id, 120, null);
        //and should now be able to acquire again
        boolean isLeader = consulClient.acquireLeadership(service.name, session.id, "");
        Assert.assertTrue(isLeader);
        final FinalStringWrapper finalStringWrapper = new FinalStringWrapper("foo");
        //other tests - blocking leader change
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    finalStringWrapper.value = consulClient.waitForLeaderChange(service, session.id);
                } catch (IOException e) {
                    ZimbraLog.test.error("ioexception waiting for leader", e);
                }
            }
        };
        t.start();

        //drop leadership; should trigger the change
        consulClient.releaseLeadership(service.name, session.id);
        int maxwait = 100000;
        t.join(maxwait);
        Assert.assertNull(finalStringWrapper.value);
        Assert.assertFalse(t.isAlive());

        t = new Thread() {
            @Override
            public void run() {
                try {
                    finalStringWrapper.value = consulClient.waitForLeaderChange(service, null);
                } catch (IOException e) {
                    ZimbraLog.test.error("ioexception waiting for leader", e);
                }
            }
        };
        t.start();

        //acquire with new session
        SessionResponse session2 = consulClient.createSession(service.id, 120, null);
        isLeader = consulClient.acquireLeadership(service.name, session2.id, "");
        t.join(maxwait);
        Assert.assertEquals(session2.id, finalStringWrapper.value);
        Assert.assertFalse(t.isAlive());
    }
}
