/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.account.ldap;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.net.ssl.SSLHandshakeException;

import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ExceptionToString;
import com.zimbra.common.util.ZimbraLog;

public class Check {

    public static final String STATUS_OK = "check.OK";

    /** unknown hostname */
    public static final String STATUS_UNKNOWN_HOST = "check.UNKNOWN_HOST";
    
    /** connection was refused */
    public static final String STATUS_CONNECTION_REFUSED = "check.CONNECTION_REFUSED";
    
    /** SSL problem (most likely untrusted certificate) */
    public static final String STATUS_SSL_HANDSHAKE_FAILURE = "check.SSL_HANDSHAKE_FAILURE";
    
    /** generic communication failure */
    public static final String STATUS_COMMUNICATION_FAILURE = "check.COMMUNICATION_FAILURE";    
    
    /** authentication failed. invalid credentials (bad dn/password) */
    public static final String STATUS_AUTH_FAILED= "check.AUTH_FAILED";

    /** authentication flavor not supported. */
    public static final String STATUS_AUTH_NOT_SUPPORTED = "check.AUTH_NOT_SUPPORTED";

    /** jndi name not found. most likley an invalid search base */
    public static final String STATUS_NAME_NOT_FOUND = "check.NAME_NOT_FOUND";

    /** jndi invalid search filter. */
    public static final String STATUS_INVALID_SEARCH_FILTER = "check.INVALID_SEARCH_FILTER";

    /** some other error occurred  */
    public static final String STATUS_FAILURE = "check.FAILURE";

    public static class Result {
        String code;
        String message;
        Object detail;

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getComputedDn() {return (String) detail; }
        public List<GalContact> getContacts() { return (List<GalContact>) detail; }
        public Object getDetail() { return  detail; }        

        public Result(String status, String message, Object detail) {
            this.code = status;
            this.message = message;
            this.detail = detail;
        }

        public Result(String status, Exception e, Object detail) {
            this.code = status;
            this.message = ExceptionToString.ToString(e);
            this.detail = detail;
        }

        public String toString() {
            return "Result { code: "+code+" detail: "+detail+" message: "+message+" }";
        }
    }

    private static String getRequiredAttr(Map attrs, String name) throws ServiceException {
        String value = (String) attrs.get(name);
        if (value == null)
            throw ServiceException.INVALID_REQUEST("must specifiy: "+name, null);
        return value;
    }

    private static String[] getRequiredMultiAttr(Map attrs, String name) throws ServiceException {
        Object v = attrs.get(name);
        if (v instanceof String) return new String[] {(String)v};
        else if (v instanceof String[]) {
            String value[] = (String[]) v;
            if (value != null && value.length > 0)
                return value;
        }
        throw ServiceException.INVALID_REQUEST("must specifiy: "+name, null);
    }

    public static Result checkHostnameResolve(String hostname) {
        try {
            InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return new Result(STATUS_UNKNOWN_HOST, e, (String)null);
        }
        return new Result(STATUS_OK, "", (String) null);
    }

