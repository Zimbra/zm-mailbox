/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 4, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/** @author schemers */
public abstract class AdminDocumentHandler extends DocumentHandler {


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
                Account acct = prov.get(AccountBy.id, acctId);
                if (acct != null && !Provisioning.onLocalServer(acct))
                    return proxyRequest(request, context, acctId);
            }

            xpath = getProxiedAccountElementPath();
            Element elt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (elt != null) {
                Account acct = prov.get(AccountBy.fromString(elt.getAttribute(AdminService.A_BY)), elt.getText());
                if (acct != null && !Provisioning.onLocalServer(acct))
                    return proxyRequest(request, context, acct.getId());
            }

            // check whether we need to proxy to the home server of a target calendar resource
            xpath = getProxiedResourcePath();
            String rsrcId = (xpath != null ? getXPath(request, xpath) : null);
            if (rsrcId != null) {
                CalendarResource rsrc = prov.get(CalendarResourceBy.id, rsrcId);
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

    /** Fetches the in-memory {@link Session} object appropriate for this request.
     *  If none already exists, one is created.
     * @return An {@link com.zimbra.cs.session.AdminSession}. */
    @Override
    public Session getSession(Map<String, Object> context) {
        return getSession(context, SessionCache.SESSION_ADMIN);
    }
}
