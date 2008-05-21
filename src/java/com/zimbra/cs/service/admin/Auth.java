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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthContext;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class Auth extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        AuthToken at = null;
        Account acct = null;
        
        Element authTokenEl = request.getOptionalElement(AdminConstants.E_AUTH_TOKEN);
        if (authTokenEl != null) {
            try {
                at = AuthProvider.getAuthToken(authTokenEl, new HashMap<String, Object>());
                if (at == null)
                    throw ServiceException.AUTH_EXPIRED();
                if (at.isExpired())
                    throw ServiceException.AUTH_EXPIRED();
                
                // make sure that the authenticated account is active and has not been deleted/disabled since the last request
                acct = Provisioning.getInstance().get(AccountBy.id, at.getAccountId(), at);
                if (acct == null || !acct.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                    throw ServiceException.AUTH_EXPIRED();
                
                // make sure the authenticated account is an admin account
                checkAdmin(acct);
            } catch (AuthTokenException e) {
                throw ServiceException.AUTH_REQUIRED();
            }
            
        } else {
            String namePassedIn = request.getAttribute(AdminConstants.E_NAME);
            String name = namePassedIn;
    		String password = request.getAttribute(AdminConstants.E_PASSWORD);
    		Provisioning prov = Provisioning.getInstance();
            
            Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
            String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
     
            try {
                
                if (name.indexOf("@") == -1) {
    
                    acct = prov.get(AccountBy.adminName, name, zsc.getAuthToken());
                    
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
                    throw AuthFailedServiceException.AUTH_FAILED(name, namePassedIn, "account not found");
            
                ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "AdminAuth","account", name})); 
            
                Map<String, Object> authCtxt = new HashMap<String, Object>();
                authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, context.get(SoapEngine.ORIG_REQUEST_IP));
                authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, namePassedIn);
                prov.authAccount(acct, password, "soap", authCtxt);
                checkAdmin(acct);
                at = AuthProvider.getAuthToken(acct, true);
                
            } catch (ServiceException se) {
                ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "AdminAuth","account", name, "error", se.getMessage()}));    
                throw se;
            }
        }
        
        return doResponse(at, zsc, acct);
	}
	
	private void checkAdmin(Account acct) throws ServiceException {
	    boolean isDomainAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        boolean isAdmin= acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);            
        boolean ok = (isDomainAdmin || isAdmin);
        if (!ok) 
            throw ServiceException.PERM_DENIED("not an admin account");
	}

	private Element doResponse(AuthToken at, ZimbraSoapContext zsc, Account acct) throws ServiceException {
	    Element response = zsc.createElement(AdminConstants.AUTH_RESPONSE);
        at.encodeAuthResp(response, true);
        
        response.addAttribute(AdminConstants.E_LIFETIME, at.getExpires() - System.currentTimeMillis(), Element.Disposition.CONTENT);
        
        boolean isDomainAdmin = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        response.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, Provisioning.A_zimbraIsDomainAdminAccount).setText(isDomainAdmin+"");
        Session session = updateAuthenticatedAccount(zsc, at, true);
        if (session != null)
            ZimbraSoapContext.encodeSession(response, session.getSessionId(), session.getSessionType());
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
