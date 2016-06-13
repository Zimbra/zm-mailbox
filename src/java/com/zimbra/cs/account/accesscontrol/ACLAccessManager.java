/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.accesscontrol.generated.UserRights;
import com.zimbra.cs.account.names.NameUtil;


/**
 * @author pshao
 */
public class ACLAccessManager extends AccessManager implements AdminConsoleCapable {

    public ACLAccessManager() throws ServiceException {
        // initialize RightManager
        RightManager.getInstance();
    }

    @Override
    public boolean isAdequateAdminAccount(Account acct) {
        return acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false) ||
               acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
    }

    private Account actualTargetForAdminLoginAs(Account target) throws ServiceException {
        if (target.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            return Provisioning.getInstance().get(Key.CalendarResourceBy.id, target.getId());
        } else {
            return target;
        }
    }

    private AdminRight actualRightForAdminLoginAs(Account target) {
        if (target.isCalendarResource()) {
            return Admin.R_adminLoginCalendarResourceAs;
        } else {
            return Admin.R_adminLoginAs;
        }
    }

    @Override
    public boolean isDomainAdminOnly(AuthToken at) {
        // there is no such thing as domain admin in the realm of ACL checking.
        return false;
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin)
    throws ServiceException {

        if (!StringUtil.equal(at.getAccount().getDomainId(), target.getDomainId())) {
            checkDomainStatus(target);
        }

        if (isParentOf(at, target)) {
            return true;
        }

        if (asAdmin) {
            return canDo(at, actualTargetForAdminLoginAs(target),
                    actualRightForAdminLoginAs(target), asAdmin);
        } else {
            return canDo(at, target, User.R_loginAs, asAdmin);
        }
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target) throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target, boolean asAdmin)
    throws ServiceException {

        checkDomainStatus(target);

        if (isParentOf(credentials, target)) {
            return true;
        }

        if (asAdmin) {
            return canDo(credentials, actualTargetForAdminLoginAs(target),
                    actualRightForAdminLoginAs(target), asAdmin);
        } else {
            return canDo(credentials, target, User.R_loginAs, asAdmin);
        }
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target)
    throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }

    @Override
    public boolean canAccessCos(AuthToken at, Cos cos) throws ServiceException {
        return false;
    }

    @Override
    public boolean canCreateGroup(AuthToken at, String groupEmail)
            throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomainByEmailAddr(groupEmail);
        checkDomainStatus(domain);

        return canDo(at, domain, User.R_createDistList, true);
    }

    @Override
    public boolean canCreateGroup(Account credentials, String groupEmail)
            throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomainByEmailAddr(groupEmail);
        checkDomainStatus(domain);

        return canDo(credentials, domain, User.R_createDistList, true);
    }

    @Override
    public boolean canAccessGroup(AuthToken at, Group group)
            throws ServiceException {
        checkDomainStatus(group);
        return canDo(at, group, Group.GroupOwner.GROUP_OWNER_RIGHT, true);
    }

    @Override
    public boolean canAccessGroup(Account credentials, Group group, boolean asAdmin)
            throws ServiceException {
        checkDomainStatus(group);
        return canDo(credentials, group, Group.GroupOwner.GROUP_OWNER_RIGHT, asAdmin);
    }

    @Override
    public boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);  // should never be called
    }

    @Override
    public boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);  // should never be called
    }

    @Override
    public boolean canAccessEmail(AuthToken at, String email) throws ServiceException {
        throw ServiceException.FAILURE("internal error", null);  // should never be called
    }

    @Override
    public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota)
    throws ServiceException {
        // throw ServiceException.FAILURE("internal error", null);  // should never be called

        // for bug 42896, we now have to do the same check on zimbraDomainAdminMaxMailQuota
        // until we come up with a framework to support constraints on a per admin basis.
        // the following call is ugly!
        return com.zimbra.cs.account.DomainAccessManager.canSetMailQuota(at, targetAccount, mailQuota);
    }

    @Override
    /**
     * User right entrance - do not throw
     */
    public boolean canDo(MailTarget grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        try {
            return canDo(grantee, target, rightNeeded, asAdmin, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("right denied", e);
            return false;
        }
    }

    @Override
    /**
     * User right entrance - do not throw
     */
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin) {
        try {
            return canDo(grantee, target, rightNeeded, asAdmin, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("right denied", e);
            return false;
        }
    }

    @Override
    /**
     * User right entrance - do not throw
     */
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin) {
        try {
            return canDo(granteeEmail, target, rightNeeded, asAdmin, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("right denied", e);
            return false;
        }
    }

    @Override
    public boolean canDo(MailTarget grantee, Entry target, Right rightNeeded,
            boolean asAdmin, ViaGrant via) throws ServiceException {

        // check hard rules
        Boolean hardRulesResult = HardRules.checkHardRules(grantee, asAdmin, target, rightNeeded);
        if (hardRulesResult != null) {
            return hardRulesResult.booleanValue();
        }

        if (checkOverridingRules(grantee, asAdmin, target, rightNeeded)) {
            return true;
        }

        // check pseudo rights
        if (asAdmin) {
            if (rightNeeded == AdminRight.PR_ALWAYS_ALLOW) {
                return true;
            } else if (rightNeeded == AdminRight.PR_SYSTEM_ADMIN_ONLY) {
                return false;
            }
        }

        return checkPresetRight(grantee, target, rightNeeded, false, asAdmin, via);
    }

    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded,
            boolean asAdmin, ViaGrant via) throws ServiceException {
        try {
            Account granteeAcct = AccessControlUtil.authTokenToAccount(grantee, rightNeeded);
            if (granteeAcct != null) {
                return canDo(granteeAcct, target, rightNeeded, asAdmin, via);
            }
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("ACL checking failed", e);
        }

        return false;
    }

    @Override
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded,
            boolean asAdmin, ViaGrant via) throws ServiceException {
        try {
            MailTarget grantee = AccessControlUtil.emailAddrToMailTarget(granteeEmail, rightNeeded);
            if (grantee != null) {
                return canDo(grantee, target, rightNeeded, asAdmin, via);
            }
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("ACL checking failed", e);
        }

        return false;
    }

    @Override
    public boolean canGetAttrs(Account grantee, Entry target, Set<String> attrsNeeded, boolean asAdmin)
    throws ServiceException {

        // check hard rules
        Boolean hardRulesResult = HardRules.checkHardRules(grantee, asAdmin, target, null);
        if (hardRulesResult != null) {
            return hardRulesResult.booleanValue();
        }

        return canGetAttrsInternal(grantee, target, attrsNeeded, false);
    }

    @Override
    public boolean canGetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin)
    throws ServiceException {
        return canGetAttrs(grantee.getAccount(), target, attrs, asAdmin);
    }

    @Override
    public AttrRightChecker getGetAttrsChecker(Account credentials, Entry target, boolean asAdmin)
    throws ServiceException {
        Boolean hardRulesResult = HardRules.checkHardRules(credentials, asAdmin, target, null);

        if (hardRulesResult == Boolean.TRUE) {
            return AllowedAttrs.ALLOW_ALL_ATTRS();
        } else if (hardRulesResult == Boolean.FALSE) {
            return AllowedAttrs.DENY_ALL_ATTRS();
        } else {
            Grantee grantee = Grantee.getGrantee(credentials);
            AttrRightChecker rightChecker = CheckAttrRight.accessibleAttrs(grantee, target, AdminRight.PR_GET_ATTRS, false);
            return rightChecker;
        }
    }

    @Override
    public AttrRightChecker getGetAttrsChecker(AuthToken credentials, Entry target, boolean asAdmin)
    throws ServiceException {
        return getGetAttrsChecker(credentials.getAccount(), target, asAdmin);
    }


    @Override
    // this API does not check constraints
    public boolean canSetAttrs(Account grantee, Entry target, Set<String> attrsNeeded, boolean asAdmin)
    throws ServiceException {

        // check hard rules
        Boolean hardRulesResult = HardRules.checkHardRules(grantee, asAdmin, target, null);
        if (hardRulesResult != null) {
            return hardRulesResult.booleanValue();
        }

        return canSetAttrsInternal(grantee, target, attrsNeeded, false);
    }

    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Set<String> attrs, boolean asAdmin)
    throws ServiceException {
        return canSetAttrs(grantee.getAccount(), target, attrs, asAdmin);
    }

    @Override
    // this API does check constraints
    public boolean canSetAttrs(Account granteeAcct, Entry target, Map<String, Object> attrsNeeded, boolean asAdmin)
    throws ServiceException {

        // check hard rules
        Boolean hardRulesResult = HardRules.checkHardRules(granteeAcct, asAdmin, target, null);
        if (hardRulesResult != null) {
            return hardRulesResult.booleanValue();
        }

        Grantee grantee = Grantee.getGrantee(granteeAcct);
        AllowedAttrs allowedAttrs = CheckAttrRight.accessibleAttrs(grantee, target, AdminRight.PR_SET_ATTRS, false);
        return allowedAttrs.canSetAttrsWithinConstraints(grantee, target, attrsNeeded);
    }

    @Override
    public boolean canSetAttrs(AuthToken grantee, Entry target, Map<String, Object> attrs, boolean asAdmin)
    throws ServiceException {
        return canSetAttrs(grantee.getAccount(), target, attrs, asAdmin);
    }

    @Override
    public boolean canSetAttrsOnCreate(Account grantee, TargetType targetType, String entryName,
            Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        Key.DomainBy domainBy = null;
        String domainStr = null;
        CosBy cosBy = null;
        String cosStr = null;

        if (targetType.isDomained()) {
            String parts[] = EmailUtil.getLocalPartAndDomain(entryName);
            if (parts == null) {
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+entryName, null);
            }

            domainBy = Key.DomainBy.name;
            domainStr = parts[1];
        }

        if (targetType == TargetType.account ||
            targetType == TargetType.calresource) {
            cosStr = (String)attrs.get(Provisioning.A_zimbraCOSId);
            if (cosStr != null) {
                if (Provisioning.isUUID(cosStr)) {
                    cosBy = CosBy.id;
                } else {
                    cosBy = CosBy.name;
                }
            }
        }

        Entry target = PseudoTarget.createPseudoTarget(Provisioning.getInstance(),
                targetType, domainBy, domainStr, false, cosBy, cosStr, entryName);
        return canSetAttrs(grantee, target, attrs, asAdmin);
    }

    @Override
    public boolean canPerform(MailTarget grantee, Entry target,
            Right rightNeeded, boolean canDelegateNeeded,
            Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant)
    throws ServiceException {

        // check hard rules
        Boolean hardRulesResult = HardRules.checkHardRules(grantee, asAdmin, target, rightNeeded);
        if (hardRulesResult != null) {
            return hardRulesResult.booleanValue();
        }

        if (checkOverridingRules(grantee, asAdmin, target, rightNeeded)) {
            return true;
        }

        boolean allowed = false;
        if (rightNeeded.isPresetRight()) {
            allowed = checkPresetRight(grantee, target, rightNeeded, canDelegateNeeded, asAdmin, viaGrant);
        } else if (rightNeeded.isAttrRight()) {
            if (grantee instanceof Account) {
                allowed = checkAttrRight((Account)grantee, target, (AttrRight)rightNeeded, canDelegateNeeded, attrs, asAdmin);
            }
        } else if (rightNeeded.isComboRight()) {
            ComboRight comboRight = (ComboRight)rightNeeded;
            // check all directly and indirectly contained rights
            for (Right right : comboRight.getAllRights()) {
                // via is not set for combo right. maybe we should just get rid of via
                if (!canPerform(grantee, target, right, canDelegateNeeded, attrs, asAdmin, null)) {
                    return false;
                }
            }
            allowed = true;
        }

        return allowed;
    }

    /**
     * Check for cases where the grantee has capabilities that automatically mean they should be able to perform
     * the grant.
     * This is useful for allowing domain admins to do certain things
     * @return true if this grantee should be allowed the right.
     */
    private boolean checkOverridingRules(MailTarget grantee, boolean asAdmin, Entry target, Right rightNeeded) {
        return domainAdminAllowedToChangeUserRight(grantee, asAdmin, target, rightNeeded)
                || checkForDomainAdminAssigningDLowner(grantee, asAdmin, target, rightNeeded);
    }

    /**
     * Bug 88604
     * This allows delegate admins to assign user rights without having to be given the right they want to assign.
     * Currently restricted to just user rights as those seem to be what a delegate admin might want to assign
     */
    private boolean domainAdminAllowedToChangeUserRight(
            MailTarget grantee, boolean asAdmin, Entry target, Right rightNeeded) {
        if (!asAdmin || !rightNeeded.isUserRight() || !(grantee instanceof Account)
                || !(target instanceof MailTarget)) {
            return false;
        }
        Account authedAcct = (Account) grantee;
        MailTarget mailTarget = (MailTarget) target;
        if (!AccessControlUtil.isDelegatedAdmin(authedAcct, asAdmin)) {
            return false;
        }
        try {
            Domain domain = Provisioning.getInstance().getDomain(mailTarget);
            if (domain == null) {
                return false;
            }
            checkDomainStatus(domain);
            Map<String, Object> attrsNeeded = Maps.newHashMap();
            attrsNeeded.put(Provisioning.A_zimbraACE, rightNeeded.getName());
            if (canSetAttrs(authedAcct, target, attrsNeeded, asAdmin)) {
                ZimbraLog.acl.debug(
                        "Right [%s] ALLOWED to '%s' for target '%s' because '%s' is allowed to set '%s' for '%s'",
                        rightNeeded.getName(), authedAcct.getName(), mailTarget.getName(), authedAcct.getName(),
                        Provisioning.A_zimbraACE, mailTarget.getName());
                return true;
            }
        } catch (ServiceException e) {
            return false;
        }
        return false;
    }

    /**
     * Check whether this is a domain admin assigning an owner to a DL.  If so, then having
     * createGroup / createDistributionList caps for the target DL should be sufficient as specifying an owner
     * can be considered to be part of the creation process.  (Note - the equivalent modify caps are not preset rights
     * so "canDo" can't be used on them, hence sticking with the create caps)
     *
     * This is needed because there is a bootstrapping problem for assigning owners to DLs created by a domain admin.
     * Without this over-ride, only a full admin or someone who already has ownDistList rights could assign owners.
     * Also, it isn't possible to assign ownDistList rights to domain admins for all DLs in a domain because
     * the only valid target for ownDistList is a DL.
     * @return true if this grantee should be allowed the right.
     */
    private boolean checkForDomainAdminAssigningDLowner(
            MailTarget grantee, boolean asAdmin, Entry target, Right rightNeeded) {
        if (!UserRights.R_ownDistList.equals(rightNeeded)) {
            return false;
        }
        if ((grantee instanceof Account) && (target instanceof Group)) {
            Account authedAcct = (Account) grantee;
            Group group = (Group) target;
            if (!AccessControlUtil.isDelegatedAdmin(authedAcct, asAdmin)) {
                return false;
            }
            String domainName;
            try {
                domainName = NameUtil.EmailAddress.getDomainNameFromEmail(group.getName());
                Domain domain = Provisioning.getInstance().get(Key.DomainBy.name, domainName);
                if (domain == null) {
                    return false;
                }
                checkDomainStatus(domain);
                Right alternativeRight = group.isDynamic() ? Admin.R_createGroup : Admin.R_createDistributionList;
                if (canDo(authedAcct, domain, alternativeRight, true, null)) {
                    ZimbraLog.acl.debug(
                        "Right [%s] ALLOWED to '%s' for Group '%s' because %s is allowed right [%s] for domain '%s'",
                        rightNeeded.getName(), authedAcct.getName(), group.getName(), authedAcct.getName(),
                        alternativeRight.getName(), domain.getName());
                    return true;
                }
            } catch (ServiceException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean canPerform(AuthToken grantee, Entry target, Right rightNeeded,
            boolean canDelegate, Map<String, Object> attrs, boolean asAdmin, ViaGrant viaGrant)
    throws ServiceException {
        Account authedAcct = grantee.getAccount();
        return canPerform(authedAcct, target, rightNeeded, canDelegate,
                          attrs, asAdmin, viaGrant);
    }


    // all user and admin preset rights go through here
    private boolean checkPresetRight(MailTarget grantee, Entry target,
            Right rightNeeded, boolean canDelegateNeeded, boolean asAdmin, ViaGrant via) {
        try {
            if (grantee == null) {
                if (canDelegateNeeded) {
                    return false;
                }

                if (rightNeeded.isUserRight()) {
                    grantee = GuestAccount.ANONYMOUS_ACCT;
                } else {
                    return false;
                }
            }

            //
            // user right treatment
            //
            if (rightNeeded.isUserRight()) {
                if (target instanceof Account) {
                    // always allow self for user right, self can also delegate(i.e. grant) the right
                    if (((Account)target).getId().equals(grantee.getId())) {
                        return true;
                    }

                    // check the loginAs right and family access - if the right being asked for is not loginAs
                    if (rightNeeded != Rights.User.R_loginAs) {
                        if (canAccessAccount((Account)grantee, (Account)target, asAdmin)) {
                            return true;
                        }
                    }
                }
            } else {
                // not a user right, must have a target object
                // if it is a user right, let it fall through to return the default permission.
                if (target == null) {
                    return false;
                }
            }


            //
            // check ACL
            //
            Boolean result = null;
            if (target != null) {
                result = CheckPresetRight.check(grantee, target, rightNeeded, canDelegateNeeded, via);
            }

            if (result != null && result.booleanValue()) {
                return result.booleanValue();  // allowed by ACL
            } else {
                // either no matching ACL for the right or is not allowed by ACL

                if (canDelegateNeeded) {
                    return false;
                }

                // call the fallback if there is one for the right
                CheckRightFallback fallback = rightNeeded.getFallback();
                if ((fallback != null) && (grantee instanceof Account)) {
                    Boolean fallbackResult = fallback.checkRight((Account)grantee, target, asAdmin);
                    if (fallbackResult != null) {
                        ZimbraLog.acl.debug("checkPresetRight fallback to: " + fallbackResult.booleanValue());
                        return fallbackResult.booleanValue();
                    }
                }

                if (result == null) {
                    // no matching ACL for the right, and no fallback (or no fallback result),
                    // see if there is a configured default
                    Boolean defaultValue = rightNeeded.getDefault();
                    if (defaultValue != null) {
                        ZimbraLog.acl.debug("checkPresetRight default to: " + defaultValue.booleanValue());
                        return defaultValue.booleanValue();
                    }
                }
            }

        } catch (ServiceException e) {
            ZimbraLog.acl.warn("ACL checking failed: " +
                               "grantee=" + grantee.getName() +
                               ", target=" + target.getLabel() +
                               ", right=" + rightNeeded.getName() +
                               " => denied", e);
        }
        return false;
    }

    private boolean checkAttrRight(Account grantee, Entry target,
            AttrRight rightNeeded, boolean canDelegateNeeded,
            Map<String, Object> attrs, boolean asAdmin)
    throws ServiceException {

        TargetType targetType = TargetType.getTargetType(target);
        if (!CheckRight.rightApplicableOnTargetType(targetType, rightNeeded, canDelegateNeeded)) {
            return false;
        }

        boolean allowed = false;

        if (rightNeeded.getRightType() == Right.RightType.getAttrs) {
            allowed = checkAttrRight(grantee, target, rightNeeded, canDelegateNeeded);
        } else {
            if (attrs == null || attrs.isEmpty()) {
                // no attr/value map, just check if all attrs in the right are covered (constraints are not checked)
                allowed = checkAttrRight(grantee, target, rightNeeded, canDelegateNeeded);
            } else {
                // attr/value map is provided, check it (constraints are checked)

                // sanity check, we should *not* be needing "can delegate"
                if (canDelegateNeeded) {
                    throw ServiceException.FAILURE("internal error", null);
                }

                allowed = canSetAttrs(grantee, target, attrs, asAdmin);
            }
        }

        return allowed;
    }

    private boolean checkAttrRight(Account granteeAcct, Entry target,
            AttrRight rightNeeded, boolean canDelegateNeeded) throws ServiceException {
        AllowedAttrs allowedAttrs =
            CheckAttrRight.accessibleAttrs(Grantee.getGrantee(granteeAcct), target, rightNeeded, canDelegateNeeded);
        return allowedAttrs.canAccessAttrs(rightNeeded.getAttrs(), target);
    }

    private boolean canGetAttrsInternal(Account granteeAcct, Entry target,
            Set<String> attrsNeeded, boolean canDelegateNeeded) throws ServiceException {
        AllowedAttrs allowedAttrs =
            CheckAttrRight.accessibleAttrs(Grantee.getGrantee(granteeAcct), target,
                    AdminRight.PR_GET_ATTRS, canDelegateNeeded);

        return allowedAttrs.canAccessAttrs(attrsNeeded, target);
    }

    private boolean canSetAttrsInternal(Account granteeAcct, Entry target,
            Set<String> attrsNeeded, boolean canDelegateNeeded) throws ServiceException {
        AllowedAttrs allowedAttrs =
            CheckAttrRight.accessibleAttrs(Grantee.getGrantee(granteeAcct), target,
                    AdminRight.PR_SET_ATTRS, canDelegateNeeded);
        return allowedAttrs.canAccessAttrs(attrsNeeded, target);
    }


    // ===========================
    // discover user rights
    // ===========================
    @Override
    public Map<Right, Set<Entry>> discoverUserRights(Account credentials, Set<Right> rights,
            boolean onMaster) throws ServiceException {
        DiscoverUserRights discoverRights = new DiscoverUserRights(credentials, rights, onMaster);
        return discoverRights.handle();
    }


    // ===========================
    // AdminConsoleCapable methods
    // ===========================

    @Override
    public void getAllEffectiveRights(RightBearer rightBearer,
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights result) throws ServiceException {
        CollectAllEffectiveRights.getAllEffectiveRights(
                rightBearer, expandSetAttrs, expandGetAttrs, result);
    }

    @Override
    public void getEffectiveRights(RightBearer rightBearer, Entry target,
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {
        CollectEffectiveRights.getEffectiveRights(
                rightBearer, target, expandSetAttrs, expandGetAttrs, result);

    }

    @Override
    public Set<TargetType> targetTypesForGrantSearch() {
        // we want all target types
        return new HashSet<TargetType>(Arrays.asList(TargetType.values()));
    }

}
