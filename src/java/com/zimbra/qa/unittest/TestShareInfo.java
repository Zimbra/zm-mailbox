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
import com.zimbra.cs.account.ShareInfo.Publishing;
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
     */
    private void createFolderAndGrantRight(Account owner, Account grantee, String folderPath, String rights) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, grantee.getName(), rights, null);
    }
    
    /*
     * create a folder and grant the rights to a distribution list
     */
    private void createFolderAndGrantRight(Account owner, DistributionList grantee, String folderPath, String rights) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.grp, grantee.getName(), rights, null);
    }
    
    private ZFolder createFolder(Account owner, String folderPath) 
        throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        return folder;
    }
    
    static class Expected {
        // TODO
    }
    
    // TODO, get the getShareInfo SOAP is implemented
    private void verify(Account entry, List<Publishing> shareInfo) {
        
    }
    
    private void doTestPublishShareInfo(Account publishingEntry, Account owner, String folderPath, Expected expected) 
        throws ServiceException {
        
        List<Publishing> shareInfo = new ArrayList<Publishing>();
        Publishing si = new Publishing(Publishing.Action.add, owner.getId(), folderPath, null);
        shareInfo.add(si);
        mProv.modifyShareInfo(publishingEntry, shareInfo);
        
        // TODO: verify
    }
    
    private void doTestPublishShareInfo(DistributionList publishingEntry, Account owner, String folderPath, Expected expected) 
        throws ServiceException {
        
        List<Publishing> shareInfo = new ArrayList<Publishing>();
        Publishing si = new Publishing(Publishing.Action.add, owner.getId(), folderPath, null);
        shareInfo.add(si);
        mProv.modifyShareInfo(publishingEntry, shareInfo);
        
        // TODO: verify
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
        createFolderAndGrantRight(owner, grantee, folderPath, "rw");
        
        doTestPublishShareInfo(grantee, owner, folderPath, null);
        doTestPublishShareInfo(otherAcct, owner, folderPath, null);
        
        // publishing ones own folder to himself, no share info should be published
        doTestPublishShareInfo(owner, owner, folderPath, null);
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
        createFolderAndGrantRight(owner, grantee, folderPath, "rw");
        
        // create a sub folder
        String subFolderPath = "/test/sub";
        createFolder(owner, subFolderPath);
        
        doTestPublishShareInfo(grantee, owner, subFolderPath, null);
        doTestPublishShareInfo(otherAcct, owner, subFolderPath, null);
        
        // publishing ones own folder to himself, no share info should be published
        doTestPublishShareInfo(owner, owner, folderPath, null);
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
        createFolderAndGrantRight(owner, grantee, folderPath, "rw");
        
        // create a sub folder
        String subFolderPath = "/test/sub";
        createFolder(owner, subFolderPath);
        
        doTestPublishShareInfo(grantee, owner, folderPath, null);
        doTestPublishShareInfo(acctInDl, owner, folderPath, null);
        doTestPublishShareInfo(acctNotInDl, owner, folderPath, null);
        doTestPublishShareInfo(dlInDL, owner, folderPath, null);
        doTestPublishShareInfo(dlNotInDL, owner, folderPath, null);
        
        doTestPublishShareInfo(grantee, owner, subFolderPath, null);
        doTestPublishShareInfo(acctInDl, owner, subFolderPath, null);
        doTestPublishShareInfo(acctNotInDl, owner, subFolderPath, null);
        doTestPublishShareInfo(dlInDL, owner, subFolderPath, null);
        doTestPublishShareInfo(dlNotInDL, owner, subFolderPath, null);
    }

    public static void main(String[] args) throws Exception {
        TestUtil.cliSetup();  // will set SoapProvisioning
        
        init();

        TestUtil.runTest(TestShareInfo.class);
    }
}
