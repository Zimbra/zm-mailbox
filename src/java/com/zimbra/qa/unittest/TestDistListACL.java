/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Locale;

import junit.framework.TestCase;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;

public class TestDistListACL extends TestCase {

    final String otherDomain = "other.example.test";
    String listAddress;
    String listAddress2;
    String auser;
    String alias;
    String dlalias;
    Provisioning prov;
    AccessManager accessMgr;

    @Override
    public void setUp() throws Exception {

        listAddress = String.format("testdistlistacl@%s", TestUtil.getDomain());
        listAddress2 = String.format("testDLacl2@%s", TestUtil.getDomain());
        auser = String.format("userWithAlias@%s", TestUtil.getDomain());
        alias = String.format("alias@%s", otherDomain);
        dlalias = String.format("dlalias@%s", otherDomain);
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
        dl = prov.get(DistributionListBy.name, listAddress2);
        if (dl != null) {
            prov.deleteDistributionList(dl.getId());
        }
        Account acct = prov.get(AccountBy.name, auser);
        if (acct != null) {
            prov.deleteAccount(acct.getId());
        }
        Domain domain  = prov.get(DomainBy.name, otherDomain);
        if (domain != null) {
            prov.deleteDomain(domain.getId());
        }
    }

    private void doCheckSentToDistListGuestRight(DistributionList targetDl, String email, String guest,
            boolean expected)
    throws ServiceException {
        ZimbraLog.test.info("DL name %s ID %s", targetDl.getName(), targetDl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_GUEST, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            assertTrue(String.format("%s should be able to send to DL (as guest %s)", email, guest),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertTrue(String.format("%s should have right to send to DL (as guest %s)", email, guest),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, guest, via);
        } else {
            assertFalse(String.format("%s should NOT be able to send to DL (because not guest %s)",
                    email, guest), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertFalse(String.format("%s should NOT have right to send to DL (because not guest %s)",
                    email, guest), result);
        }
    }

    /**
     * "gst" GranteeType testing.
     * Sender must match the configured guest email address.  The secret is ignored!
     */
    public void testMilterGuestSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String guestName = "fred@example.test";
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_GUEST.getCode(), GranteeBy.name,
                guestName, "" /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListGuestRight(dl, guestName, guestName, true);
        doCheckSentToDistListGuestRight(dl, "pete@example.test", guestName, false);
        // Bug 83252 case shouldn't matter
        doCheckSentToDistListGuestRight(dl, "FreD@example.test", guestName, true);
    }

    private void doCheckSentToDistListEmailRight(DistributionList targetDl, String email, String grantEmail,
            boolean expected)
    throws ServiceException {
        ZimbraLog.test.info("DL name %s ID %s", targetDl.getName(), targetDl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            assertTrue(String.format("%s should be able to send to DL (using email %s)", email, grantEmail),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertTrue(String.format("%s should have right to send to DL (using email %s)", email, grantEmail),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, grantEmail, via);
        } else {
            assertFalse(String.format("%s should NOT be able to send to DL (because not email %s)",
                    email, grantEmail), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertFalse(String.format("%s should NOT have right to send to DL (because not email %s)",
                    email, grantEmail), result);
        }
    }

    /**
     * "email" GranteeType testing.
     * Sender must match the configured email address - address can be internal, dl, guest etc
     */
    public void testMilterEmailSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String guestName = "fred@example.test";
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EMAIL.getCode(), GranteeBy.name,
                guestName, null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        prov.createDistributionList(listAddress2, new HashMap<String, Object>());
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EMAIL.getCode(), GranteeBy.name,
                listAddress2, null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        String user1email = TestUtil.getAddress("user1");
        String user2email = TestUtil.getAddress("user2");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EMAIL.getCode(), GranteeBy.name,
                user1email, null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListEmailRight(dl, guestName, guestName, true);
        doCheckSentToDistListEmailRight(dl, "pete@example.test", guestName, false);
        doCheckSentToDistListEmailRight(dl, "FreD@example.test", guestName, true);
        doCheckSentToDistListEmailRight(dl, listAddress2, listAddress2, true);
        doCheckSentToDistListEmailRight(dl, listAddress, listAddress2, false);
        doCheckSentToDistListEmailRight(dl, user1email.toUpperCase(Locale.ENGLISH), user1email, true);
        doCheckSentToDistListEmailRight(dl, user2email, user1email, false);
        prov.revokeRight("dl", TargetBy.name, listAddress, GranteeType.GT_EMAIL.getCode(), GranteeBy.name,
                guestName, RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        prov.revokeRight("dl", TargetBy.name, listAddress, GranteeType.GT_EMAIL.getCode(), GranteeBy.name,
                listAddress2, RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        prov.revokeRight("dl", TargetBy.name, listAddress, GranteeType.GT_EMAIL.getCode(), GranteeBy.name,
                user1email, RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListEmailRight(dl, user2email, "no grants in place", true);
    }

    private void doCheckSentToDistListUserRight(DistributionList targetDl, String email, String user,
            boolean expected)
    throws ServiceException {
        ZimbraLog.test.info("DL name %s ID %s", targetDl.getName(), targetDl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        //  More permissive that GT_USER - want to test called functions
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            assertTrue(String.format("%s should be able to send to DL (as user %s)", email, user),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertTrue(String.format("%s should have right to send to DL (as user %s)", email, user),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, user, via);
        } else {
            assertFalse(String.format("%s should NOT be able to send to DL (because not user %s)",
                    email, user), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertFalse(String.format("%s should NOT have right to send to DL (because not user %s)",
                    email, user), result);
        }
    }

    /**
     * "usr" GranteeType testing.
     * Sender must match the configured user email address.
     */
    public void testMilterUserSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress("user1");
        String user2email = TestUtil.getAddress("user2");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_USER.getCode(), GranteeBy.name,
                user1email, null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListUserRight(dl, user1email, user1email, true);
        doCheckSentToDistListUserRight(dl, "pete@example.test", user1email, false);
        doCheckSentToDistListUserRight(dl, user2email, user1email, false);
        doCheckSentToDistListUserRight(dl, user1email.toUpperCase(Locale.ENGLISH), user1email, true);
    }

    private void doCheckSentToDistListDomRight(DistributionList targetDl, String email, String grantDomain, boolean expected)
    throws ServiceException {
        ZimbraLog.test.info("DL name %s ID %s", targetDl.getName(), targetDl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            assertTrue(String.format("%s should be able to send to DL (because in domain %s)", email, grantDomain),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertTrue(String.format("%s should have right to send to DL (because in domain %s)", email, grantDomain),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, grantDomain, via);
        } else {
            assertFalse(String.format("%s should NOT be able to send to DL (because not in domain %s)",
                    email, grantDomain), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            assertFalse(String.format("%s should NOT have right to send to DL (because not in domain %s)",
                    email, grantDomain), result);
        }
    }

    /**
     * "dom" GranteeType testing.
     * Sender must exist and be in the domain that is allowed to send to the DL
     */
    public void testMilterDomainSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress("user1");
        Account user1account = TestUtil.getAccount("user1");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_DOMAIN.getCode(), GranteeBy.name,
                user1account.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListDomRight(dl, user1email, user1account.getDomainName(), true);
        doCheckSentToDistListDomRight(dl, "pete@example.test", user1account.getDomainName(), false);
        doCheckSentToDistListDomRight(dl, user1email.toUpperCase(Locale.ENGLISH), user1account.getDomainName(), true);
    }

    /**
     * "dom" GranteeType testing.
     * Sender must exist and be in the domain that is allowed to send to the DL
     * Note that currently, an alias address is resolved to the associated account before the domain
     * check is done which might or might not be the best approach.
     */
    public void testMilterDomainSendToDLWithAcctAliasSender() throws Exception {
        prov.createDomain(otherDomain, new HashMap<String, Object>());
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        Account acct = prov.createAccount(auser, "test123", new HashMap<String, Object>());
        prov.addAlias(acct, alias);
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_DOMAIN.getCode(), GranteeBy.name,
                acct.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListDomRight(dl, alias, acct.getDomainName(), true);
    }
    /**
     * "dom" GranteeType testing.
     * Sender must exist and be in the domain that is allowed to send to the DL - Sender MAY be a DL
     */
    public void testMilterDomainSendToDLWithDlSender() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        DistributionList dl2 = prov.createDistributionList(listAddress2, new HashMap<String, Object>());
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_DOMAIN.getCode(), GranteeBy.name,
                dl2.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListDomRight(dl, listAddress2, dl2.getDomainName(), true);
    }

    /**
     * "dom" GranteeType testing.
     * Note that currently, an alias address is resolved to the associated account before the domain
     * check is done which might or might not be the best approach.
     */
    public void testMilterDomainSendToDLWithDlAliasSender() throws Exception {
        prov.createDomain(otherDomain, new HashMap<String, Object>());
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        DistributionList dl2 = prov.createDistributionList(listAddress2, new HashMap<String, Object>());
        prov.addAlias(dl2, dlalias);
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_DOMAIN.getCode(), GranteeBy.name,
                dl2.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        doCheckSentToDistListDomRight(dl, dlalias, dl2.getDomainName(), true);
    }

    /**
     * "edom" GranteeType testing.  Check that a sender whose address has a domain which matches the
     * external domain will be able to send to the DL
     */
    public void testMilterExternalDomainSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress("user1");
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EXT_DOMAIN.getCode(), GranteeBy.name,
                "example.test", null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        assertNotNull("Unable to find Group object for DL by name", group);
        assertTrue("pete@example.test should be able to send to DL (in domain example.test)",
                accessMgr.canDo("pete@example.test", group, User.R_sendToDistList, false));
        assertFalse(String.format("%s should NOT be able to send to DL (in domain example.test)", user1email),
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
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EXT_DOMAIN.getCode(), GranteeBy.name,
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
