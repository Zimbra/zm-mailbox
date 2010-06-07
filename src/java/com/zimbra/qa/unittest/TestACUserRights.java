package com.zimbra.qa.unittest;

import java.util.List;
import java.util.ArrayList;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.zclient.ZAce;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZMailbox;

/*
 * TODO: move all user right specific testing from TestACLGrantee 
 *       to this class, and deprecate TestACLGrantee.
 *
 */


public class TestACUserRights extends TestProv {
    
    private int grantFolderRight(Account owner, Account grantee, String folderPath, String rights) 
    throws ServiceException {
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        ZFolder folder = TestUtil.createFolder(ownerMbox, folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, grantee.getName(), rights, null);
        
        return Integer.valueOf(folder.getId());
    }
    
    // bug 42146
    public void testFallbackToFolderRight() throws Exception {
        
        useSoapProv();
        
        Domain domain = createDomain();
        
        // grantees
        Account allowed = createUserAccount("allowed", domain);
        Account denied = createUserAccount("denied", domain);
        Account noAclButHasFolderGrant = createUserAccount("noAclButHasFolderGrant", domain);
        Account noAclAndNoFolderGrant = createUserAccount("noAclAndNoFolderGrant", domain);
        
        // owner
        Account owner = createUserAccount("owner", domain);
        
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        
        // grant account right
        ZAce aceAllow = new ZAce(ZAce.GranteeType.usr, allowed.getId(), allowed.getName(), "invite", false, null);
        ownerMbox.grantPermission(aceAllow);
        ZAce aceDeny = new ZAce(ZAce.GranteeType.usr, denied.getId(), denied.getName(), "invite", true, null);
        ownerMbox.grantPermission(aceDeny);
        
        // grant folder right
        String folderPath = "/Calendar";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_INSERT | ACL.RIGHT_DELETE;
        String rightsStr = ACL.rightsToString(rights);
        ZFolder folder = ownerMbox.getFolder(folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, denied.getName(), rightsStr, null);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, noAclButHasFolderGrant.getName(), rightsStr, null);
        
        // check permission
        List<String> rightsToCheck = new ArrayList<String>();
        rightsToCheck.add("invite");
        boolean result;
            
        result = TestUtil.getZMailbox(allowed.getName()).checkPermission(owner.getName(), rightsToCheck);
        assertTrue(result);
         
        result = TestUtil.getZMailbox(denied.getName()).checkPermission(owner.getName(), rightsToCheck);
        assertTrue(result);
        
        result = TestUtil.getZMailbox(noAclButHasFolderGrant.getName()).checkPermission(owner.getName(), rightsToCheck);
        assertTrue(result);
        
        result = TestUtil.getZMailbox(noAclAndNoFolderGrant.getName()).checkPermission(owner.getName(), rightsToCheck);
        assertFalse(result);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup("INFO");
        // TestACL.logToConsole("DEBUG");
        
        TestUtil.runTest(TestACUserRights.class);
    }
}
