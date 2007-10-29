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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
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

    protected String[] getProxiedAccountPath()         { return null; }
    protected String[] getProxiedAccountElementPath()  { return null; }
    protected String[] getProxiedResourcePath()        { return null; }
    protected String[] getProxiedServerPath()          { return null; }

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
                Account acct = prov.get(AccountBy.id, acctId, true);
                if (acct != null && !Provisioning.onLocalServer(acct))
                    return proxyRequest(request, context, acctId);
            }

            xpath = getProxiedAccountElementPath();
            Element elt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (elt != null) {
                Account acct = prov.get(AccountBy.fromString(elt.getAttribute(AdminConstants.A_BY)), elt.getText(), true);
                if (acct != null && !Provisioning.onLocalServer(acct))
                    return proxyRequest(request, context, acct.getId());
            }

            // check whether we need to proxy to the home server of a target calendar resource
            xpath = getProxiedResourcePath();
            String rsrcId = (xpath != null ? getXPath(request, xpath) : null);
            if (rsrcId != null) {
                CalendarResource rsrc = prov.get(CalendarResourceBy.id, rsrcId, true);
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

    public boolean canAccessEmail(ZimbraSoapContext zsc, String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        return canAccessDomain(zsc, parts[1]);
    }
}
