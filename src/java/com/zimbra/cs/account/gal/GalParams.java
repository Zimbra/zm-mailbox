package com.zimbra.cs.account.gal;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapGalCredential;

public abstract class GalParams {
    
    int mPageSize;
    String mTokenizeAutoCompleteKey;
    String mTokenizeSearchKey;
    
    GalParams(Domain domain, GalOp galOp) throws ServiceException {
        
        String pageSize = null;
        if (galOp == GalOp.sync) {
            pageSize = domain.getAttr(Provisioning.A_zimbraGalSyncLdapPageSize);
            
            if (pageSize == null)
                pageSize = domain.getAttr(Provisioning.A_zimbraGalLdapPageSize);
        } else {
            pageSize = domain.getAttr(Provisioning.A_zimbraGalLdapPageSize);
        }
        
        setPageSize(pageSize);
        
        mTokenizeAutoCompleteKey = domain.getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey);
        mTokenizeSearchKey = domain.getAttr(Provisioning.A_zimbraGalTokenizeSearchKey);
        
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
            pageSize = "0";
        
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
            
            if (galOp == GalOp.sync) {
                mSearchBase = domain.getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
                if (mSearchBase == null)
                    mSearchBase = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
            } else {
                mSearchBase = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
            }
        }
        
        public String searchBase() { return mSearchBase; }
    }
    
    public static class ExternalGalParams extends GalParams {
        String mUrl[];
        String mSearchBase;
        String mFilter;
        LdapGalCredential mCredential;
        
        public ExternalGalParams(Domain domain, GalOp galOp) throws ServiceException {
            super(domain, galOp);
            
            String authMech;
            String bindDn;
            String bindPassword;
            String krb5Principal;
            String krb5Keytab;
            
            if (galOp == GalOp.sync) {
                mUrl = domain.getMultiAttr(Provisioning.A_zimbraGalSyncLdapURL);
                mSearchBase = domain.getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase);
                mFilter = domain.getAttr(Provisioning.A_zimbraGalSyncLdapFilter);
                
                authMech = domain.getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech);
                bindDn = domain.getAttr(Provisioning.A_zimbraGalSyncLdapBindDn);
                bindPassword = domain.getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword);
                krb5Principal = domain.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
                krb5Keytab = domain.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
                
                // fallback to zimbraGalLdap attrs is sync specific params are not set
                if (mUrl == null || mUrl.length == 0)
                    mUrl = domain.getMultiAttr(Provisioning.A_zimbraGalLdapURL);
                if (mSearchBase == null)
                    mSearchBase = domain.getAttr(Provisioning.A_zimbraGalLdapSearchBase, "");
                if (mFilter == null)
                    mFilter = domain.getAttr(Provisioning.A_zimbraGalLdapFilter);
                
                if (authMech == null)
                    authMech = domain.getAttr(Provisioning.A_zimbraGalLdapAuthMech);
                if (bindDn == null)
                    bindDn = domain.getAttr(Provisioning.A_zimbraGalLdapBindDn);
                if (bindPassword == null)
                    bindPassword = domain.getAttr(Provisioning.A_zimbraGalLdapBindPassword);
                if (krb5Principal == null)
                    krb5Principal = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
                if (krb5Keytab == null)
                    krb5Keytab = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
                
            } else {
                mUrl = domain.getMultiAttr(Provisioning.A_zimbraGalLdapURL);
                mSearchBase = domain.getAttr(Provisioning.A_zimbraGalLdapSearchBase, "");
                
                if (galOp == GalOp.autocomplete)
                    mFilter = domain.getAttr(Provisioning.A_zimbraGalAutoCompleteLdapFilter);
                else
                    mFilter = domain.getAttr(Provisioning.A_zimbraGalLdapFilter);
            
                authMech = domain.getAttr(Provisioning.A_zimbraGalLdapAuthMech);
                bindDn = domain.getAttr(Provisioning.A_zimbraGalLdapBindDn);
                bindPassword = domain.getAttr(Provisioning.A_zimbraGalLdapBindPassword);
                krb5Principal = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
                krb5Keytab = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
            }
            
            mCredential = new LdapGalCredential(authMech, bindDn, bindPassword, krb5Principal, krb5Keytab);
        }
        
        
        /*
         * called from Check, where there isn't a domain object
         */
        public ExternalGalParams(Map attrs, GalOp galOp) throws ServiceException {
            super(attrs, galOp);
            
            String authMech;
            String bindDn;
            String bindPassword;
            String krb5Principal;
            String krb5Keytab;
            
            if (galOp == GalOp.sync) {
                mUrl = getMultiAttr(attrs, Provisioning.A_zimbraGalSyncLdapURL, false);
                mSearchBase = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapSearchBase);
                mFilter = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapFilter);
                
                authMech = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapAuthMech);
                bindDn = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapBindDn);
                bindPassword = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapBindPassword);
                krb5Principal = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
                krb5Keytab = (String)attrs.get(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
                
                // fallback to zimbraGalLdap attrs is sync specific params are not set
                if (mUrl == null || mUrl.length == 0)
                    mUrl = getMultiAttr(attrs, Provisioning.A_zimbraGalLdapURL, true);
                if (mSearchBase == null)
                    mSearchBase = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapSearchBase);
                if (mFilter == null)
                    mFilter = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapFilter);
                
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
                mSearchBase = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapSearchBase);
                
                if (galOp == GalOp.autocomplete)
                    mFilter = getRequiredAttr(attrs, Provisioning.A_zimbraGalAutoCompleteLdapFilter);
                else
                    mFilter = getRequiredAttr(attrs, Provisioning.A_zimbraGalLdapFilter);
            
                authMech = (String)attrs.get(Provisioning.A_zimbraGalLdapAuthMech);
                bindDn = (String)attrs.get(Provisioning.A_zimbraGalLdapBindDn);
                bindPassword = (String)attrs.get(Provisioning.A_zimbraGalLdapBindPassword);
                krb5Principal = (String)attrs.get(Provisioning.A_zimbraGalLdapKerberos5Principal);
                krb5Keytab = (String)attrs.get(Provisioning.A_zimbraGalLdapKerberos5Keytab);
            }
                
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
        public String searchBase() { return mSearchBase; }
        public String filter() { return mFilter; }
        public LdapGalCredential credential() { return mCredential; }

    }
}
