package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;

public class LdapGalCredential {
    String mAuthMech;
    
    String mBindDn;
    String mBindPassword;
    
    String mKrb5Principle;
    String mKrb5Keytab;
    
    public static LdapGalCredential init(String authMech, 
                                         String bindDn, String bindPassword,
                                         String krb5Principle, String krb5Keytab) throws ServiceException {
        
        if (StringUtil.isNullOrEmpty(authMech)) {
            if (bindDn != null && bindPassword != null)
                authMech = Provisioning.LDAP_AM_SIMPLE;
            else
                authMech = Provisioning.LDAP_AM_NONE;
        }
        
        if (authMech.equals(Provisioning.LDAP_AM_NONE)) {
            
        } else if (authMech.equals(Provisioning.LDAP_AM_SIMPLE)) {
            if (bindDn == null || bindPassword == null)
                throw ServiceException.INVALID_REQUEST("missing bindDn or bindPassword for LDAP GAL auth mechenism " + authMech, null);
        } else if (authMech.equals(Provisioning.LDAP_AM_KERBEROS5)) {
            if (krb5Principle == null || krb5Keytab == null)
                throw ServiceException.INVALID_REQUEST("missing krb5Principle or krb5Keytab for LDAP GAL auth mechenism " + authMech, null);
        } else
            throw ServiceException.INVALID_REQUEST("invalid LDAP GAL auth mechenism " + authMech, null);
            
        return new LdapGalCredential(authMech, bindDn, bindPassword, krb5Principle, krb5Keytab);
    }
    
    private LdapGalCredential(String authMech, 
                              String bindDn, String bindPassword,
                              String krb5Principle, String krb5Keytab) {

        mAuthMech = authMech;
        mBindDn = bindDn;
        mBindPassword = bindPassword;
        mKrb5Principle = krb5Principle;
        mKrb5Keytab = krb5Keytab;
    }
    
    String getAuthMech() {
        return mAuthMech;
    }
    
    String getBindDn() {
        return mBindDn;
    }
    
    String getBindPassword() {
        return mBindPassword;
    }
    
    String getKrb5Principle() {
        return mKrb5Principle;
    }
    
    String getKrb5Keytab() {
        return mKrb5Keytab;
    }
}

