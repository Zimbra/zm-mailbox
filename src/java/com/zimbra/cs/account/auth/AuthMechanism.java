/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.login.LoginException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.krb5.Krb5Login;
import com.zimbra.cs.account.krb5.Krb5Principal;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public abstract class AuthMechanism {
    
    public static enum AuthMech {
        /**
         * zimbraAuthMech type of "zimbra" means our own (use userPassword)
         */
        zimbra,
        
        /**
         * zimbraAuthMech type of "ldap" means use configured LDAP attrs
         * (zimbraAuthLdapURL, zimbraAuthLdapBindDn)
         */
        ldap,
        
        /**
         * zimbraAuthMech type of "ad" means use configured LDAP attrs
         * (zimbraAuthLdapURL, zimbraAuthLdapBindDn) for use with ActiveDirectory
         */
        ad,
        
        /**
         * zimbraAuthMech type of "kerberos5" means use kerberos5 authentication.
         * The principal can be obtained by, either:
         * (1) {email-local-part}@{domain-attr-zimbraAuthKerberos5Realm}
         * or
         * (2) {principal-name} if account zimbraForeignPrincipal is in the format of
         *     kerberos5:{principal-name}
         */
        kerberos5,
        
        /**
         * zimbraAuthMech type of "custom:{handler}" means use registered extension
         * of ZimbraCustomAuth.authenticate() method
         * see customauth.txt
         */
        custom;
        
        public static AuthMech fromString(String authMechStr) throws ServiceException {
            try {
                return AuthMech.valueOf(authMechStr);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown auth mech: " + authMechStr, e);
            }
        }
    }
    
    protected AuthMech authMech;
        
    protected AuthMechanism(AuthMech authMech) {
        this.authMech = authMech;
    }
    
    public static AuthMechanism newInstance(Account acct, Map<String, Object> context) 
    throws ServiceException {
        String authMechStr = AuthMech.zimbra.name();
        
        // bypass domain AuthMech and always use Zimbra auth for external virtual accounts 
        
        if (!acct.isIsExternalVirtualAccount()) {
            Provisioning prov = Provisioning.getInstance();
            Domain domain = prov.getDomain(acct);
            
            // see if it specifies an alternate auth
            if (domain != null) {
                String am;
                Boolean asAdmin = context == null ? null : (Boolean) context.get(AuthContext.AC_AS_ADMIN);
                if (asAdmin != null && asAdmin) {
                    am = domain.getAuthMechAdmin();
                    if (am == null) {
                        // fallback to zimbraAuthMech if zimbraAuthMechAdmin is not specified
                        am = domain.getAuthMech();
                    }
                } else {
                    am = domain.getAuthMech();
                }
                
                if (am != null) {
                    authMechStr = am;
                }
            }
        }
        
        
        if (authMechStr.startsWith(AuthMech.custom.name() + ":")) {
            return new CustomAuth(AuthMech.custom, authMechStr);
        } else {
            try {
                AuthMech authMech = AuthMech.fromString(authMechStr);
                
                switch (authMech) {
                    case zimbra:
                        return new ZimbraAuth(authMech);
                    case ldap:
                    case ad:
                        return new LdapAuth(authMech);
                    case kerberos5:
                        return new Kerberos5Auth(authMech);
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("invalid auth mech", e);
            }
            
            ZimbraLog.account.warn("unknown value for " + Provisioning.A_zimbraAuthMech+": " 
                    + authMechStr+", falling back to default mech");
            return new ZimbraAuth(AuthMech.zimbra);
        }

    }
    
    public static void doZimbraAuth(LdapProv prov, Domain domain, Account acct, String password, 
            Map<String, Object> authCtxt) 
    throws ServiceException {
        ZimbraAuth zimbraAuth = new ZimbraAuth(AuthMech.zimbra);
        zimbraAuth.doAuth(prov, domain, acct, password, authCtxt);
    }

    public boolean isZimbraAuth() {
        return false;
    }
    
    public abstract boolean checkPasswordAging() throws ServiceException;
    
    public abstract void doAuth(LdapProv prov, Domain domain, Account acct, String password, 
            Map<String, Object> authCtxt) 
    throws ServiceException;

    public AuthMech getMechanism() {
        return authMech;
    }
    
    public static String namePassedIn(Map<String, Object> authCtxt) {
        String npi;
        if (authCtxt != null) {
            npi = (String)authCtxt.get(AuthContext.AC_ACCOUNT_NAME_PASSEDIN);
            if (npi==null)
                npi = "";
        } else
            npi = "";
        return npi;    
    }
    
    /*
     * ZimbraAuth 
     */
    public static class ZimbraAuth extends AuthMechanism {
        ZimbraAuth(AuthMech authMech) {
            super(authMech);
        }
        
        public boolean isZimbraAuth() {
            return true;
        }
        
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password, 
                Map<String, Object> authCtxt) throws ServiceException {
            
            String encodedPassword = acct.getAttr(Provisioning.A_userPassword);

            if (encodedPassword == null) {
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), 
                        namePassedIn(authCtxt), "missing "+Provisioning.A_userPassword);
            }
            
            if (PasswordUtil.SSHA.isSSHA(encodedPassword)) {
                if (PasswordUtil.SSHA.verifySSHA(encodedPassword, password)) {
                    return; // good password, RETURN
                }  else {
                    throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), 
                            namePassedIn(authCtxt), "invalid password"); 
                }
            } else if (acct instanceof LdapEntry) {
                // not SSHA, authenticate to Zimbra LDAP
                prov.zimbraLdapAuthenticate(acct, password, authCtxt);
                return;  // good password, RETURN   
            }
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), namePassedIn(authCtxt));       
        }
        
        public boolean checkPasswordAging() throws ServiceException {
            return true;
        }
    }
    
    /*
     * LdapAuth
     */
    static class LdapAuth extends AuthMechanism {
        LdapAuth(AuthMech authMech) {
            super(authMech);
        }
        
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password, 
                Map<String, Object> authCtxt) throws ServiceException {
            prov.externalLdapAuth(domain, authMech, acct, password, authCtxt);
        }
        
        public boolean checkPasswordAging() throws ServiceException {
            return false;
        }
    }
    
    /*
     * Kerberos5Auth
     */
    static class Kerberos5Auth extends AuthMechanism {
        Kerberos5Auth(AuthMech authMech) {
            super(authMech);
        }
        
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password, 
                Map<String, Object> authCtxt) throws ServiceException {
            String principal = Krb5Principal.getKrb5Principal(domain, acct);
            
            if (principal == null)
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), 
                        namePassedIn(authCtxt), "cannot obtain principal for " + authMech.name() + " auth");
            
            if (principal != null) {
                try {
                    Krb5Login.verifyPassword(principal, password);
                } catch (LoginException e) {
                    throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), 
                            namePassedIn(authCtxt) + "(kerberos5 principal: " + principal + ")", e.getMessage(), e);
                }
            }
        }
        
        public boolean checkPasswordAging() throws ServiceException {
            return false;
        }
    }
    
    /*
     * CustomAuth
     */
    static class CustomAuth extends AuthMechanism {
        private String authMechStr;  // value of the zimbraAuthMech attribute
        
        private String mHandlerName = ""; 
        private ZimbraCustomAuth mHandler;
        List<String> mArgs;
        
        CustomAuth(AuthMech authMech, String authMechStr) {
            super(authMech);
            this.authMechStr = authMechStr;
         
            /*
             * value is in the format of custom:{handler} [arg1 arg2 ...]
             * args can be quoted.  whitespace and empty string in quoted args are preserved
             * 
             * e.g. http://blah.com:123    green " ocean blue   "  "" yelllow "" 
             * will be parsed and passed to the custom handler as:
             * [http://blah.com:123]
             * [green]
             * [ ocean blue   ]
             * []
             * [yelllow]
             * []
             * 
             */
            int handlerNameStart = authMechStr.indexOf(':');
            if (handlerNameStart != -1) {
                int handlerNameEnd = authMechStr.indexOf(' ');
                if (handlerNameEnd != -1) {
                    mHandlerName = authMechStr.substring(handlerNameStart+1, handlerNameEnd);
                    QuotedStringParser parser = new QuotedStringParser(authMechStr.substring(handlerNameEnd+1));
                    mArgs = parser.parse();
                    if (mArgs.size() == 0)
                        mArgs = null;
                } else {    
                    mHandlerName = authMechStr.substring(handlerNameStart+1);
                }
                
                if (!StringUtil.isNullOrEmpty(mHandlerName))
                    mHandler = ZimbraCustomAuth.getHandler(mHandlerName);
            }
            
            if (ZimbraLog.account.isDebugEnabled()) {
                StringBuffer sb = null;
                if (mArgs != null) {
                    sb = new StringBuffer();
                    for (String s : mArgs)
                        sb.append("[" + s + "] ");
                }
                ZimbraLog.account.debug("CustomAuth: handlerName=" + mHandlerName + ", args=" + sb);
            }
        }
        
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password, 
                Map<String, Object> authCtxt) throws ServiceException {
            
            if (mHandler == null)
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), 
                        namePassedIn(authCtxt), "handler " + mHandlerName + 
                        " for custom auth for domain " + domain.getName() + " not found");
            
            try {
                mHandler.authenticate(acct, password, authCtxt, mArgs);
                return;
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw (ServiceException)e;
                } else {   
                    String msg = e.getMessage();
                    if (StringUtil.isNullOrEmpty(msg))
                        msg = "";
                    else
                        msg = " (" + msg + ")";
                    /*
                     * include msg in the response, in addition to logs.  This is because custom 
                     * auth handlers might want to pass the reason back to the client.
                     */ 
                    throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), namePassedIn(authCtxt)+msg, msg, e);
                }
            }
            
        }
        
        public boolean checkPasswordAging() throws ServiceException {
            if (mHandler == null)
                throw ServiceException.FAILURE("custom auth handler " + mHandlerName + " not found", null);
            return mHandler.checkPasswordAging();
        }
    }
    
    static class QuotedStringParser {
        private String mInput;
          
        //the parser flips between these two sets of delimiters
        private static final String DELIM_WHITESPACE_AND_QUOTES = " \t\r\n\"";
        private static final String DELIM_QUOTES_ONLY ="\"";

        public QuotedStringParser(String input) {
            if (input == null)
                throw new IllegalArgumentException("Search Text cannot be null.");
            mInput = input;
        }

        public List<String> parse() {
            List<String> result = new ArrayList<String>();

            boolean returnTokens = true;
            String currentDelims = DELIM_WHITESPACE_AND_QUOTES;
            StringTokenizer parser = new StringTokenizer(mInput, currentDelims, returnTokens);

            boolean openDoubleQuote = false;
            boolean gotContent = false;
            String token = null;
            while (parser.hasMoreTokens()) {
                token = parser.nextToken(currentDelims);
                if (!isDoubleQuote(token)) {
                    if (!currentDelims.contains(token)) {
                        result.add(token);
                        gotContent = true;
                    }
                } else {
                    currentDelims = flipDelimiters(currentDelims);
                    // allow empty string in double quotes
                    if (openDoubleQuote && !gotContent)
                        result.add("");
                    openDoubleQuote = !openDoubleQuote;
                    gotContent = false;
                }
            }
            return result;
        }

        private boolean isDoubleQuote(String token) {
            return token.equals("\"");
        }

        private String flipDelimiters(String curDelims) {
            if (curDelims.equals(DELIM_WHITESPACE_AND_QUOTES))
                return DELIM_QUOTES_ONLY;
            else
                return DELIM_WHITESPACE_AND_QUOTES;
        }
    }
    
    public static void main(String[] args) {
        QuotedStringParser parser = new QuotedStringParser( "http://blah.com:123    green \" ocean blue   \"  \"\" yelllow \"\"");
        List<String> tokens = parser.parse();
        int i = 0;
        for (String s : tokens)
            System.out.format("%d [%s]\n", ++i, s);
        
        CustomAuth ca = new CustomAuth(AuthMech.custom, 
                "custom:sample http://blah.com:123    green \" ocean blue   \"  \"\" yelllow \"\"");
         
    }
}

