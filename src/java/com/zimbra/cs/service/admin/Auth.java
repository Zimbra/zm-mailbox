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
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.session.Session;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class Auth extends AdminDocumentHandler {

    /** Returns (or creates) the in-memory {@link Session} object appropriate
     *  for this request.<p>
     * 
     *  Auth commands do not create a session by default, as issues with the 
     *  ordering of operations might cause the new session to be for the old
     *  credentials rather than for the new ones.
     * 
     * @return <code>null</code> in all cases */
    public Session getSession(Map context) {
        return null;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String name = request.getAttribute(AdminConstants.E_NAME);
		String password = request.getAttribute(AdminConstants.E_PASSWORD);
		Provisioning prov = Provisioning.getInstance();
		Account acct = null;
        boolean isDomainAdmin = false;
        
        Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
        String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
 
        try {
            
            if (name.indexOf("@") == -1) {

                acct = prov.get(AccountBy.adminName, name);
                
                if (acct == null) {
                    if (virtualHost != null) {
                        Domain d = prov.get(DomainBy.virtualHostname, virtualHost);
                        if (d != null)
                            name = name + "@" + d.getName();
                    }                    
                } 
            }

            if (acct == null)
                acct = prov.get(AccountBy.name, name);

            if (acct == null)
                throw AuthFailedServiceException.AUTH_FAILED(name, "account not found");
        
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "AdminAuth","account", name})); 
        
            prov.authAccount(acct, password, "soap");
            
            isDomainAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
            boolean isAdmin= acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);            
            boolean ok = (isDomainAdmin || isAdmin);
            if (!ok) 
                    throw ServiceException.PERM_DENIED("not an admin account");

        } catch (ServiceException se) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "AdminAuth","account", name, "error", se.getMessage()}));    
            throw se;
        }

        Element response = zsc.createElement(AdminConstants.AUTH_RESPONSE);
        AuthToken at = AuthToken.getAuthToken(acct, true);
        at.encodeAuthResp(response, true);
        
        response.addAttribute(AdminConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
        response.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, Provisioning.A_zimbraIsDomainAdminAccount).setText(isDomainAdmin+"");
        Session session = updateAuthenticatedAccount(zsc, at, true);
        if (session != null)
            ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType(), true);
		return response;
	}

    public boolean needsAuth(Map<String, Object> context) {
        // can't require auth on auth request
        return false;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        // can't require auth on auth request
        return false;
    }
}
