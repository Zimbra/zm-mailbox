package com.zimbra.cs.account.ldap.upgrade.legacy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class Bug57855 extends LegacyLdapUpgrade {
    Bug57855() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapFilterDef();
    }
    
    void upgradeZimbraGalLdapFilterDef() throws ServiceException {
        Config config = mProv.getConfig();
        
        String attrName = Provisioning.A_zimbraGalLdapFilterDef;
        String[] addValues = new String[] {
                "email_has:(mail=*%s*)",
                "email2_has:(mail=*%s*)",
                "email3_has:(mail=*%s*)",
                "department_has:(ou=*%s*)"
        };
        
        Set<String> curValues = config.getMultiAttrSet(attrName);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (String value : addValues) {
            if (!curValues.contains(value)) {
                StringUtil.addToMultiMap(attrs, "+" + attrName, value);
            }
        }
        
        modifyAttrs(config, attrs);
    }

}
