package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class Bug57039 extends LdapUpgrade {
    Bug57039() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapAttrMap();
    }
    
    void upgradeZimbraGalLdapAttrMap() throws ServiceException {
        Config config = mProv.getConfig();
        
        String attrName = Provisioning.A_zimbraGalLdapAttrMap;
        String oldValue = "zimbraCalResLocationDisplayName,displayName=zimbraCalResLocationDisplayName";
        String newValue = "zimbraCalResLocationDisplayName=zimbraCalResLocationDisplayName";
        
        String[] curValues = config.getMultiAttr(attrName);
         
        for (String value : curValues) {
            if (value.equalsIgnoreCase(oldValue)) {
                Map<String, Object> attrs = new HashMap<String, Object>();
                StringUtil.addToMultiMap(attrs, "-" + attrName, oldValue);
                StringUtil.addToMultiMap(attrs, "+" + attrName, newValue);
                
                modifyAttrs(config, attrs);
            }
        }
    }


}
