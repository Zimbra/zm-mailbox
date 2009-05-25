package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.ACLAccessManager;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttrRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;

/**
 * This class serves as:
 * 
 * 1. Compatibility layer between legacy domain based access manager 
 *    and pure ACL based access manager.   This is so we can go back and 
 *    forth between the two, by a LC key(zimbra_class_accessmanager) change:  
 *   
 *    DomainAccessControl can be deprecated after we decide to not support
 *    domain based access manager at all.
 * 
 * 2. A utility layer between service handlers(SOAP and servlet handlers) 
 *    and access manger.  We want to keep the AccessManager API simple and 
 *    use this class to provide convenient methods, for right checking 
 *    callsites.
 *
 */
public abstract class AdminAccessControl {
    protected AccessManager mAccessMgr;
    protected ZimbraSoapContext mZsc; // for SOAP callsites only
    protected Account mAuthedAcct;
    protected AuthToken mAuthToken;
    
    /* ==========
     * public API
     * ==========
     */
    
    /**
     * instantiate an AdminAccessControl
     * 
     * for SOAP callsites
     */
    public static AdminAccessControl getAdminAccessControl(ZimbraSoapContext zsc) throws ServiceException {
        Account authedAcct = DocumentHandler.getAuthenticatedAccount(zsc);
        AuthToken authToken = zsc.getAuthToken();
        return newAdminAccessControl(zsc, authToken, authedAcct);
    }
    
    /**
     * instantiate an AdminAccessControl
     * 
     * for non-SOAP callsites
     */
    public static AdminAccessControl getAdminAccessControl(AuthToken authToken) throws ServiceException {
        String acctId = authToken.getAccountId();
        Account authedAcct = Provisioning.getInstance().get(AccountBy.id, acctId);
        if (authedAcct == null)
            throw ServiceException.AUTH_REQUIRED();
        return newAdminAccessControl(null, authToken, authedAcct);
    }
    
    /**
     *  only called for domain based access manager
     */
    public abstract void checkModifyAttrs(AttributeClass attrClass, Map<String, Object> attrs) throws ServiceException;
    
    /**
     * This has to be called *after* the *can create* check.
     * For domain based AccessManager, all attrs are allowed if the admin can create.
     */
    public abstract void checkSetAttrsOnCreate(TargetType targetType, String entryName, Map<String, Object> attrs) throws ServiceException;
    
    /**
     * for an entry to be listed in the Search*** and GetAll*** response,
     * the authed admin needs to have both the "list" right and "get all attrs"
     * right.
     * 
     * Note: if the AccessManager is a domain based AccessManager, it always 
     *       returns true.  Callsites of the this method must have either 
     *       already checked the domain right (passing a pseudo "always allow"
     *       AdminRight to the checker), or the handler is not for domain 
     *       admins anyway so domain admins would have been blocked at SoapEngine
     *       and won't even get into the soap handler.
     * 
     */
    public abstract boolean hasRightsToList(NamedEntry target, AdminRight listRight, Object getAttrRight) throws ServiceException;
   
    public abstract boolean hasRightsToListCos(Cos target, AdminRight listRight, Object getAttrRight) throws ServiceException;

    /**
     * For non-domained rights
     * (i.e. not: account, calendar resource, distribution list, domain)
     * 
     * For checking ACL rights only, domain based access manager
     * will always return OK.  This should be called only when 
     * domain based permission checking has passed.
     */
    public abstract void checkRight(Entry target, Object needed) throws ServiceException;
    
    /**
     * cos right
     */
    public abstract void checkCosRight(Cos cos, Object needed) throws ServiceException;

    /**
     * account right (SOAP only)
     */
    public abstract void checkAccountRight(AdminDocumentHandler handler, Account account, Object needed) throws ServiceException;
    
    /**
     * calendar resource right (SOAP only)
     */
    public abstract void checkCalendarResourceRight(AdminDocumentHandler handler, CalendarResource cr, Object needed) throws ServiceException;
    
    /**
     * DL right (SOAP only)
     */
    public abstract void checkDistributionListRight(AdminDocumentHandler handler, DistributionList dl, Object needed) throws ServiceException;
    
    /**
     * domain right (SOAP only)
     *
     * called by handlers that need to check right on a domain for domain-ed objects:
     * account, alias, cr, dl.
     * 
     * Note: this method *does* check domain status.  
     */
    public abstract void checkDomainRightByEmail(AdminDocumentHandler handler, String email, AdminRight needed) throws ServiceException;

    /**
     * domain right (SOAP only)
     * Note: this method does *not* check domain status.  
     */
    public abstract void checkDomainRight(AdminDocumentHandler handler, String domainName, Object needed) throws ServiceException;

