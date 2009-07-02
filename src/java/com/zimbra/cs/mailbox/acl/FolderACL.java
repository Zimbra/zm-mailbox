package com.zimbra.cs.mailbox.acl;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;

/*
 * TODO:
 *   1. move/refactor com.zimbra.cs.mailbox.ACL to this package
 *      and change ACL.encode, ACL.getGrantedRights back to non-public
 *      
 *   2. merge getAuthenticatedAccount with Mailbox.getAuthenticatedAccount
 *      (extract the octxt and call a static Mailbox method to do the logic)
 *      
 *   3. merge isUsingAdminPrivileges with Mailbox.isUsingAdminPrivileges
 *      (extract the octxt and call a static Mailbox method to do the logic)   
 */

public class FolderACL {
    
    OperationContext mOctxt;
    ShareTarget mShareTarget;
    
    private static class ShareTarget {
        private Account mOwnerAcct;
        int mFolderId;
        
        private ShareTarget(String ownerAcctId, int folderId) throws ServiceException {
            mOwnerAcct = Provisioning.getInstance().get(AccountBy.id, ownerAcctId);
            if (mOwnerAcct == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(ownerAcctId);
            
            mFolderId = folderId;
        }
        
        private Account getAccount() {
            return mOwnerAcct;
        }
        
        private String getAccountId() {
            return mOwnerAcct.getId();
        }
        
        int getFolderId() {
            return mFolderId;
        }
        
        boolean onLocalServer() throws ServiceException {
            return Provisioning.onLocalServer(mOwnerAcct);
            // return false;
        }
    }
    
    /**
     * 
     * @param octxt        The context (authenticated user, redo player, other
     *                     constraints) under which this operation is executed.
     * @param ownerAcctId  Onwer account id of the folder.
     * @param folderId     The folder whose permissions we need to query.
     * 
     * @throws ServiceException
     */
    public FolderACL(OperationContext octxt, String ownerAcctId, int folderId) throws ServiceException {
        mOctxt = octxt;
        mShareTarget = new ShareTarget(ownerAcctId, folderId);
    }
    
    /**
     * 
     * @return
     * @throws ServiceException
     * 
     * @see Mailbox.getEffectivePermissions()
     */
    public short getEffectivePermissions() throws ServiceException {
        // use ~0 to query *all* rights; may need to change this when we do negative rights
        short rightsNeeded = (short)~0;
        return checkRights(rightsNeeded);
    }
    
    /**
     * 
     * @param rightsNeeded
     * @return
     * @throws ServiceException
     * 
     * @see MailItem.canAccess()
     */
    public boolean canAccess(short rightsNeeded) throws ServiceException {
        short hasRights = checkRights(rightsNeeded);
        return (hasRights & rightsNeeded) == rightsNeeded;
    }
    
    /**
     * 
     * @param rightsNeeded
     * @return
     * @throws ServiceException
     * 
     * @see Folder.checkRights()
     */
    private short checkRights(short rightsNeeded) throws ServiceException {
        if (mShareTarget.onLocalServer())
            return checkRightsLocal(rightsNeeded);
        else
            return checkRightsRemote(rightsNeeded);
    }
    
    /**
     * mimicing Mailbox.getAuthenticatedAccount()
     * 
     * @return
     * @throws ServiceException
     * 
     * @see Mailbox.getAuthenticatedAccount()
     */
    private Account getAuthenticatedAccount() throws ServiceException {
        Account authuser = null;
        if (mOctxt != null)
            authuser = mOctxt.getAuthenticatedUser();
        
        /* Mailbox.getAuthenticatedAccount() says:
        // XXX if the current authenticated user is the owner, it will return null.
        // later on in Folder.checkRights(), the same assumption is used to validate
        // the access.
         * 
         * unfortunately we can't do the same here, because the path to here could 
         * be not equipped with stuffing a public user in the octxt, or the octxt 
         * can be null.  So we do it here, and our FolderACL.checkRights will *not* 
         * make the same assumption that a nukk auth user means the owner himself.
         * it always requirs a non-null auth user.
         */
        
        /*
        if (authuser != null && authuser.getId().equals(mShareTarget.getAccountId()))
            authuser = null;
        */  
        if (authuser == null)
            authuser = ACL.ANONYMOUS_ACCT;
        
        return authuser;
    }

