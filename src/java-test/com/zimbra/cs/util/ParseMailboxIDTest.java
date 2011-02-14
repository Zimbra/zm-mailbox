/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.util.ParseMailboxID;

/**
 * Unit test for {@link ParseMailboxID}.
 *
 * @author ysasaki
 */
public class ParseMailboxIDTest {

    @BeforeClass
    public static void init() throws Exception {
        LC.zimbra_class_provisioning.setDefault(MockProvisioning.class.getName());
        MockProvisioning prov = (MockProvisioning) Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-0");
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Test
    public void parseLocalMailboxId() throws Exception {
        ParseMailboxID id = ParseMailboxID.parse("1");
        Assert.assertTrue(id.isLocal());
        Assert.assertNull(id.getServer());
        Assert.assertEquals(1, id.getMailboxId());
        Assert.assertFalse(id.isAllMailboxIds());
        Assert.assertFalse(id.isAllServers());
        Assert.assertNull(id.getAccount());
    }

    @Test
    public void parseEmail() throws Exception {
        ParseMailboxID id = ParseMailboxID.parse("test@zimbra.com");
        Assert.assertTrue(id.isLocal());
        Assert.assertEquals("localhost", id.getServer());
        Assert.assertEquals(0, id.getMailboxId());
        Assert.assertFalse(id.isAllMailboxIds());
        Assert.assertFalse(id.isAllServers());
        Assert.assertEquals(Provisioning.getInstance().getAccountByName("test@zimbra.com"), id.getAccount());
    }

    @Test
    public void parseAccountId() throws Exception {
        ParseMailboxID id = ParseMailboxID.parse("0-0-0");
        Assert.assertTrue(id.isLocal());
        Assert.assertEquals("localhost", id.getServer());
        Assert.assertEquals(0, id.getMailboxId());
        Assert.assertFalse(id.isAllMailboxIds());
        Assert.assertFalse(id.isAllServers());
        Assert.assertEquals(Provisioning.getInstance().getAccountByName("test@zimbra.com"), id.getAccount());
    }

    @Test
    public void parseMailboxId() throws Exception {
        ParseMailboxID id = ParseMailboxID.parse("/localhost/1");
        Assert.assertTrue(id.isLocal());
        Assert.assertEquals("localhost", id.getServer());
        Assert.assertEquals(1, id.getMailboxId());
        Assert.assertFalse(id.isAllMailboxIds());
        Assert.assertFalse(id.isAllServers());
        Assert.assertNull(id.getAccount());

        try {
            ParseMailboxID.parse("localhost*/3");
            Assert.fail();
        } catch (ServiceException expected) {
        }
    }

    @Test
    public void parseAllServers() throws Exception {
        ParseMailboxID id = ParseMailboxID.parse("*");
        Assert.assertFalse(id.isLocal());
        Assert.assertEquals("*", id.getServer());
        Assert.assertEquals(0, id.getMailboxId());
        Assert.assertTrue(id.isAllMailboxIds());
        Assert.assertTrue(id.isAllServers());
        Assert.assertNull(id.getAccount());
    }

    @Test
    public void parseAllMailboxes() throws Exception {
        ParseMailboxID id = ParseMailboxID.parse("/localhost/*");
        Assert.assertTrue(id.isLocal());
        Assert.assertEquals("localhost", id.getServer());
        Assert.assertEquals(0, id.getMailboxId());
        Assert.assertTrue(id.isAllMailboxIds());
        Assert.assertFalse(id.isAllServers());
        Assert.assertNull(id.getAccount());
    }

}
