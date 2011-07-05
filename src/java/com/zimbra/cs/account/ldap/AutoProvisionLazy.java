package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.cs.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.cs.ldap.ZAttributes;

class AutoProvisionLazy extends AutoProvision {
    private String loginName;
    private String loginPassword;
    private AutoProvAuthMech authedByMech;

    AutoProvisionLazy(LdapProv prov, Domain domain, String loginName, String loginPassword,
            AutoProvAuthMech authedByMech) {
        super(prov, domain);
        this.loginName = loginName;
        this.loginPassword = loginPassword;
        this.authedByMech = authedByMech;
    }

    @Override
    Account handle() throws ServiceException {
        if (domain == null) {
            domain = prov.getDefaultDomain();
            if (domain == null) {
                return null; 
            }
        }
       
        if (authedByMech == null) {
            // principal had not been authenticated, try to auth it
            authedByMech = auth();
        }
        
        if (authedByMech == null) {
            // principal cannot be authenticated by the auth mechanism 
            // configured on the domain
            return null;
        }
        
        if (!autoProvisionEnabled()) {
            return null;
        }
        
        ZAttributes externalAttrs = getExternalAttrsByName(authedByMech, loginName, loginPassword);
        String acctZimbraName = mapName(externalAttrs, loginName);
        return createAccount(acctZimbraName, externalAttrs);
    }
    
    private boolean autoProvisionEnabled() {
        Set<String> authMechsEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvAuthMech);
        Set<String> modesEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode);
        return authMechsEnabled.contains(authedByMech.name()) && modesEnabled.contains(AutoProvMode.LAZY.name());
    }
    
    private AutoProvAuthMech auth() {
        String authMech = domain.getAttr(Provisioning.A_zimbraAuthMech);
        
        // only support external LDAP auth for now
        if (Provisioning.AM_LDAP.equals(authMech)  || Provisioning.AM_AD.equals(authMech)) {
            Map<String, Object> authCtxt = new HashMap<String, Object>();
            try {
                prov.externalLdapAuth(domain, authMech, loginName, loginPassword, authCtxt);
                return AutoProvAuthMech.LDAP;
            } catch (ServiceException e) {
                ZimbraLog.account.info("unable to authenticate " + loginName + " for auto provisioning", e);
            }
        } else {
            //TODO 
        }
        
        return null;
    }

}