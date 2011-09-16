/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
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
