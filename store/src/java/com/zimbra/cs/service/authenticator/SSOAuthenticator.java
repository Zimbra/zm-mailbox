/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
