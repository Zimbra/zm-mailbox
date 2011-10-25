/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.gal;

import java.util.Map;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapGalCredential;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.gal.ZimbraGalSearchBase;
import com.zimbra.cs.ldap.LdapConnType;

public abstract class GalParams {
    
    int mPageSize;
    String mTokenizeAutoCompleteKey;
    String mTokenizeSearchKey;
    
    GalParams(Entry ldapEntry, GalOp galOp) throws ServiceException {
        
        String pageSize = null;
        if (galOp == GalOp.sync) {
            pageSize = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapPageSize);
            
            if (pageSize == null)
                pageSize = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapPageSize);
        } else {
            pageSize = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapPageSize);
        }
        
        setPageSize(pageSize);
        
        mTokenizeAutoCompleteKey = ldapEntry.getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey);
        mTokenizeSearchKey = ldapEntry.getAttr(Provisioning.A_zimbraGalTokenizeSearchKey);
        
    }
    
    GalParams(Map attrs, GalOp galOp) {
        String pageSize = null;
        if (galOp == GalOp.sync) {
            pageSize = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapPageSize);
            
            if (pageSize == null)
                pageSize = (String)attrs.get(Provisioning.A_zimbraGalLdapPageSize);
        } else {
            pageSize = (String)attrs.get(Provisioning.A_zimbraGalLdapPageSize);
        }
        
        setPageSize(pageSize);
        
        mTokenizeAutoCompleteKey = (String)attrs.get(Provisioning.A_zimbraGalTokenizeAutoCompleteKey);
        mTokenizeSearchKey = (String)attrs.get(Provisioning.A_zimbraGalTokenizeSearchKey);
    }
    
    private void setPageSize(String pageSize) {
        if (pageSize == null)
            pageSize = "1000";
        
        try {
            mPageSize = Integer.parseInt(pageSize);
        } catch (NumberFormatException e) {
            mPageSize = 0;
        }
        
    }
    
    public int pageSize() { return mPageSize; }
    public String tokenizeAutoCompleteKey() { return mTokenizeAutoCompleteKey; }
    public String tokenizeSearchKey() { return mTokenizeSearchKey; } 
    
    /*
     * ZimbraGalParams
     *
     */
    public static class ZimbraGalParams extends GalParams {
        String mSearchBase;
        
        public ZimbraGalParams(Domain domain, GalOp galOp) throws ServiceException {
            super(domain, galOp); 
            mSearchBase = ZimbraGalSearchBase.getSearchBase(domain, galOp);
        }
        
        public String searchBase() { return mSearchBase; }
    }
    
    public static class ExternalGalParams extends GalParams {
        String mUrl[];
        boolean mRequireStartTLS;
        String mSearchBase;
        String mFilter;
        LdapGalCredential mCredential;
        
        public ExternalGalParams(Entry ldapEntry, GalOp galOp) throws ServiceException {
            super(ldapEntry, galOp);
            
            String startTlsEnabled;
            String authMech;
            String bindDn;
            String bindPassword;
            String krb5Principal;
            String krb5Keytab;
            
            if (galOp == GalOp.sync) {
                mUrl = ldapEntry.getMultiAttr(Provisioning.A_zimbraGalSyncLdapURL);
                mSearchBase = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase);
                mFilter = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapFilter);
                
                startTlsEnabled = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled);
                authMech = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech);
                bindDn = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapBindDn);
                bindPassword = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword);
                krb5Principal = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
                krb5Keytab = ldapEntry.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
                
                // fallback to zimbraGalLdap attrs is sync specific params are not set
                if (mUrl == null || mUrl.length == 0)
                    mUrl = ldapEntry.getMultiAttr(Provisioning.A_zimbraGalLdapURL);
                if (mSearchBase == null)
                    mSearchBase = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapSearchBase, "");
                if (mFilter == null)
                    mFilter = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapFilter);
                
                if (startTlsEnabled == null)
                    startTlsEnabled = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapStartTlsEnabled);
                if (authMech == null)
                    authMech = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapAuthMech);
                if (bindDn == null)
                    bindDn = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapBindDn);
                if (bindPassword == null)
                    bindPassword = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapBindPassword);
                if (krb5Principal == null)
                    krb5Principal = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
                if (krb5Keytab == null)
                    krb5Keytab = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
                
            } else {
                mUrl = ldapEntry.getMultiAttr(Provisioning.A_zimbraGalLdapURL);
                mSearchBase = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapSearchBase, "");
                
                if (galOp == GalOp.autocomplete)
                    mFilter = ldapEntry.getAttr(Provisioning.A_zimbraGalAutoCompleteLdapFilter);
                else
                    mFilter = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapFilter);
            
                startTlsEnabled = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapStartTlsEnabled);
                authMech = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapAuthMech);
                bindDn = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapBindDn);
                bindPassword = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapBindPassword);
                krb5Principal = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
                krb5Keytab = ldapEntry.getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
            }
            
            boolean startTLS = startTlsEnabled == null ? false : ProvisioningConstants.TRUE.equals(startTlsEnabled);
            mRequireStartTLS = LdapConnType.requireStartTLS(mUrl,  startTLS);
            mCredential = new LdapGalCredential(authMech, bindDn, bindPassword, krb5Principal, krb5Keytab);
        }
        
        
        /*
         * called from Check, where there isn't a domain object
         * 
         * TODO, need admin UI work for zimbraGalLdapStartTlsEnabled/zimbraGalSyncLdapStartTlsEnabled
         */
        public ExternalGalParams(Map attrs, GalOp galOp) throws ServiceException {
            super(attrs, galOp);
            
            String startTlsEnabled;
            String authMech;
            String bindDn;
            String bindPassword;
            String krb5Principal;
            String krb5Keytab;
            
            if (galOp == GalOp.sync) {
                mUrl = getMultiAttr(attrs, Provisioning.A_zimbraGalSyncLdapURL, false);
                mSearchBase = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapSearchBase);
                mFilter = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapFilter);
                
                startTlsEnabled = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled);
                authMech = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapAuthMech);
                bindDn = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapBindDn);
                bindPassword = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapBindPassword);
                krb5Principal = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
                krb5Keytab = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
                
                // fallback to zimbraGalLdap attrs is sync specific params are not set
                if (mUrl == null || mUrl.length == 0)
                    mUrl = getMultiAttr(attrs, Provisioning.A_zimbraGalLdapURL, true);
                if (mSearchBase == null)
                    mSearchBase = (String)attrs.get(Provisioning.A_zimbraGalLdapSearchBase);
                if (mFilter == null)
                    mFilter = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapFilter);
                
                if (startTlsEnabled == null)
                    startTlsEnabled = (String)attrs.get(Provisioning.A_zimbraGalLdapStartTlsEnabled);
                if (authMech == null)
                    authMech = (String)attrs.get(Provisioning.A_zimbraGalLdapAuthMech);
                if (bindDn == null)
                    bindDn = (String)attrs.get(Provisioning.A_zimbraGalLdapBindDn);
                if (bindPassword == null)
                    bindPassword = (String)attrs.get(Provisioning.A_zimbraGalLdapBindPassword);
                if (krb5Principal == null)
                    krb5Principal = (String)attrs.get(Provisioning.A_zimbraGalLdapKerberos5Principal);
                if (krb5Keytab == null)
                    krb5Keytab = (String)attrs.get(Provisioning.A_zimbraGalLdapKerberos5Keytab);
                
            } else {
                mUrl = getMultiAttr(attrs, Provisioning.A_zimbraGalLdapURL, true);
                mSearchBase = (String)attrs.get(Provisioning.A_zimbraGalLdapSearchBase);
                
                if (galOp == GalOp.autocomplete)
                    mFilter = getRequiredAttr(attrs, Provisioning.A_zimbraGalAutoCompleteLdapFilter);
                else
                    mFilter = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapFilter);
            
                startTlsEnabled = (String)attrs.get(Provisioning.A_zimbraGalLdapStartTlsEnabled);
                authMech = (String)attrs.get(Provisioning.A_zimbraGalLdapAuthMech);
                bindDn = (String)attrs.get(Provisioning.A_zimbraGalLdapBindDn);
                bindPassword = (String)attrs.get(Provisioning.A_zimbraGalLdapBindPassword);
                krb5Principal = (String)attrs.get(Provisioning.A_zimbraGalLdapKerberos5Principal);
                krb5Keytab = (String)attrs.get(Provisioning.A_zimbraGalLdapKerberos5Keytab);
            }
                
            boolean startTLS = startTlsEnabled == null ? false : ProvisioningConstants.TRUE.equals(startTlsEnabled);
            mRequireStartTLS = LdapConnType.requireStartTLS(mUrl,  startTLS);
            mCredential = new LdapGalCredential(authMech, bindDn, bindPassword, krb5Principal, krb5Keytab);
        }
        
        private static String[] getMultiAttr(Map attrs, String name, boolean required) throws ServiceException {
            Object v = attrs.get(name);
            if (v instanceof String) return new String[] {(String)v};
            else if (v instanceof String[]) {
                String value[] = (String[]) v;
                if (value != null && value.length > 0)
                    return value;
            }
            if (required)
                throw ServiceException.INVALID_REQUEST("must specifiy: "+name, null);
            else
                return null;
        }
        
        private static String getRequiredAttr(Map attrs, String name) throws ServiceException {
            String value = (String) attrs.get(name);
            if (value == null)
                throw ServiceException.INVALID_REQUEST("must specifiy: "+name, null);
            return value;
        }
        
        public String[] url() { return mUrl; }
        public boolean requireStartTLS() { return mRequireStartTLS; }
        public String searchBase() { return GalSearchConfig.fixupExternalGalSearchBase(mSearchBase); }
        public String filter() { return mFilter; }
        public LdapGalCredential credential() { return mCredential; }

    }
}
