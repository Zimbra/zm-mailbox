/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import org.junit.Ignore;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class SharedFolderTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs;
        attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("owner@zimbra.com", "secret", attrs);
        attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("grantee1@zimbra.com", "secret", attrs);
        attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("grantee2@zimbra.com", "secret", attrs);
        attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraIsExternalVirtualAccount, "TRUE");
        prov.createAccount("virtual_grantee1@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void adminGrant() throws Exception {
        Account owner, grantee, virtualGrantee;
        Provisioning prov = Provisioning.getInstance();
        owner = prov.get(Key.AccountBy.name, "owner@zimbra.com");
        grantee = prov.get(Key.AccountBy.name, "grantee1@zimbra.com");
        virtualGrantee = prov.get(Key.AccountBy.name, "virtual_grantee1@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(owner.getId());

        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setDefaultView(MailItem.Type.MESSAGE);
        Folder f1 = mbox.createFolder(null, "/Inbox/f1", fopt);

        String exception;

        // allow granting admin right to internal user
        try {
            exception = null;
            mbox.grantAccess(null, f1.getId(), grantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // don't allow granting admin right to virtual account
        try {
            exception = null;
            mbox.grantAccess(null, f1.getId(), virtualGrantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", MailServiceException.CANNOT_GRANT, exception);

        // don't allow granting admin right to public
        try {
            exception = null;
            mbox.grantAccess(null, f1.getId(), null, ACL.GRANTEE_PUBLIC, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", MailServiceException.CANNOT_GRANT, exception);

        // allow granting non-admin rights to virtual account
        try {
            exception = null;
            mbox.grantAccess(null, f1.getId(), virtualGrantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidx"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // allow granting non-admin rights to public
        try {
            exception = null;
            mbox.grantAccess(null, f1.getId(), null, ACL.GRANTEE_PUBLIC, ACL.stringToRights("rwidx"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);
    }
}
