/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.account.message.GetShareInfoRequest;
import com.zimbra.soap.account.message.GetShareInfoResponse;
import com.zimbra.soap.type.ShareInfo;

/**
 */
public class TestAclPush {
    private static final String NAME_PREFIX = "TestAclPush_";
    private static final String USER1_NAME = NAME_PREFIX + "user1";
    private static final String USER2_NAME = NAME_PREFIX + "user2";
    private static final String DL_NAME = NAME_PREFIX + "dl";
    private Account acct1;
    private DistributionList dl;

    @Before
    public void setUp() throws Exception {
        cleanUp();
        acct1 = TestUtil.createAccount(USER1_NAME);
        TestUtil.createAccount(USER2_NAME);
        dl = TestUtil.createDistributionList(DL_NAME);
        dl.addMembers(new String[] { TestUtil.getAddress(USER2_NAME) });
    }

    private void cleanUp() throws Exception {
        if(TestUtil.accountExists(USER1_NAME)) {
            TestUtil.deleteAccount(USER1_NAME);
        }
        if(TestUtil.accountExists(USER2_NAME)) {
            TestUtil.deleteAccount(USER2_NAME);
        }
        if(TestUtil.DLExists(DL_NAME)) {
            TestUtil.deleteDistributionList(DL_NAME);
        }
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        dl = null;
        acct1 = null;
    }

    @Test
    public void testAclPushAndDiscovery() throws Exception {
        // create a folder in user1's mailbox
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        Folder.FolderOptions fopt = new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT);
        Folder folder = mbox1.createFolder(null, "/folder1", fopt);

        // grant access to the created folder to user2
        Account acct2 = Provisioning.getInstance().getAccountByName(TestUtil.getAddress(USER2_NAME));
        mbox1.grantAccess(null, folder.getId(), acct2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("r"), null);
        // Give some time for AclPushTask to run
        Thread.sleep(100);

        // invoke GetShareInfoRequest from user2 and check that the shared is discovered
        ZMailbox zMailbox2 = TestUtil.getZMailbox(USER2_NAME);
        GetShareInfoRequest req = new GetShareInfoRequest();
        GetShareInfoResponse resp = zMailbox2.invokeJaxb(req);
        List<ShareInfo> shares = resp.getShares();
        ShareInfo share = getShare(shares, "/folder1");
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
        mbox1.rename(null, folder.getId(), MailItem.Type.FOLDER, "/folder2");
        Thread.sleep(100);
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        assertNotNull(getShare(shares, "/folder2"));

        // create another folder and share with DL
        Folder dlFolder = mbox1.createFolder(null, "/" + DL_NAME, fopt);
        mbox1.grantAccess(null, dlFolder.getId(), dl.getId(), ACL.GRANTEE_GROUP, ACL.stringToRights("rwi"), null);
        Thread.sleep(100);
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        ShareInfo dlShare = getShare(shares, "/" + DL_NAME);
        assertNotNull(dlShare);
        assertEquals(dl.getId(), dlShare.getGranteeId());
        assertEquals(dlFolder.getPath(), dlShare.getFolderPath());
        assertEquals(dlFolder.getDefaultView().toString(), dlShare.getDefaultView());
        assertEquals("rwi", dlShare.getRights());
        assertEquals(acct1.getId(), dlShare.getOwnerId());

        // create another folder and share with "public"
        Folder pubFolder = mbox1.createFolder(null, "/public", fopt);
        mbox1.grantAccess(null, pubFolder.getId(), null, ACL.GRANTEE_PUBLIC, ACL.stringToRights("rw"), null);
        Thread.sleep(100);
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        ShareInfo pubShare = getShare(shares, "/public");
        assertNotNull(pubShare);
        assertEquals(pubFolder.getPath(), pubShare.getFolderPath());
        assertEquals(pubFolder.getDefaultView().toString(), pubShare.getDefaultView());
        assertEquals("rw", pubShare.getRights());
        assertEquals(acct1.getId(), pubShare.getOwnerId());

        // revoke access for user2 on the first folder
        mbox1.revokeAccess(null, folder.getId(), acct2.getId());
        Thread.sleep(100);
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        assertNull(getShare(shares, "/folder2"));
        assertNotNull(getShare(shares, "/" + DL_NAME));
        assertNotNull(getShare(shares, "/public"));

        // delete dlFolder and pubFolder
        mbox1.delete(null, dlFolder.getId(), MailItem.Type.FOLDER);
        mbox1.delete(null, pubFolder.getId(), MailItem.Type.FOLDER);
        Thread.sleep(100);
        resp = zMailbox2.invokeJaxb(req);
        shares = resp.getShares();
        assertNull(getShare(shares, "/" + DL_NAME));
        assertNull(getShare(shares, "/public"));
    }

    private static ShareInfo getShare(List<ShareInfo> shares, String folderPath) {
        for (ShareInfo si : shares) {
            if (folderPath.equals(si.getFolderPath())) {
                return si;
            }
        }
        return null;
    }
}
