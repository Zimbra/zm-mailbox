/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Oct 4, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
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
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.operation.BlockingOperation;
import com.zimbra.cs.operation.Requester;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/** @author schemers */
public abstract class AdminDocumentHandler extends DocumentHandler implements AdminRightCheckPoint {

    @Override
    public Object preHandle(Element request, Map<String, Object> context) throws ServiceException { 
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Session session = getSession(zsc);
        OperationContext octxt = null;
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
                    if (server != null && !getLocalHostId().equalsIgnoreCase(server.getId()))
                        return proxyRequest(request, context, server);
                }
            }
            
            xpath = getProxiedResourceElementPath();
            Element resourceElt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (resourceElt != null) {
                CalendarResource rsrc = getCalendarResource(prov, CalendarResourceBy.fromString(resourceElt.getAttribute(AdminConstants.A_BY)), resourceElt.getText(), zsc.getAuthToken());
                if (rsrc != null) {
                    Server server = prov.get(ServerBy.name, rsrc.getAttr(Provisioning.A_zimbraMailHost));
                    if (server != null && !getLocalHostId().equalsIgnoreCase(server.getId()))
                        return proxyRequest(request, context, server);
                }
            }
 
            // check whether we need to proxy to a target server
            xpath = getProxiedServerPath();
            String serverId = (xpath != null ? getXPath(request, xpath) : null);
            if (serverId != null) {
                Server server = prov.get(ServerBy.id, serverId);
                if (server != null && !getLocalHostId().equalsIgnoreCase(server.getId()))
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
    
    /*
     * if specific attrs are requested on Get{ldap-object}:
     * - INVALID_REQUEST is thrown if any of the requested attrs is not a valid attribute on the entry
     * - PERM_DENIED is thrown if the authed account does not have get attr right for all the requested attrs.
     * 
     * Because for the get{Object} calls, we want to be strict, as opposed to misleading the client that 
     * a requested attribute is not set on the entry.
     * 
     * Note: the behavior is different than the behavior of SearchDirectory, in that:
     *    - if any of the requested attrs is not a valid attribute on the entry: ignored
     *    - if the authed account does not have get attr right for all the requested attrs: the entry is not included in the response
     *    
     */
    protected Set<String> getReqAttrs(Element request, AttributeClass klass) throws ServiceException {
        String attrsStr = request.getAttribute(AdminConstants.A_ATTRS, null);
        if (attrsStr == null)
            return null;
        
        String[] attrs = attrsStr.split(",");
        
        Set<String> attrsOnEntry = AttributeManager.getInstance().getAllAttrsInClass(klass);
        Set<String> validAttrs = new HashSet<String>();
        
        for (String attr : attrs) {
            if (attrsOnEntry.contains(attr))
                validAttrs.add(attr);
            else 
                throw ServiceException.INVALID_REQUEST("requested attribute " + attr + " is not on " + klass.name(), null);
        }
        
        // check and throw if validAttrs is empty?  
        // probably not, to be compatible with SearchDirectory
        
        return validAttrs;
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


    
    /*
     * TODO:  can't be private yet, still called from ZimbraAdminExt and ZimbraCustomerServices/hosted
     *        Need to fix those callsite to call one of the check*** methods.
     *        
     *        after that, move this method and related methods to AdminAccessControl and 
     *        only call this method from there. 
     */
    public boolean canAccessEmail(ZimbraSoapContext zsc, String email) throws ServiceException {
        return canAccessDomain(zsc, AdminAccessControl.getDomainFromEmail(email));
    }
    

    
    /*
     * ======================================================================
     *     Connector methods between domain based access manager and 
     *     pure ACL based access manager.
     * ======================================================================
     */

    /**
     * only called for domain based access manager
     * 
     * @param attrClass
     * @param attrs
     * @throws ServiceException
     */
    public void checkModifyAttrs(ZimbraSoapContext zsc, AttributeClass attrClass, Map<String, Object> attrs) throws ServiceException {
        AdminAccessControl.getAdminAccessControl(zsc).checkModifyAttrs(attrClass, attrs);
    }

    /**
     * This has to be called *after* the *can create* check.
     * For domain based AccessManager, all attrs are allowed if the admin can create.
     */
    protected void checkSetAttrsOnCreate(ZimbraSoapContext zsc, TargetType targetType, String entryName, Map<String, Object> attrs) 
        throws ServiceException {
        AdminAccessControl.getAdminAccessControl(zsc).checkSetAttrsOnCreate(targetType, entryName, attrs);
    }
    
    protected boolean hasRightsToList(ZimbraSoapContext zsc, NamedEntry target, 
            AdminRight listRight, Object getAttrRight) throws ServiceException {
        return AdminAccessControl.getAdminAccessControl(zsc).hasRightsToList(target, listRight, getAttrRight);
    }
    
    protected boolean hasRightsToListCos(ZimbraSoapContext zsc, Cos target, 
            AdminRight listRight, Object getAttrRight) throws ServiceException {
        return AdminAccessControl.getAdminAccessControl(zsc).hasRightsToListCos(target, listRight, getAttrRight);
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
     */
    protected AdminAccessControl checkRight(ZimbraSoapContext zsc,
        Map<String, Object> context, Entry target, Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        
        //
        // yuck, the isDomainBasedAccessManager logic has to be here
        // only because of the call to domainAuthSufficient
        //
        if (AdminAccessControl.isDomainBasedAccessManager(am)) {
            // sanity check, this path is really for global admins 
            if (isDomainAdminOnly(zsc)) {
                if (!domainAuthSufficient(context))
                    throw ServiceException.PERM_DENIED("cannot access entry");
            }
            
            // yuck, return a AdminAccessControl object instead of null so
            // we don't NPE at callsites or having to check null if they need 
            // to use the aac.
            // this whole method should probably be deleted anyway.
            return AdminAccessControl.getAdminAccessControl(zsc);
            
        } else {
            return checkRight(zsc, target, needed);
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
     *     API has other dependency on AdminDocumentHanlder/DocumentHandler instance 
     *     methods, also, the sanity is really not necessary, we should remove it 
     *     at some point when we can completely retire the domain based access 
     *     manager.
     *     
     *     TODO: find callsites of non AdminDocumentHanlder and call AdminAccessControl 
     *           directly, after this method can be protected and non-static
     * 
     * @param zsc
     * @param target
     * @param needed
     * @throws ServiceException
     */
    public static AdminAccessControl checkRight(ZimbraSoapContext zsc, Entry target, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkRight(target, needed);
        return aac;
    }
   
    /* 
     * -------------
     * cos right
     * -------------
     */
    protected AdminAccessControl checkCosRight(ZimbraSoapContext zsc, Cos cos, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkCosRight(cos, needed);
        return aac;
    }
    
    /* 
     * -------------
     * account right
     * -------------
     */
    protected AdminAccessControl checkAccountRight(ZimbraSoapContext zsc, Account account, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkAccountRight(this, account, needed);
        return aac;
    }

    /*
     * -----------------------
     * calendar resource right
     * -----------------------
     */
    protected AdminAccessControl checkCalendarResourceRight(ZimbraSoapContext zsc, CalendarResource cr, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkCalendarResourceRight(this, cr, needed);
        return aac;
    }
    
    /*
     * convenient method for checking the admin login as right
     */
    protected AdminAccessControl checkAdminLoginAsRight(ZimbraSoapContext zsc, Provisioning prov, Account account) throws ServiceException {
        if (account.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            CalendarResource resource = prov.get(CalendarResourceBy.id, account.getId());
            return checkCalendarResourceRight(zsc, resource, Admin.R_adminLoginCalendarResourceAs);
        } else
            return checkAccountRight(zsc, account, Admin.R_adminLoginAs);
    }
    
    /* 
     * --------
     * DL right
     * --------
     */
    protected AdminAccessControl checkDistributionListRight(ZimbraSoapContext zsc, DistributionList dl, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDistributionListRight(this, dl, needed);
        return aac;
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
    protected AdminAccessControl checkDomainRightByEmail(ZimbraSoapContext zsc, String email, AdminRight needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDomainRightByEmail(this, email, needed);
        return aac;
    }
    
    /**
     * Note: this method do *not* check domain status.  
     */
    protected AdminAccessControl checkDomainRight(ZimbraSoapContext zsc, String domainName, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDomainRight(this, domainName, needed);
        return aac;
    }
    
    /**
     * Note: this method do *not* check domain status.  
     */
    protected AdminAccessControl checkDomainRight(ZimbraSoapContext zsc, Domain domain, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDomainRight(this, domain, needed);
        return aac;
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
