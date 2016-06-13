/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.service.admin.LockoutMailbox;
import com.zimbra.soap.admin.message.GrantRightRequest;
import com.zimbra.soap.admin.message.GrantRightResponse;
import com.zimbra.soap.admin.message.LockoutMailboxRequest;
import com.zimbra.soap.admin.message.LockoutMailboxResponse;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.RightModifierInfo;
import com.zimbra.soap.type.AccountNameSelector;
import com.zimbra.soap.type.GranteeType;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

public class TestLockoutMailbox extends TestCase {
    private final static String MY_DOMAIN = "TestLockoutMailbox-mydomain.com";
    private final static String OFFLIMITS_DOMAIN = "offlimits.com";
    private final static String DELEGATED_ADMIN_NAME = "delegated-admin@" + MY_DOMAIN;
    private final static String MY_USER = "user1@" + MY_DOMAIN;
    private final static String MY_NON_EXISTING_USER = "user2@" + MY_DOMAIN;
    private final static String OFFLIMITS_NON_EXISTING_USER = "user2@" + OFFLIMITS_DOMAIN;
    private final static String OFFLIMITS_USER1 = "user1@" + OFFLIMITS_DOMAIN;

    private Account domainAdmin = null;
    private SoapProvisioning adminSoapProv = null;
    private SoapProvisioning delegatedSoapProv = null;

    @Before
    public void setUp() throws Exception {
        cleanup();
        adminSoapProv = TestUtil.newSoapProvisioning();
        TestJaxbProvisioning.ensureDomainExists(MY_DOMAIN);
        TestJaxbProvisioning.ensureDomainExists(OFFLIMITS_DOMAIN);
        adminSoapProv.createAccount(MY_USER, TestUtil.DEFAULT_PASSWORD, null);
        adminSoapProv.createAccount(OFFLIMITS_USER1, TestUtil.DEFAULT_PASSWORD, null);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() throws Exception {
        Account acct = TestUtil.getAccount(MY_USER);
        if (acct != null) {
            if (MailboxManager.getInstance().isMailboxLockedOut(acct.getId())) {
                MailboxManager.getInstance().undoLockout(acct.getId());
            }
            TestUtil.deleteAccount(MY_USER);
        }
        if (domainAdmin != null) {
            domainAdmin.deleteAccount();
        }
        TestJaxbProvisioning.deleteAccountIfExists(OFFLIMITS_USER1);
        TestJaxbProvisioning.deleteDomainIfExists(MY_DOMAIN);
        TestJaxbProvisioning.deleteDomainIfExists(OFFLIMITS_DOMAIN);
    }

    @Test
    public void testLockout() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(MY_USER);
        TestUtil.addMessage(mbox, "test");
        TestUtil.waitForMessage(TestUtil.getZMailbox(MY_USER), "test");
        assertFalse("mailbox should not be locked yet",
                MailboxManager.getInstance().isMailboxLockedOut(mbox.getAccountId()));
        LockoutMailboxRequest req = LockoutMailboxRequest.create(AccountNameSelector.fromName(MY_USER));
        req.setOperation(AdminConstants.A_START);
        LockoutMailboxResponse resp = adminSoapProv.invokeJaxb(req);
        assertNotNull("LockoutMailboxRequest return null response", resp);
        assertTrue("mailbox should be locked now", MailboxManager.getInstance().isMailboxLockedOut(mbox.getAccountId()));
    }

    @Test
    public void testUnlock() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(MY_USER);
        TestUtil.addMessage(mbox, "test");
        TestUtil.waitForMessage(TestUtil.getZMailbox(MY_USER), "test");
        assertFalse("mailbox should not be locked yet",
                MailboxManager.getInstance().isMailboxLockedOut(mbox.getAccountId()));
        MailboxManager.getInstance().lockoutMailbox(mbox.getAccountId());
        assertTrue("mailbox should be locked now", MailboxManager.getInstance().isMailboxLockedOut(mbox.getAccountId()));

