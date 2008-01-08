package com.zimbra.cs.account.ldap.gal;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapGalCredential;

public class ExternalGalParams {
    String mUrl[];
    String mSearchBase;
    String mFilter;
    LdapGalCredential mCredential;
    
    public ExternalGalParams(Domain domain, GalOp galOp) throws ServiceException {
        
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
    
    public String[] getUrl() { return mUrl; }
    public String getSearchBase() { return mSearchBase; }
    public String getFilter() { return mFilter; }
    public LdapGalCredential getCredential() { return mCredential; }

}
