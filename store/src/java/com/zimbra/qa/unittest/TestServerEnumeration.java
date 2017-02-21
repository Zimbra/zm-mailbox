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
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.service.admin.ModifyAccount;
import com.zimbra.cs.service.admin.ModifyCalendarResource;
import com.zimbra.soap.admin.message.GrantRightRequest;
import com.zimbra.soap.admin.message.GrantRightResponse;
import com.zimbra.soap.admin.message.ModifyAccountRequest;
import com.zimbra.soap.admin.message.ModifyCalendarResourceRequest;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.RightModifierInfo;
import com.zimbra.soap.type.GranteeType;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

public class TestServerEnumeration extends TestCase {
    private final static String MY_DOMAIN = "mydomain.com";
    private final static String DELEGATED_ADMIN_NAME = "delegated-admin@" + MY_DOMAIN;
    private final static String MY_USER = "user1@" + MY_DOMAIN;
    private final static String MY_CALRES = "calendar_resource1@" + MY_DOMAIN;
    private final static String NON_EXISTING_SERVER = "server-that-is.not";
    private Account domainAdmin = null;
    private SoapProvisioning adminSoapProv = null;
    private SoapProvisioning delegatedSoapProv = null;
    private Account myUser = null;
    private CalendarResource myCalRes = null;
    private Domain myDomain = null;

    @Before
    public void setUp() throws Exception {
        cleanup();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "Bart Simpson");
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        adminSoapProv = TestUtil.newSoapProvisioning();
        myDomain = TestJaxbProvisioning.ensureDomainExists(MY_DOMAIN);
        myUser = adminSoapProv.createAccount(MY_USER, TestUtil.DEFAULT_PASSWORD, null);
        myCalRes = adminSoapProv.createCalendarResource(MY_CALRES, TestUtil.DEFAULT_PASSWORD, attrs);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() throws Exception {
        if (domainAdmin != null) {
            domainAdmin.deleteAccount();
        }
        TestJaxbProvisioning.deleteAccountIfExists(MY_USER);
        TestJaxbProvisioning.deleteCalendarResourceIfExists(MY_CALRES);
        TestJaxbProvisioning.deleteDomainIfExists(MY_DOMAIN);
    }

