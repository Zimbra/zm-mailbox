/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class SharedFolderTest {

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

// comment out test for now
//    @Test
    public void nestedDocumentShares() throws Exception {
        Account owner, grantee1, grantee2;
        Provisioning prov = Provisioning.getInstance();
        owner = prov.get(Key.AccountBy.name, "owner@zimbra.com");
        grantee1 = prov.get(Key.AccountBy.name, "grantee1@zimbra.com");
        grantee2 = prov.get(Key.AccountBy.name, "grantee2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(owner.getId());

        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setDefaultView(MailItem.Type.DOCUMENT);

        // f1 is shared initially
        Folder f1 = mbox.createFolder(null, "/Briefcase/f1", fopt);
        mbox.grantAccess(null, f1.getId(), grantee1.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        Folder f1_f11 = mbox.createFolder(null, "/Briefcase/f1/f1.1", fopt);
        mbox.createFolder(null, "/Briefcase/f1/f1.1/f1.1.1", fopt);
        mbox.createFolder(null, "/Briefcase/f1/f1.2", fopt);
        mbox.createFolder(null, "/Briefcase/f1/f1.2/f1.2.1", fopt);

        // f2/f2.1 is shared initially
        Folder f2 = mbox.createFolder(null, "/Briefcase/f2", fopt);
        Folder f2_f21 = mbox.createFolder(null, "/Briefcase/f2/f2.1", fopt);
        mbox.grantAccess(null, f2_f21.getId(), grantee1.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        mbox.createFolder(null, "/Briefcase/f2/f2.1/f2.1.1", fopt);
        Folder f2_f22 = mbox.createFolder(null, "/Briefcase/f2/f2.2", fopt);
        mbox.createFolder(null, "/Briefcase/f2/f2.2/f2.2.1", fopt);

        String exception;

        // allow creating a new share if there is no shared folder above or below
        try {
            exception = null;
            mbox.grantAccess(null, f2_f22.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // allow adding grantee to an existing share
        try {
            exception = null;
            mbox.grantAccess(null, f2_f21.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // must not allow creating a share under a share
        try {
            exception = null;
            mbox.grantAccess(null, f1_f11.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", MailServiceException.CANNOT_NEST_SHARES, exception);

        // must not allow sharing a folder that contains a share underneath
        try {
            exception = null;
            mbox.grantAccess(null, f2.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", MailServiceException.CANNOT_NEST_SHARES, exception);
    }

    @Test
    public void nestedNonDocumentShares() throws Exception {
        Account owner, grantee1, grantee2;
        Provisioning prov = Provisioning.getInstance();
        owner = prov.get(Key.AccountBy.name, "owner@zimbra.com");
        grantee1 = prov.get(Key.AccountBy.name, "grantee1@zimbra.com");
        grantee2 = prov.get(Key.AccountBy.name, "grantee2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(owner.getId());

        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setDefaultView(MailItem.Type.MESSAGE);

        // f1 is shared initially
        Folder f1 = mbox.createFolder(null, "/Inbox/f1", fopt);
        mbox.grantAccess(null, f1.getId(), grantee1.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        Folder f1_f11 = mbox.createFolder(null, "/Inbox/f1/f1.1", fopt);
        mbox.createFolder(null, "/Inbox/f1/f1.1/f1.1.1", fopt);
        mbox.createFolder(null, "/Inbox/f1/f1.2", fopt);
        mbox.createFolder(null, "/Inbox/f1/f1.2/f1.2.1", fopt);

        // f2/f2.1 is shared initially
        Folder f2 = mbox.createFolder(null, "/Inbox/f2", fopt);
        Folder f2_f21 = mbox.createFolder(null, "/Inbox/f2/f2.1", fopt);
        mbox.grantAccess(null, f2_f21.getId(), grantee1.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        mbox.createFolder(null, "/Inbox/f2/f2.1/f2.1.1", fopt);
        Folder f2_f22 = mbox.createFolder(null, "/Inbox/f2/f2.2", fopt);
        mbox.createFolder(null, "/Inbox/f2/f2.2/f2.2.1", fopt);

        String exception;

        // allow creating a new share
        try {
            exception = null;
            mbox.grantAccess(null, f2_f22.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // allow adding grantee to an existing share
        try {
            exception = null;
            mbox.grantAccess(null, f2_f21.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // allow creating a share under a share
        try {
            exception = null;
            mbox.grantAccess(null, f1_f11.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);

        // allow sharing a folder that contains a share underneath
        try {
            exception = null;
            mbox.grantAccess(null, f2.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwidxa"), null);
        } catch (ServiceException e) {
            exception = e.getCode();
        }
        Assert.assertEquals("got wrong exception", null, exception);
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
