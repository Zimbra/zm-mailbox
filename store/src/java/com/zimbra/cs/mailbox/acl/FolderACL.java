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
package com.zimbra.cs.mailbox.acl;

import java.io.IOException;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
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

    // caches whether the authed account can access the target account as an admin or via the loginAs right
    // only looked at if the authed account is not the owner of the target account
    // callsites of FolderACL can construct FolderACL with a non-null canAccessOwnerAccount value to bypass
    // the AccessManager.getInstance().canAccessAccount() call if it has been evaluated else where
    // (e.g. if called from a CheckRightCallback).
    Boolean mCanAccessOwnerAccount;

    private static class ShareTarget {
        private Account mOwnerAcct;
        int mFolderId;

        private ShareTarget(String ownerAcctId, int folderId) throws ServiceException {
            mOwnerAcct = Provisioning.getInstance().get(AccountBy.id, ownerAcctId);
            if (mOwnerAcct == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(ownerAcctId);

            mFolderId = folderId;
        }

        private ShareTarget(Account ownerAcct, int folderId) throws ServiceException {
            mOwnerAcct = ownerAcct;
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
        mCanAccessOwnerAccount = null;
    }

    public FolderACL(OperationContext octxt, Account ownerAcct, int folderId, Boolean canAccessOwnerAccount) throws ServiceException {
        mOctxt = octxt;
        mShareTarget = new ShareTarget(ownerAcct, folderId);
        mCanAccessOwnerAccount = canAccessOwnerAccount;
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
        return checkRights(rightsNeeded, getAuthenticatedAccount(), isUsingAdminPrivileges());
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
        // XXX if the current authenticated user is the owner, it will return null.
        // later on in Folder.checkRights(), the same assumption is used to validate
        // the access.
        if (authuser != null && authuser.getId().equals(mShareTarget.getAccountId()))
            authuser = null;
        return authuser;
    }

    private boolean canAccessOwnerAccount(Account authuser, boolean asAdmin) throws ServiceException {
        if (mCanAccessOwnerAccount == null) {
            boolean caa = AccessManager.getInstance().canAccessAccount(authuser, mShareTarget.getAccount(), asAdmin);
            mCanAccessOwnerAccount = Boolean.valueOf(caa);
        }
        return mCanAccessOwnerAccount.booleanValue();
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
     * merge with Folder.checkRights?
     */
    private short checkRights(short rightsNeeded, Account authuser, boolean asAdmin) throws ServiceException {

        if (rightsNeeded == 0)
            return rightsNeeded;

        // XXX: in getAuthenticatedAccount, authuser is set to null if authuser == owner.
        // the mailbox owner can do anything they want
        if (authuser == null || authuser.getId().equals(mShareTarget.getAccountId()))
            return rightsNeeded;

        // check admin access and login right
        if (canAccessOwnerAccount(authuser, asAdmin))
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

    /*
     * if we are here, has had a cache miss
     */
    private Short getEffectivePermissionsFromServer() throws ServiceException {
        if (mShareTarget.onLocalServer())
            return getEffectivePermissionsLocal();
        else
            return getEffectivePermissionsRemote();
    }

    private Short getEffectivePermissionsLocal() throws ServiceException {
        Mailbox ownerMbx = MailboxManager.getInstance().getMailboxByAccountId(mShareTarget.getAccountId());
        Folder folder = ownerMbx.getFolderById(null, mShareTarget.getFolderId());
        return getEffectivePermissionsLocal(mOctxt, ownerMbx, folder);
    }

    public static Short getEffectivePermissionsLocal(OperationContext octxt, Mailbox ownerMbx, Folder folder) throws ServiceException {

        // cache the effective folder ACL in memcached - independent of the authed user
        ACL acl = folder.getEffectiveACL();
        EffectiveACLCache.put(folder.getAccount().getId(), folder.getId(), acl);

        // return the effective permission - auth user dependent
        return ownerMbx.getEffectivePermissions(octxt.getAuthenticatedUser(), octxt.isUsingAdminPrivileges(),
                folder.getId(), MailItem.Type.FOLDER);
    }

    private Short getEffectivePermissionsRemote() throws ServiceException {

        Element request = new XMLElement(MailConstants.GET_EFFECTIVE_FOLDER_PERMS_REQUEST);

        ItemId iid = new ItemId(mShareTarget.getAccountId(), mShareTarget.getFolderId());
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, iid.toString((Account)null));

        Server server = Provisioning.getInstance().getServer(mShareTarget.getAccount());
        String url = URLUtil.getSoapURL(server, false);
        SoapHttpTransport transport = new SoapHttpTransport(url);

        AuthToken authToken = null;
        if (mOctxt != null)
            authToken = AuthToken.getCsrfUnsecuredAuthToken(mOctxt.getAuthToken());
        if (authToken == null)
            authToken = AuthProvider.getAuthToken(GuestAccount.ANONYMOUS_ACCT);
        transport.setAuthToken(authToken.toZAuthToken());
        transport.setTargetAcctId(mShareTarget.getAccountId());

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

}
