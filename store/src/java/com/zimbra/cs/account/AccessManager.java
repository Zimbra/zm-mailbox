/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;

public abstract class AccessManager {

    private static AccessManager sManager;

    public static AccessManager getInstance() {
        if (sManager == null) {

            // 1. try getting it from global config attr zimbraAdminAccessControlMech.
            //    zimbraAdminAccessControlMech can only be set to an access manager that is
            //    "admin console capable".  That is, it must implement the AdminConsoleCapable
            //    interface.
            try {
                Config config = Provisioning.getInstance().getConfig();
                String accessManager = config.getAttr(Provisioning.A_zimbraAdminAccessControlMech);
                if (accessManager != null) {
                    Provisioning.AdminAccessControlMech am = Provisioning.AdminAccessControlMech.fromString(accessManager);
                    if (am == Provisioning.AdminAccessControlMech.acl)
                        sManager = new com.zimbra.cs.account.accesscontrol.ACLAccessManager();
                    else if (am == Provisioning.AdminAccessControlMech.global)
                        sManager = new com.zimbra.cs.account.accesscontrol.GlobalAccessManager();
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to determine access manager from global config attribute " +
                        Provisioning.A_zimbraAdminAccessControlMech + ", fallback to use LC key " +
                        LC.zimbra_class_accessmanager.key(), e);
            }

            //
            // 2. if not set, fallback to localconfig key zimbra_class_accessmanager.
            //    zimbra_class_accessmanager can be set to any class that is an
            //    AccessManager.  No guarantee it will support the admin console.
            //    Allow this path for backward compatibility.
            //
            if (sManager == null) {
                String className = LC.zimbra_class_accessmanager.value();

                if (className != null && !className.equals("")) {
                    try {
                        sManager = (AccessManager) Class.forName(className).newInstance();
                    } catch (Exception e) {
                        ZimbraLog.account.error("could not instantiate AccessManager interface of class '"
                                + className + "'; defaulting to DomainAccessManager", e);
                    }
                }
            }

            //
            // 3. if still null, default to GlobalAccessManager
            //
            if (sManager == null)
            	sManager = new com.zimbra.cs.account.accesscontrol.GlobalAccessManager();

            if (Zimbra.started()) {
                ZimbraLog.account.info("Initialized access manager: " + sManager.getClass().getCanonicalName());
            }
        }

        return sManager;
    }

    public abstract boolean isDomainAdminOnly(AuthToken at);

    public abstract boolean isAdequateAdminAccount(Account acct);

    public Account getAccount(AuthToken at) throws ServiceException {
        return Provisioning.getInstance().get(AccountBy.id, at.getAccountId(), at);
    }

    protected Account getAdminAccount(AuthToken at) throws ServiceException {
        String adminAcctId = at.getAdminAccountId();
        if (adminAcctId == null)
            return null;
        else
            return Provisioning.getInstance().get(AccountBy.id, adminAcctId, at);
    }

    public Domain getDomain(AuthToken at) throws ServiceException {
        return Provisioning.getInstance().getDomain(getAccount(at));
    }

