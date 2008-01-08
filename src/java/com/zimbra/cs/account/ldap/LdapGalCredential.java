/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;

public class LdapGalCredential {
    String mAuthMech;
    
    String mBindDn;
    String mBindPassword;
    
    String mKrb5Principal;
    String mKrb5Keytab;
    
    public LdapGalCredential(String authMech, 
                              String bindDn, String bindPassword,
                              String krb5Principal, String krb5Keytab) throws ServiceException {

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
            if (krb5Principal == null || krb5Keytab == null)
                throw ServiceException.INVALID_REQUEST("missing krb5Principal or krb5Keytab for LDAP GAL auth mechenism " + authMech, null);
        } else
            throw ServiceException.INVALID_REQUEST("invalid LDAP GAL auth mechenism " + authMech, null);

        mAuthMech = authMech;
        mBindDn = bindDn;
        mBindPassword = bindPassword;
        mKrb5Principal = krb5Principal;
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
    
    String getKrb5Principal() {
        return mKrb5Principal;
    }
    
    String getKrb5Keytab() {
        return mKrb5Keytab;
    }
}

