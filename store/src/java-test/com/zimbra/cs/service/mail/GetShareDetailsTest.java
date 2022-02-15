/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

@Ignore("ZCS-9144 - Please restore when redis is setup on Circleci")
public class GetShareDetailsTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_displayName, "Test User 2");
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraIsExternalVirtualAccount, "TRUE");
        prov.createAccount("virtual@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void nonexistent() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Account acct2 = Provisioning.getInstance().getAccountByName("test2@zimbra.com");

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, acct.getId() + ":" + 700);
        try {
            new GetShareDetails().handle(request, ServiceTestUtil.getRequestContext(acct2, acct));
            Assert.fail("did not throw exception on nonexistent item");
        } catch (ServiceException e) {
            Assert.assertEquals("nonexistent items should give PERM_DENIED", ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void denied() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Account acct2 = Provisioning.getInstance().getAccountByName("test2@zimbra.com");

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, acct.getId() + ":" + Mailbox.ID_FOLDER_BRIEFCASE);
        try {
            new GetShareDetails().handle(request, ServiceTestUtil.getRequestContext(acct2, acct));
            Assert.fail("did not throw exception on non-visible item");
        } catch (ServiceException e) {
            Assert.assertEquals("nonexistent items should give PERM_DENIED", ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void noacl() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, acct.getId() + ":" + Mailbox.ID_FOLDER_BRIEFCASE);
        Element response = new GetShareDetails().handle(request, ServiceTestUtil.getRequestContext(acct));

        Element item = response.getElement(MailConstants.E_ITEM);
        Assert.assertEquals("item ID normalized", "" + Mailbox.ID_FOLDER_BRIEFCASE, item.getAttribute(MailConstants.A_ID));
        Assert.assertEquals("no grants", 0, item.listElements(MailConstants.E_GRANTEE).size());
    }

    @Test
    public void visible() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Account acct2 = Provisioning.getInstance().getAccountByName("test2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.grantAccess(null, Mailbox.ID_FOLDER_BRIEFCASE, acct2.getId(), ACL.GRANTEE_USER, ACL.RIGHT_READ, null);

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, acct.getId() + ":" + Mailbox.ID_FOLDER_BRIEFCASE);
        Element response = new GetShareDetails().handle(request, ServiceTestUtil.getRequestContext(acct2, acct));

        List<Element> grants = response.getElement(MailConstants.E_ITEM).listElements(MailConstants.E_GRANTEE);
        Assert.assertEquals("1 grant", 1, grants.size());
        Element grant = grants.get(0);
        Assert.assertEquals("read perm granted", "r", grant.getAttribute(MailConstants.A_RIGHTS));
        Assert.assertEquals("grant type is user", "usr", grant.getAttribute(MailConstants.A_GRANT_TYPE));
        Assert.assertEquals("granted to test2", "test2@zimbra.com", grant.getAttribute(MailConstants.A_EMAIL));
        Assert.assertEquals("granted to Test User 2", "Test User 2", grant.getAttribute(MailConstants.A_NAME));
    }

    @Test
    public void pubshare() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Account acct2 = Provisioning.getInstance().getAccountByName("test2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.grantAccess(null, Mailbox.ID_FOLDER_BRIEFCASE, null, ACL.GRANTEE_PUBLIC, ACL.RIGHT_READ, null);

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, acct.getId() + ":" + Mailbox.ID_FOLDER_BRIEFCASE);
        Element response = new GetShareDetails().handle(request, ServiceTestUtil.getRequestContext(acct2, acct));

        List<Element> grants = response.getElement(MailConstants.E_ITEM).listElements(MailConstants.E_GRANTEE);
        Assert.assertEquals("1 grant", 1, grants.size());
        Element grant = grants.get(0);
        Assert.assertEquals("read perm granted", "r", grant.getAttribute(MailConstants.A_RIGHTS));
        Assert.assertEquals("grant type is public", "pub", grant.getAttribute(MailConstants.A_GRANT_TYPE));
        Assert.assertEquals("no email set", null, grant.getAttribute(MailConstants.A_EMAIL, null));
        Assert.assertEquals("no name set", null, grant.getAttribute(MailConstants.A_NAME, null));
    }

    @Test
    public void external() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.grantAccess(null, Mailbox.ID_FOLDER_BRIEFCASE, null, ACL.GRANTEE_PUBLIC, ACL.RIGHT_READ, null);

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, acct.getId() + ":" + Mailbox.ID_FOLDER_BRIEFCASE);
        try {
            new GetShareDetails().handle(request, ServiceTestUtil.getExternalRequestContext("external@domain.com", acct));
            Assert.fail("did not throw exception on external user");
        } catch (ServiceException e) {
            Assert.assertEquals("external users should give PERM_DENIED", ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void mountpoint() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Account acct2 = Provisioning.getInstance().getAccountByName("test2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.grantAccess(null, Mailbox.ID_FOLDER_BRIEFCASE, acct2.getId(), ACL.GRANTEE_USER, ACL.RIGHT_READ, null);

        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);
        int mptId = mbox2.createMountpoint(null, Mailbox.ID_FOLDER_BRIEFCASE, "link", acct.getId(), Mailbox.ID_FOLDER_BRIEFCASE, null, MailItem.Type.DOCUMENT, 0, (byte) 0, false).getId();

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, mptId);
        Element response = new GetShareDetails().proxyIfNecessary(request, ServiceTestUtil.getRequestContext(acct2, acct2, new MailService()));

        List<Element> grants = response.getElement(MailConstants.E_ITEM).listElements(MailConstants.E_GRANTEE);
        Assert.assertEquals("1 grant", 1, grants.size());
        Element grant = grants.get(0);
        Assert.assertEquals("read perm granted", "r", grant.getAttribute(MailConstants.A_RIGHTS));
        Assert.assertEquals("grant type is user", "usr", grant.getAttribute(MailConstants.A_GRANT_TYPE));
        Assert.assertEquals("granted to test2", "test2@zimbra.com", grant.getAttribute(MailConstants.A_EMAIL));
        Assert.assertEquals("granted to Test User 2", "Test User 2", grant.getAttribute(MailConstants.A_NAME));
    }

    @Test
    public void virtual() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Account virtual = Provisioning.getInstance().getAccountByName("virtual@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.grantAccess(null, Mailbox.ID_FOLDER_BRIEFCASE, virtual.getId(), ACL.GRANTEE_USER, ACL.RIGHT_READ, null);

        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(virtual);
        int mptId = mbox2.createMountpoint(null, Mailbox.ID_FOLDER_BRIEFCASE, "link", acct.getId(), Mailbox.ID_FOLDER_BRIEFCASE, null, MailItem.Type.DOCUMENT, 0, (byte) 0, false).getId();

        Element request = new Element.XMLElement(OctopusXmlConstants.GET_SHARE_DETAILS_REQUEST);
        request.addUniqueElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, mptId);
        try {
            new GetShareDetails().proxyIfNecessary(request, ServiceTestUtil.getRequestContext(virtual, virtual, new MailService()));
            Assert.fail("did not throw exception on external virtual user");
        } catch (SoapFaultException e) {
            Assert.assertEquals("external virtual users should give PERM_DENIED", ServiceException.PERM_DENIED, e.getCode());
        }
    }
}