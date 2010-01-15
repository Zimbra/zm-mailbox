/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
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
import com.zimbra.cs.account.Provisioning.PublishShareInfoAction;
import com.zimbra.cs.account.Provisioning.PublishedShareInfoVisitor;
import com.zimbra.cs.account.ShareInfo.Published;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
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
    
    private void grantRight(Account owner, DistributionList grantee, String folderId, String rights) throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ownerMbox.modifyFolderGrant(folderId, ZGrant.GranteeType.grp, grantee.getName(), rights, null);
    }
    
    private ZFolder createFolder(Account owner, String folderPath) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        return folder;
    }
    
    private void deleteFolder(Account owner, String folderId) throws Exception {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ownerMbox.deleteFolder(folderId);
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
        
        void verify(ShareInfoData sid) throws ServiceException {
            for (ExpectedShareInfo esi : mExpected) {
                if (esi.isTheSame(sid)) {
                    mExpected.remove(esi);
                    return;
                }
            }
            
            // shareInfo is not in expected
            sid.dump();
            fail();
        }
        
        /*
         * asserts that all expected shares are found
         * 
         * (verify already checked that each found share is expected.
         *  verify removes a share info as it finds it in the expected List,
         *  here we want to verify that the expected List is empty, i.e. 
         *  all expected share info are found)
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
            
            boolean isTheSame(ShareInfoData sid) throws ServiceException {
                
                if (!mOwnerAcctId.equals(sid.getOwnerAcctId()))
                    return false;
                
                if (!mOwnerAcctName.equals(sid.getOwnerAcctEmail()))
                    return false;
                
                if (mFolderId != sid.getFolderId())
                    return false;
                
                if (!mFolderPath.equals(sid.getFolderPath()))
                    return false;
                
                if (mRights != ACL.stringToRights(sid.getRights()))
                    return false;
                
                if (mGranteeType != ACL.stringToType(sid.getGranteeType()))
                    return false;
                
                if (!mGranteeId.equals(sid.getGranteeId()))
                    return false;
                
                if (!mGranteeName.equals(sid.getGranteeName()))
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
    
    private static class VerifyPublishedVisitor implements PublishedShareInfoVisitor {
        Expected mExpected;
        
        VerifyPublishedVisitor(Expected expected) {
            mExpected = expected;
        }
        
        public void visit(ShareInfoData sid) throws ServiceException {
            mExpected.verify(sid);
        }

    }
    
    /**
     * 
     * @param publishingEntry
     * @param ownerForPublishing
     * @param ownerForGet if null, get published share info owned by all users
     *                    otherwise, get published share info owned by the specified owner
     * @param folderPath
     * @param expectedDirectOnly
     * @param expectedIncludeAll
     * @throws ServiceException
     */
    private void doTestPublishShareInfo(DistributionList publishingEntry, 
            PublishShareInfoAction action,
            Account ownerForPublishing, Account ownerForGet,
            String folderPath, Expected expectedDirectOnly, Expected expectedIncludeAll) throws ServiceException {
        
        mProv.publishShareInfo(publishingEntry, action, ownerForPublishing, folderPath);
        
        VerifyPublishedVisitor visitor;
        
        Expected verifyMe;
        if (ownerForGet == null)
            verifyMe = expectedIncludeAll;
        else
            verifyMe = expectedDirectOnly;
        
        visitor = new VerifyPublishedVisitor(verifyMe);
        mProv.getPublishedShareInfo(publishingEntry, ownerForGet, visitor);
        verifyMe.OK();
    }
    
    private void doTestPublish(DistributionList publishingEntry, 
            Account ownerForPublishing, Account ownerForGet,
            String folderPath, Expected expectedDirectOnly, Expected expectedIncludeAll) throws ServiceException{
        
        doTestPublishShareInfo(publishingEntry, 
                PublishShareInfoAction.add,
                ownerForPublishing, ownerForGet,
                folderPath, expectedDirectOnly, expectedIncludeAll);
    }
    
    private void doTestUnpublish(DistributionList publishingEntry, 
            Account ownerForPublishing, Account ownerForGet,
            String folderPath, Expected expectedDirectOnly, Expected expectedIncludeAll) throws ServiceException{
        
        doTestPublishShareInfo(publishingEntry, 
                PublishShareInfoAction.remove,
                ownerForPublishing, ownerForGet,
                folderPath, expectedDirectOnly, expectedIncludeAll);
    }
    
    private void doTestGetShareInfo(Account authedAcct, Account ownerAcct, Expected expected) throws ServiceException {
        Provisioning prov = TestProvisioningUtil.getSoapProvisioningUser(authedAcct.getName(), PASSWORD);
        
        VerifyPublishedVisitor visitor = new VerifyPublishedVisitor(expected);
        prov.getShareInfo(ownerAcct, visitor);
        expected.OK();
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
         * create two accounts, one is member of the grantee DL, the other is not 
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
         * create a folder in owner's mailbox and grant rights to the grantee DL
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
        doTestPublish(grantee, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // === a DL in the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedAll = new Expected();
        expectedAll.add(esi);
        doTestPublish(dlInDL, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // === a DL not in the grantee DL
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublish(dlNotInDL, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        
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
        doTestPublish(grantee, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // === a DL in the grantee DL
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder);
        expectedAll = new Expected();
        expectedAll.add(esi);
        expectedAll.add(esiSubFolder);
        doTestPublish(dlInDL, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
        
        // === a DL not in the grantee DL
        expectedDirectOnly = new Expected();
        expectedAll = new Expected();
        doTestPublish(dlNotInDL, owner, owner, subFolderPath, expectedDirectOnly, expectedAll);
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
         * create an account in the DL, and an account not in DL
         */
        String acctInDlEmail = getEmail("acct-in-dl", testName);
        Account acctInDl= mProv.createAccount(acctInDlEmail, PASSWORD, null);
        grantee.addMembers(new String[]{acctInDlEmail});
        
        String acctNotInDlEmail = getEmail("acct-not-in-dl", testName);
        Account acctNotInDl = mProv.createAccount(acctNotInDlEmail, PASSWORD, null);
        
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
        // pass null for ownerForGet so we will get all published shares regardless which owner they belong to
        doTestPublish(grantee, owner1, null, folderPathOwner1, expectedDirectOnly, expectedAll);
        
        // publish owner2's share
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esiOwner1);
        expectedDirectOnly.add(esiOwner2);
        expectedAll = new Expected();
        expectedAll.add(esiOwner1);
        expectedAll.add(esiOwner2);
        // pass null for ownerForGet so we will get all published shares regardless which owner they belong to
        doTestPublish(grantee, owner2, null, folderPathOwner2, expectedDirectOnly, expectedAll);
        
        /*
         * The following cannot be done for now.  Because we want to access the GetShareInfo in account 
         * namespace, while the one in Provisioning interface is calling the admin version.
         * 
         * TODO: add GetShareInfo in zclient and use that for the followin tests.
         */
        
        /*
        // authenticated as a shared user, get shares shared by owner1
        Expected expected = new Expected();
        expected.add(esiOwner1);
        doTestGetShareInfo(acctInDl, owner1, expected);
        
        // authenticated as a shared user, get shares shared by owner2
        expected = new Expected();
        expected.add(esiOwner2);
        doTestGetShareInfo(acctInDl, owner2, expected);
        
        // authenticated as a not-shared user, should not see any share info shared by any owner
        expected = new Expected();
        doTestGetShareInfo(acctNotInDl, owner1, expected);
        expected = new Expected();
        doTestGetShareInfo(acctNotInDl, owner2, expected);
        */
    }
    
    public void testRemoveShareInfo() throws Exception {
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
         * create a folder in owner's mailbox and grant rights to the grantee DL
         */
        String folderPath = "/test";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE;
        int folderId = createFolderAndGrantRight(owner, grantee, folderPath, ACL.rightsToString(rights));
        
        // create a sub folder
        String subFolder1Path = "/test/sub1";
        ZFolder subFolder1 = createFolder(owner, subFolder1Path);
        int subFolder1Id = Integer.valueOf(subFolder1.getId());
        
        // create another sub folder
        String subFolder2Path = "/test/sub1/sub2";
        ZFolder subFolder2 = createFolder(owner, subFolder2Path);
        int subFolder2Id = Integer.valueOf(subFolder2.getId());
        
        Expected.ExpectedShareInfo esi = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), folderId, folderPath, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());
        Expected.ExpectedShareInfo esiSubFolder1 = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), subFolder1Id, subFolder1Path, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());
        Expected.ExpectedShareInfo esiSubFolder2 = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), subFolder2Id, subFolder2Path, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());

        
        Expected expectedDirectOnly;
        Expected expectedAll = null;
        
        // publish for the parent folder
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        doTestPublish(grantee, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // publish for the subfolder1
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder1);
        doTestPublish(grantee, owner, owner, subFolder1Path, expectedDirectOnly, expectedAll);
        
        // publish for the subfolder2
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder1);
        expectedDirectOnly.add(esiSubFolder2);
        doTestPublish(grantee, owner, owner, subFolder2Path, expectedDirectOnly, expectedAll);
        
        // remove the subfolder1 
        // bug 42469: should be able to unpublish if the folder is deleted
        deleteFolder(owner, "" + subFolder1Id);
        
        /*
        ShareInfo.DumpShareInfoVisitor visitor = new ShareInfo.DumpShareInfoVisitor();
        visitor.printHeadings();
        mProv.getPublishedShareInfo(grantee, owner, visitor);
        */
        
        // unpublish the subfolder, should only see the parent folder and subfolder2
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        expectedDirectOnly.add(esiSubFolder2);
        doTestUnpublish(grantee, owner, owner, subFolder1Path, expectedDirectOnly, expectedAll);
        
        // unpublish all folders(pass null for folder), should see nothing now
        expectedDirectOnly = new Expected();
        doTestUnpublish(grantee, owner, owner, null, expectedDirectOnly, expectedAll);
    }
    
    public void testRepublish() throws Exception {
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
         * create a folder in owner's mailbox and grant rights to the grantee DL
         */
        String folderPath = "/test";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE;
        int folderId = createFolderAndGrantRight(owner, grantee, folderPath, ACL.rightsToString(rights));
        
        Expected.ExpectedShareInfo esi = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), folderId, folderPath, rights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());
        
        Expected expectedDirectOnly = null;
        Expected expectedAll = null;
        
        // publish the share
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esi);
        doTestPublish(grantee, owner, owner, folderPath, expectedDirectOnly, expectedAll);
        
        // re-grant, change the right
        short newRights = ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_ADMIN;
        grantRight(owner, grantee, String.valueOf(folderId), ACL.rightsToString(newRights));
        
        // now should see only the new publish, previous published should be gone
        Expected.ExpectedShareInfo esiWithNewRights = new Expected.ExpectedShareInfo(owner.getId(), owner.getName(), folderId, folderPath, newRights, ACL.GRANTEE_GROUP, grantee.getId(), grantee.getName());
        expectedDirectOnly = new Expected();
        expectedDirectOnly.add(esiWithNewRights);
        doTestPublish(grantee, owner, owner, folderPath, expectedDirectOnly, expectedAll);
    }
    
    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();  // will set SoapProvisioning
        
        init();

        TestUtil.runTest(TestShareInfo.class);
    }
}


