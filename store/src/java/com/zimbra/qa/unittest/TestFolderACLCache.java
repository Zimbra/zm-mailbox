/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGrant;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.FolderACL;

public class TestFolderACLCache {

    @Rule
    public TestName testName = new TestName();

    /*
     * setup owner(user1) folders and grants with zmmailbox or webclient:
     *
     * /inbox/sub1       share with user2 with w right
     *                   share with user3 with rw rights
     *
     * /inbox/sub1/sub2  should inherit grants from sub1
     *
     * /inbox/sub1/sub3  share with public with r right
     *
     *                inbox
     *                /   \
     *             sub1   sub3
     *              |
     *             sub2
     *
     * zmmailbox -z -m user1 cf /inbox/sub1
     * zmmailbox -z -m user1 cf /inbox/sub1/sub2
     * zmmailbox -z -m user1 cf /inbox/sub3
     * zmmailbox -z -m user1 mfg /inbox/sub1 account user2 w
     * zmmailbox -z -m user1 mfg /inbox/sub1 account user3 rw
     * zmmailbox -z -m user1 mfg /inbox/sub3 public r
     *
     * To setup memcached:
     * zmprov mcf zimbraMemcachedClientServerList 'localhost:11211'
     * /opt/zimbra/memcached/bin/memcached -vv
     *
     * To test all scenarios, after reset-the-world:
     *
     * 1. Test the case when memcached is not configured/running.
     *    (A) just run the test
     *            This tests the case when the effective permissions
     *            are computed locally.
     *
     *    (B) modify FolderACL.ShareTarget.onLocalServer() to
     *     boolean onLocalServer() throws ServiceException {
     *       // return Provisioning.onLocalServer(mOwnerAcct);
     *       return false;
     *     }
     *     ==> run the test
     *     This tests the GetEffectiveFolderPerms soap.
     *
     * 2. Test the case when memcached is configured/running.
     *    zmprov mcf zimbraMemcachedClientServerList 'localhost:11211'
     *    (restart server)
     *    /opt/zimbra/memcached/bin/memcached -vv
     *    ==> run the test
     */

    String OWNER_ACCT_ID;
    Account USER1 = null;
    Account USER2 = null;
    Account USER3 = null;

    int INBOX_FID;
    int SUB1_FID;
    int SUB2_FID;
    int SUB3_FID;

    @Before
    public void setUp() throws Exception {

        Provisioning prov = Provisioning.getInstance();

        Account ownerAcct = prov.get(AccountBy.name, "user1");
        ZMailbox ownerMbx = TestUtil.getZMailbox(ownerAcct.getName());

        ZFolder inbox = ownerMbx.getFolderByPath("inbox");

        String sub1Path = "/inbox/sub1";
        ZFolder sub1 = ownerMbx.getFolderByPath(sub1Path);
        if (sub1 == null) {
            sub1 = TestUtil.createFolder(ownerMbx, sub1Path);
            ownerMbx.modifyFolderGrant(sub1.getId(), ZGrant.GranteeType.usr, "user2", "w", null);
            ownerMbx.modifyFolderGrant(sub1.getId(), ZGrant.GranteeType.usr, "user3", "rw", null);
        }

        String sub2Path = "/inbox/sub1/sub2";
        ZFolder sub2 = ownerMbx.getFolderByPath(sub2Path);
        if (sub2 == null)
            sub2 = TestUtil.createFolder(ownerMbx, sub2Path);

        String sub3Path = "/inbox/sub3";
        ZFolder sub3 = ownerMbx.getFolderByPath(sub3Path);
        if (sub3 == null) {
            sub3 = TestUtil.createFolder(ownerMbx, sub3Path);
            ownerMbx.modifyFolderGrant(sub3.getId(), ZGrant.GranteeType.pub, null, "r", null);
        }

        OWNER_ACCT_ID = ownerAcct.getId();
        USER1 = prov.get(AccountBy.name, "user1");
        USER2 = prov.get(AccountBy.name, "user2");
        USER3 = prov.get(AccountBy.name, "user3");

        INBOX_FID = Integer.valueOf(inbox.getId());
        SUB1_FID = Integer.valueOf(sub1.getId());
        SUB2_FID = Integer.valueOf(sub2.getId());
        SUB3_FID = Integer.valueOf(sub3.getId());
    }

    @After
    public void cleanUp() throws Exception {
    }

    private String formatRights(short rights) {
        return ACL.rightsToString(rights) + "(" + rights + ")";
    }

