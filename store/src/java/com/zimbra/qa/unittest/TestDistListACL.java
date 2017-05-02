/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

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

public class TestDistListACL {
    @Rule
    public TestName testInfo = new TestName();
    private static String USER_NAME = null;
    private static String USER_NAME2 = null;
    private static final String NAME_PREFIX = TestStoreManager.class.getSimpleName();

    private static final String otherDomain = "other.example.test";
    private static String listAddress;
    private static String listAddress2;
    private static String auser;
    private static String alias;
    private static String dlalias;
    private static Provisioning prov;
    private static AccessManager accessMgr;

    @Before
    public void setUp() throws Exception {

        listAddress = String.format("testdistlistacl@%s", TestUtil.getDomain());
        listAddress2 = String.format("testDLacl2@%s", TestUtil.getDomain());
        auser = String.format("userWithAlias@%s", TestUtil.getDomain());
        alias = String.format("alias@%s", otherDomain);
        dlalias = String.format("dlalias@%s", otherDomain);
        prov = Provisioning.getInstance();
        accessMgr = AccessManager.getInstance();

        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user1";
        USER_NAME2 = prefix + "user2";
        tearDown();
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(USER_NAME2);
    }

    @After
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
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(USER_NAME2);
    }

    private void doCheckSentToDistListGuestRight(DistributionList targetDl, String email, String guest,
            boolean expected)
    throws ServiceException {
        ZimbraLog.test.info("DL name %s ID %s", targetDl.getName(), targetDl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        Assert.assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_GUEST, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            Assert.assertTrue(String.format("%s should be able to send to DL (as guest %s)", email, guest),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertTrue(String.format("%s should have right to send to DL (as guest %s)", email, guest),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, guest, via);
        } else {
            Assert.assertFalse(String.format("%s should NOT be able to send to DL (because not guest %s)",
                    email, guest), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertFalse(String.format("%s should NOT have right to send to DL (because not guest %s)",
                    email, guest), result);
        }
    }

    /**
     * "gst" GranteeType testing.
     * Sender must match the configured guest email address.  The secret is ignored!
     */
    @Test
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
        Assert.assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            Assert.assertTrue(String.format("%s should be able to send to DL (using email %s)", email, grantEmail),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertTrue(String.format("%s should have right to send to DL (using email %s)", email, grantEmail),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, grantEmail, via);
        } else {
            Assert.assertFalse(String.format("%s should NOT be able to send to DL (because not email %s)",
                    email, grantEmail), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertFalse(String.format("%s should NOT have right to send to DL (because not email %s)",
                    email, grantEmail), result);
        }
    }

    /**
     * "email" GranteeType testing.
     * Sender must match the configured email address - address can be internal, dl, guest etc
     */
    @Test
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
        String user1email = TestUtil.getAddress(USER_NAME);
        String user2email = TestUtil.getAddress(USER_NAME2);
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
        Assert.assertNotNull("Unable to find Group object for DL by name", group);
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
            Assert.assertTrue(String.format("%s should be able to send to DL (as user %s)", email, user),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertTrue(String.format("%s should have right to send to DL (as user %s)", email, user),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, user, via);
        } else {
            Assert.assertFalse(String.format("%s should NOT be able to send to DL (because not user %s)",
                    email, user), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertFalse(String.format("%s should NOT have right to send to DL (because not user %s)",
                    email, user), result);
        }
    }

    /**
     * "usr" GranteeType testing.
     * Sender must match the configured user email address.
     */
    @Test
    public void testMilterUserSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress(USER_NAME);
        String user2email = TestUtil.getAddress(USER_NAME2);
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
        Assert.assertNotNull("Unable to find Group object for DL by name", group);
        AccessManager.ViaGrant via = new AccessManager.ViaGrant();
        NamedEntry ne = GranteeType.lookupGrantee(prov, GranteeType.GT_EMAIL, GranteeBy.name, email);
        MailTarget grantee = null;
        if (ne instanceof MailTarget) {
            grantee = (MailTarget) ne;
        }
        boolean result = RightCommand.checkRight(prov, "dl" /* targetType */, TargetBy.name,
                listAddress, grantee, RightConsts.RT_sendToDistList, null /* attrs */, via);
        if (expected) {
            Assert.assertTrue(String.format("%s should be able to send to DL (because in domain %s)", email, grantDomain),
                    accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertTrue(String.format("%s should have right to send to DL (because in domain %s)", email, grantDomain),
                    result);
            ZimbraLog.test.info("Test for %s against dom %s Via=%s", email, grantDomain, via);
        } else {
            Assert.assertFalse(String.format("%s should NOT be able to send to DL (because not in domain %s)",
                    email, grantDomain), accessMgr.canDo(email, group, User.R_sendToDistList, false));
            Assert.assertFalse(String.format("%s should NOT have right to send to DL (because not in domain %s)",
                    email, grantDomain), result);
        }
    }

    /**
     * "dom" GranteeType testing.
     * Sender must exist and be in the domain that is allowed to send to the DL
     */
    @Test
    public void testMilterDomainSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress(USER_NAME);
        Account user1account = TestUtil.getAccount(USER_NAME);
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
    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testMilterExternalDomainSendToDL() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress(USER_NAME);
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EXT_DOMAIN.getCode(), GranteeBy.name,
                "example.test", null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        Assert.assertNotNull("Unable to find Group object for DL by name", group);
        Assert.assertTrue("pete@example.test should be able to send to DL (in domain example.test)",
                accessMgr.canDo("pete@example.test", group, User.R_sendToDistList, false));
        Assert.assertFalse(String.format("%s should NOT be able to send to DL (in domain example.test)", user1email),
                accessMgr.canDo(user1email, group, User.R_sendToDistList, false));
    }

    /**
     * "edom" GranteeType testing.
     * Addresses for local domains will also match right for "edom" GranteeType
     * (if we decide we don't want this, just testing for a guest account in ZimbraACE won't be sufficient,
     * we will need to make sure that the external domain isn't a local domain.
     */
    @Test
    public void testMilterEdomWithLocalDomain() throws Exception {
        DistributionList dl = prov.createDistributionList(listAddress, new HashMap<String, Object>());
        String user1email = TestUtil.getAddress(USER_NAME);
        Account user1account = TestUtil.getAccount(USER_NAME);
        prov.grantRight("dl", TargetBy.name, listAddress, GranteeType.GT_EXT_DOMAIN.getCode(), GranteeBy.name,
                user1account.getDomainName(), null /* secret */,
                RightConsts.RT_sendToDistList, (RightModifier) null /* rightModifier */);
        ZimbraLog.test.info("DL name %s ID %s", dl.getName(), dl.getId());
        Group group = prov.getGroupBasic(Key.DistributionListBy.name, listAddress);
        Assert.assertNotNull("Unable to find Group object for DL by name", group);
        Assert.assertTrue(String.format("%s should be able to send to DL (in domain %s)",
                user1email, user1account.getDomainName()),
                accessMgr.canDo(user1email, group, User.R_sendToDistList, false));
        String badName = "unconfigured@" + user1account.getDomainName();
        Assert.assertTrue(String.format("%s should be able to send to DL (in domain %s)",
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
