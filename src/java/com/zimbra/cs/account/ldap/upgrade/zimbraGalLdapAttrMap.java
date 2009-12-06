package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class zimbraGalLdapAttrMap extends LdapUpgrade {

    zimbraGalLdapAttrMap() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        Config config = mProv.getConfig();
        
        String[] values = 
        {
         "facsimileTelephoneNumber,fax=workFax",
         "homeTelephoneNumber,homePhone=homePhone",
         "mobileTelephoneNumber,mobile=mobilePhone",
         "pagerTelephoneNumber,pager=pager"
        };
         
        Map<String, Object> attr = new HashMap<String, Object>();
        attr.put("+" + Provisioning.A_zimbraGalLdapAttrMap, values);

        System.out.println("Adding workFax, homePhone, mobilePhone, pager attr maps to global config " + Provisioning.A_zimbraGalLdapAttrMap);
        mProv.modifyAttrs(config, attr);
    }

}
