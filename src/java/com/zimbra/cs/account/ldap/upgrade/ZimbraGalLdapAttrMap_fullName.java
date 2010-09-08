package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class ZimbraGalLdapAttrMap_fullName extends LdapUpgrade {
    
    ZimbraGalLdapAttrMap_fullName() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        Config config = mProv.getConfig();
        
        String oldValue = "displayName,cn=fullName";
        String newValue = "displayName,cn=fullName,fullName2,fullName3,fullName4,fullName5,fullName6,fullName7,fullName8,fullName9,fullName10";
        
        String[] curValues = config.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
         
        for (String value : curValues) {
            if (value.equalsIgnoreCase(oldValue)) {
                Map<String, Object> attr = new HashMap<String, Object>();
                attr.put("-" + Provisioning.A_zimbraGalLdapAttrMap, oldValue);
                attr.put("+" + Provisioning.A_zimbraGalLdapAttrMap, newValue);
                
                System.out.println("Modifying " + Provisioning.A_zimbraGalLdapAttrMap + " on global config:");
                System.out.println("    removing value: " + oldValue);
                System.out.println("    adding value: " + newValue);
                mProv.modifyAttrs(config, attr);
                
            }
        }
    }
}