    /**
     * 
     * @return
     * 
     * @see Mailbox.isUsingAdminPrivileges()
     */
    private boolean isUsingAdminPrivileges() {
        return mOctxt != null && mOctxt.isUsingAdminPrivileges();

    }
    
    /*
     * check rights locally the normal way
     */
    private short checkRightsLocal(short rightsNeeded) throws ServiceException {
        Mailbox ownerMbx = MailboxManager.getInstance().getMailboxByAccountId(mShareTarget.getAccountId(), false);
        short hasRights = ownerMbx.getEffectivePermissions(mOctxt, mShareTarget.getFolderId(), MailItem.TYPE_FOLDER);
        return (short)(hasRights & rightsNeeded);
    }
    
    private short checkRightsRemote(short rightsNeeded) throws ServiceException {
        Account authedAcct = getAuthenticatedAccount();
        return checkRightsRemote(rightsNeeded, authedAcct, isUsingAdminPrivileges());
    }
    
    private short checkRightsRemote(short rightsNeeded, Account authuser, boolean asAdmin) throws ServiceException {
        
        if (rightsNeeded == 0)
            return rightsNeeded;
        
        // the mailbox owner can do anything they want
        if (authuser.getId().equals(mShareTarget.getAccountId()))
            return rightsNeeded;
        
        // admin users (and the appropriate domain admins) can also do anything they want
        if (AccessManager.getInstance().canAccessAccount(authuser, mShareTarget.getAccount(), asAdmin))
            return rightsNeeded;
        
        Short granted = null;
         
        // check the ACLs to see if access has been explicitly granted
        ACL rights = getEffectiveACLFromCache();
        if (rights != null)
            granted = rights.getGrantedRights(authuser);
        else
            granted = getEffectivePermissionsFromServer();
        
        if (granted != null)
            return (short) (granted.shortValue() & rightsNeeded);

        return 0;
    }
    
    /*
     * returns null if not in cache
     */
    private ACL getEffectiveACLFromCache() throws ServiceException {
        return EffectiveACLCache.get(mShareTarget.getAccountId(), mShareTarget.getFolderId());
    }

    private Short getEffectivePermissionsFromServer() throws ServiceException {
        Element request = new XMLElement(MailConstants.GET_EFFECTIVE_FOLDER_PERMS_REQUEST);
        
        ItemId iid = new ItemId(mShareTarget.getAccountId(), mShareTarget.getFolderId());
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, iid.toString((Account)null));

        Server server = Provisioning.getInstance().getServer(mShareTarget.getAccount());
        String url = URLUtil.getSoapURL(server, false);
        SoapHttpTransport transport = new SoapHttpTransport(url);
        
        if (mOctxt != null && mOctxt.getAuthToken() != null)
            transport.setAuthToken(mOctxt.getAuthToken().toZAuthToken());
        
        Short perms = null;
        try {
            Element response = transport.invoke(request);
            Element eFolder = response.getElement(MailConstants.E_FOLDER);
            String permsStr = eFolder.getAttribute(MailConstants.A_RIGHTS);
            perms = Short.valueOf(ACL.stringToRights(permsStr));
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("cannot get effective perms from server " + server.getName(), e);
        } catch (IOException e) {
            ZimbraLog.misc.warn("cannot get effective perms from server " + server.getName(), e);
        } finally {
            transport.shutdown();
        }
        
        return perms;
    }
    
    private static String formatRights(short rights) {
        return ACL.rightsToString(rights) + "(" + rights + ")";
    }
    
