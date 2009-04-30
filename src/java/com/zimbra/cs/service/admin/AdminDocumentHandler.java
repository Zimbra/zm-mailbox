/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 4, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
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
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.accesscontrol.ACLAccessManager;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttrRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.operation.BlockingOperation;
import com.zimbra.cs.operation.Requester;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;

/** @author schemers */
public abstract class AdminDocumentHandler extends DocumentHandler implements AdminRightCheckPoint {

    @Override
    public Object preHandle(Element request, Map<String, Object> context) throws ServiceException { 
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Session session = getSession(zsc);
        Mailbox.OperationContext octxt = null;
        Mailbox mbox = null;

        if (zsc.getAuthToken() != null)
            octxt = getOperationContext(zsc, context);
        return BlockingOperation.schedule(request.getName(), session, octxt, mbox, Requester.ADMIN, getSchedulerPriority(), 1);   
    }

    @Override
    public void postHandle(Object userObj) { 
        ((BlockingOperation) userObj).finish();
    }

    protected Priority getSchedulerPriority() {
        return Priority.INTERACTIVE_HIGH;
    }


    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    protected String[] getProxiedAccountPath()          { return null; }
    protected String[] getProxiedAccountElementPath()   { return null; }
    protected String[] getProxiedResourcePath()         { return null; }
    protected String[] getProxiedResourceElementPath()  { return null; }
    protected String[] getProxiedServerPath()           { return null; }
    
    protected Account getAccount(Provisioning prov, AccountBy accountBy, String value, AuthToken authToken) throws ServiceException {
        Account acct = null;
        
        // first try getting it from master if not in cache
        try {
            acct = prov.get(accountBy, value, true, authToken);
        } catch (ServiceException e) {
            // try the replica
            acct = prov.get(accountBy, value, false, authToken);
        }
        return acct;
    }
    
    private CalendarResource getCalendarResource(Provisioning prov, CalendarResourceBy crBy, String value, AuthToken authToken) throws ServiceException {
        CalendarResource cr = null;
        
        // first try getting it from master if not in cache
        try {
            cr = prov.get(crBy, value, true);
        } catch (ServiceException e) {
            // try the replica
            cr = prov.get(crBy, value, false);
        }
        return cr;
    }

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // if we've explicitly been told to execute here, don't proxy
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (zsc.getProxyTarget() != null)
            return null;