        LockoutMailboxRequest req = LockoutMailboxRequest.create(AccountNameSelector.fromName(MY_USER));
        req.setOperation(AdminConstants.A_END);
        LockoutMailboxResponse resp = adminSoapProv.invokeJaxb(req);
        assertNotNull("LockoutMailboxRequest return null response", resp);
        assertFalse("mailbox should not be locked any more",
                MailboxManager.getInstance().isMailboxLockedOut(mbox.getAccountId()));
    }

    @Test
    public void testLockAccountEnumeration() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(MY_USER);
        TestUtil.addMessage(mbox, "test");
        TestUtil.waitForMessage(TestUtil.getZMailbox(MY_USER), "test");
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        AdminDocumentHandler handler = new LockoutMailbox();
        handler.docRights(relatedRights, notes);
        createDelegatedAdmin(relatedRights);
        LockoutMailboxRequest req = LockoutMailboxRequest.create(AccountNameSelector.fromName(OFFLIMITS_NON_EXISTING_USER));
        req.setOperation(AdminConstants.A_START);
        try {
            delegatedSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'Permission Denied' response", ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void testLockoutSufficientPermissions() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(MY_USER);
        TestUtil.addMessage(mbox, "test");
        TestUtil.waitForMessage(TestUtil.getZMailbox(MY_USER), "test");
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        AdminDocumentHandler handler = new LockoutMailbox();
        handler.docRights(relatedRights, notes);
        createDelegatedAdmin(relatedRights);
        LockoutMailboxRequest req = LockoutMailboxRequest.create(AccountNameSelector.fromName(MY_USER));
        req.setOperation(AdminConstants.A_START);
        try {
            LockoutMailboxResponse resp = delegatedSoapProv.invokeJaxb(req);
            assertNotNull("LockoutMailboxResponse should not be null", resp);
        } catch (SoapFaultException e) {
            fail("should not be getting an exception");
        }

        req = LockoutMailboxRequest.create(AccountNameSelector.fromName(MY_NON_EXISTING_USER));
        req.setOperation(AdminConstants.A_START);
        try {
            delegatedSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'no such account' response", AccountServiceException.NO_SUCH_ACCOUNT,
                    e.getCode());
        }
    }

    @Test
    public void testLockoutAsGlobalAdmin() throws Exception {
        Mailbox mbox = TestUtil.getMailbox(MY_USER);
        TestUtil.addMessage(mbox, "test");
        TestUtil.waitForMessage(TestUtil.getZMailbox(MY_USER), "test");
        LockoutMailboxRequest req = LockoutMailboxRequest.create(AccountNameSelector.fromName(MY_USER));
        req.setOperation(AdminConstants.A_START);
        try {
            LockoutMailboxResponse resp = adminSoapProv.invokeJaxb(req);
            assertNotNull("LockoutMailboxResponse should not be null", resp);
        } catch (SoapFaultException e) {
            fail("should not be getting an exception");
        }

        req = LockoutMailboxRequest.create(AccountNameSelector.fromName(OFFLIMITS_NON_EXISTING_USER));
        req.setOperation(AdminConstants.A_START);
        try {
            adminSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'no such account' response", AccountServiceException.NO_SUCH_ACCOUNT,
                    e.getCode());
        }
    }

    public void createDelegatedAdmin(List<AdminRight> relatedRights) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        StringUtil.addToMultiMap(attrs, Provisioning.A_zimbraIsDelegatedAdminAccount, LdapConstants.LDAP_TRUE);
        domainAdmin = adminSoapProv.createAccount(DELEGATED_ADMIN_NAME, TestUtil.DEFAULT_PASSWORD, attrs);
        assertNotNull("failed to create domin admin account", domainAdmin);
        for (AdminRight r : relatedRights) {
            String target = null;
            com.zimbra.cs.account.accesscontrol.TargetType targetType = null;
            if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.domain) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.domain;
                target = MY_DOMAIN;
            } else if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.account
                    || r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.calresource) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.domain;
                target = MY_DOMAIN;
            } else if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.server) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.server;
                target = Provisioning.getInstance().getLocalServer().getName();
            }
            grantRightToAdmin(adminSoapProv, com.zimbra.soap.type.TargetType.fromString(targetType.toString()), target,
                    DELEGATED_ADMIN_NAME, r.getName());
        }
        adminSoapProv.flushCache(CacheEntryType.acl, null);
        delegatedSoapProv = TestUtil.newDelegatedSoapProvisioning(DELEGATED_ADMIN_NAME, TestUtil.DEFAULT_PASSWORD);
    }

    private static void grantRightToAdmin(SoapProvisioning adminSoapProv, TargetType targetType, String targetName,
            String granteeName, String rightName) throws ServiceException {
        GranteeSelector grantee = new GranteeSelector(GranteeType.usr, GranteeBy.name, granteeName);
        EffectiveRightsTargetSelector target = null;
        if (targetName == null) {
            target = new EffectiveRightsTargetSelector(targetType, null, null);
        } else {
            target = new EffectiveRightsTargetSelector(targetType, TargetBy.name, targetName);
        }

        RightModifierInfo right = new RightModifierInfo(rightName);
        GrantRightResponse grResp = adminSoapProv.invokeJaxb(new GrantRightRequest(target, grantee, right));
        assertNotNull("GrantRightResponse for " + right.getValue(), grResp);
    }

}