    /*
     * To test remote: hardcode FolderACL.ShareTarget.onLocalServer() to return false.
     *                 make sure you change it back after testing
     */
    private void doTest(
            Account authedAcct, String ownerAcctId, int targetFolderId,
            short expectedEffectivePermissions,
            short needRightsForCanAccessTest, boolean expectedCanAccess) throws ServiceException {

        OperationContext octxt;
        if (authedAcct == null)
            octxt = null;
        else
            octxt = new OperationContext(authedAcct);

        FolderACL folderAcl = new FolderACL(octxt, ownerAcctId, targetFolderId);

        short effectivePermissions = folderAcl.getEffectivePermissions();
        boolean canAccess = folderAcl.canAccess(needRightsForCanAccessTest);

        boolean good = false;

        // mask out the create folder right, it is an internal right, which is returned
        // by getEffectivePermissions if owner is on local server but not returned if
        // the owner is remote.
        //
        // The diff is not a real bug and can be ignored
        short actual = effectivePermissions;
        short expected = expectedEffectivePermissions;
        if (actual == -1)
            actual = ACL.stringToRights(ACL.rightsToString(actual));
        if (expected == -1)
            expected = ACL.stringToRights(ACL.rightsToString(expected));

        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expectedCanAccess, canAccess);

        good = true;
        ZimbraLog.test.info("authedAcct='%s' targetFolderId='%s' %s\n effectivePermissions: %s  (expected: %s)\n" +
                             "    canAccess:            %s (expected: %s)", authedAcct, targetFolderId,
                             (!good?"***FAILED***":""), formatRights(effectivePermissions),
                             formatRights(expectedEffectivePermissions), canAccess, expectedCanAccess);
    }

    @Test
    public void testPublicUser() throws Exception {
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, INBOX_FID, (short)0,       ACL.RIGHT_READ, false);
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB1_FID,  (short)0,       ACL.RIGHT_READ, false);
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB2_FID,  (short)0,       ACL.RIGHT_READ, false);

        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
    }

    @Test
    public void testOwner() throws Exception {
        // pass a null authed account
        // the owner itself accessing, should have all rights
        doTest(null, OWNER_ACCT_ID, INBOX_FID, (short)~0, (short)~0, true);
        doTest(null, OWNER_ACCT_ID, SUB1_FID,  (short)~0, (short)~0, true);
        doTest(null, OWNER_ACCT_ID, SUB2_FID,  (short)~0, (short)~0, true);
        doTest(null, OWNER_ACCT_ID, SUB3_FID,  (short)~0, (short)~0, true);
    }

    @Test
    public void testUser1() throws Exception {
        // the owner itself accessing, should have all rights
        doTest(USER1, OWNER_ACCT_ID, INBOX_FID, (short)~0, (short)~0, true);
        doTest(USER1, OWNER_ACCT_ID, SUB1_FID,  (short)~0, (short)~0, true);
        doTest(USER1, OWNER_ACCT_ID, SUB2_FID,  (short)~0, (short)~0, true);
        doTest(USER1, OWNER_ACCT_ID, SUB3_FID,  (short)~0, (short)~0, true);
    }

    @Test
    public void testUser2() throws Exception {
        doTest(USER2, OWNER_ACCT_ID, INBOX_FID, (short)0,         ACL.RIGHT_WRITE, false);
        doTest(USER2, OWNER_ACCT_ID, SUB1_FID,  ACL.RIGHT_WRITE,  ACL.RIGHT_WRITE, true);
        doTest(USER2, OWNER_ACCT_ID, SUB2_FID,  ACL.RIGHT_WRITE,  ACL.RIGHT_WRITE, true);

        doTest(USER2, OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest(USER2, OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
   }

    @Test
    public void testUser3() throws Exception {
        doTest(USER3, OWNER_ACCT_ID, INBOX_FID, (short)0,                                ACL.RIGHT_WRITE, false);
        doTest(USER3, OWNER_ACCT_ID, SUB1_FID,  (short)(ACL.RIGHT_READ|ACL.RIGHT_WRITE), ACL.RIGHT_WRITE, true);
        doTest(USER3, OWNER_ACCT_ID, SUB2_FID,  (short)(ACL.RIGHT_READ|ACL.RIGHT_WRITE), ACL.RIGHT_WRITE, true);

        doTest(USER3, OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest(USER3, OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
    }

    public static void main(String[] args) throws ServiceException {

        com.zimbra.cs.db.DbPool.startup();
        com.zimbra.cs.memcached.MemcachedConnector.startup();

        CliUtil.toolSetup();
        TestUtil.runTest(TestFolderACLCache.class);
    }
}
