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
package com.zimbra.qa.unittest;

import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import com.zimbra.client.ZMailbox;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.acl.AclPushTask;
import com.zimbra.soap.account.message.GetShareInfoRequest;
import com.zimbra.soap.account.message.GetShareInfoResponse;
import com.zimbra.soap.type.ShareInfo;

/**
 */
public class TestAclPush extends TestCase {

    private static final String NAME_PREFIX = "TestAclPush";
    private static final String USER1_NAME = "user1";
    private static final String USER2_NAME = "user2";
    private Account acct1;
    private String[] origSharedItem;
    private DistributionList dl;

    @Override
    protected void setUp() throws Exception {
        acct1 = Provisioning.getInstance().getAccountByName(TestUtil.getAddress(USER1_NAME));
        origSharedItem = acct1.getSharedItem();
        acct1.unsetSharedItem();
        dl = Provisioning.getInstance().createDistributionList(TestUtil.getAddress(NAME_PREFIX + "-dl"),
                                                               Collections.<String, Object>emptyMap());
        dl.addMembers(new String[] { TestUtil.getAddress(USER2_NAME) });
    }

    public void testAclPushAndDiscovery() throws Exception {
        // create a folder in user1's mailbox
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        Folder folder = mbox1.createFolder(null, "/" + NAME_PREFIX + "-folder1", (byte) 0, MailItem.Type.DOCUMENT);

        // grant access to the created folder to user2
        Account acct2 = Provisioning.getInstance().getAccountByName(TestUtil.getAddress(USER2_NAME));
        mbox1.grantAccess(null, folder.getId(), acct2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("r"), null);

        // push ACLs to LDAP
        AclPushTask.doWork();

        // invoke GetShareInfoRequest from user2 and check that the shared is discovered
        ZMailbox zMailbox2 = TestUtil.getZMailbox(USER2_NAME);
        GetShareInfoRequest req = new GetShareInfoRequest();
        GetShareInfoResponse resp = zMailbox2.invokeJaxb(req);
        List<ShareInfo> shares = resp.getShares();
        ShareInfo share = getShare(shares, "/" + NAME_PREFIX + "-folder1");
        assertNotNull(share);
        assertEquals(acct2.getId(), share.getGranteeId());
        assertEquals(ACL.typeToString(ACL.GRANTEE_USER), share.getGranteeType());
        assertEquals(folder.getPath(), share.getFolderPath());
        assertEquals(folder.getId(), share.getFolderId());
        assertEquals(folder.getUuid(), share.getFolderUuid());
        assertEquals(folder.getDefaultView().toString(), share.getDefaultView());
        assertEquals("r", share.getRights());
        assertEquals(acct1.getId(), share.getOwnerId());

        // rename folder
        mbox1.rename(null, folder.getId(), MailItem.Type.FOLDER, "/" + NAME_PREFIX + "-folder2");
        AclPushTask.doWork();
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        assertNotNull(getShare(shares, "/" + NAME_PREFIX + "-folder2"));

        // create another folder and share with DL
        Folder dlFolder = mbox1.createFolder(null, "/" + NAME_PREFIX + "-dl", (byte) 0, MailItem.Type.DOCUMENT);
        mbox1.grantAccess(null, dlFolder.getId(), dl.getId(), ACL.GRANTEE_GROUP, ACL.stringToRights("rwi"), null);
        AclPushTask.doWork();
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        ShareInfo dlShare = getShare(shares, "/" + NAME_PREFIX + "-dl");
        assertNotNull(dlShare);
        assertEquals(dl.getId(), dlShare.getGranteeId());
        assertEquals(dlFolder.getPath(), dlShare.getFolderPath());
        assertEquals(dlFolder.getDefaultView().toString(), dlShare.getDefaultView());
        assertEquals("rwi", dlShare.getRights());
        assertEquals(acct1.getId(), dlShare.getOwnerId());

        // create another folder and share with "public"
        Folder pubFolder = mbox1.createFolder(null, "/" + NAME_PREFIX + "-public", (byte) 0, MailItem.Type.DOCUMENT);
        mbox1.grantAccess(null, pubFolder.getId(), null, ACL.GRANTEE_PUBLIC, ACL.stringToRights("rw"), null);
        AclPushTask.doWork();
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        ShareInfo pubShare = getShare(shares, "/" + NAME_PREFIX + "-public");
        assertNotNull(pubShare);
        assertEquals(pubFolder.getPath(), pubShare.getFolderPath());
        assertEquals(pubFolder.getDefaultView().toString(), pubShare.getDefaultView());
        assertEquals("rw", pubShare.getRights());
        assertEquals(acct1.getId(), pubShare.getOwnerId());

        // revoke access for user2 on the first folder
        mbox1.revokeAccess(null, folder.getId(), acct2.getId());
        AclPushTask.doWork();
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        assertNull(getShare(shares, "/" + NAME_PREFIX + "-folder2"));
        assertNotNull(getShare(shares, "/" + NAME_PREFIX + "-dl"));
        assertNotNull(getShare(shares, "/" + NAME_PREFIX + "-public"));

        // delete dlFolder and pubFolder
        mbox1.delete(null, dlFolder.getId(), MailItem.Type.FOLDER);
        mbox1.delete(null, pubFolder.getId(), MailItem.Type.FOLDER);
        AclPushTask.doWork();
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        assertNull(getShare(shares, "/" + NAME_PREFIX + "-dl"));
        assertNull(getShare(shares, "/" + NAME_PREFIX + "-public"));
    }

    private static ShareInfo getShare(List<ShareInfo> shares, String folderPath) {
        for (ShareInfo si : shares) {
            if (folderPath.equals(si.getFolderPath())) {
                return si;
            }
        }
        return null;
    }

    @Override
    protected void tearDown() throws Exception {
        TestUtil.deleteTestData(USER1_NAME, NAME_PREFIX);
        acct1.setSharedItem(origSharedItem);
        Provisioning.getInstance().deleteDistributionList(dl.getId());
    }
}
