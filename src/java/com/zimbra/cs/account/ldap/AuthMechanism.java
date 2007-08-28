/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.krb5.Krb5Login;
import com.zimbra.cs.account.krb5.Krb5Principal;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

abstract class AuthMechanism {
    
    protected String mAuthMech;  // value of the zimbraAuthMech attribute
    
    protected AuthMechanism(String authMech) {
        mAuthMech = authMech;
    }
    
    public static AuthMechanism makeInstance(Account acct) throws ServiceException {
        String authMech = Provisioning.AM_ZIMBRA;
        
        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.getDomain(acct);
        
        // see if it specifies an alternate auth
        if (domain != null) {
            String am = domain.getAttr(Provisioning.A_zimbraAuthMech);
            if (am != null)
                authMech = am;
        }
        
        if (authMech.equals(Provisioning.AM_ZIMBRA))
            return new ZimbraAuth(authMech);
        else if (authMech.equals(Provisioning.AM_LDAP) || authMech.equals(Provisioning.AM_AD)) 
            return new LdapAuth(authMech);
        else if (authMech.equals(Provisioning.AM_KERBEROS5))
            return new Kerberos5Auth(authMech);
        else if (authMech.startsWith(Provisioning.AM_CUSTOM))
            return new CustomAuth(authMech);
        else {
            // we didn't have a valid auth mech, fallback to zimbra default
            ZimbraLog.account.warn("unknown value for "+Provisioning.A_zimbraAuthMech+": "+authMech+", falling back to default mech");
            return new ZimbraAuth(authMech);
        }
    }
    
    public static void doDefaultAuth(AuthMechanism authMech, LdapProvisioning prov, Domain domain, Account acct, String password) throws ServiceException {
        ZimbraAuth zimbraAuth = new ZimbraAuth(authMech.getMechanism());
        zimbraAuth.doAuth(prov, domain, acct, password);
    }

    
    public boolean isZimbraAuth() {
        return false;
    }
    
    abstract void doAuth(LdapProvisioning prov, Domain domain, Account acct, String password) throws ServiceException;
    
    String getMechanism() {
        return mAuthMech;
    }
    
    /*
     * ZimbraAuth 
     */
    static class ZimbraAuth extends AuthMechanism {
        ZimbraAuth(String authMech) {
            super(authMech);
        }
        
        public boolean isZimbraAuth() {
            return true;
        }
        
        void doAuth(LdapProvisioning prov, Domain domain, Account acct, String password) throws ServiceException {
            String encodedPassword = acct.getAttr(Provisioning.A_userPassword);

            if (encodedPassword == null)
                throw AccountServiceException.AUTH_FAILED(acct.getName());

            if (LdapUtil.isSSHA(encodedPassword)) {
                if (LdapUtil.verifySSHA(encodedPassword, password)) {
                    return; // good password, RETURN
                }

            } else if (acct instanceof LdapEntry) {
                String[] urls = new String[] { LdapUtil.getLdapURL() };
                try {
                    LdapUtil.ldapAuthenticate(urls, ((LdapEntry)acct).getDN(), password);
                    return; // good password, RETURN                
                } catch (AuthenticationException e) {
                    throw AccountServiceException.AUTH_FAILED(acct.getName(), e);
                } catch (AuthenticationNotSupportedException e) {
                    throw AccountServiceException.AUTH_FAILED(acct.getName(), e);
                } catch (NamingException e) {
                    throw ServiceException.FAILURE(e.getMessage(), e);
                }
            }
            throw AccountServiceException.AUTH_FAILED(acct.getName());       
        }
    }
    
    /*
     * LdapAuth
     */
    static class LdapAuth extends AuthMechanism {
        LdapAuth(String authMech) {
            super(authMech);
        }
        
        void doAuth(LdapProvisioning prov, Domain domain, Account acct, String password) throws ServiceException {
            prov.externalLdapAuth(domain, mAuthMech, acct, password);
        }
    }
    
    /*
     * Kerberos5Auth
     */
    static class Kerberos5Auth extends AuthMechanism {
        Kerberos5Auth(String authMech) {
            super(authMech);
        }
        
        void doAuth(LdapProvisioning prov, Domain domain, Account acct, String password) throws ServiceException {
            String principal = Krb5Principal.getKrb5Principal(domain, acct);
            
            if (principal == null)
                throw AccountServiceException.AUTH_FAILED(acct.getName(), new Exception("cannot obtain principal for " + mAuthMech + " auth"));
            
            if (principal != null) {
                try {
                    Krb5Login.verifyPassword(principal, password);
                } catch (LoginException e) {
                    throw AccountServiceException.AUTH_FAILED(acct.getName() + "(kerberos5 principal: " + principal + ")", e);
                }
            }
        }
    }
    
    /*
     * CustomAuth
     */
    static class CustomAuth extends AuthMechanism {
        private String mAuthHandler; 
        
        CustomAuth(String authMech) {
            super(authMech);
         
            // value is in the format of custom:{handler}
            int idx = mAuthMech.indexOf(':');
            if (idx != -1) {
                mAuthHandler = mAuthMech.substring(idx+1);
            }
        }
        
        void doAuth(LdapProvisioning prov, Domain domain, Account acct, String password) throws ServiceException {
            
            if (mAuthHandler == null)
                throw AccountServiceException.AUTH_FAILED(acct.getName(), new Exception("missing handler for custom auth"));
            
            ZimbraCustomAuth handler = ZimbraCustomAuth.getHandler(mAuthHandler);
            if (handler == null) {
                throw AccountServiceException.AUTH_FAILED(acct.getName(), new Exception("handler " + mAuthHandler + " for custom auth for domain " + domain.getName() + " not found"));
            } else {
                try {
                    handler.authenticate(acct, password);
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
                        throw AccountServiceException.AUTH_FAILED(acct.getName() + msg , e);
                    }
                }
            }
        }
    }
}

