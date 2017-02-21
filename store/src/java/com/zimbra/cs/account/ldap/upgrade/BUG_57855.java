/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
