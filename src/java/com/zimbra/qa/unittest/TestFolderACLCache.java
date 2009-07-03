package com.zimbra.qa.unittest;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.FolderACL;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZMailbox;

import junit.framework.TestCase;

public class TestFolderACLCache extends TestCase {

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
     * 1. ==> run the test
     *    This tests the local host case.
     * 
     * 2. modify FolderACL.ShareTarget.onLocalServer() to 
     *    boolean onLocalServer() throws ServiceException {
     *       // return Provisioning.onLocalServer(mOwnerAcct);
     *       return false;
     *     } 
     *     ==> run the test
     *     This tests the remote host case, with memcached off.
     *     
     * 3. zmprov mcf zimbraMemcachedClientServerList 'localhost:11211'
     *    /opt/zimbra/memcached/bin/memcached -vv
     *    ==> run the test
     *    This tests the remote host case, with memcached on.
     */
    
    String OWNER_ACCT_ID;
    int INBOX_FID;
    int SUB1_FID;
    int SUB2_FID;
    int SUB3_FID;
    
    public void setUp() throws Exception {
        Account ownerAcct = Provisioning.getInstance().get(AccountBy.name, "user1");
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
        INBOX_FID = Integer.valueOf(inbox.getId());
        SUB1_FID = Integer.valueOf(sub1.getId());
        SUB2_FID = Integer.valueOf(sub2.getId());
        SUB3_FID = Integer.valueOf(sub3.getId());
    }
    
    private String formatRights(short rights) {
        return ACL.rightsToString(rights) + "(" + rights + ")";
    }
    
    /*
     * To test remote: hardcode FolderACL.ShareTarget.onLocalServer() to return false.
     *                 make sure you change it back after testing
     */
    private void doTest(
            String authedAcctName, String ownerAcctId, int targetFolderId,
            short expectedEffectivePermissions, 
            short needRightsForCanAccessTest, boolean expectedCanAccess) throws ServiceException {
        
        Account authedAcct = null;
        if (authedAcctName != null)
            authedAcct = Provisioning.getInstance().get(AccountBy.name, authedAcctName);
        else
            authedAcct = ACL.ANONYMOUS_ACCT;
        
        OperationContext octxt = new OperationContext(authedAcct);
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
        
        assertEquals(expected, actual);
        assertEquals(expectedCanAccess, canAccess);
        
        good = true;
        /*
        System.out.println();
        System.out.println("authedAcctName=" + authedAcctName + "  targetFolderId=" + targetFolderId + " " + (!good?"***FAILED***":""));
        System.out.println("    effectivePermissions: " + formatRights(effectivePermissions) + " (expected: " + formatRights(expectedEffectivePermissions) + ")");
        System.out.println("    canAccess:            " + canAccess                          + " (expected: " + expectedCanAccess + ")");
        */
    }
    
    public void testPublicUser() throws Exception {
        doTest(null,    OWNER_ACCT_ID, INBOX_FID, (short)0,       ACL.RIGHT_READ, false);
        doTest(null,    OWNER_ACCT_ID, SUB1_FID,  (short)0,       ACL.RIGHT_READ, false);
        doTest(null,    OWNER_ACCT_ID, SUB2_FID,  (short)0,       ACL.RIGHT_READ, false);
        
        doTest(null,    OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest(null,    OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
    }
    
    public void testUser1() throws Exception {
        // the owner itself accessing, should have all rights
        doTest("user1", OWNER_ACCT_ID, INBOX_FID, (short)~0, (short)~0, true);
        doTest("user1", OWNER_ACCT_ID, SUB1_FID,  (short)~0, (short)~0, true);
        doTest("user1", OWNER_ACCT_ID, SUB2_FID,  (short)~0, (short)~0, true);
        doTest("user1", OWNER_ACCT_ID, SUB3_FID,  (short)~0, (short)~0, true);
    }
    
    public void testUser2() throws Exception {
        doTest("user2", OWNER_ACCT_ID, INBOX_FID, (short)0,         ACL.RIGHT_WRITE, false);
        doTest("user2", OWNER_ACCT_ID, SUB1_FID,  ACL.RIGHT_WRITE,  ACL.RIGHT_WRITE, true);
        doTest("user2", OWNER_ACCT_ID, SUB2_FID,  ACL.RIGHT_WRITE,  ACL.RIGHT_WRITE, true);
        
        doTest("user2", OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest("user2", OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
   }
    
    public void testUser3() throws Exception {
        doTest("user3", OWNER_ACCT_ID, INBOX_FID, (short)0,                                ACL.RIGHT_WRITE, false);
        doTest("user3", OWNER_ACCT_ID, SUB1_FID,  (short)(ACL.RIGHT_READ|ACL.RIGHT_WRITE), ACL.RIGHT_WRITE, true);
        doTest("user3", OWNER_ACCT_ID, SUB2_FID,  (short)(ACL.RIGHT_READ|ACL.RIGHT_WRITE), ACL.RIGHT_WRITE, true);
        
        doTest("user3", OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_READ,  true);
        doTest("user3", OWNER_ACCT_ID, SUB3_FID,  ACL.RIGHT_READ, ACL.RIGHT_WRITE, false);
    }
    
    public static void main(String[] args) throws ServiceException {
        
        com.zimbra.cs.db.DbPool.startup();
        com.zimbra.cs.memcached.MemcachedConnector.startup();
        
        CliUtil.toolSetup();
        TestUtil.runTest(TestFolderACLCache.class);
    }
}
