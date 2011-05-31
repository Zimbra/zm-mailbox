/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class BUG_57855 extends UpgradeOp {

    
    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapFilterDef();
    }
    
    void upgradeZimbraGalLdapFilterDef() throws ServiceException {
        Config config = prov.getConfig();
        
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
