package com.zimbra.cs.account.ldap;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AutoProvPrincipalBy;
import com.zimbra.cs.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.cs.ldap.ZAttributes;

public class AutoProvisionManual extends AutoProvision {

    private AutoProvPrincipalBy by;
    private String principal;
    
    protected AutoProvisionManual(LdapProv prov, Domain domain, 
            AutoProvPrincipalBy by, String principal) {
        super(prov, domain);
        this.by = by;
        this.principal = principal;
    }

    @Override
    Account handle() throws ServiceException {
        if (!autoProvisionEnabled()) {
            return null;
        }
        
        String acctZimbraName;
        ZAttributes externalAttrs;
        if (by == AutoProvPrincipalBy.dn) {
            externalAttrs = getExternalAttrsByDn(principal);
            acctZimbraName = mapName(externalAttrs, null);
        } else if (by == AutoProvPrincipalBy.name) {
            externalAttrs = getExternalAttrsByName(null, principal, null);
            acctZimbraName = mapName(externalAttrs, principal);
        } else {
            throw ServiceException.FAILURE("unknown AutoProvPrincipalBy", null);
        }

        return createAccount(acctZimbraName, externalAttrs);
    }
    
    private boolean autoProvisionEnabled() {
        Set<String> modesEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode);
        return modesEnabled.contains(AutoProvMode.MANUAL.name());
    }

}
