/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra Software, LLC.
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
package com.zimbra.cs.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.consul.ConsulClient;
import com.zimbra.common.consul.ConsulServiceLocator;
import com.zimbra.common.consul.ServiceHealthResponse;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.servicelocator.ZimbraServiceNames;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link ServerAssigner}.
 */
public class ServerAssignerTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<>());
        prov.createServer("A.foo.bar", new HashMap<>());
        prov.createServer("B.foo.bar", new HashMap<>());
    }

    @Test
    public void testFindOneSkipOver() throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        server.setOfflineForMaintenance(true);
        ConsulClient consulClient = new ConsulClient() {
            @Override
            public List<ServiceHealthResponse> health(String serviceName, String tag, boolean passingOnly) throws IOException {
                List<ServiceHealthResponse> result = new ArrayList<>();

                ServiceHealthResponse service1 = new ServiceHealthResponse();
                service1.node = new ServiceHealthResponse.Node();
                service1.node.name = "A.foo.bar";
                service1.service = new ServiceHealthResponse.Service();
                service1.service.port = "8000";
                result.add(service1);

                ServiceHealthResponse service2 = new ServiceHealthResponse();
                service2.node = new ServiceHealthResponse.Node();
                service2.node.name = "B.foo.bar";
                service2.service = new ServiceHealthResponse.Service();
                service2.service.port = "8001";
                result.add(service2);

                return result;
            }
        };
        ServiceLocator serviceLocator = new ConsulServiceLocator(consulClient);
        ServerAssigner serverAssigner = new ServerAssigner(serviceLocator);
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        serverAssigner.reassign(acct, ZimbraServiceNames.MAILSTORE);

        try {
            Assert.assertEquals("B.foo.bar", acct.getServerName());
        } finally {
            server.setOfflineForMaintenance(false);
        }
    }
}