    public static Result checkAuthConfig(Map attrs, String name, String password) throws ServiceException {
        String mech = getRequiredAttr(attrs, Provisioning.A_zimbraAuthMech);
        if (!(mech.equals(Provisioning.AM_LDAP) || mech.equals(Provisioning.AM_AD)))
            throw ServiceException.INVALID_REQUEST("auth mech must be: "+Provisioning.AM_LDAP+" or "+Provisioning.AM_AD, null);

        String url[] = getRequiredMultiAttr(attrs, Provisioning.A_zimbraAuthLdapURL);
        
        try {
            String searchFilter = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchFilter);
            if (searchFilter != null) {
                String searchPassword = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchBindPassword);
                String searchDn = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchBindDn);
                String searchBase = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchBase);
                if (searchBase == null) searchBase = "";
                searchFilter = LdapUtil.computeAuthDn(name, searchFilter);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with search filter of "+searchFilter);
                LdapUtil.ldapAuthenticate(url, password, searchBase, searchFilter, searchDn, searchPassword);
                return new Result(STATUS_OK, "", searchFilter);                
            }
        
            String bindDn = (String) attrs.get(Provisioning.A_zimbraAuthLdapBindDn);
            if (bindDn != null) {
                String dn = LdapUtil.computeAuthDn(name, bindDn);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with bind dn template of "+dn);
                LdapUtil.ldapAuthenticate(url, dn, password);
                return new Result(STATUS_OK, "", dn);
            }
            
            throw ServiceException.INVALID_REQUEST("must specify "+Provisioning.A_zimbraAuthLdapSearchFilter+" or "+Provisioning.A_zimbraAuthLdapBindDn, null);
        } catch (NamingException e) {
            return toResult(e, "");
        }
    }

    
    public static Result checkGalConfig(Map attrs, String query, int limit) throws ServiceException {
        String mode = getRequiredAttr(attrs, Provisioning.A_zimbraGalMode);
        if (!mode.equals(Provisioning.GM_LDAP))
            throw ServiceException.INVALID_REQUEST("gal mode must be: "+Provisioning.GM_LDAP, null);

        // TODO: change to ExteralGalParams, and need admin console work
        String url[] = getRequiredMultiAttr(attrs, Provisioning.A_zimbraGalLdapURL);
        String authMech = (String)attrs.get(Provisioning.A_zimbraGalLdapAuthMech);
        String bindDn = (String) attrs.get(Provisioning.A_zimbraGalLdapBindDn);
        String bindPassword = (String) attrs.get(Provisioning.A_zimbraGalLdapBindPassword);
        String krb5Principal = (String) attrs.get(Provisioning.A_zimbraGalLdapKerberos5Principal);
        String krb5Keytab = (String) attrs.get(Provisioning.A_zimbraGalLdapKerberos5Keytab);
        String searchBase = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapSearchBase);
        String filter = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapFilter);
        GalParams.ExternalGalParams galParams = new GalParams.ExternalGalParams(url,
                                                                                authMech,
                                                                                bindDn,
                                                                                bindPassword,
                                                                                krb5Principal,
                                                                                krb5Keytab,
                                                                                searchBase,
                                                                                filter,
                                                                                null);

        String[] galAttrs = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        LdapGalMapRules rules = new LdapGalMapRules(galAttrs);

        try {
            LdapGalCredential credential = new LdapGalCredential(authMech, bindDn, bindPassword, krb5Principal, krb5Keytab);
            SearchGalResult result = LdapUtil.searchLdapGal(galParams, GalOp.search, query, limit, rules, null); 
            List contacts = result.matches;
            return new Result(STATUS_OK, "", contacts);
        } catch (NamingException e) {
            return toResult(e, null);
        }
    }

    private static Result toResult(NamingException e, String dn) {
        if (e instanceof CommunicationException) {
            if (e.getRootCause() instanceof UnknownHostException) {
                return new Result(STATUS_UNKNOWN_HOST, e, dn);
            } else if (e.getRootCause() instanceof ConnectException) {
                return new Result(STATUS_CONNECTION_REFUSED, e, dn);
            } else if (e.getRootCause() instanceof SSLHandshakeException) {
                return new Result(STATUS_SSL_HANDSHAKE_FAILURE, e, dn);
            } else {
                return new Result(STATUS_COMMUNICATION_FAILURE, e, dn);
            }
        } else if (e instanceof AuthenticationException) {
            return new Result(STATUS_AUTH_FAILED, e, dn);
        } else if (e instanceof AuthenticationNotSupportedException) {
            return new Result(STATUS_AUTH_NOT_SUPPORTED, e, dn);
        } else if (e instanceof NameNotFoundException) {
            return new Result(STATUS_NAME_NOT_FOUND, e, dn);
        } else if (e instanceof InvalidSearchFilterException) {
            return new Result(STATUS_INVALID_SEARCH_FILTER, e, dn);            
        }  else {
            return new Result(STATUS_FAILURE, e, dn);
        }
    }
    
    private static void testCheckAuth() {
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraAuthMech, Provisioning.AM_LDAP);
        attrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://exch1.example.zimbra.com/");
        attrs.put(Provisioning.A_zimbraAuthLdapBindDn, "%u@example.zimbra.com");
        try {
            Result r = checkAuthConfig(attrs, "schemers", "xxxxx");
            System.out.println(r);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
    
    private static void testCheckHostnameResolve() {
        Result r = checkHostnameResolve("slapshot");
        System.out.println(r);
    }

   private static void testCheckGal() {
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraGalMode, Provisioning.GM_LDAP);
        attrs.put(Provisioning.A_zimbraGalLdapURL, "ldap://exch1.example.zimbra.com/");
        attrs.put(Provisioning.A_zimbraGalLdapBindDn, "zz_gal");
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, "zz_gal");
        attrs.put(Provisioning.A_zimbraGalLdapSearchBase, "dc=example,dc=zimbra,dc=com");        
        attrs.put(Provisioning.A_zimbraGalLdapFilter, "ad");
        try {
            Result r = checkGalConfig(attrs, "sam", 10);
            System.out.println(r);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
   
   public static void main(String args[]) {
       testCheckHostnameResolve();
       //testCheckGal();       
   }
}
