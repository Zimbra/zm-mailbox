package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ShareInfo.Published;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZMailbox;

public class TestShareInfo extends TestCase {
    
    private static String TEST_NAME = "test-shareinfo";
    private static String TEST_ID = TestProvisioningUtil.genTestId();
    private static String PASSWORD = "test123";
   
    private static Provisioning mProv;
    
    private static String DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
    
    private static void init() throws Exception {
        mProv = Provisioning.getInstance();
        Domain d = getDomain();  // trigger create the test domain
    }
    
    private static Domain getDomain() throws Exception {
        Domain domain = mProv.get(DomainBy.name, DOMAIN_NAME);
        if (domain == null)
            domain = mProv.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        return domain;
    }
    
    private String getEmail(String localPart, String testName) {
        return localPart + "-" + testName + "@" + DOMAIN_NAME;
    }
    
    private String getEmail(String localPart, String testName, String domainName) {
        return localPart + "-" + testName + "@" + domainName;
    }
    
    /*
     * create a folder and grant the rights to an account
     * 
     * returns the folder id
     */
    private int createFolderAndGrantRight(Account owner, Account grantee, String folderPath, String rights) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, grantee.getName(), rights, null);
        
        return Integer.valueOf(folder.getId());
    }
    
    /*
     * create a folder and grant the rights to a distribution list
     */
    private int createFolderAndGrantRight(Account owner, DistributionList grantee, String folderPath, String rights) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.grp, grantee.getName(), rights, null);
        
        return Integer.valueOf(folder.getId());
    }
    
    private ZFolder createFolder(Account owner, String folderPath) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        return folder;
    }
    
    static class Expected {
        
        private List<ExpectedShareInfo> mExpected = new ArrayList<ExpectedShareInfo>();
        
        Expected() {
        }
        
        ExpectedShareInfo add(String ownerAcctId, String ownerAcctName,
                 int folderid, String folderPath, 
                 short rights,
                 byte granteeType, String granteeId, String granteeName) {
            
            ExpectedShareInfo esi = new ExpectedShareInfo(ownerAcctId, ownerAcctName,
                                                          folderid, folderPath, 
                                                          rights,
                                                          granteeType, granteeId, granteeName);
            mExpected.add(esi);
            return esi;
        }
        
        ExpectedShareInfo add(ExpectedShareInfo copy) {
            ExpectedShareInfo esi = new ExpectedShareInfo(copy);
            mExpected.add(esi);
            return esi;
        }
        
        private void remove(ExpectedShareInfo esi) {
            mExpected.remove(esi);
        }
        
        void verify(ShareInfo.Published shareInfo) throws ServiceException {
            for (ExpectedShareInfo esi : mExpected) {
                if (esi.isTheSame(shareInfo)) {
                    mExpected.remove(esi);
                    return;
                }
            }
            
            // shareInfo is not in expected
            fail();
        }
        
        /*
         * asserts that all expected shares are found
         * (verify already checked that each found share is expected)
         */
        void OK() {
            assertTrue(mExpected.isEmpty());
        }
        
        static class ExpectedShareInfo {
            ExpectedShareInfo(String ownerAcctId, String ownerAcctName,
                              int folderid, String folderPath, 
                              short rights,
                              byte granteeType, String granteeId, String granteeName) {
                
                mOwnerAcctId   = ownerAcctId;
                mOwnerAcctName = ownerAcctName;
                mFolderId      = folderid;
                mFolderPath    = folderPath;
                mRights        = rights;
                mGranteeType   = granteeType;
                mGranteeId     = granteeId;
                mGranteeName   = granteeName;
            }
            
            ExpectedShareInfo(ExpectedShareInfo copy) {
                this(copy.mOwnerAcctId,
                     copy.mOwnerAcctName,
                     copy.mFolderId,
                     copy.mFolderPath,
                     copy.mRights,
                     copy.mGranteeType,
                     copy.mGranteeId,
                     copy.mGranteeName);
            }
            
            boolean isTheSame(ShareInfo.Published shareInfo) throws ServiceException {
                
                if (!mOwnerAcctId.equals(shareInfo.getOwnerAcctId()))
                    return false;
                
                if (!mOwnerAcctName.equals(shareInfo.getOwnerAcctEmail()))
                    return false;
                
                if (mFolderId != shareInfo.getFolderId())
                    return false;
                
                if (!mFolderPath.equals(shareInfo.getFolderPath()))
                    return false;
                
                if (mRights != ACL.stringToRights(shareInfo.getRights()))
                    return false;
                
                if (mGranteeType != ACL.stringToType(shareInfo.getGranteeType()))
                    return false;
                
                if (!mGranteeId.equals(shareInfo.getGranteeId()))
                    return false;
                
                if (!mGranteeName.equals(shareInfo.getGranteeName()))
                    return false;
                
                return true;
            }
            
            private String mOwnerAcctId;
            private String mOwnerAcctName;
            private int    mFolderId;
            private String mFolderPath;
            private short  mRights;
            private byte   mGranteeType;
            private String mGranteeId;
            private String mGranteeName;
        }
    }
    
    private static class VerifyPublishedVisitor implements ShareInfo.Published.Visitor {
        Expected mExpected;
        
        VerifyPublishedVisitor(Expected expected) {
            mExpected = expected;
        }
        
        public void visit(ShareInfo.Published shareInfo) throws ServiceException {
            mExpected.verify(shareInfo);
        }

    }
    
    private void doTestPublishShareInfo(Account publishingEntry, Account ownerForPublishing, Account ownerForGet,
            String folderPath, Expected expectedDirectOnly, Expected expectedIncludeAll) 
        throws ServiceException {
        
        ShareInfo.Publishing si = new ShareInfo.Publishing(ShareInfo.Publishing.Action.add, ownerForPublishing.getId(), folderPath, null);
        mProv.modifyShareInfo(publishingEntry, si);
        
        VerifyPublishedVisitor visitor;
        
        visitor = new VerifyPublishedVisitor(expectedDirectOnly);
        mProv.getShareInfo(publishingEntry, true, ownerForGet, visitor);
        expectedDirectOnly.OK();
        
        visitor = new VerifyPublishedVisitor(expectedIncludeAll);
        mProv.getShareInfo(publishingEntry, false, ownerForGet, visitor);
        expectedDirectOnly.OK();
    }
    
    private void doTestPublishShareInfo(DistributionList publishingEntry, Account ownerForPublishing, Account ownerForGet,
            String folderPath, Expected expectedDirectOnly, Expected expectedIncludeAll) 
        throws ServiceException {
        
        ShareInfo.Publishing si = new ShareInfo.Publishing(ShareInfo.Publishing.Action.add, ownerForPublishing.getId(), folderPath, null);
        mProv.modifyShareInfo(publishingEntry, si);
        
        VerifyPublishedVisitor visitor;
        
        visitor = new VerifyPublishedVisitor(expectedDirectOnly);
        mProv.getShareInfo(publishingEntry, true, ownerForGet, visitor);
        expectedDirectOnly.OK();
        
        visitor = new VerifyPublishedVisitor(expectedIncludeAll);
        mProv.getShareInfo(publishingEntry, false, ownerForGet, visitor);
        expectedDirectOnly.OK();
    }
    
    public void testGrantOnTheFolder() throws Exception {
        String testName = getName();
        
        /*
         * owner account
         */
        String ownerEmail = getEmail("owner", testName);
        Account owner = mProv.createAccount(ownerEmail, PASSWORD, null);
        
        /*
         * grantee account
         */
        String granteeEmail = getEmail("grantee", testName);
        Account grantee = mProv.createAccount(granteeEmail, PASSWORD, null);
        
        /*
         * other account
         */
        String otherEmail = getEmail("other", testName);
        Account otherAcct = mProv.createAccount(otherEmail, PASSWORD, null);
        
        /*
         * create a folder in owner's mailbox and grant rights to the grantee account
         */
        String folderPath = "/test";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE;
        int folderId = createFolderAndGrantRight(owner, grantee, folderPath, ACL.rightsToString(rights));
        
        Expected expectedDirectOnly;
        Expected expectedAll;
        
        Expected.ExpectedShareInfo esi = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), folderId, folderPath, rights, ACL.GRANTEE_USER, grantee.getId(), grantee.getName());

        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedAll = new Expected();
        expectedAll.add(esi);
        doTestPublishShareInfo(grantee, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // should not find any
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(otherAcct, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // publishing ones own folder to himself, no share info should be published
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(owner, owner, owner, folderPath, expectedDirectOnly, expectedAll);
    }
    
    public void testGrantOnParentFolder() throws Exception {
        String testName = getName();
        
        /*
         * owner account
         */
        String ownerEmail = getEmail("owner", testName);
        Account owner = mProv.createAccount(ownerEmail, PASSWORD, null);
        
        /*
         * grantee account
         */
        String granteeEmail = getEmail("grantee", testName);
        Account grantee = mProv.createAccount(granteeEmail, PASSWORD, null);
        
        /*
         * other account
         */
        String otherEmail = getEmail("other", testName);
        Account otherAcct = mProv.createAccount(otherEmail, PASSWORD, null);
        
        /*
         * create a folder in owner's mailbox and grant rights to the grantee account
         */
        String folderPath = "/test";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE;
        int folderId = createFolderAndGrantRight(owner, grantee, folderPath, ACL.rightsToString(rights));
        
        // create a sub folder
        String subFolderPath = "/test/sub";
        ZFolder subFolder = createFolder(owner, subFolderPath);
        int subFolderId = Integer.valueOf(subFolder.getId());
        
        Expected expectedDirectOnly;
        Expected expectedAll;
        
        Expected.ExpectedShareInfo esi = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), subFolderId, subFolderPath, rights, ACL.GRANTEE_USER, grantee.getId(), grantee.getName());
        
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedAll = new Expected();
        expectedAll.add(esi);
        doTestPublishShareInfo(grantee, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(otherAcct, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // publishing ones own folder to himself, no share info should be published
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(owner, owner, owner, folderPath, expectedDirectOnly, expectedAll);
    }
    
    public void testDLShareInfoGrantToDL() throws Exception {
        String testName = getName();
        
        /*
         * owner account
         */
        String ownerEmail = getEmail("owner", testName);
        Account owner = mProv.createAccount(ownerEmail, PASSWORD, null);
        
        /*
         * grantee DL
         */
        String granteeEmail = getEmail("grantee-dl", testName);
        DistributionList grantee = mProv.createDistributionList(granteeEmail, null);
        
        /*
         * create two accounts, one is member of the grantee LD, the other is not 
         */
        String acctInDlEmail = getEmail("acct-in-dl", testName);
        Account acctInDl= mProv.createAccount(acctInDlEmail, PASSWORD, null);
        grantee.addMembers(new String[]{acctInDlEmail});
        
        String acctNotInDlEmail = getEmail("acct-not-in-dl", testName);
        Account acctNotInDl = mProv.createAccount(acctNotInDlEmail, PASSWORD, null);
        
        /*
         * create two DLs, one is member of the grantee LD, the other is not 
         */
        String dlInDlEmail = getEmail("dl-in-dl", testName);
        DistributionList dlInDL = mProv.createDistributionList(dlInDlEmail, null);
        grantee.addMembers(new String[]{dlInDlEmail});
        
        String dlNotInDlEmail = getEmail("dl-not-in-dl", testName);
        DistributionList dlNotInDL = mProv.createDistributionList(dlNotInDlEmail, null);
        
        /*
         * create a folder in owner's mailbox and grant rights to the grantee account
         */
        String folderPath = "/test";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE;
        int folderId = createFolderAndGrantRight(owner, grantee, folderPath, ACL.rightsToString(rights));
        
        // create a sub folder
        String subFolderPath = "/test/sub";
        ZFolder subFolder = createFolder(owner, subFolderPath);
        int subFolderId = Integer.valueOf(subFolder.getId());
        
        Expected expectedDirectOnly;
        Expected expectedAll;
        
        Expected.ExpectedShareInfo esi = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), folderId, folderPath, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());
        
        /*
         * =============
         * parent folder
         * =============
         */
        // === the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedAll = new Expected();
        expectedAll.add(esi);
        doTestPublishShareInfo(grantee, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // === an account in the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedAll = new Expected();
        expectedAll.add(esi);
        doTestPublishShareInfo(acctInDl, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // === an account not in the grantee DL
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(acctNotInDl, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // === a DL in the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedAll = new Expected();
        expectedAll.add(esi);
        doTestPublishShareInfo(dlInDL, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // === a DL not in the grantee DL
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(dlNotInDL, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        
        /*
         * =============
         * sub folder
         * =============
         */
        Expected.ExpectedShareInfo esiSubFolder = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), subFolderId, subFolderPath, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());

        
        // === the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder);
        expectedAll = new Expected();
        expectedAll.add(esi);
        expectedAll.add(esiSubFolder);
        doTestPublishShareInfo(grantee, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // === an account in the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder);
        expectedAll = new Expected();
        expectedAll.add(esi);
        expectedAll.add(esiSubFolder);
        doTestPublishShareInfo(acctInDl, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // === an account not in the grantee DL
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(acctNotInDl, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // === a DL in the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder);
        expectedAll = new Expected();
        expectedAll.add(esi);
        expectedAll.add(esiSubFolder);
        doTestPublishShareInfo(dlInDL, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // === a DL not in the grantee DL
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublishShareInfo(dlNotInDL, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
    }
    
    public void testGetShareInfoByOwner() throws Exception {
        String testName = getName();
        
        /*
         * owner account
         */
        String owner1Email = getEmail("owner1", testName);
        Account owner1 = mProv.createAccount(owner1Email, PASSWORD, null);
        
        String owner2Email = getEmail("owner2", testName);
        Account owner2 = mProv.createAccount(owner2Email, PASSWORD, null);
        
        /*
         * grantee DL
         */
        String granteeEmail = getEmail("grantee-dl", testName);
        DistributionList grantee = mProv.createDistributionList(granteeEmail, null);
        
        /*
         * create an account in the DL
         */
        String acctInDlEmail = getEmail("acct-in-dl", testName);
        Account acctInDl= mProv.createAccount(acctInDlEmail, PASSWORD, null);
        grantee.addMembers(new String[]{acctInDlEmail});
        
        /*
         * create a folder in owner1 mailbox and grant rights to the grantee DL
         */
        String folderPathOwner1 = "/test-of-owner1";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE;
        int folderIdOfOwner1 = createFolderAndGrantRight(owner1, grantee, folderPathOwner1, ACL.rightsToString(rights));
        
        String folderPathOwner2 = "/test-of-owner2";
        int folderIdOfOwner2 = createFolderAndGrantRight(owner2, grantee, folderPathOwner2, ACL.rightsToString(rights));
        
        Expected expectedDirectOnly;
        Expected expectedAll;
        
        Expected.ExpectedShareInfo esiOwner1 = new Expected.ExpectedShareInfo(owner1.getId(), owner1.getName(), folderIdOfOwner1, folderPathOwner1, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());
        Expected.ExpectedShareInfo esiOwner2 = new Expected.ExpectedShareInfo(owner2.getId(), owner2.getName(), folderIdOfOwner2, folderPathOwner2, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());

        
        // publish owner1's share
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esiOwner1);
        expectedAll = new Expected();
        expectedAll.add(esiOwner1);
        // pass null for ownerForGet so we will get all published shares regardlesss which owner they belong to
        doTestPublishShareInfo(grantee, owner1, null, folderPathOwner1, expectedDirectOnly, expectedAll);
        
        // publish owner2's share
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esiOwner1);
        expectedDirectOnly.add(esiOwner2);
        expectedAll = new Expected();
        expectedAll.add(esiOwner1);
        expectedAll.add(esiOwner2);
        // pass null for ownerForGet so we will get all published shares regardlesss which owner they belong to
        doTestPublishShareInfo(grantee, owner2, null, folderPathOwner2, expectedDirectOnly, expectedAll);
        
        // get only owner1's share
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        expectedAll.add(esiOwner1);
        
        VerifyPublishedVisitor visitor;
        
        visitor = new VerifyPublishedVisitor(expectedDirectOnly);
        mProv.getShareInfo(acctInDl, true, owner1, visitor);
        expectedDirectOnly.OK();
        
        visitor = new VerifyPublishedVisitor(expectedAll);
        mProv.getShareInfo(acctInDl, false, owner1, visitor);
        expectedDirectOnly.OK(); 
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();  // will set SoapProvisioning
        
        init();

        TestUtil.runTest(TestShareInfo.class);
    }
}
