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
    public TestName testInfo = new TestName();
    private String testName = null;
    private String userNamePrefix = null;

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

    String ownerName = null;
    String user2Name = null;
    String user3Name = null;
    Account ownerAcct = null;
    Account USER2 = null;
    Account USER3 = null;
    String OWNER_ACCT_ID = null;
    Provisioning prov = Provisioning.getInstance();

    int INBOX_FID;
    int SUB1_FID;
    int SUB2_FID;
    int SUB3_FID;

    @Before
    public void setUp() throws Exception {
        testName = testInfo.getMethodName();
        userNamePrefix = "testfolderaclcache-" + testName + "-user-";
        ownerName = userNamePrefix + "owner";
        user2Name = userNamePrefix + "haswriteaccess";
        user3Name = userNamePrefix + "hasreadwriteaccess";
        cleanUp();

        ownerAcct = createAcct(ownerName);
        USER2 = createAcct(user2Name);
        USER3 = createAcct(user3Name);
        ZMailbox ownerMbx = TestUtil.getZMailbox(ownerAcct.getName());

        ZFolder inbox = ownerMbx.getFolderByPath("inbox");

        String sub1Path = "/inbox/sub1";
        ZFolder sub1 = ownerMbx.getFolderByPath(sub1Path);
        if (sub1 == null) {
            sub1 = TestUtil.createFolder(ownerMbx, sub1Path);
            ownerMbx.modifyFolderGrant(sub1.getId(), ZGrant.GranteeType.usr, USER2.getName(), "w", null);
            ownerMbx.modifyFolderGrant(sub1.getId(), ZGrant.GranteeType.usr, USER3.getName(), "rw", null);
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

        INBOX_FID = Integer.valueOf(inbox.getId());
        SUB1_FID = Integer.valueOf(sub1.getId());
        SUB2_FID = Integer.valueOf(sub2.getId());
        SUB3_FID = Integer.valueOf(sub3.getId());
    }

    @After
    public void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(ownerName);
        TestUtil.deleteAccountIfExists(user2Name);
        TestUtil.deleteAccountIfExists(user3Name);
    }

    private Account createAcct(String name) throws ServiceException {
        Account acct = TestUtil.createAccount(name);
        Assert.assertNotNull(String.format("Unable to create account for %s", name), acct);
        return acct;
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
        ZimbraLog.test.debug(
                "Test %s:authedAcct='%s' targetFolderId='%s' %s\n effectivePermissions: %s  (expected: %s)\n" +
                "    canAccess:            %s (expected: %s)", testName, authedAcct, targetFolderId,
                (!good?"***FAILED***":""),
                formatRights(effectivePermissions), formatRights(expectedEffectivePermissions),
                canAccess, expectedCanAccess);
    }

    @Test
    public void publicUser() throws Exception {
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, INBOX_FID, (short)0,       ACL.RIGHT_READ, false);
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB1_FID,  (short)0,       ACL.RIGHT_READ, false);
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB2_FID,  (short)0,       ACL.RIGHT_READ, false);

        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest(GuestAccount.ANONYMOUS_ACCT,    OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
    }

    @Test
    public void nullAuthedAcct() throws Exception {
        // pass a null authed account
        // the owner itself accessing, should have all rights
        doTest(null, OWNER_ACCT_ID, INBOX_FID, (short)~0, (short)~0, true);
        doTest(null, OWNER_ACCT_ID, SUB1_FID,  (short)~0, (short)~0, true);
        doTest(null, OWNER_ACCT_ID, SUB2_FID,  (short)~0, (short)~0, true);
        doTest(null, OWNER_ACCT_ID, SUB3_FID,  (short)~0, (short)~0, true);
    }

    @Test
    public void authedAcctIsOwner() throws Exception {
        // the owner itself accessing, should have all rights
        doTest(ownerAcct, OWNER_ACCT_ID, INBOX_FID, (short)~0, (short)~0, true);
        doTest(ownerAcct, OWNER_ACCT_ID, SUB1_FID,  (short)~0, (short)~0, true);
        doTest(ownerAcct, OWNER_ACCT_ID, SUB2_FID,  (short)~0, (short)~0, true);
        doTest(ownerAcct, OWNER_ACCT_ID, SUB3_FID,  (short)~0, (short)~0, true);
    }

    @Test
    public void authedAcctHasWriteAccess() throws Exception {
        doTest(USER2, OWNER_ACCT_ID, INBOX_FID, (short)0,         ACL.RIGHT_WRITE, false);
        doTest(USER2, OWNER_ACCT_ID, SUB1_FID,  ACL.RIGHT_WRITE,  ACL.RIGHT_WRITE, true);
        doTest(USER2, OWNER_ACCT_ID, SUB2_FID,  ACL.RIGHT_WRITE,  ACL.RIGHT_WRITE, true);

        doTest(USER2, OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest(USER2, OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
   }

    @Test
    public void authedAcctHasReadWriteAccess() throws Exception {
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
