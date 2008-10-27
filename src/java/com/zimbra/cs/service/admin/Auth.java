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
import com.zimbra.cs.account.ZimbraAuthToken;
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
        
        Provisioning prov = Provisioning.getInstance();
        
        Element authTokenEl = request.getOptionalElement(AdminConstants.E_AUTH_TOKEN);
        if (authTokenEl != null) {
            // authtoken admin auth is only supported by Yahoo auth provider, not the default Zimbra auth provider.
            try {
                at = AuthProvider.getAuthToken(authTokenEl, new HashMap<String, Object>());
                if (at == null)
                    throw ServiceException.AUTH_EXPIRED();
                
                com.zimbra.cs.service.account.Auth.addAccountToLogContextByAuthToken(prov, at);
                
                if (at.isExpired())
                    throw ServiceException.AUTH_EXPIRED();
                
                // make sure that the authenticated account is active and has not been deleted/disabled since the last request
                acct = prov.get(AccountBy.id, at.getAccountId(), at);
                if (acct == null || !acct.getAccountStatus(prov).equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                    throw ServiceException.AUTH_EXPIRED();
                
                // make sure the authenticated account is an admin account
                checkAdmin(acct);
            } catch (AuthTokenException e) {
                throw ServiceException.AUTH_REQUIRED();
            }
            
        } else {
            /*
             * only one of  
             *     <name>...</name>
             * or 
             *     <account by="name|id|foreignPrincipal">...</account>
             * can/must be specified    
             */
            String name = request.getAttribute(AdminConstants.E_NAME, null);
            Element acctEl = request.getOptionalElement(AccountConstants.E_ACCOUNT);
            if (name != null && acctEl != null)
                throw ServiceException.INVALID_REQUEST("only one of <name> or <account> can be specified", null);
            if (name == null && acctEl == null)
                throw ServiceException.INVALID_REQUEST("missing <name> or <account>", null);
            
            String password = request.getAttribute(AdminConstants.E_PASSWORD);
            Element virtualHostEl = request.getOptionalElement(AccountConstants.E_VIRTUAL_HOST);
            String virtualHost = virtualHostEl == null ? null : virtualHostEl.getText().toLowerCase();
            
            String valuePassedIn;
            AccountBy by;
            String value;
            if (name != null) {
                valuePassedIn = name;
                by = AccountBy.name;
            } else {
                valuePassedIn = acctEl.getText();
                String byStr = acctEl.getAttribute(AccountConstants.A_BY, AccountBy.name.name());
                by = AccountBy.fromString(byStr);
            }
            value = valuePassedIn;
     
            try {
                
                if (by == AccountBy.name && value.indexOf("@") == -1) {
                    // first try to get by adminName, which resolves the account under cn=admins,cn=zimbra
                    // and does not need a domain 
                    acct = prov.get(AccountBy.adminName, value, zsc.getAuthToken());
                    
                    // not found, try applying virtual host name
                    if (acct == null) {
                        if (virtualHost != null) {
                            Domain d = prov.get(DomainBy.virtualHostname, virtualHost);
                            if (d != null)
                                value = value + "@" + d.getName();
                        }                    
                    } 
                }
    
                if (acct == null)
                    acct = prov.get(by, value);
    
                if (acct == null)
                    throw AuthFailedServiceException.AUTH_FAILED(value, valuePassedIn, "account not found");
            
                Account.addAccountToLogContext(prov, acct.getId(), ZimbraLog.C_NAME, ZimbraLog.C_ID, null);
                
                ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "AdminAuth","account", value})); 
            
                Map<String, Object> authCtxt = new HashMap<String, Object>();
                authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, context.get(SoapEngine.ORIG_REQUEST_IP));
                authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, valuePassedIn);
                prov.authAccount(acct, password, "soap", authCtxt);
                checkAdmin(acct);
                at = AuthProvider.getAuthToken(acct, true);
                
            } catch (ServiceException se) {
                ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "AdminAuth","account", value, "error", se.getMessage()}));    
                throw se;
            }
        }
        
        return doResponse(at, zsc, acct);
	}
    
    private AuthToken dummyYCCTokenTestNeverCallMe(Element authTokenEl) throws ServiceException, AuthTokenException  {
        String atType = authTokenEl.getAttribute(AdminConstants.A_TYPE);
        if ("YAHOO_CALENDAR_AUTH_PROVIDER".equals(atType)) {
            for (Element a : authTokenEl.listElements(AdminConstants.E_A)) {
                String name = a.getAttribute(AdminConstants.A_N);
                String value = a.getText();
                if ("ADMIN_AUTH_KEY".equals(name) &&
                    "1210713456+dDedin1lO8d1_j8Kl.vl".equals(value)) {
                    Account acct = Provisioning.getInstance().get(AccountBy.name, "admin@phoebe.mac");
                    return new ZimbraAuthToken(acct, true);
                }
            }
        }
        return null;
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