    public abstract boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) throws ServiceException;
    public abstract boolean canAccessAccount(AuthToken at, Account target) throws ServiceException;

    /** @return Returns whether the specified account's credentials are sufficient
     *  to perform operations on the target account.  <i>Note: This method
     *  checks only for admin access, and passing the same account for
     *  <code>credentials</code> and <code>target</code> will not succeed
     *  for non-admin accounts.</i>
     * @param credentials  The authenticated account performing the action.
     * @param target       The target account for the proposed action.
     * @param asAdmin      If the authenticated account is acting as an admin account */
    public abstract boolean canAccessAccount(Account credentials, Account target, boolean asAdmin) throws ServiceException;
    public abstract boolean canAccessAccount(Account credentials, Account target) throws ServiceException;

    public abstract boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException;
    public abstract boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException;

    public abstract boolean canAccessCos(AuthToken at, Cos cos) throws ServiceException;

    public abstract boolean canCreateGroup(AuthToken at, String groupEmail) throws ServiceException;
    public abstract boolean canCreateGroup(Account credentials, String groupEmail) throws ServiceException;
    public abstract boolean canAccessGroup(AuthToken at, Group group) throws ServiceException;
    public abstract boolean canAccessGroup(Account credentials, Group group, boolean asAdmin) throws ServiceException;

    public abstract boolean canAccessEmail(AuthToken at, String email) throws ServiceException;

    public abstract boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException;

    /**
     * Returns true if authAccount should be allowed access to private data in appointments owned
     * by targetAccount.  Returns true if authAccount and targetAccount are the same account or if
     * authAccount has admin rights over targetAccount.
     *
     * @param authAccount
     * @param targetAccount
     * @param asAdmin true if authAccount is authenticated with admin privileges
     * @return
     * @throws com.zimbra.common.service.ServiceException
     */
    public boolean allowPrivateAccess(Account authAccount, Account targetAccount, boolean asAdmin)
    throws ServiceException {
        if (authAccount != null && targetAccount != null) {
            if (authAccount.getId().equalsIgnoreCase(targetAccount.getId()))
                return true;
            if (canAccessAccount(authAccount, targetAccount, asAdmin))
                return true;
        }
        return false;
    }

    protected boolean isParentOf(AuthToken at, Account target) throws ServiceException {

        /*
         * first, try using the admin account in the token
         */
        /*  uncomment after we allow the parent to grab a delegated auth token.
        try {
            Account adminAcct = getAdminAccount(at);
            if (adminAcct != null) {
                if (isParentOf(adminAcct, target))
                    return true;
            }
        } catch (ServiceException e) {
            // not an admin account
        }
        */
        Account acct = getAccount(at);
        return isParentOf(acct, target);
    }

    /** Returns whether the specified account's credentials indicationg that
     *  it is the parent account of the target account.
     *  <i>Note: This method checks only for family parent account access,
     *  and passing the same account for <code>credentials</code> and
     *  <code>target</code> will not succeed for non-parent accounts.</i>
     * @param credentials  The authenticated account performing the action.
     * @param target       The target account for the proposed action. */
    protected boolean isParentOf(Account credentials, Account target) {

        Set<String> childAccts = credentials.getMultiAttrSet(Provisioning.A_zimbraChildAccount);
        String targetId = target.getId();

        if (childAccts.contains(targetId))
            return true;

        return false;
    }

    protected void checkDomainStatus(Account acct) throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomain(acct);
        checkDomainStatus(domain);
    }

    protected void checkDomainStatus(Group group) throws ServiceException {
        Domain domain = group.getDomain();
        checkDomainStatus(domain);
    }

    protected void checkDomainStatus(String domainName) throws ServiceException {
        Domain domain = Provisioning.getInstance().get(Key.DomainBy.name, domainName);
        checkDomainStatus(domain);
    }

    public void checkDomainStatus(Domain domain) throws ServiceException {
        if (domain != null) {
            if (domain.isSuspended() || domain.isShutdown())
                throw ServiceException.PERM_DENIED("domain is " + domain.getDomainStatusAsString());
        }
    }


    //
    // ACL based methods
    //

    /*
     * ====================
     * user right entrances
     * ====================
     */
    public abstract boolean canDo(MailTarget grantee,     Entry target, Right rightNeeded, boolean asAdmin);
    public abstract boolean canDo(AuthToken grantee,   Entry target, Right rightNeeded, boolean asAdmin);
    public abstract boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin);


    /*
     * =====================
     * admin right entrances
     * =====================
     */
    // for admin calls to return the decisive grant that lead to the result
    public static class ViaGrant {
        private ViaGrant mImpl;
        public void setImpl(ViaGrant impl) { mImpl = impl;}
        public String getTargetType()   { return (mImpl==null)?null:mImpl.getTargetType(); }
        public String getTargetName()   { return (mImpl==null)?null:mImpl.getTargetName(); }
        public String getGranteeType()  { return (mImpl==null)?null:mImpl.getGranteeType(); }
        public String getGranteeName()  { return (mImpl==null)?null:mImpl.getGranteeName(); }
        public String getRight()        { return (mImpl==null)?null:mImpl.getRight(); }
        public boolean isNegativeGrant(){ return (mImpl==null)?false:mImpl.isNegativeGrant(); }

        public boolean available() { return mImpl != null; }

        @Override
        public String toString() {
            if (mImpl == null) {
                return MoreObjects.toStringHelper(this).toString();
            }
            return MoreObjects.toStringHelper(this)
                .add("targetType", mImpl.getTargetType())
                .add("targetName", mImpl.getTargetName())
                .add("granteeType", mImpl.getGranteeType())
                .add("granteeName", mImpl.getGranteeName())
                .add("right", mImpl.getRight())
                .add("negativeGrant", mImpl.isNegativeGrant()).toString();
        }
    }

    public boolean canDo(MailTarget grantee, Entry target, Right rightNeeded, boolean asAdmin,
            ViaGrant viaGrant) throws ServiceException {
        return canDo(grantee, target, rightNeeded, asAdmin);
    }

    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin,
            ViaGrant viaGrant) throws ServiceException {
        return canDo(grantee, target, rightNeeded, asAdmin);
    }

    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin,
            ViaGrant viaGrant) throws ServiceException {
        return canDo(granteeEmail, target, rightNeeded, asAdmin);
    }


    /**
     * returns if the specified account's credentials can get the specified attrs on target
     *
     * @param credentials The authenticated account performing the action.
     * @param target      The target entry.
     * @param attrs       Attrs to get.
     * @param asAdmin     If the authenticated account is acting as an admin account.
     * @return
     * @throws ServiceException
     */
    public abstract boolean canGetAttrs(Account credentials,
            Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;
    public abstract boolean canGetAttrs(AuthToken credentials,
            Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;


    public interface AttrRightChecker {
        /**
         * returns if the specified attr is allowed
         *
         * @param attrName
         * @return
         * @throws ServiceException
         */
        public boolean allowAttr(String attrName);
    }

    /**
     * returns an AttrRightChecker for the specified target
     *
     * @param credentials
     * @param target
     * @param asAdmin
     * @return
     * @throws ServiceException
     */
    public AttrRightChecker getGetAttrsChecker(Account credentials,
            Entry target, boolean asAdmin) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }

    public AttrRightChecker getGetAttrsChecker(AuthToken credentials,
            Entry target, boolean asAdmin) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }


    /**
     *
     * returns if the specified account's credentials can set the specified attrs on target
     * constraints are not checked
     *
     * @param credentials The authenticated account performing the action.
     * @param target      The target entry.
     * @param attrs       Attrs to set.
     * @param asAdmin     If the authenticated account is acting as an admin account.
     * @return
     * @throws ServiceException
     */
    public abstract boolean canSetAttrs(Account credentials,
            Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;
    public abstract boolean canSetAttrs(AuthToken credentials,
            Entry target, Set<String> attrs, boolean asAdmin) throws ServiceException;


    /**
     * returns if the specified account's credentials can set the specified attrs to
     * the specified values on target.
     *
     * constraints are checked.
     *
     * @param credentials The authenticated account performing the action.
     * @param target      The target entry.
     * @param attrs       Attrs/values to set.
     * @param asAdmin     If the authenticated account is acting as an admin account.
     * @return
     * @throws ServiceException
     */
    public abstract boolean canSetAttrs(Account credentials,
            Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException;
    public abstract boolean canSetAttrs(AuthToken credentials,
            Entry target, Map<String, Object> attrs, boolean asAdmin) throws ServiceException;

    public boolean canSetAttrsOnCreate(Account credentials, TargetType targetType, String entryName,
            Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }

    public Map<Right, Set<Entry>> discoverUserRights(Account credentials, Set<Right> rights, boolean onMaster)
    throws ServiceException {
        return Maps.newHashMap();  // return empty result
    }

    // for access manager internal use and unittest only, do not call this API, use the canDo API instead.
    public boolean canPerform(MailTarget credentials, Entry target,
            Right rightNeeded, boolean canDelegate,
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant)
    throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }

    // for access manager internal use and unittest only, do not call this API, use the canDo API instead.
    public boolean canPerform(AuthToken credentials, Entry target,
            Right rightNeeded, boolean canDelegate,
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant)
    throws ServiceException {
        throw ServiceException.FAILURE("not supported", null);
    }

    public boolean canSendAs(Account grantee, Account targetAccount, String targetAddress, boolean asAdmin)
            throws ServiceException {
        return canSendInternal(grantee, targetAccount, targetAddress, User.R_sendAs, asAdmin);
    }

    public boolean canSendOnBehalfOf(Account grantee, Account targetAccount, String targetAddress, boolean asAdmin)
            throws ServiceException {
        return canSendInternal(grantee, targetAccount, targetAddress, User.R_sendOnBehalfOf, asAdmin);
    }

    private boolean canSendInternal(Account grantee, Account targetAccount, String targetAddress, Right sendRight, boolean asAdmin)
            throws ServiceException {
        boolean allowed = false;
        Right dlSendRight;
        if (User.R_sendAs.equals(sendRight)) {
            dlSendRight = User.R_sendAsDistList;
        } else if (User.R_sendOnBehalfOf.equals(sendRight)) {
            dlSendRight = User.R_sendOnBehalfOfDistList;
        } else {
            throw ServiceException.FAILURE("invalid send right " + sendRight, null);
        }
        NamedEntry target = null;
        if (AccountUtil.addressHasInternalDomain(targetAddress)) {
            // If targetAddress has an internal domain, it could be another account or a distribution list.
            Provisioning prov = Provisioning.getInstance();
            if (prov.isDistributionList(targetAddress)) {
                target = prov.getGroupBasic(DistributionListBy.name, targetAddress);
                sendRight = dlSendRight;
            } else {
                target = prov.get(AccountBy.name, targetAddress);
            }
        } else if (targetAccount != null) {
            // If targetAddress has an external domain, it must be a zimbraAllowFromAddress of the target account.
            Set<String> addrs = new HashSet<String>();
            String[] allowedFromAddrs = targetAccount.getMultiAttr(Provisioning.A_zimbraAllowFromAddress);
            for (String addr : allowedFromAddrs) {
                addrs.add(addr.toLowerCase());
            }
            if (addrs.contains(targetAddress.toLowerCase())) {
                target = targetAccount;
            }
        }
        if (target != null) {
            allowed = canDo(grantee, target, sendRight, asAdmin);
            if (allowed && !asAdmin) {
                // Admins can send as any address of the target.  Non-admins can only use the addresses designated
                // by the target user/DL.
                allowed = AccountUtil.isAllowedSendAddress(target, targetAddress);
            }
        }
        return allowed;
    }
}
