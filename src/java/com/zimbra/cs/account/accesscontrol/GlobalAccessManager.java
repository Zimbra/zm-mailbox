package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.Rights.User;

public class GlobalAccessManager extends AccessManager implements AdminConsoleCapable {

    public GlobalAccessManager() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target,
            boolean asAdmin) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        
        checkDomainStatus(target);
        
        if (isGlobalAdmin(at, asAdmin)) 
            return true;
        
        if (isParentOf(at, target)) 
            return true;
        
        return canDo(at, target, User.R_loginAs, asAdmin);
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target)
            throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target,
            boolean asAdmin) throws ServiceException {
        if (credentials == null)
            return false;
        
        checkDomainStatus(target);
        
        // admin auth account will always succeed
        if (AccessControlUtil.isGlobalAdmin(credentials, asAdmin))
            return true;
        
        // parent auth account will always succeed
        if (isParentOf(credentials, target))
            return true;
        
        return canDo(credentials, target, User.R_loginAs, asAdmin);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target)
            throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }

    @Override
    public boolean canAccessCos(AuthToken at, Cos cos) throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        
        return isGlobalAdmin(at);
    }
    
    @Override
    public boolean canAccessDomain(AuthToken at, String domainName)
            throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        checkDomainStatus(domainName);
        
        return isGlobalAdmin(at);
    }

    @Override
    public boolean canAccessDomain(AuthToken at, Domain domain)
            throws ServiceException {
        if (!at.isZimbraUser())
            return false;
        checkDomainStatus(domain);
        
        return isGlobalAdmin(at);
    }

    @Override
    public boolean canAccessEmail(AuthToken at, String email)
            throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        
        // check for family mailbox
        Account targetAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, email, at);
        if (targetAcct != null) {
            if (isParentOf(at, targetAcct))
                return true;
        }
        return canAccessDomain(at, parts[1]);
    }

    @Override
    public boolean canDo(Account grantee, Entry target, Right rightNeeded,
            boolean asAdmin) {
        // TODO Auto-generated method stub
        return AccessControlUtil.isGlobalAdmin(grantee, asAdmin);
    }

    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded,
            boolean asAdmin) {
        
        Account granteeAcct = AccessControlUtil.authTokenToAccount(grantee, rightNeeded);
        if (granteeAcct != null)
            return canDo(granteeAcct, target, rightNeeded, asAdmin);
        else
            return false;
    }

    @Override
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded,
            boolean asAdmin) {
        Account granteeAcct = AccessControlUtil.emailAddrToAccount(granteeEmail, rightNeeded);
        if (granteeAcct != null)
            return canDo(granteeAcct, target, rightNeeded, asAdmin);
        else
            return false;
    }


    @Override
    public AttrRightChecker canGetAttrs(Account credentials,   Entry target, boolean asAdmin) throws ServiceException {
        if (AccessControlUtil.isGlobalAdmin(credentials, asAdmin) == Boolean.TRUE)
            return AllowedAttrs.ALLOW_ALL_ATTRS();
        else
            return AllowedAttrs.DENY_ALL_ATTRS();
    }
    
    @Override
    public AttrRightChecker canGetAttrs(AuthToken credentials, Entry target, boolean asAdmin) throws ServiceException {
        return canGetAttrs(credentials.getAccount(), target, asAdmin);
    }
    
    @Override
    public boolean canGetAttrs(Account credentials, Entry target,
            Set<String> attrs, boolean asAdmin) throws ServiceException {
        return AccessControlUtil.isGlobalAdmin(credentials, asAdmin);
    }

    @Override
    public boolean canGetAttrs(AuthToken credentials, Entry target,
            Set<String> attrs, boolean asAdmin) throws ServiceException {
        return isGlobalAdmin(credentials, asAdmin);
    }

    @Override
    public boolean canModifyMailQuota(AuthToken at, Account targetAccount,
            long mailQuota) throws ServiceException {
        return isGlobalAdmin(at);
    }

    @Override
    public boolean canSetAttrs(Account credentials, Entry target,
            Set<String> attrs, boolean asAdmin) throws ServiceException {
        return AccessControlUtil.isGlobalAdmin(credentials, asAdmin);
    }

    @Override
    public boolean canSetAttrs(AuthToken credentials, Entry target,
            Set<String> attrs, boolean asAdmin) throws ServiceException {
        return isGlobalAdmin(credentials, asAdmin);
    }

    @Override
    public boolean canSetAttrs(Account credentials, Entry target,
            Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        return AccessControlUtil.isGlobalAdmin(credentials, asAdmin);
    }

    @Override
    public boolean canSetAttrs(AuthToken credentials, Entry target,
            Map<String, Object> attrs, boolean asAdmin) throws ServiceException {
        return isGlobalAdmin(credentials, asAdmin);
    }

    @Override
    public boolean isDomainAdminOnly(AuthToken at) {
        return false;
    }
    
    private boolean isGlobalAdmin(AuthToken at) {
        return isGlobalAdmin(at, true);
    }
    
    private boolean isGlobalAdmin(AuthToken at, boolean asAdmin) {
        return asAdmin && at.isAdmin();
    }
    
    
    // ===========================
    // AdminConsoleCapable methods
    // ===========================

    public void getAllEffectiveRights(RightBearer rightBearer, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights result) throws ServiceException {
        CollectAllEffectiveRights.getAllEffectiveRights(rightBearer, expandSetAttrs, expandGetAttrs, result);
    }
    
    public void getEffectiveRights(RightBearer rightBearer, Entry target, 
            boolean expandSetAttrs, boolean expandGetAttrs,
            RightCommand.EffectiveRights result) throws ServiceException {
        CollectEffectiveRights.getEffectiveRights(rightBearer, target, expandSetAttrs, expandGetAttrs, result);
        
    }
    
    public Set<TargetType> targetTypesForGrantSearch() {
        // we want only targets type on which user can grant rights on
        HashSet<TargetType> tts = new HashSet<TargetType>();
        tts.add(TargetType.account);
        tts.add(TargetType.calresource);
        tts.add(TargetType.dl);
        
        return tts;
    }
    
}
