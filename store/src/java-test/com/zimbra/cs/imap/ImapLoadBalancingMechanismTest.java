/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockServer;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapLoadBalancingMechanism.CustomLBMech;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.MockHttpServletRequest;

/**
 * Unit test for {@link ImapLoadBalancingMechanism}.
 *
 */
public final class ImapLoadBalancingMechanismTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void ClientIpHashMechanismEmptyServerList()
    throws Exception
    {
        try {
            ImapLoadBalancingMechanism mech = ImapLoadBalancingMechanism.newInstance(ImapLoadBalancingMechanism.ImapLBMech.ClientIpHash.name());
            ArrayList<Server> pool = new ArrayList<Server>();
            mech.getImapServerFromPool(null, pool);
            Assert.fail("should have raised ServiceException");

        }
        catch (Exception e) {
            Assert.assertTrue("expected ServiceException", e instanceof ServiceException);
        }
    }

    @Test
    public void ClientIpHashMechanismIpHashFromPool()
    throws Exception
    {
        ImapLoadBalancingMechanism mech = ImapLoadBalancingMechanism.newInstance(ImapLoadBalancingMechanism.ImapLBMech.ClientIpHash.name());
        ArrayList<Server> pool = new ArrayList<Server>();
		Server s0 = new MockServer("server-0", "0");
		Server s1 = new MockServer("server-1", "1");
		Server s2 = new MockServer("server-2", "2");
		// Add to pool out-of-order to verify that we are sorting correctly
        pool.add(s1);
        pool.add(s0);
        pool.add(s2);
        HashMap<String, String> headers = new HashMap<String, String>();
		/**
		 * For IPV4, Java uses the 32bit value of an IPV4 address as the hashCode of an IPV4
		 * address, so with the current test setup, and a pool size of 3:
		 *   127.0.0.2 == (0x7f << 24) + 2 == 2130706434
		 * And 2130706434 % 3 = 0
		 * So 127.0.0.2 should return server 0 in our 3 node pool,
		 *    127.0.0.3 should return server 1 in our 3 node pool,
		 *    127.0.0.4 should return server 2 in our 3 node pool
		 */
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "127.0.0.2");
        HttpServletRequest req = new MockHttpServletRequest(null, null, null, 123, "127.0.0.1", headers);
        Assert.assertEquals(s0, mech.getImapServerFromPool(req, pool));
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "127.0.0.3");
        Assert.assertEquals(s1, mech.getImapServerFromPool(req, pool));
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "127.0.0.4");
        Assert.assertEquals(s2, mech.getImapServerFromPool(req, pool));
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "192.168.56.1");
        req = new MockHttpServletRequest(null, null, null, 123, "127.0.0.1", headers);
        Assert.assertEquals("Expected address 192.168.56.1 to hash to s1", s1, mech.getImapServerFromPool(req, pool));


		/* Verify we get the same results using IPV6 */
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "::FFFF:127.0.0.2");
        Assert.assertEquals(s0, mech.getImapServerFromPool(req, pool));
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "::FFFF:127.0.0.3");
        Assert.assertEquals(s1, mech.getImapServerFromPool(req, pool));
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "::FFFF:127.0.0.4");
        Assert.assertEquals(s2, mech.getImapServerFromPool(req, pool));
        headers.put(ImapLoadBalancingMechanism.ClientIpHashMechanism.CLIENT_IP, "2001:db8:cafe:f9::6");
        Assert.assertEquals("Expected address 2001:db8:cafe:f9::6 to hash to s2", s2, mech.getImapServerFromPool(req, pool));
    }

    @Test
    public void testCustomLoadBalancingMech() throws Exception {
        CustomLBMech.register("testmech",TestCustomLBMech.class);
        ImapLoadBalancingMechanism mech = CustomLBMech.loadCustomLBMech("custom:testmech foo bar");
        Assert.assertTrue((mech instanceof TestCustomLBMech));
        CustomLBMech customMech = (CustomLBMech) mech;
        Assert.assertEquals(customMech.args.get(0), "foo");
        Assert.assertEquals(customMech.args.get(1), "bar");
        mech = CustomLBMech.loadCustomLBMech("custom:testmech");
        Assert.assertTrue((mech instanceof TestCustomLBMech));
        Assert.assertNull(((CustomLBMech) mech).args);
        mech = CustomLBMech.loadCustomLBMech("custom:unregisteredmech");
        Assert.assertTrue((mech instanceof ImapLoadBalancingMechanism.ClientIpHashMechanism));
    }

    public static class TestCustomLBMech extends CustomLBMech {

        protected TestCustomLBMech(List<String> args) {
            super(args);
        }

        @Override
        public Server getImapServerFromPool(HttpServletRequest httpReq,
                List<Server> pool) throws ServiceException {
            return null;
        }
    }
}
