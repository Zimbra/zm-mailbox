package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class Bug55649 extends LdapUpgrade {
    Bug55649() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapAttrMap();
    }
    
    private void upgradeZimbraGalLdapAttrMap() throws ServiceException {
        
        String valueToAdd = "binary zimbraPrefMailSMIMECertificate,userCertificate,userSMIMECertificate=SMIMECertificate";
        
        Config config = mProv.getConfig();
        
        Set<String> curValues = config.getMultiAttrSet(Provisioning.A_zimbraGalLdapAttrMap);
        if (curValues.contains(valueToAdd)) {
            return;
        }
         
        Map<String, Object> attrs = new HashMap<String, Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapAttrMap, valueToAdd);
        
        modifyAttrs(config, attrs);
    }

}
