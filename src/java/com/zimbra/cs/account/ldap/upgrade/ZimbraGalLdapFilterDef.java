package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class ZimbraGalLdapFilterDef extends LdapUpgrade {
    
    ZimbraGalLdapFilterDef() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        Config config = mProv.getConfig();
        
        String value = 
            "zimbraSync:(&(|(displayName=*)(cn=*)(sn=*)(gn=*)(mail=*)(zimbraMailDeliveryAddress=*)(zimbraMailAlias=*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList))(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))";
        
        Map<String, Object> attr = new HashMap<String, Object>();
        attr.put("+" + Provisioning.A_zimbraGalLdapFilterDef, value);
        
        System.out.println("Adding zimbraSync to global config " + Provisioning.A_zimbraGalLdapFilterDef);
        mProv.modifyAttrs(config, attr);
    }
}
