/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.authenticator;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.security.Constraint;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.ZimbraServlet;

public abstract class SSOAuthenticator {

    public static class ZimbraPrincipal implements Principal {

        private String authenticationName;  // name in the authenticating material 
        private Account account;  // resolved zimbra account
        
        ZimbraPrincipal(String authenticationName, Account account) throws ServiceException {
            if (authenticationName == null) {
                throw ServiceException.INVALID_REQUEST("no authentication name", null);
            }
            if (account == null) {
                throw ServiceException.DEFEND_ACCOUNT_HARVEST(authenticationName);
            }
            
            ZimbraLog.account.debug("SSOAuthenticator - %s resolved to Zimbra account %s",
                    authenticationName, account.getName());
            
            this.authenticationName = authenticationName;
            this.account = account;
        }
        
        @Override
        // will never return null
        public String getName() {
            return authenticationName;
        }
        
        // will never return null
        public Account getAccount() {
            return account;
        }
        
    }
    
    public static class SSOAuthenticatorServiceException extends AccountServiceException {
        public static final String NO_CLIENT_CERTIFICATE     = "account.NO_CLIENT_CERTIFICATE";
        public static final String SENT_CHALLENGE            = "account.SENT_CHALLENGE";
        
        protected SSOAuthenticatorServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
            super(message, code, isReceiversFault, cause);
        }
        
        public static SSOAuthenticatorServiceException NO_CLIENT_CERTIFICATE() {
            return new SSOAuthenticatorServiceException("no client certificate", NO_CLIENT_CERTIFICATE, SENDERS_FAULT, null);
        }
        
        public static SSOAuthenticatorServiceException SENT_CHALLENGE() {
            return new SSOAuthenticatorServiceException("sent challenge", SENT_CHALLENGE, SENDERS_FAULT, null);
        }
    }
    
    protected HttpServletRequest req;
    protected HttpServletResponse resp;
    
    SSOAuthenticator(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
    }
    
    public abstract String getAuthType();
    
    // should never return a null ZimbraPrincipal
    public abstract ZimbraPrincipal authenticate() throws ServiceException;

}
