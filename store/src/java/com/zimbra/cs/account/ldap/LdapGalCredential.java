/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
    
    public String getAuthMech() {
        return mAuthMech;
    }
    
    public String getBindDn() {
        return mBindDn;
    }
    
    public String getBindPassword() {
        return mBindPassword;
    }
    
    public String getKrb5Principal() {
        return mKrb5Principal;
    }
    
    public String getKrb5Keytab() {
        return mKrb5Keytab;
    }
}

