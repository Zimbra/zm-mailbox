/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.zimbra.cs.account.AccountServiceException;
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
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.ACLAccessManager;
import com.zimbra.cs.account.accesscontrol.AccessControlUtil;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttrRight;
import com.zimbra.cs.account.accesscontrol.GlobalAccessManager;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PseudoTarget;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
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
     * Returns if the auth token is sufficient for a SOAP request that requires 
     * some level of admin privileges.  
     * 
     * Called from SoapEngine as a gate keeping checking before dispatching to 
     * the handler.  If this method returns true, the request is eligible to 
     * be dispatched to the handler, the actual right checking for the request 
     * is handled in the handler.  If this method returns false, the request 
     * can no longer proceed and is rejected by the SoapEngine.
     * 
     * @param soapCtxt
     * @param handler
     * @return
     * @throws ServiceException
     */
    public abstract boolean isSufficientAdminForSoap(Map<String, Object> soapCtxt, DocumentHandler handler);
    
    /**
     * Returns if the auth token is sufficient for the zimlet filter servlet.
     * 
     * @return
     */
    public abstract boolean isSufficientAdminForZimletFilterServlet();
    
    
    /**
     * Returns if the specified account is sufficient for delegated auth.
     * 
     * Note: this method is static and it checks the specified account.
     *
     * @param account
     * @return
     */
    public static boolean isSufficientAdminForSoapDelegatedAuth(Account acct) {
        AccessManager accessMgr = AccessManager.getInstance();
        boolean isAdmin;
        
        if (isDomainBasedAccessManager(accessMgr))
            isAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false) ||
                      acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        else
            isAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false) ||
                      acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        
        return isAdmin;
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
     * the authed admin needs to have the "list***" right.
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

    
    /**
     * returns an AttrRightChecker for the target
     */
    public abstract AccessManager.AttrRightChecker getAttrRightChecker(Entry target) throws ServiceException;
    

    
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
        
        if (accessMgr.getClass() == ACLAccessManager.class)
            return new ACLAccessControl(accessMgr, zsc, authToken, authedAcct);
        else if (accessMgr.getClass() == GlobalAccessManager.class) {
            return new GlobalAccessControl(accessMgr, zsc, authToken, authedAcct);
        } else {
            return new DomainAccessControl(accessMgr, zsc, authToken, authedAcct);
        }
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
        
        public boolean isSufficientAdminForSoap(Map<String, Object> soapCtxt, 
                DocumentHandler handler) {
            
            if (mAuthToken.isAdmin())
                return true;  // is a global admin, all is well
            
            // if the request is OK for domain admins, see if the authed is a domain admin
            boolean ok = handler.domainAuthSufficient(soapCtxt) && mAuthToken.isDomainAdmin();
            return ok;
        }
        
        public boolean isSufficientAdminForZimletFilterServlet() {
            return mAuthToken.isAdmin() || mAuthToken.isDomainAdmin();
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
        
        @Override
        public AccessManager.AttrRightChecker getAttrRightChecker(Entry target) throws ServiceException {
            return null;
        }

    }
    
    /**
     * 
     * Class GlobalAccessControl
     *
     */
    private static class GlobalAccessControl extends AdminAccessControl {
        private GlobalAccessControl(AccessManager accessMgr, ZimbraSoapContext zsc, AuthToken authToken, Account authedAcct) {
            super(accessMgr, zsc, authToken, authedAcct);
        }
        
        @Override
        public void checkAccountRight(AdminDocumentHandler handler,
                Account account, Object needed) throws ServiceException {
            soapOnly();
            
            checkDomainStatus(account);
            
            Boolean canAccess = handler.canAccessAccountCommon(mZsc, account, false);
            
            if (canAccess == null)
                throwIfNotAllowed();
            else {
                boolean hasRight = canAccess.booleanValue();
                if (!hasRight)
                    throw ServiceException.PERM_DENIED("only global admin is allowed");
            }     
        }

        @Override
        public void checkCalendarResourceRight(AdminDocumentHandler handler,
                CalendarResource cr, Object needed) throws ServiceException {
            checkAccountRight(handler, cr, needed);
        }

        @Override
        public void checkCosRight(Cos cos, Object needed) throws ServiceException {
            throwIfNotAllowed();
        }

        @Override
        public void checkDistributionListRight(AdminDocumentHandler handler,
                DistributionList dl, Object needed) throws ServiceException {
            soapOnly();
            checkDomainStatus(dl);
            throwIfNotAllowed();
        }

        @Override
        public void checkDomainRight(AdminDocumentHandler handler,
                String domainName, Object needed) throws ServiceException {
            soapOnly();
            
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw ServiceException.PERM_DENIED("no such domain: " + domainName);
            
            throwIfNotAllowed();
        }

        @Override
        public void checkDomainRight(AdminDocumentHandler handler, Domain domain,
                Object needed) throws ServiceException {
            soapOnly();
            throwIfNotAllowed();
        }

        @Override
        public void checkDomainRightByEmail(AdminDocumentHandler handler,
                String email, AdminRight needed) throws ServiceException {
            soapOnly();
            
            String domainName = getDomainFromEmail(email);
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domainName);
            
            checkDomainStatus(domain);
            throwIfNotAllowed();
        }

        @Override
        public void checkModifyAttrs(AttributeClass attrClass,
                Map<String, Object> attrs) throws ServiceException {
            throwIfNotAllowed();
        }

        @Override
        public void checkRight(Entry target, Object needed) throws ServiceException {
            throwIfNotAllowed();
        }

        @Override
        public void checkSetAttrsOnCreate(TargetType targetType, String entryName,
                Map<String, Object> attrs) throws ServiceException {
            throwIfNotAllowed();
        }

        @Override
        public AttrRightChecker getAttrRightChecker(Entry target)
                throws ServiceException {
            return new AttributeRightChecker(this, target);
        }

        @Override
        public boolean hasRightsToList(NamedEntry target, AdminRight listRight,
                Object getAttrRight) throws ServiceException {
            return doCheckRight();
        }

        @Override
        public boolean hasRightsToListCos(Cos target, AdminRight listRight,
                Object getAttrRight) throws ServiceException {
            return doCheckRight();
        }

        @Override
        public boolean isSufficientAdminForSoap(Map<String, Object> soapCtxt,
                DocumentHandler handler) {
            return mAuthToken.isAdmin();
        }

        @Override
        public boolean isSufficientAdminForZimletFilterServlet() {
            return mAuthToken.isAdmin();
        }
        
        //
        // private methods
        //
        private void throwIfNotAllowed() throws ServiceException {
            boolean hasRight = doCheckRight();
            
            if (!hasRight)
                throw ServiceException.PERM_DENIED("only global admin is allowed");
        }
        
        private boolean doCheckRight() {
            return mAccessMgr.canDo(mAuthedAcct, null, null, true);
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
        
        public boolean isSufficientAdminForSoap(Map<String, Object> soapCtxt, 
                DocumentHandler handler) {
            return mAuthToken.isAdmin() || mAuthToken.isDelegatedAdmin();
        }
        
        public boolean isSufficientAdminForZimletFilterServlet() {
            return mAuthToken.isAdmin() || mAuthToken.isDelegatedAdmin();
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
                // if PERM_DENIED, log and return false, do not throw, so we can continue with the next entry
                if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                    ZimbraLog.acl.warn(getClass().getName() + ": skipping entry " + target.getName() + ": " + e.getMessage());
                    return false;
                } else
                    throw e;
            }
            
            // check only the list right, do not check the get attrs right
            if (getAttrRight == null)
                return true;
            
            if (getAttrRight instanceof Set) {
                if (((Set)getAttrRight).isEmpty()) {
                    ZimbraLog.acl.warn(getClass().getName() + ": skipping entry " + target.getName() + ": " + "non of the requested attrs is valid on the entry");
                    return false;
                }
            }
            
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
            
            Boolean canAccess = handler.canAccessAccountCommon(mZsc, account, false);
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
            
            Boolean canAccess = handler.canAccessAccountCommon(mZsc, cr, false);
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
                throw AccountServiceException.NO_SUCH_DOMAIN(domainName);
            
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
        
        
        @Override
        public AccessManager.AttrRightChecker getAttrRightChecker(Entry target) throws ServiceException {
            return new AttributeRightChecker(this, target);
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
                    return mAccessMgr.canDo(mAuthedAcct, target, (AdminRight)needed, true, null);
                else if (adminRight.isAttrRight()) {
                    if (adminRight.getRightType() == Right.RightType.getAttrs)
                        return mAccessMgr.canGetAttrs(mAuthedAcct, target, ((AttrRight)needed).getAttrs(), true);
                    else if (adminRight.getRightType() == Right.RightType.setAttrs)
                        // note: this does not check for constraints
                        return mAccessMgr.canSetAttrs(mAuthedAcct, target, ((AttrRight)needed).getAttrs(), true); 
                }
                throw ServiceException.FAILURE("internal error", null);
                
            } else if (needed instanceof Set) {
                return mAccessMgr.canGetAttrs(mAuthedAcct, target, (Set<String>)needed, true);
            } else if (needed instanceof Map) {
                // note: this does check for constraints
                return mAccessMgr.canSetAttrs(mAuthedAcct, target, (Map<String, Object>)needed, true);
            } else if (needed instanceof DynamicAttrsRight) {
                DynamicAttrsRight dar = (DynamicAttrsRight)needed;
                return dar.checkRight(mAccessMgr, mAuthedAcct, target);
            } else
                throw ServiceException.FAILURE("internal error", null);
        }
        
        private String printNeededRight(Entry target, Object needed) throws ServiceException {
            String targetInfo;
            if (PseudoTarget.isPseudoEntry(target))
                targetInfo = "";
            else if (target instanceof Alias) // see comments in SearchDirectory.hasRightsToListDanglingAlias
                targetInfo = " for alias " + target.getLabel();
            else
                targetInfo = " for " + TargetType.getTargetType(target).name() + " " + target.getLabel();
            
            if (needed instanceof AdminRight)
                return "need right: " + ((AdminRight)needed).getName() + targetInfo;
            else if (needed instanceof Set)
                return "cannot get attrs on " + targetInfo;
            else if (needed instanceof Map)
                return "cannot set attrs on " + targetInfo;
            else
                throw ServiceException.FAILURE("internal error", null);
        }
    }
    
    /**
     * A right checker for checking rights for large number of objects.
     * e.g. SearchDirectory, SearchMultiMailbox (all mailboxes on server)
     * 
     * We can't use the regular way to check right for those because of 
     * bad response time.  bug 39514.
     * 
     * The regular way will lead to a LDAP search (if not already loaded) 
     * to find the groups/domain the target belongs, yuck!
     * 
     *  Note: this is the only place where permission is computed from an 
     *  AllEffectiveRights object - because of bug 39514.
     */
    public static abstract class BulkRightChecker implements NamedEntry.CheckRight {
        protected AdminAccessControl mAC;
        protected Provisioning mProv;
        RightCommand.AllEffectiveRights mAllEffRights;
        
        public BulkRightChecker(AdminAccessControl accessControl, Provisioning prov) throws ServiceException {
            mAC = accessControl;
            mProv = (prov == null)? Provisioning.getInstance() : prov;
        }
        
        /* Can't do this because of perf bug 39514
         * 
         * For each entry found, this will lead to a LDAP search to find the groups this entry belongs, yuck.
         * 
        private boolean hasRightsToList(NamedEntry target, AdminRight listRight) throws ServiceException {
            return mAC.hasRightsToList(target, listRight, null);
        }
        */
        
        //
        // a short cut for subclass to call before spending resources to prepare 
        // objects for right checking.  A bit ugly, because it should really only 
        // be checked in AccessControlUtil.checkHardRules.  But for perf reason
        // we use this shortcut.
        //
        protected boolean allowAll() {
            return AccessControlUtil.isGlobalAdmin(mAC.mAuthedAcct, true);
        }
        
        protected boolean hasRight(NamedEntry target, AdminRight rightNeeded) throws ServiceException {
            
            // can use the bulk mechanism only the access control object is pure ACL based
            if (mAC instanceof ACLAccessControl)
                return hasRightImplBulk(target, rightNeeded);
            else {
                // fallback to the normal right checking
                return hasRightImplBulkDefault(target, rightNeeded);
            }
        }
        
        // use the normal way to check right
        private boolean hasRightImplBulkDefault(NamedEntry target, AdminRight rightNeeded) throws ServiceException {
            try {
                mAC.checkRight(target, rightNeeded);
                return true;  // survived the right checking
            } catch (ServiceException e) {
                
            }
            return false;
        }
        
        private boolean hasRightImplBulk(NamedEntry target, AdminRight rightNeeded) throws ServiceException {
            
            try {
                Boolean hardRulesResult = AccessControlUtil.checkHardRules(mAC.mAuthedAcct, true, target, rightNeeded);
                if (hardRulesResult != null)
                    return hardRulesResult.booleanValue();
            } catch (ServiceException e) {
                // if PERM_DENIED, log and return false, do not throw, so we can continue with the next entry
                if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                    ZimbraLog.acl.warn(getClass().getName() + ": skipping entry " + target.getName() + ": " + e.getMessage());
                    return false;
                } else
                    throw e;
            }
            
            if (mAllEffRights == null)
                mAllEffRights = mProv.getAllEffectiveRights(GranteeType.GT_USER.getCode(), 
                        Provisioning.GranteeBy.id, mAC.mAuthedAcct.getId(),
                        false, false);
            
            TargetType targetType = rightNeeded.getTargetType();
            
            RightCommand.RightsByTargetType rbtt = mAllEffRights.rightsByTargetType().get(targetType);
            
            // no right for this target type
            if (rbtt == null || rbtt.hasNoRight())
                return false;
            
            // has some rights for this target type
            
            
            // 1. see if the admin has the right on any entries
            String targetName = target.getName();
            for (RightCommand.RightAggregation rightsByEntries : rbtt.entries()) {
                if (rightsByEntries.entries().contains(targetName)) {
                    RightCommand.EffectiveRights effRights = rightsByEntries.effectiveRights();
                    return hasRightBulk(effRights, target, rightNeeded);
                }
            }
            
            // 2. see if the admin has the right on domain scope
            String targetDomainName = targetType.getTargetDomainName(mProv, target);
            if (targetDomainName != null) {
                if (rbtt instanceof RightCommand.DomainedRightsByTargetType) {
                    RightCommand.DomainedRightsByTargetType domainedRights = (RightCommand.DomainedRightsByTargetType)rbtt;
                    
                    for (RightCommand.RightAggregation rightsByDomains : domainedRights.domains()) {
                        if (rightsByDomains.entries().contains(targetDomainName)) {
                            RightCommand.EffectiveRights effRights = rightsByDomains.effectiveRights();
                            return hasRightBulk(effRights, target, rightNeeded);
                        }
                    }
                }
            }
            
            // 3. see if the admin has the right on all entries of the type on the system
            RightCommand.EffectiveRights er = rbtt.all();
            if (hasRightBulk(er, target, rightNeeded))
                return true;
            
            return false;
        }
        
        private boolean hasRightBulk(RightCommand.EffectiveRights effRights, NamedEntry target, AdminRight rightNeeded) {
            if (effRights == null)
                return false;
            
            List<String> presetRights = effRights.presetRights();
            return presetRights != null && presetRights.contains(rightNeeded.getName());
        }
        
        public abstract boolean allow(NamedEntry entry) throws ServiceException;
    }
    
    /**
     * 
     * class SearchDirectoryRightChecker
     *
     */
    public static class SearchDirectoryRightChecker extends BulkRightChecker {
        
        protected boolean mAllowAll; // short cut for global admin
        
        public SearchDirectoryRightChecker(AdminAccessControl accessControl, Provisioning prov, Set<String> reqAttrs) throws ServiceException {
            // reqAttrs is no longer needed, TODO, cleanup from all callsites 
            super(accessControl, prov);
            mAllowAll = allowAll();
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
        
        // no longer used for perf reason, for bug 46205
        private boolean hasRightsToListAlias_old(Alias alias) throws ServiceException {
            boolean hasRight;
            
            // if an admin can list the account/cr/dl, he can do the same on their aliases
            // don't need any getAttrs rights on the account/cr/dl, because the returned alias
            // entry contains only attrs on the alias, not the target entry.
            NamedEntry aliasTarget = alias.getTarget(mProv);
            
            if (aliasTarget == null) // we have a dangling alias, can't check right, allows only system admin
                hasRight = hasRightsToListDanglingAlias(alias);
            else 
                hasRight = allow(aliasTarget);
            
            return hasRight;
        }
        
        // bug 46205.  
        // 
        // list alias is now a domain right.
        //
        // the old way of checking the list*** right on the target object(account/cr/dl) 
        // of the alias has perf issue, because we will have to load the target object 
        // if it is not in cache - for all aliases returned by the LDAP search.
        private boolean hasRightsToListAlias(Alias alias) throws ServiceException {
            Domain domain = mProv.getDomain(alias);
            if (domain == null)
                return false;
            else
                return hasRight(domain, Admin.R_listAlias);
        }
    
        private AdminRight needRight(NamedEntry entry) throws ServiceException {
            if (entry instanceof CalendarResource) {
                return Admin.R_listCalendarResource;
            } else if (entry instanceof Account) {
                return Admin.R_listAccount;
            } else if (entry instanceof DistributionList) {
                return Admin.R_listDistributionList;
            } else if (entry instanceof Domain) {
                return Admin.R_listDomain;
            } else if (entry instanceof Cos) {
                return Admin.R_listCos;
            } else
                return null;
        }
        
        /**
         * returns if entry is allowed.
         */
        public boolean allow(NamedEntry entry) throws ServiceException {
            
            if (mAllowAll)
                return true;
            
            if (entry instanceof Alias)
                return hasRightsToListAlias((Alias)entry);
            else {
                AdminRight listRightNeeded = needRight(entry);  
                if (listRightNeeded != null)
                    return hasRight(entry, listRightNeeded);
                else
                    return false;
            }
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
    
    /*
     * wraps a AccessManager.AttrRightChecker impl
     * so in case we need to change the impl, we only need to change this class.
     * 
     * This is used by:
     *     SearchDirectory
     *     GetAll{ldap-object}
     *     get{ldap-object}
     *     
     * to determine if an attribute should be returned or hidden.    
     *     
     */
    static class AttributeRightChecker implements AccessManager.AttrRightChecker {
        private AccessManager.AttrRightChecker mRightChecker;
        
        private AttributeRightChecker(AdminAccessControl accessControl, Entry target) throws ServiceException {
            mRightChecker = accessControl.mAccessMgr.canGetAttrs(accessControl.mAuthedAcct, target, true);
        }
            
        public boolean allowAttr(String attrName) {
            return mRightChecker.allowAttr(attrName);
        }
    }
    
    /**
     * For putting together a getAttrs/setAttrs right dynamically
     * 
     * TODO: migrate check*Right callsites that are calling the Set/Map 
     *       interface to use this class, instead of buidling the Set/Map at the call site.
     *       
     * e.g. in a handler
     *     
        checkAccountRight(zsc, acct, needRight());
        
        private SetAttrsRight needRight() {
            SetAttrsRight sar = new SetAttrsRight();
            sar.addAttr(Provisioning.A_zimbraAdminSavedSearches);
            return sar;
        }      
     */
    public static abstract class DynamicAttrsRight {
        abstract boolean checkRight(AccessManager am, Account authedAcct, Entry target) throws ServiceException;
    }
    
    /*
     * dynamic get attrs right
     */
    public static class GetAttrsRight extends DynamicAttrsRight {
        private Set<String> mAttrs = new HashSet<String>();
        
        public void addAttr(String attrName) {
            mAttrs.add(attrName);
        }
        
        boolean checkRight(AccessManager am, Account authedAcct, Entry target) throws ServiceException {
            return am.canGetAttrs(authedAcct, target, mAttrs, true);
        }
    }
    
    /*
     * dynamic set attrs right, no constraint checking
     */
    public static class SetAttrsRight extends DynamicAttrsRight {
        private Set<String> mAttrs = new HashSet<String>();
        
        public void addAttr(String attrName) {
            mAttrs.add(attrName);
        }
        
        boolean checkRight(AccessManager am, Account authedAcct, Entry target) throws ServiceException {
            return am.canSetAttrs(authedAcct, target, mAttrs, true);
        }
    }
    
    public static class SetAttrsRightWithConstraintChecking extends DynamicAttrsRight {
        private Map<String, Object> mAttrs = new HashMap<String, Object>();
        
        public void addAttr(String attrName, Object attrValue) {
            mAttrs.put(attrName, attrValue);
        }
        
        boolean checkRight(AccessManager am, Account authedAcct, Entry target) throws ServiceException {
            return am.canSetAttrs(authedAcct, target, mAttrs, true);
        }
    }
}
