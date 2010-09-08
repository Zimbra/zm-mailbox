package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class ZimbraGalLdapFilterDef_zimbraGroup extends LdapUpgrade {
    ZimbraGalLdapFilterDef_zimbraGroup() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        Config config = mProv.getConfig();
        
        String[] value = 
        {
         "zimbraGroupAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(objectclass=zimbraDistributionList))",
         "zimbraGroupSync:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraDistributionList))",
         "zimbraGroups:(&(|(displayName=*%s*)(cn=*%s*)(sn=*%s*)(gn=*%s*)(mail=*%s*)(zimbraMailDeliveryAddress=*%s*)(zimbraMailAlias=*%s*))(objectclass=zimbraDistributionList))"
        };
         
        Map<String, Object> attr = new HashMap<String, Object>();
        attr.put("+" + Provisioning.A_zimbraGalLdapFilterDef, value);
        
        System.out.println("Adding zimbraGroupAutoComplete, zimbraGroupSync, and zimbraGroups filters to global config " + Provisioning.A_zimbraGalLdapFilterDef);
        mProv.modifyAttrs(config, attr);
    }
}