    /*
     * To test remote: hardcode ShareTarget.onLocalServer() to return false.
     *                 make sure you change it back after testing
     */
    private static void doTest(String authedAcctName, String ownerAcctId, int targetFolderId,
            short expectedEffectivePermissions, 
            short needRightsForCheckRightsTest, short expectedCheckRights,
            short needRightsForCanAccessTest, boolean expectedCanAccess) throws ServiceException {
        
        Account authedAcct = Provisioning.getInstance().get(AccountBy.name, authedAcctName);
        OperationContext octxt = new OperationContext(authedAcct);
        FolderACL folderAcl = new FolderACL(octxt, ownerAcctId, targetFolderId);
        
        short effectivePermissions = folderAcl.getEffectivePermissions();
        short checkRights = folderAcl.checkRights(needRightsForCheckRightsTest);
        boolean canAccess = folderAcl.canAccess(needRightsForCanAccessTest);
        
        String result = "***FAILED***";
        
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
        
        if (actual == expected &&
            checkRights == expectedCheckRights &&
            canAccess == expectedCanAccess) {
            result = "";
        }
        System.out.println();
        System.out.println("authedAcctName=" + authedAcctName + "  targetFolderId=" + targetFolderId + " " + result);
        System.out.println("    effectivePermissions: " + formatRights(effectivePermissions) + " (expected: " + formatRights(expectedEffectivePermissions) + ")");
        System.out.println("    checkRights:          " + formatRights(checkRights)          + " (expected: " + formatRights(expectedCheckRights) + ")");
        System.out.println("    canAccess:            " + canAccess                          + " (expected: " + expectedCanAccess + ")");
    }
    
    public static void main(String[] args) throws ServiceException {
        
        com.zimbra.cs.db.DbPool.startup();
        com.zimbra.cs.memcached.MemcachedConnector.startup();
        /*
         * setup owner(user1) folders and grants with zmmailbox or webclient:
         * 
         * /inbox/sub1       share with user2 with w right
         *                   share with user3 with rw rights
         *                   
         * /inbox/sub1/sub2  should inherit grants from sub1
         * 
         * zmmailbox -z -m user1 cf /inbox/sub1
         * zmmailbox -z -m user1 cf /inbox/sub1/sub2
         * zmmailbox -z -m user1 mfg /inbox/sub1 account user2 w
         * zmmailbox -z -m user1 mfg /inbox/sub1 account user3 rw
         * 
         * To setup memcached:
         * zmprov mcf zimbraMemcachedClientServerList 'localhost:11211'
         * /opt/zimbra/memcached/bin/memcached -vv       
         */
        Account ownerAcct = Provisioning.getInstance().get(AccountBy.name, "user1");
        Mailbox ownerMbx = MailboxManager.getInstance().getMailboxByAccountId(ownerAcct.getId(), false);
        Folder inbox = ownerMbx.getFolderByPath(null, "/inbox");
        Folder sub1 = ownerMbx.getFolderByPath(null, "/inbox/sub1");
        Folder sub2 = ownerMbx.getFolderByPath(null, "/inbox/sub1/sub2");
        
        // the owner itself accessing, should have all rights
        doTest("user1", ownerAcct.getId(), inbox.getId(), (short)~0, ACL.RIGHT_READ, ACL.RIGHT_READ, ACL.RIGHT_ADMIN, true);
        doTest("user1", ownerAcct.getId(), sub1.getId(),  (short)~0, ACL.RIGHT_READ, ACL.RIGHT_READ, ACL.RIGHT_ADMIN, true);
        doTest("user1", ownerAcct.getId(), sub2.getId(),  (short)~0, ACL.RIGHT_READ, ACL.RIGHT_READ, ACL.RIGHT_ADMIN, true);
        
        doTest("user2", ownerAcct.getId(), inbox.getId(), (short)0,           ACL.RIGHT_WRITE, (short)0,        ACL.RIGHT_WRITE, false);
        doTest("user2", ownerAcct.getId(), sub1.getId(),  ACL.RIGHT_WRITE,    ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, true);
        doTest("user2", ownerAcct.getId(), sub2.getId(),  ACL.RIGHT_WRITE,    ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, true);

        doTest("user3", ownerAcct.getId(), inbox.getId(), (short)0,                                   ACL.RIGHT_WRITE, (short)0,        ACL.RIGHT_WRITE, false);
        doTest("user3", ownerAcct.getId(), sub1.getId(),  (short)(ACL.RIGHT_READ|ACL.RIGHT_WRITE),    ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, true);
        doTest("user3", ownerAcct.getId(), sub2.getId(),  (short)(ACL.RIGHT_READ|ACL.RIGHT_WRITE),    ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, ACL.RIGHT_WRITE, true);
       
    }
}
