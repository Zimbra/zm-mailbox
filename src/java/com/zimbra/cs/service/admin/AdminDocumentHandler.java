/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 4, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RoleAccessManager;
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
import com.zimbra.soap.ZimbraSoapContext;

/** @author schemers */
public abstract class AdminDocumentHandler extends DocumentHandler {

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
                        return proxyRequest(request, context, server, zsc);
                }
            }
            
            xpath = getProxiedResourceElementPath();
            Element resourceElt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (resourceElt != null) {
                CalendarResource rsrc = getCalendarResource(prov, CalendarResourceBy.fromString(resourceElt.getAttribute(AdminConstants.A_BY)), resourceElt.getText(), zsc.getAuthToken());
                if (rsrc != null) {
                    Server server = prov.get(ServerBy.name, rsrc.getAttr(Provisioning.A_zimbraMailHost));
                    if (server != null && !LOCAL_HOST_ID.equalsIgnoreCase(server.getId()))
                        return proxyRequest(request, context, server, zsc);
                }
            }
 
            // check whether we need to proxy to a target server
            xpath = getProxiedServerPath();
            String serverId = (xpath != null ? getXPath(request, xpath) : null);
            if (serverId != null) {
                Server server = prov.get(ServerBy.id, serverId);
                if (server != null && !LOCAL_HOST_ID.equalsIgnoreCase(server.getId()))
                    return proxyRequest(request, context, server, zsc);
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
    
    public boolean canAccessDomain(ZimbraSoapContext zsc, String domainName) throws ServiceException {
        return AccessManager.getInstance().canAccessDomain(zsc.getAuthToken(), domainName);
    }

    public boolean canAccessDomain(ZimbraSoapContext zsc, Domain domain) throws ServiceException {
        return canAccessDomain(zsc, domain.getName());
    }

    public boolean canModifyMailQuota(ZimbraSoapContext zsc, Account target, long mailQuota) throws ServiceException {
        return AccessManager.getInstance().canModifyMailQuota(zsc.getAuthToken(), target, mailQuota);
    }

    private String getDomainFromEmail(String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        return parts[1];
    }
    
    public boolean canAccessEmail(ZimbraSoapContext zsc, String email) throws ServiceException {
        return canAccessDomain(zsc, getDomainFromEmail(email));
    }
    
    public boolean canAccessCos(ZimbraSoapContext zsc, String cosId) throws ServiceException {
        return AccessManager.getInstance().canAccessCos(zsc.getAuthToken(), cosId);
    }
    

    
    /*
     * ======================================================================
     *     connector methods between domain based access manager and 
     *     pure ACL based access manager.
     *     
     *     Maybe we should just make them all AccessManager methods, instead 
     *     if doing the isDomainBasedAccessManager test here.  TODO
     *     
     *     TODO: make sure only the following methods are called from
     *           all admin handlers, non of the legacy ones:
     *           (Admin)AccessManager.canAccessAccount, canAccessEmail, canAccessDomain 
     *           should be called.
     * ======================================================================
     */

    
    private boolean canDo(AccessManager am, ZimbraSoapContext zsc,
                          Entry target, Right rightNeeded) throws ServiceException {
        return am.canDo(zsc.getAuthToken(), target, rightNeeded, true, false, null);
    }
    
    private boolean isDomainBasedAccessManager(AccessManager am) {
        return (!(am instanceof RoleAccessManager));
    }
    
    /* 
     * ====================
     * get/set attrs rights
     * ====================
     */
    /*
     * This has to be called *after* the *can create* check.
     * For domain based AccessManager, all attrs are allowed if the admin can create.
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

    
    /*
     * =============
     * preset rights
     * =============
     */
    
    /*
     * -------------------
     * non-domained rights
     * (i.e. not: account, calendar resource, distribution list, domain)
     * 
     * For domain based access manager, if the authed admin is domain admin only,
     * it should have been rejected in SoapEngine.  So it should just return true 
     * here.  But we sanity check again, just in case.
     * -------------------
     */
    // context is only needed for the domainAuthSufficient call, we don't really need it, as 
    // none of the domainAuthSufficient impl uses it, cam remove.  TODO
    protected void checkRight(ZimbraSoapContext zsc, Map context, Entry entry, AdminRight rightNeeded) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            // sanity check, this path is really for global admins 
            if (isDomainAdminOnly(zsc))
                hasRight = domainAuthSufficient(context);
            else
                hasRight = true;
            if (!hasRight)
                throw ServiceException.PERM_DENIED("cannot access entry");
        } else {
            if (entry == null)
                entry = Provisioning.getInstance().getGlobalGrant();
            hasRight = canDo(am, zsc, entry, rightNeeded);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("need right " + rightNeeded.getName());
        }
    }
    
    
    /*
     * ------------------------------------------------------------
     * domain-ed rights
     * (i.e. account, calendar resource, distribution list, domain)
     * 
     * methods for backward compatibility so we can switch back to
     * domain based AccessManager if needed.
     * ------------------------------------------------------------
     */

    
    /* 
     * -------------
     * account right
     * -------------
     */
    protected void checkAccountRight(ZimbraSoapContext zsc, Account account, AdminRight rightNeeded) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            hasRight = canAccessAccount(zsc, account);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("can not access account");
        } else {
            Boolean canAccess = canAccessAccountCommon(zsc, account);
            if (canAccess == null)
                hasRight = canDo(am, zsc, account, rightNeeded);
            else
                hasRight = canAccess.booleanValue();
            if (!hasRight)
                throw ServiceException.PERM_DENIED("need right " + rightNeeded.getName());
        }
    }

    /*
     * -----------------------
     * calendar resource right
     * -----------------------
     */
    protected void checkCalendarResourceRight(ZimbraSoapContext zsc, CalendarResource cr, AdminRight rightNeeded) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            hasRight = canAccessAccount(zsc, cr);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("can not access calendar resource");
        } else {
            Boolean canAccess = canAccessAccountCommon(zsc, cr);
            if (canAccess == null)
                hasRight = canDo(am, zsc, cr, rightNeeded);
            else
                hasRight = canAccess.booleanValue();
            if (!hasRight)
                throw ServiceException.PERM_DENIED("need right " + rightNeeded.getName());
        }
    }
        
    /* 
     * --------
     * DL right
     * --------
     */
    protected void checkDistributionListRight(ZimbraSoapContext zsc, DistributionList dl, AdminRight rightNeeded) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            hasRight = canAccessEmail(zsc, dl.getName());
            if (!hasRight)
                throw ServiceException.PERM_DENIED("can not access dl");
        } else {
            hasRight = canDo(am, zsc, dl, rightNeeded);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("need right " + rightNeeded.getName());
        }
    }
    
    /*
     * ------------
     * domain right
     * ------------
     */
    protected void checkDomainRightByEmail(ZimbraSoapContext zsc, String email, AdminRight rightNeeded) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            hasRight = canAccessEmail(zsc, email);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("can not access email:" + email);
        } else {
            String domainName = getDomainFromEmail(email);
            Domain domain = Provisioning.getInstance().get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                throw ServiceException.PERM_DENIED("no such domain: " + domainName);
            
            hasRight = canDo(am, zsc, domain, rightNeeded);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("need right " + rightNeeded.getName());
        }
    }
    
    protected void checkDomainRight(ZimbraSoapContext zsc, Domain domain, AdminRight rightNeeded) throws ServiceException {
        AccessManager am = AccessManager.getInstance();
        boolean hasRight;
        
        if (isDomainBasedAccessManager(am)) {
            if (isDomainAdminOnly(zsc))  
                hasRight = canAccessDomain(zsc, domain);
            else
                hasRight = true;
            if (!hasRight)
                throw ServiceException.PERM_DENIED("can not access domain");
        } else {
            hasRight = canDo(am, zsc, domain, rightNeeded);
            if (!hasRight)
                throw ServiceException.PERM_DENIED("need right " + rightNeeded.getName());
        }
    }
    

}
