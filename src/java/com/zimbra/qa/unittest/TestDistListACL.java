/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 VMware, Inc.
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
package com.zimbra.qa.unittest;

import java.util.HashMap;

import junit.framework.TestCase;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.soap.type.TargetBy;

public class TestDistListACL extends TestCase {

    String listAddress;
    Provisioning prov;
    AccessManager accessMgr;

    @Override
    public void setUp() throws Exception {

        listAddress = "testdistlistacl@" + TestUtil.getDomain();
        prov = Provisioning.getInstance();
        accessMgr = AccessManager.getInstance();
        tearDown();
    }

    @Override
    public void tearDown() throws Exception {

        DistributionList dl = prov.get(DistributionListBy.name, listAddress);
        if (dl != null) {
            prov.deleteDistributionList(dl.getId());
        }
    }

    /**
     * "gst" GranteeType testing.
     * Sender must match the configured guest email address.  The secret is ignored!
     */
    public void testMilterGuest() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_GUEST.getCode(), Key.GranteeBy.name,
                "fred@example.test", "" /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        assertTrue("fred@example.test should be able to send to DL",
                accessMgr.canDo("fred@example.test", group, User.R_sendToDistList, false));
        assertFalse("pete@example.test should NOT be able to send to DL",
                accessMgr.canDo("pete@example.test", group, User.R_sendToDistList, false));
    }

    /**
     * "dom" GranteeType testing.
     * Sender must exist and be in the domain that is allowed to send to the DL
     */
    public void testMilterDomain() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress("user1");
        Account user1account = TestUtil.getAccount("user1");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_DOMAIN.getCode(), Key.GranteeBy.name,
                user1account.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        assertTrue(String.format("%s should be able to send to DL (in domain %s)",
                user1email, user1account.getDomainName()),
                accessMgr.canDo(user1email, group, User.R_sendToDistList, false));
        assertFalse("pete@example.test should NOT be able to send to DL (in domain example.test)",
                accessMgr.canDo("pete@example.test", group, User.R_sendToDistList, false));
    }

    /**
     * "edom" GranteeType testing.  Check that a sender whose address has a domain which matches the
     * external domain will be able to send to the DL
     */
    public void testMilterExternalDomain() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress("user1");
        Account user1account = TestUtil.getAccount("user1");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EXT_DOMAIN.getCode(), Key.GranteeBy.name,
                "example.test", null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        assertTrue("pete@example.test should be able to send to DL (in domain example.test)",
                accessMgr.canDo("pete@example.test", group, User.R_sendToDistList, false));
        assertFalse(String.format("%s should NOT be able to send to DL (in domain %s)",
                user1email, user1account.getDomainName()),
                accessMgr.canDo(user1email, group, User.R_sendToDistList, false));
    }

    /**
     * "edom" GranteeType testing.
     * Addresses for local domains will also match right for "edom" GranteeType
     * (if we decide we don't want this, just testing for a guest account in ZimbraACE won't be sufficient,
     * we will need to make sure that the external domain isn't a local domain.
     */
    public void testMilterEdomWithLocalDomain() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress("user1");
        Account user1account = TestUtil.getAccount("user1");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EXT_DOMAIN.getCode(), Key.GranteeBy.name,
                user1account.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        assertTrue(String.format("%s should be able to send to DL (in domain %s)",
                user1email, user1account.getDomainName()),
                accessMgr.canDo(user1email, group, User.R_sendToDistList, false));
        String badName = "unconfigured@" + user1account.getDomainName();
        assertTrue(String.format("%s should be able to send to DL (in domain %s)",
                badName, user1account.getDomainName()),
                accessMgr.canDo(badName, group, User.R_sendToDistList, false));
    }

    public static void main(String[] args) throws ServiceException {

        com.zimbra.cs.db.DbPool.startup();
        com.zimbra.cs.memcached.MemcachedConnector.startup();

        CliUtil.toolSetup();
        TestUtil.runTest(TestDistListACL.class);
    }
}