    @Test
    public void testModifyAccount() throws Exception {
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        AdminDocumentHandler handler = new ModifyAccount();
        handler.docRights(relatedRights, notes);
        createDelegatedAdmin(relatedRights);
        grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.account
                        .toString()), MY_USER, DELEGATED_ADMIN_NAME, Admin.R_modifyAccount.getName());

        adminSoapProv.flushCache(CacheEntryType.acl, null);
        ModifyAccountRequest req = new ModifyAccountRequest(myUser.getId());
        req.addAttr(new Attr(Provisioning.A_zimbraMailHost, NON_EXISTING_SERVER));
        req.addAttr(new Attr(Provisioning.A_description, "test description"));
        try {
            delegatedSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'Permission Denied' response", ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void testModifyAccountSufficientPermissions() throws Exception {
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        AdminDocumentHandler handler = new ModifyAccount();
        handler.docRights(relatedRights, notes);
        createDelegatedAdmin(relatedRights);
        grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.account
                        .toString()), MY_USER, DELEGATED_ADMIN_NAME, Admin.R_modifyAccount.getName());
        grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.global
                        .toString()), null, DELEGATED_ADMIN_NAME, Admin.R_listServer.getName());
        adminSoapProv.flushCache(CacheEntryType.acl, null);
        ModifyAccountRequest req = new ModifyAccountRequest(myUser.getId());
        req.addAttr(new Attr(Provisioning.A_zimbraMailHost, NON_EXISTING_SERVER));
        req.addAttr(new Attr(Provisioning.A_description, "test description"));
        try {
            delegatedSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'no such server' response", AccountServiceException.NO_SUCH_SERVER,
                    e.getCode());
        }
    }

    @Test
    public void testModifyAccountAsGlobalAdmin() throws Exception {
        ModifyAccountRequest req = new ModifyAccountRequest(myUser.getId());
        req.addAttr(new Attr(Provisioning.A_zimbraMailHost, NON_EXISTING_SERVER));
        req.addAttr(new Attr(Provisioning.A_description, "test description"));
        try {
            adminSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'no such server' response", AccountServiceException.NO_SUCH_SERVER,
                    e.getCode());
        }
    }

    @Test
    public void testModifyCalres() throws Exception {
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        AdminDocumentHandler handler = new ModifyCalendarResource();
        handler.docRights(relatedRights, notes);
        createDelegatedAdmin(relatedRights);
        grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.calresource
                        .toString()), MY_CALRES, DELEGATED_ADMIN_NAME, Admin.R_modifyCalendarResource.getName());

        adminSoapProv.flushCache(CacheEntryType.acl, null);
        ModifyCalendarResourceRequest req = new ModifyCalendarResourceRequest(myCalRes.getId());
        req.addAttr(new Attr(Provisioning.A_zimbraMailHost, NON_EXISTING_SERVER));
        req.addAttr(new Attr(Provisioning.A_description, "test description"));
        try {
            delegatedSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'Permission Denied' response", ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void testModifyCalresSufficientPermissions() throws Exception {
        List<AdminRight> relatedRights = new ArrayList<AdminRight>();
        List<String> notes = new ArrayList<String>();
        AdminDocumentHandler handler = new ModifyCalendarResource();
        handler.docRights(relatedRights, notes);
        createDelegatedAdmin(relatedRights);
        grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.calresource
                        .toString()), MY_CALRES, DELEGATED_ADMIN_NAME, Admin.R_modifyCalendarResource.getName());
        grantRightToAdmin(adminSoapProv,
                com.zimbra.soap.type.TargetType.fromString(com.zimbra.cs.account.accesscontrol.TargetType.global
                        .toString()), null, DELEGATED_ADMIN_NAME, Admin.R_listServer.getName());
        adminSoapProv.flushCache(CacheEntryType.acl, null);
        ModifyCalendarResourceRequest req = new ModifyCalendarResourceRequest(myCalRes.getId());
        req.addAttr(new Attr(Provisioning.A_zimbraMailHost, NON_EXISTING_SERVER));
        req.addAttr(new Attr(Provisioning.A_description, "test description"));
        try {
            delegatedSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'no such server' response", AccountServiceException.NO_SUCH_SERVER,
                    e.getCode());
        }
    }

    @Test
    public void testModifyCalresAsGlobalAdmin() throws Exception {
        ModifyCalendarResourceRequest req = new ModifyCalendarResourceRequest(myCalRes.getId());
        req.addAttr(new Attr(Provisioning.A_zimbraMailHost, NON_EXISTING_SERVER));
        req.addAttr(new Attr(Provisioning.A_description, "test description"));
        try {
            adminSoapProv.invokeJaxb(req);
            fail("should have caught an exception");
        } catch (SoapFaultException e) {
            assertEquals("should be getting 'no such server' response", AccountServiceException.NO_SUCH_SERVER,
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
            } else if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.account) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.account;
                target = MY_USER;
            } else if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.calresource) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.calresource;
                target = MY_CALRES;
            } else if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.server) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.server;
                target = Provisioning.getInstance().getLocalServer().getName();
            } else if (r.getTargetType() == com.zimbra.cs.account.accesscontrol.TargetType.cos) {
                targetType = com.zimbra.cs.account.accesscontrol.TargetType.cos;
                target = Provisioning.getInstance().getDefaultCOS(myDomain).getName();
            }
            if (targetType != null) {
                grantRightToAdmin(adminSoapProv, com.zimbra.soap.type.TargetType.fromString(targetType.toString()),
                        target, DELEGATED_ADMIN_NAME, r.getName());
            }
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
