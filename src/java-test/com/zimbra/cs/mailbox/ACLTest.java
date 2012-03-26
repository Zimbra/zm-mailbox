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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class ACLTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "17dd075e-2b47-44e6-8cb8-7fdfa18c1a9f");
        prov.createAccount("owner@zimbra.com", "secret", attrs);
        attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "a4e41fbe-9c3e-4ab5-8b34-c42f17e251cd");
        prov.createAccount("principal@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testRegrant() throws Exception {
        Account owner = Provisioning.getInstance().get(Key.AccountBy.name, "owner@zimbra.com");
        Account grantee = Provisioning.getInstance().get(Key.AccountBy.name, "principal@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);

        Folder folder = mbox.createFolder(null, "shared", (byte) 0, MailItem.Type.DOCUMENT);
        OperationContext octxt = new OperationContext(owner);
        mbox.grantAccess(octxt, folder.getId(), grantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("r"), null);
        try {
            mbox.grantAccess(octxt, folder.getId(), grantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("r"), null);
            Assert.fail("regrant has succeeded");
        } catch (ServiceException se) {
            if (!se.getCode().equals(MailServiceException.GRANT_EXISTS)) {
                Assert.fail("regrant throws ServiceException with code "+se.getCode());
            }
        }
    }

    @Test
    public void testRegrantDifferentPermission() throws Exception {
        Account owner = Provisioning.getInstance().get(Key.AccountBy.name, "owner@zimbra.com");
        Account grantee = Provisioning.getInstance().get(Key.AccountBy.name, "principal@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);

        Folder folder = mbox.createFolder(null, "shared", (byte) 0, MailItem.Type.DOCUMENT);
        OperationContext octxt = new OperationContext(owner);
        mbox.grantAccess(octxt, folder.getId(), grantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("r"), null);
        try {
            mbox.grantAccess(octxt, folder.getId(), grantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rw"), null);
        } catch (ServiceException se) {
            if (!se.getCode().equals(MailServiceException.GRANT_EXISTS)) {
                Assert.fail("regrant throws ServiceException with code "+se.getCode());
            }
            Assert.fail("regrant has failed");
        }
    }
}
