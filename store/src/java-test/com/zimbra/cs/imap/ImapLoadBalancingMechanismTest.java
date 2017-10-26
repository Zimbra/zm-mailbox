/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockServer;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapLoadBalancingMechanism.AccountIdHashMechanism;
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
    public void accountIdHashMechanismEmptyServerList()
    throws Exception
    {
        try {
            ImapLoadBalancingMechanism mech = ImapLoadBalancingMechanism.newInstance(
                    ImapLoadBalancingMechanism.ImapLBMech.AccountIdHash.name());
            ArrayList<Server> pool = new ArrayList<Server>();
            mech.getImapServerFromPool(null, "dummyAccountId", pool);
            fail("should have raised ServiceException");
        } catch (ServiceException se) {
            /* this is what we expect */
        } catch (Exception e) {
            fail(String.format("Unexpected exception thrown %s", e.getMessage()));
        }
    }

    @Test
    public void accountIdHashMechanismHashFromPool()
    throws Exception
    {
        ImapLoadBalancingMechanism mech = ImapLoadBalancingMechanism.newInstance(
                ImapLoadBalancingMechanism.ImapLBMech.AccountIdHash.name());
        ArrayList<Server> pool = new ArrayList<Server>();
        Server s0 = new MockServer("server-0", "0");
        Server s1 = new MockServer("server-1", "1");
        Server s2 = new MockServer("server-2", "2");
        // Add to pool out-of-order to verify that we are sorting correctly
        pool.add(s1);
        pool.add(s0);
        pool.add(s2);
        HashMap<String, String> headers = new HashMap<String, String>();
        HttpServletRequest req = new MockHttpServletRequest(null, null, null, 123, "127.0.0.1", headers);
        String acctId0 = "79e9a595-c34b-469a-a1ee-e2b9d03f9aa7"; /* hash=1536494685, hash % 3 = 0 */
        String acctId1 = "615922e5-2318-4b8b-8734-d209a99f8252"; /* hash=454270162,  hash % 3 = 1 */
        String acctId2 = "f5c68357-61d9-4658-a7fc-e7273929ca0c"; /* hash=1373626454, hash % 3 = 2 */
        assertEquals("Should have got 0th entry from sorted pool",
                s0, mech.getImapServerFromPool(req, acctId0, pool));
        assertEquals("Should have got 1st entry from sorted pool",
                s1, mech.getImapServerFromPool(req, acctId1, pool));
        assertEquals("Should have got 2nd entry from sorted pool",
                s2, mech.getImapServerFromPool(req, acctId2, pool));
    }

    @Test
    public void testCustomLoadBalancingMech() throws Exception {
        CustomLBMech.register("testmech",TestCustomLBMech.class);
        ImapLoadBalancingMechanism mech = CustomLBMech.loadCustomLBMech("custom:testmech foo bar");
        assertNotNull("Loaded mechanism should be a TestCustomLBMech", mech);
        assertTrue(String.format("Loaded mechanism '%s' should be a TestCustomLBMech",
                mech.getClass().getName()), (mech instanceof TestCustomLBMech));
        CustomLBMech customMech = (CustomLBMech) mech;
        assertEquals("Custom Mech arg[0]", customMech.args.get(0), "foo");
        assertEquals("Custom Mech arg[1]", customMech.args.get(1), "bar");
        mech = CustomLBMech.loadCustomLBMech("custom:testmech");
        assertNotNull("2nd Loaded mechanism should be a TestCustomLBMech", mech);
        assertTrue(String.format("2nd Loaded mechanism '%s' should be a TestCustomLBMech",
                mech.getClass().getName()), (mech instanceof TestCustomLBMech));
        assertNull("Args for custom mech after 2nd load", ((CustomLBMech) mech).args);
        mech = CustomLBMech.loadCustomLBMech("custom:unregisteredmech");
        assertNotNull("3rd Loaded mechanism should be AccountIdHashMechanism", mech);
        assertTrue(String.format(
                "Loaded mechanism '%s' when configured bad custom mech should be AccountIdHashMechanism",
                mech.getClass().getName()), (mech instanceof AccountIdHashMechanism));
    }

    public static class TestCustomLBMech extends CustomLBMech {

        protected TestCustomLBMech(List<String> args) {
            super(args);
        }

        @Override
        public Server getImapServerFromPool(HttpServletRequest httpReq, String accountID,
                List<Server> pool) throws ServiceException {
            return null;
        }
    }
}