    /**
     * domain right (SOAP only)
     * Note: this method does *not* check domain status.  
     */
    public abstract void checkDomainRight(AdminDocumentHandler handler, Domain domain, Object needed) throws ServiceException;

    
    /* ================
     * internal methods
     * ================
     */
    private AdminAccessControl(AccessManager accessMgr, ZimbraSoapContext zsc, AuthToken authToken, Account authedAcct) {
        mAccessMgr = accessMgr;
        mZsc = zsc;
        mAuthToken = authToken;
        mAuthedAcct = authedAcct;
    }
    
    private static AdminAccessControl newAdminAccessControl(ZimbraSoapContext zsc, AuthToken authToken, Account authedAcct) {
        AccessManager accessMgr = AccessManager.getInstance();
        if (isDomainBasedAccessManager(accessMgr))
            return new DomainAccessControl(accessMgr, zsc, authToken, authedAcct);
        else
            return new ACLAccessControl(accessMgr, zsc, authToken, authedAcct);
    }

    public boolean isDomainAdminOnly() {
        return mAccessMgr.isDomainAdminOnly(mAuthToken);
    }
    
    static boolean isDomainBasedAccessManager(AccessManager am) {
        return (!(am instanceof ACLAccessManager));
    }
    
    /**
     * check domain status for domain-ed targets: account, cr, dl
     * 
     * We can't put this check in ACLAccessManager because some rights, like 
     * listAccount, listCalendarResource, listDistributionList that takes 
     * a account/cr/dl target object, but should be allowed even when 
     * domain status is suspended/shutdown.
     * 
     * Note: if target *is* a domain, domain status is *not* checked here.
     *       - if domain status is "shutdown":
     *             Modify/DeleteDomain would've been already blocked in the SOAP handlers
     *       - if domain status is "suspended":
     *             Modify/DeleteDomain are allowed/denied by our regular ACL checking:
     *             (i.e. system admin or whoever has the right)
     *       - for both "shutdown" and "suspended" status:      
     *             List/Get domain are allowed/denied by our regular ACL checking.
     * 
     * @param target
     */
    protected void checkDomainStatus(Entry target) throws ServiceException {
        Domain domain;
        if (target instanceof Domain)
            domain = (Domain)target;
        else
            domain = TargetType.getTargetDomain(Provisioning.getInstance(), target);
        mAccessMgr.checkDomainStatus(domain); // will throw if domain is not in an accessible state
    }
    
    // static just for called from AdminDocumentHandler.canAccessEmail
    // that method should have been cleaned up (see comments on AdminDocumentHandler.canAccessEmail)
    // and after that this methods can be protected and non-static.
    static String getDomainFromEmail(String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        return parts[1];
    }
    
    protected void soapOnly() throws ServiceException {
        if (mZsc == null)
            throw ServiceException.FAILURE("internal error, called from non-SOAP servlet", null);
    }

    
    /**
     * 
     * Class DomainAccessControl
     *
     */
    private static class DomainAccessControl extends AdminAccessControl {
        private DomainAccessControl(AccessManager accessMgr, ZimbraSoapContext zsc, AuthToken authToken, Account authedAcct) {
            super(accessMgr, zsc, authToken, authedAcct);
        }
        
        @Override
        public void checkModifyAttrs(AttributeClass attrClass, Map<String, Object> attrs) 
        throws ServiceException {
            for (String attrName : attrs.keySet()) {
                if (attrName.charAt(0) == '+' || attrName.charAt(0) == '-')
                    attrName = attrName.substring(1);

                if (!AttributeManager.getInstance().isDomainAdminModifiable(attrName, attrClass))
                    throw ServiceException.PERM_DENIED("can not modify attr: "+attrName);
            }
        }
        
        /**
         * For domain based AccessManager, all attrs are allowed if the admin can create.
         */
        public void checkSetAttrsOnCreate(TargetType targetType, String entryName, Map<String, Object> attrs) 
            throws ServiceException {
            // all attrs are allowed if the admin can create.
        }
        
        @Override
        public boolean hasRightsToList(NamedEntry target, 
                AdminRight listRight, Object getAttrRight) throws ServiceException {
            return true;
        }
        
        @Override
        public boolean hasRightsToListCos(Cos target, AdminRight listRight, Object getAttrRight) throws ServiceException {
            boolean hasRight;
            
            if (isDomainAdminOnly())  
                hasRight = mAccessMgr.canAccessCos(mAuthToken, target);
            else
                hasRight = true;
            
            return hasRight;
        }
        
