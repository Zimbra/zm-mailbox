package com.zimbra.cs.account.ldap;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ExceptionToString;

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
        public List getContacts() { return (List) detail; }
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

        String url = getRequiredAttr(attrs, Provisioning.A_zimbraAuthLdapURL);
        String bindDn = getRequiredAttr(attrs, Provisioning.A_zimbraAuthLdapBindDn);
        String dn = LdapUtil.computeAuthDn(name, bindDn);

        try {
            LdapUtil.ldapAuthenticate(url, dn,  password);
            return new Result(STATUS_OK, "", dn);
        } catch (NamingException e) {
            return toResult(e, dn);
        }
    }

    
    public static Result checkGalConfig(Map attrs, String query, int limit) throws ServiceException {
        String mode = getRequiredAttr(attrs, Provisioning.A_zimbraGalMode);
        if (!mode.equals(Provisioning.GM_LDAP))
            throw ServiceException.INVALID_REQUEST("gal mode must be: "+Provisioning.GM_LDAP, null);

        String url = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapURL);
        String bindDn = (String) attrs.get(Provisioning.A_zimbraGalLdapBindDn);
        String bindPassword = (String) attrs.get(Provisioning.A_zimbraGalLdapBindPassword);
        String searchBase = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapSearchBase);
        String filter = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapFilter);


        String[] galAttrs = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        ArrayList list = new ArrayList(galAttrs.length);
        HashMap map = new HashMap();
        LdapUtil.initGalAttrs(galAttrs, list, map);
        String[] attr_list = (String[]) list.toArray(new String[list.size()]);
        
        try {
            List contacts = LdapUtil.searchLdapGal(url, bindDn, bindPassword, searchBase, filter, query, limit, attr_list, map);
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
        HashMap attrs = new HashMap();
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
        HashMap attrs = new HashMap();
        attrs.put(Provisioning.A_zimbraGalMode, Provisioning.GM_LDAP);
        attrs.put(Provisioning.A_zimbraGalLdapURL, "ldap://exch1.example.zimbra.com/");
        attrs.put(Provisioning.A_zimbraGalLdapBindDn, "zz_gal");
        attrs.put(Provisioning.A_zimbraGalLdapBindPassword, "zz_gal");
        attrs.put(Provisioning.A_zimbraGalLdapSearchBase, "dc=liquidsys,dc=com");        
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