        try {
            Provisioning prov = Provisioning.getInstance();

            // check whether we need to proxy to the home server of a target account
            String[] xpath = getProxiedAccountPath();
            String acctId = (xpath != null ? getXPath(request, xpath) : null);
            if (acctId != null) {
                Account acct = getAccount(prov, AccountBy.id, acctId, zsc.getAuthToken());
                if (acct != null && !Provisioning.onLocalServer(acct))
                    return proxyRequest(request, context, acctId);
            }

            xpath = getProxiedAccountElementPath();
            Element acctElt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (acctElt != null) {
                Account acct = getAccount(prov, AccountBy.fromString(acctElt.getAttribute(AdminConstants.A_BY)), acctElt.getText(), zsc.getAuthToken());   
                if (acct != null && !Provisioning.onLocalServer(acct))
                    return proxyRequest(request, context, acct.getId());
            }

            // check whether we need to proxy to the home server of a target calendar resource
            xpath = getProxiedResourcePath();
            String rsrcId = (xpath != null ? getXPath(request, xpath) : null);
            if (rsrcId != null) {
                CalendarResource rsrc = getCalendarResource(prov, CalendarResourceBy.id, rsrcId, zsc.getAuthToken());
                if (rsrc != null) {
                    Server server = prov.get(ServerBy.name, rsrc.getAttr(Provisioning.A_zimbraMailHost));
                    if (server != null && !LOCAL_HOST_ID.equalsIgnoreCase(server.getId()))
                        return proxyRequest(request, context, server);
                }
            }
            
            xpath = getProxiedResourceElementPath();
            Element resourceElt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (resourceElt != null) {
                CalendarResource rsrc = getCalendarResource(prov, CalendarResourceBy.fromString(resourceElt.getAttribute(AdminConstants.A_BY)), resourceElt.getText(), zsc.getAuthToken());
                if (rsrc != null) {
                    Server server = prov.get(ServerBy.name, rsrc.getAttr(Provisioning.A_zimbraMailHost));
                    if (server != null && !LOCAL_HOST_ID.equalsIgnoreCase(server.getId()))
                        return proxyRequest(request, context, server);
                }
            }
 
            // check whether we need to proxy to a target server
            xpath = getProxiedServerPath();
            String serverId = (xpath != null ? getXPath(request, xpath) : null);
            if (serverId != null) {
                Server server = prov.get(ServerBy.id, serverId);
                if (server != null && !LOCAL_HOST_ID.equalsIgnoreCase(server.getId()))
                    return proxyRequest(request, context, server);
            }

            return null;
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode()))
                return null;
            // but if it's a real error, it's a real error
            throw e;
        }
    }
    
    @Override
    public Session.Type getDefaultSessionType() {
        return Session.Type.ADMIN;
    }

    public boolean isDomainAdminOnly(ZimbraSoapContext zsc) {
        return AccessManager.getInstance().isDomainAdminOnly(zsc.getAuthToken());
    }

    public Domain getAuthTokenAccountDomain(ZimbraSoapContext zsc) throws ServiceException {
        return AccessManager.getInstance().getDomain(zsc.getAuthToken());
    }
    
    protected boolean canAccessDomain(ZimbraSoapContext zsc, String domainName) throws ServiceException {
        return AccessManager.getInstance().canAccessDomain(zsc.getAuthToken(), domainName);
    }

    protected boolean canAccessDomain(ZimbraSoapContext zsc, Domain domain) throws ServiceException {
        return canAccessDomain(zsc, domain.getName());
    }

    protected boolean canModifyMailQuota(ZimbraSoapContext zsc, Account target, long mailQuota) throws ServiceException {
        return AccessManager.getInstance().canModifyMailQuota(zsc.getAuthToken(), target, mailQuota);
    }

    private String getDomainFromEmail(String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        return parts[1];
    }
    
    /*
     * TODO:  can't be private yet, still called from ZimbraAdminExt and ZimbraCustomerServices/hosted
     * 
     * Need to fix those callsite to call one of the check*** methods.
     */
    protected boolean canAccessEmail(ZimbraSoapContext zsc, String email) throws ServiceException {
        return canAccessDomain(zsc, getDomainFromEmail(email));
    }
    
    private boolean canAccessCos(ZimbraSoapContext zsc, Cos cos) throws ServiceException {
        return AccessManager.getInstance().canAccessCos(zsc.getAuthToken(), cos);
    }
    

    
    /*
     * ======================================================================
     *     Connector methods between domain based access manager and 
     *     pure ACL based access manager.
     *     
     *     It is cleaner this way instead of wrap all the diff in 
     *     AccessManager API.
     * ======================================================================
     */
    
    private static boolean isDomainBasedAccessManager(AccessManager am) {
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
    private void checkDomainStatus(AccessManager am, Entry target) throws ServiceException {
        Domain domain;
        if (target instanceof Domain)
            domain = (Domain)target;
        else
            domain = TargetType.getTargetDomain(Provisioning.getInstance(), target);
        am.checkDomainStatus(domain); // will throw if domain is not in an accessible state
    }

    /**
     * only called for domain based access manager
     * 
     * @param attrClass
     * @param attrs
     * @throws ServiceException
     */
    public void checkModifyAttrs(AttributeClass attrClass, Map<String, Object> attrs) throws ServiceException {
        for (String attrName : attrs.keySet()) {
            if (attrName.charAt(0) == '+' || attrName.charAt(0) == '-')
                attrName = attrName.substring(1);

            if (!AttributeManager.getInstance().isDomainAdminModifiable(attrName, attrClass))
                throw ServiceException.PERM_DENIED("can not modify attr: "+attrName);
        }
    }
    
    /**
     * Only called for pure ACL based access manager.
     * 
     * @param am
     * @param zsc
     * @param target
     * @param needed if instanceof AttrRight : a preset or attr AdminRight    
     *                             Set<String> : attrs to get
     *                             Map<String, Object> : attrs to set
     * @return
     * @throws ServiceException
     */
    private static boolean doCheckRight(AccessManager am, ZimbraSoapContext zsc, Entry target, Object needed) 
    throws ServiceException {
        
        Account authedAcct = getAuthenticatedAccount(zsc);
        
        if (needed instanceof AdminRight) {
            AdminRight adminRight = (AdminRight)needed;
            if (adminRight.isPresetRight())
                return am.canDo(authedAcct, target, (AdminRight)needed, true, false, null);
            else if (adminRight.isAttrRight() && 
                     adminRight.getRightType() == Right.RightType.getAttrs)
                return am.canGetAttrs(authedAcct, target, ((AttrRight)needed).getAttrs(), true);
            else
                throw ServiceException.FAILURE("internal error", null);
        } else if (needed instanceof Set)
            return am.canGetAttrs(authedAcct, target, (Set<String>)needed, true);
        else if (needed instanceof Map)
            return am.canSetAttrs(authedAcct, target, (Map<String, Object>)needed, true);
        else
            throw ServiceException.FAILURE("internal error", null);
    }

    private static String printNeededRight(Entry target, Object needed) throws ServiceException {
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
    
    /**
     * This has to be called *after* the *can create* check.
     * For domain based AccessManager, all attrs are allowed if the admin can create.
     * 
     * @param zsc
     * @param targetType
     * @param entryName
     * @param attrs
     * @throws ServiceException
     */
    protected void checkSetAttrsOnCreate(ZimbraSoapContext zsc, TargetType targetType, String entryName, Map<String, Object> attrs) 
        throws ServiceException {
        
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            hasRight = true;
        } else {
            hasRight = am.canSetAttrsOnCreate(zsc.getAuthToken(), targetType, entryName, attrs, true);
        }
 
        if (!hasRight)
            throw ServiceException.PERM_DENIED("cannot set attrs");
    }
    
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
     * @param zsc
     * @param target
     * @param listRight
     * @param getAttrRight
     * @return
     */
    protected boolean hasRightsToList(ZimbraSoapContext zsc, NamedEntry target, 
            AdminRight listRight, Object getAttrRight) throws ServiceException {
        
        try {
            checkRight(zsc, target, listRight);
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
            checkRight(zsc, target, getAttrRight);
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
    
    protected boolean hasRightsToListCos(ZimbraSoapContext zsc, Cos target, 
            AdminRight listRight, Object getAttrRight) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            if (isDomainAdminOnly(zsc))  
                hasRight = canAccessCos(zsc, target);
            else
                hasRight = true;
        } else {
            hasRight = hasRightsToList(zsc, target, listRight,  getAttrRight);
        }
        
        return hasRight;
    }

    /**
     * -------------------
     * non-domained rights
     * (i.e. not: account, calendar resource, distribution list, domain)
     * 
     * For domain based access manager, if the authed admin is domain admin only,
     * it should have been rejected in SoapEngine.  So it should just return true 
     * here.  But we sanity check again, just in case.
     * -------------------
     *
     * @param zsc
     * @param context
     * @param target
     * @param needed
     * @throws ServiceException
     */
    protected void checkRight(ZimbraSoapContext zsc, Map context, Entry target, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            // sanity check, this path is really for global admins 
            if (isDomainAdminOnly(zsc)) {
                if (!domainAuthSufficient(context))
                    throw ServiceException.PERM_DENIED("cannot access entry");
            }
            
        } else {
            if (target == null)
                target = Provisioning.getInstance().getGlobalGrant();
            if (!doCheckRight(am, zsc, target, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(target, needed));
        }
    }
    
    /**
     * This API is for checking ACL rights only, domain based access manager
     * will always return OK.  This should be called only when 
     * 
     * (1) domain based permission checking has passed.
     * or 
     * (2) from SOAP handlers that are actually admin commands, but can't inherit 
     *     from AdminDocumentHanlder because it already inherited from otehr class, 
     *     e.g. SearchMultipleMailboxes.
     *     This method is static just because of that.  Ideally it should call 
     *     the other checkRight API, which does sanity checking for domain admins 
     *     if the active AccessManager is a domain based access manager.  But that 
     *     AI has other dependency on AdminDocumentHanlder/DocumentHandler instance 
     *     methods, also, the sanity is really not necessary, we should remove it 
     *     at some point when we can completely retire the domain based access 
     *     manager.
     * 
     * @param zsc
     * @param target
     * @param needed
     * @throws ServiceException
     */
    public static void checkRight(ZimbraSoapContext zsc, Entry target, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            return;
        } else {
            if (target == null)
                target = Provisioning.getInstance().getGlobalGrant();
            if (!doCheckRight(am, zsc, target, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(target, needed));
        }
    }
    
    
    /*
     * ------------------------------------------------------------
     * domain-ed rights
     * (i.e. account, calendar resource, distribution list, domain)
     * 
     * methods for backward compatibility with domain based 
     * AccessManager.  so we can:
     * 1. switch back to domain based AccessManager if needed.
     * 2. callsites in Admin soap handlers looks cleaner.
     * 
     * ------------------------------------------------------------
     */
   
    
    /* 
     * -------------
     * account right
     * -------------
     */
    protected void checkAccountRight(ZimbraSoapContext zsc, Account account, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            if (!canAccessAccount(zsc, account))
                throw ServiceException.PERM_DENIED("can not access account");
            
            if (isDomainAdminOnly(zsc) && (needed instanceof Map))
                checkModifyAttrs(AttributeClass.account, (Map<String, Object>)needed);
            
        } else {
            checkDomainStatus(am, account);
            
            Boolean canAccess = canAccessAccountCommon(zsc, account);
            boolean hasRight;
            if (canAccess == null)
                hasRight = doCheckRight(am, zsc, account, needed);
            else
                hasRight = canAccess.booleanValue();
            if (!hasRight)
                throw ServiceException.PERM_DENIED(printNeededRight(account, needed));
        }
    }

    /*
     * -----------------------
     * calendar resource right
     * -----------------------
     */
    protected void checkCalendarResourceRight(ZimbraSoapContext zsc, CalendarResource cr, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            if (!canAccessAccount(zsc, cr))
                throw ServiceException.PERM_DENIED("can not access calendar resource");

            if (isDomainAdminOnly(zsc) && (needed instanceof Map))
                checkModifyAttrs(AttributeClass.calendarResource, (Map<String, Object>)needed);
            
        } else {
            checkDomainStatus(am, cr);
            
            Boolean canAccess = canAccessAccountCommon(zsc, cr);
            boolean hasRight;
            if (canAccess == null)
                hasRight = doCheckRight(am, zsc, cr, needed);
            else
                hasRight = canAccess.booleanValue();
            if (!hasRight)
                throw ServiceException.PERM_DENIED(printNeededRight(cr, needed));
        }
    }
        
    /* 
     * --------
     * DL right
     * --------
     */
    protected void checkDistributionListRight(ZimbraSoapContext zsc, DistributionList dl, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            if (!canAccessEmail(zsc, dl.getName()))
                throw ServiceException.PERM_DENIED("can not access dl");
        } else {
            checkDomainStatus(am, dl);
            
            if (!doCheckRight(am, zsc, dl, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(dl, needed));
        }
    }
    
    /*
     * ------------
     * domain right
     * ------------
     */
    /**
     * called by handlers that need to check right on a domain for domain-ed objects:
     * account, alias, cr, dl.
     * 
     * Note: this method *do* check domain status.  
     */
    protected void checkDomainRightByEmail(ZimbraSoapContext zsc, String email, AdminRight needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            if (!canAccessEmail(zsc, email))
                throw ServiceException.PERM_DENIED("can not access email:" + email);
        } else {
            String domainName = getDomainFromEmail(email);
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw ServiceException.PERM_DENIED("no such domain: " + domainName);
            
            checkDomainStatus(am, domain);
            
            if (!doCheckRight(am, zsc, domain, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(domain, needed));
        }
    }
    
    /**
     * Note: this method do *not* check domain status.  
     */
    protected void checkDomainRight(ZimbraSoapContext zsc, String domainName, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            if (isDomainAdminOnly(zsc)) {
                if (!canAccessDomain(zsc, domainName))
                    throw ServiceException.PERM_DENIED("can not access domain");
                
                if (needed instanceof Map)
                    checkModifyAttrs(AttributeClass.domain, (Map<String, Object>)needed);
            }
        } else {
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw ServiceException.PERM_DENIED("no such domain: " + domainName);
            
            if (!doCheckRight(am, zsc, domain, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(domain, needed));
        }
    }
    
    /**
     * Note: this method do *not* check domain status.  
     */
    protected void checkDomainRight(ZimbraSoapContext zsc, Domain domain, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            // delegate to the String version of checkDomainRight instead of duplicating
            // the code here, since domain based access manager resolve domain rights 
            // by comparing the domain name anyway.
            checkDomainRight(zsc, domain.getName(), needed);
            
        } else {
            if (!doCheckRight(am, zsc, domain, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(domain, needed));
        }
    }
    
    protected void checkCosRight(ZimbraSoapContext zsc, Cos cos, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        if (isDomainBasedAccessManager(am)) {
            if (isDomainAdminOnly(zsc)) {
                if (!canAccessCos(zsc, cos))
                    throw ServiceException.PERM_DENIED("can not access cos");
            }
            
        } else {
            if (!doCheckRight(am, zsc, cos, needed))
                throw ServiceException.PERM_DENIED(printNeededRight(cos, needed));
        }
    }
    
    

    // ==========================================
    //    bookkeeping and documenting gadgets
    // ==========================================
    
    // book mark for callsites still needs ACL checking but is not done yet
    // in the end, no one should call this method
    protected void checkRightTODO() {
    }
    
    // for documenting rights needed and notes for a SOAP.
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.TODO);
    }


}