        @Override
        public void checkRight(Entry target, Object needed) throws ServiceException {
            // do nothing
        }
        
        @Override
        public void checkCosRight(Cos cos, Object needed) throws ServiceException {
            if (isDomainAdminOnly()) {
                if (!mAccessMgr.canAccessCos(mAuthToken, cos))
                    throw ServiceException.PERM_DENIED("can not access cos");
            }
        }
        
        @Override
        public void checkAccountRight(AdminDocumentHandler handler, Account account, Object needed) throws ServiceException {
            soapOnly();
            
            if (!handler.canAccessAccount(mZsc, account))
                throw ServiceException.PERM_DENIED("can not access account");
            
            if (isDomainAdminOnly() && (needed instanceof Map))
                checkModifyAttrs(AttributeClass.account, (Map<String, Object>)needed);
        }

        @Override
        public void checkCalendarResourceRight(AdminDocumentHandler handler, CalendarResource cr, Object needed) throws ServiceException {
            soapOnly();
            
            if (!handler.canAccessAccount(mZsc, cr))
                throw ServiceException.PERM_DENIED("can not access calendar resource");

            if (isDomainAdminOnly() && (needed instanceof Map))
                checkModifyAttrs(AttributeClass.calendarResource, (Map<String, Object>)needed);
        }

        @Override
        public void checkDistributionListRight(AdminDocumentHandler handler, DistributionList dl, Object needed) throws ServiceException {
            soapOnly();
            
            if (!handler.canAccessEmail(mZsc, dl.getName()))
                throw ServiceException.PERM_DENIED("can not access dl");
        }

        @Override
        public void checkDomainRightByEmail(AdminDocumentHandler handler, String email, AdminRight needed) throws ServiceException {
            soapOnly();
            
            if (!handler.canAccessEmail(mZsc, email))
                throw ServiceException.PERM_DENIED("can not access email:" + email);
        }
        
        @Override
        public void checkDomainRight(AdminDocumentHandler handler, String domainName, Object needed) throws ServiceException {
            soapOnly();
            
            if (isDomainAdminOnly()) {
                if (!handler.canAccessDomain(mZsc, domainName))
                    throw ServiceException.PERM_DENIED("can not access domain");
                
                if (needed instanceof Map)
                    checkModifyAttrs(AttributeClass.domain, (Map<String, Object>)needed);
            }
        }
        
        @Override
        public void checkDomainRight(AdminDocumentHandler handler, Domain domain, Object needed) throws ServiceException {
            soapOnly();
            
            // delegate to the String version of checkDomainRight instead of duplicating
            // the code here, since domain based access manager resolve domain rights 
            // by comparing the domain name anyway.
            checkDomainRight(handler, domain.getName(), needed);
        }

    }
    
    
    /**
     * 
     * Class ACLAccessControl
     *
     */
    private static class ACLAccessControl extends AdminAccessControl {
        private ACLAccessControl(AccessManager accessMgr, ZimbraSoapContext zsc, AuthToken authToken, Account authedAcct) {
            super(accessMgr, zsc, authToken, authedAcct);
        }
        
        @Override
        public void checkModifyAttrs(AttributeClass attrClass, Map<String, Object> attrs) 
        throws ServiceException {
            throw ServiceException.FAILURE("internal error", null); 
        }
        
        /**
         * This has to be called *after* the *can create* check.
         */
        @Override
        public void checkSetAttrsOnCreate(TargetType targetType, String entryName, Map<String, Object> attrs) 
            throws ServiceException {
            
            boolean hasRight = mAccessMgr.canSetAttrsOnCreate(mAuthedAcct, targetType, entryName, attrs, true);
     
            if (!hasRight)
                throw ServiceException.PERM_DENIED("cannot set attrs");
        }
        
        /**
         * for an entry to be listed in the Search*** and GetAll*** response,
         * the authed admin needs to have both the "list" right and enough 
         * "get attrs" rights.
         */
        @Override
        public boolean hasRightsToList(NamedEntry target, AdminRight listRight, Object getAttrRight) throws ServiceException {
            
            try {
                checkRight(target, listRight);
            } catch (ServiceException e) {
                // if PERM_DENIED, log and return false, do not throw, so we 
                // can continue with the next entry
                if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                    ZimbraLog.acl.warn(getClass().getName() + ": skipping entry " + target.getName() + ": " + e.getMessage());
                    return false;
                } else
                    throw e;
            }
            
            // check only the list right, do not check the get attrs right
            if (getAttrRight == null)
                return true;
            