/*
 * 
 * 
zmmailbox -z -m user1 cf /Inbox/foo
zmmailbox -z -m user1 cf /Inbox/foo/bar

zmprov cdl dl@phoebe.mac
zmprov adlm dl@phoebe.mac user2@phoebe.mac

zmmailbox -z -m user1 mfg /Inbox         group dl@phoebe.mac rw
zmmailbox -z -m user1 mfg /Calendar      group dl@phoebe.mac arw
zmmailbox -z -m user1 mfg /Sent          account user2@phoebe.mac ar
zmmailbox -z -m user1 mfg /Inbox/foo/bar account user3@phoebe.mac cdwr

zmmailbox -z -m user2 cm /user1s-cal user1 /Calendar

zmprov mdlsi + dl@phoebe.mac user1@phoebe.mac
zmprov gasi user2@phoebe.mac 0



zmmailbox -z -m user1 cf /Inbox/foo
zmmailbox -z -m user1 cf /Inbox/foo/bar

zmmailbox -z -m user1 mfg /Inbox         account user2 rw
zmmailbox -z -m user1 mfg /Inbox/foo/bar account user2 rw
zmmailbox -z -m user1 mfg /Sent          account user2 rw

zmprov masi + user2 user1

zmprov masi + user2 user1 /Inbox
zmprov masi + user2 user1 /Inbox/foo/bar
zmprov masi + user2 user1 /Sent

zmprov gasi user2 0

*/