            try {
                checkRight(target, getAttrRight);
            } catch (ServiceException e) {
                // if PERM_DENIED, log and return false, do not throw, so we 
                // can continue with the next entry
                if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                    ZimbraLog.acl.warn(getClass().getName() + ": skipping entry " + target.getName() + ": " + e.getMessage());
                    return false;
                } else
                    throw e;
            }
            
            return true;
        }
        
        @Override
        public boolean hasRightsToListCos(Cos target, AdminRight listRight, Object getAttrRight) throws ServiceException {
            return hasRightsToList(target, listRight,  getAttrRight);
        }
        
        @Override
        public void checkRight(Entry target, Object needed) throws ServiceException {
            if (target == null)
                target = Provisioning.getInstance().getGlobalGrant();
            if (!doCheckRight(target, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(target, needed));
        }
        
        @Override
        public void checkCosRight(Cos cos, Object needed) throws ServiceException {
            if (!doCheckRight(cos, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(cos, needed));
        }
        
        @Override
        public void checkAccountRight(AdminDocumentHandler handler, Account account, Object needed) throws ServiceException {
            soapOnly();
            
            checkDomainStatus(account);
            
            Boolean canAccess = handler.canAccessAccountCommon(mZsc, account);
            boolean hasRight;
            if (canAccess == null)
                hasRight = doCheckRight(account, needed);
            else
                hasRight = canAccess.booleanValue();
            if (!hasRight)
                throw ServiceException.PERM_DENIED(printNeededRight(account, needed));
        }
        
        @Override
        public void checkCalendarResourceRight(AdminDocumentHandler handler, CalendarResource cr, Object needed) throws ServiceException {
            soapOnly();
            
            checkDomainStatus(cr);
            
            Boolean canAccess = handler.canAccessAccountCommon(mZsc, cr);
            boolean hasRight;
            if (canAccess == null)
                hasRight = doCheckRight(cr, needed);
            else
                hasRight = canAccess.booleanValue();
            if (!hasRight)
                throw ServiceException.PERM_DENIED(printNeededRight(cr, needed));
        }
        
        @Override
        public void checkDistributionListRight(AdminDocumentHandler handler, DistributionList dl, Object needed) throws ServiceException {
            soapOnly();
            
            checkDomainStatus(dl);
            
            if (!doCheckRight(dl, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(dl, needed));
        }
        
        @Override
        public void checkDomainRightByEmail(AdminDocumentHandler handler, String email, AdminRight needed) throws ServiceException {
            soapOnly();
            
            String domainName = getDomainFromEmail(email);
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw ServiceException.PERM_DENIED("no such domain: " + domainName);
            
            checkDomainStatus(domain);
            
            if (!doCheckRight(domain, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(domain, needed));
        }
        
        @Override
        public void checkDomainRight(AdminDocumentHandler handler, String domainName, Object needed) throws ServiceException {
            soapOnly();
            
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw ServiceException.PERM_DENIED("no such domain: " + domainName);
            
            if (!doCheckRight(domain, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(domain, needed));
        }
        
        @Override
        public void checkDomainRight(AdminDocumentHandler handler, Domain domain, Object needed) throws ServiceException {
            soapOnly();
            
            if (!doCheckRight(domain, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(domain, needed));
        }
        
        /*
         * =================================
         * ACLAccessControl internal methods
         * =================================
         */
        
        /**
         * @param target
         * @param needed if instanceof AttrRight : a preset or attr AdminRight    
         *                             Set<String> : attrs to get
         *                             Map<String, Object> : attrs to set
         * @return
         * @throws ServiceException
         */
        private boolean doCheckRight(Entry target, Object needed) throws ServiceException {
            
            if (needed instanceof AdminRight) {
                AdminRight adminRight = (AdminRight)needed;
                if (adminRight.isPresetRight())
                    return mAccessMgr.canDo(mAuthedAcct, target, (AdminRight)needed, true, false, null);
                else if (adminRight.isAttrRight() && 
                         adminRight.getRightType() == Right.RightType.getAttrs)
                    return mAccessMgr.canGetAttrs(mAuthedAcct, target, ((AttrRight)needed).getAttrs(), true);
                else
                    throw ServiceException.FAILURE("internal error", null);
            } else if (needed instanceof Set)
                return mAccessMgr.canGetAttrs(mAuthedAcct, target, (Set<String>)needed, true);
            else if (needed instanceof Map)
                return mAccessMgr.canSetAttrs(mAuthedAcct, target, (Map<String, Object>)needed, true);
            else
                throw ServiceException.FAILURE("internal error", null);
        }
        
        private String printNeededRight(Entry target, Object needed) throws ServiceException {
            String targetInfo;
            if (target instanceof Alias) // see comments in SearchDirectory.hasRightsToListDanglingAlias
                targetInfo = "alias " + target.getLabel();
            else
                targetInfo = TargetType.getTargetType(target).name() + " " + target.getLabel();
            
            if (needed instanceof AdminRight)
                return "need right: " + ((AdminRight)needed).getName() + " for " + targetInfo;
            else if (needed instanceof Set)
                return "cannot get attrs on " + targetInfo;
            else if (needed instanceof Map)
                return "cannot set attrs on " + targetInfo;
            else
                throw ServiceException.FAILURE("internal error", null);
        }
    }
    
    public static class SearchDirectoryRightChecker implements NamedEntry.CheckRight {
        private AdminAccessControl mAC;
        private Provisioning mProv;
        private Set<String> mReqAttrs;
        
        public SearchDirectoryRightChecker(AdminAccessControl accessControl, Provisioning prov, Set<String> reqAttrs) {
            mAC = accessControl;
            mProv = (prov == null)? Provisioning.getInstance() : prov;
            mReqAttrs = reqAttrs;
        }
        
        private boolean hasRightsToList(NamedEntry target, 
                AdminRight listRight, Object getAllAttrsRight, Set<String> getAttrsRight) throws ServiceException {
            
            if (getAttrsRight == null || getAttrsRight.isEmpty())
                return mAC.hasRightsToList(target, listRight, getAllAttrsRight);
            else
                return mAC.hasRightsToList(target, listRight, getAttrsRight);
        }
        
        private boolean hasRightsToListDanglingAlias(Alias alias) throws ServiceException {
            /*
             * gross, this is the only case we would ever pass an Alias object for ACL checking.
             * 
             * We want to pass alias instead of null so if PERM_DENIED the skipping WARN can be 
             * nicely logged just like whenever we skip listing any object.
             * 
             * Alias is *not* a valid TargetTytpe for ACL checking.  Luckily(and hackily), the pseudo 
             * right PR_SYSTEM_ADMIN_ONLY would never lead to a path that needs to refer to the 
             * target. 
             */
            return mAC.hasRightsToList(alias, AdminRight.PR_SYSTEM_ADMIN_ONLY, null);
        }
        
        private boolean hasRightsToListAlias(Alias alias) throws ServiceException {
            boolean hasRight;
            
            // if an admin can list the account/cr/dl, he can do the same on their aliases
            // don't need any getAttrs rights on the account/cr/dl, because the returned alias
            // entry contains only attrs on the alias, not the target entry.
            TargetType tt = alias.getTargetType(mProv);
            
            if (tt == null) // can't check right, allows only system admin
                hasRight = hasRightsToListDanglingAlias(alias);
            else if (tt == TargetType.dl)
                hasRight = mAC.hasRightsToList(alias.getTarget(mProv), Admin.R_listDistributionList, null);
            else if (tt == TargetType.calresource)
                hasRight = mAC.hasRightsToList(alias.getTarget(mProv), Admin.R_listCalendarResource, null);
            else
                hasRight = mAC.hasRightsToList(alias.getTarget(mProv), Admin.R_listAccount, null);
            
            return hasRight;
        }
        
        /**
         * returns if entry is allowed.
         */
        public boolean allow(NamedEntry entry) throws ServiceException {
            if (entry instanceof CalendarResource) {
                return hasRightsToList(entry, Admin.R_listCalendarResource, Admin.R_getCalendarResource, mReqAttrs);
            } else if (entry instanceof Account) {
                return hasRightsToList(entry, Admin.R_listAccount, Admin.R_getAccount, mReqAttrs);
            } else if (entry instanceof DistributionList) {
                return hasRightsToList(entry, Admin.R_listDistributionList, Admin.R_getDistributionList, mReqAttrs);
            } else if (entry instanceof Alias) {
                return hasRightsToListAlias((Alias)entry);
            } else if (entry instanceof Domain) {
                return hasRightsToList(entry, Admin.R_listDomain, Admin.R_getDomain, mReqAttrs);
            } else if (entry instanceof Cos) {
                return hasRightsToList(entry, Admin.R_listCos, Admin.R_getCos, mReqAttrs);
            } else
                return false;
        }
        
        /**
         * returns a new list that contains only allowed entries from 
         * the input list.
         */
        public List getAllowed(List entries) throws ServiceException {
            List allowedEntries = new ArrayList<String>();
            for (int i = 0; i < entries.size(); i++) {
                NamedEntry entry = (NamedEntry)entries.get(i);
                if (allow(entry))
                    allowedEntries.add(entry);
            }
            return allowedEntries;
        }
    }
}
