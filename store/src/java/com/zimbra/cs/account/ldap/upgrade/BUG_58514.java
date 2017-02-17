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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.TargetType;

public class BUG_58514 extends UpgradeOp {

    
    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapAttrMap();
        upgradeZimbraContactHiddenAttributes();
    }
    
    private void upgradeZimbraGalLdapAttrMap() throws ServiceException {
        final String attrName = Provisioning.A_zimbraGalLdapAttrMap;
        
        final String valueToRemove = "binary zimbraPrefMailSMIMECertificate,userCertificate,userSMIMECertificate=SMIMECertificate";
        
        final String[] valuesToAdd = new String[] {
            "(certificate) userCertificate=userCertificate",
            "(binary) userSMIMECertificate=userSMIMECertificate"
        };
        
        Config config = prov.getConfig();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        Set<String> curValues = config.getMultiAttrSet(attrName);
        if (curValues.contains(valueToRemove)) {
            StringUtil.addToMultiMap(attrs, "-" + attrName, valueToRemove);
        }
        
        for (String valueToAdd : valuesToAdd) {
            if (!curValues.contains(valueToAdd)) {
                StringUtil.addToMultiMap(attrs, "+" + attrName, valueToAdd);
            }
        }
        
        modifyAttrs(config, attrs);
    }
    
    private void upgradeZimbraContactHiddenAttributes(Entry entry) throws ServiceException {
        final String attrName = Provisioning.A_zimbraContactHiddenAttributes;
        final String SMIMECertificate = "SMIMECertificate";
        
        String curValue = entry.getAttr(attrName, false);
        
        if (curValue == null || !curValue.contains(SMIMECertificate)) {
            return;
        }
        
        String[] hiddenAttrs = curValue.split(",");
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String hiddenAttr : hiddenAttrs) {
            if (!hiddenAttr.equals(SMIMECertificate)) {
                if (!first) {
                    sb.append(",");
                } else {
                    first = false;
                }
                sb.append(hiddenAttr);
            }
        }
        
        printer.println("Upgrading " + TargetType.getTargetType(entry).getPrettyName() + " " + entry.getLabel());
        printer.println("    Current value of " + attrName + ": " + curValue);
        printer.println("    New value of " + attrName + ": " + sb.toString());
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, sb.toString());
        modifyAttrs(entry, attrs);
        
    }
    
    private void upgradeZimbraContactHiddenAttributes() throws ServiceException {
        Config config = prov.getConfig();
        upgradeZimbraContactHiddenAttributes(config);
        
        List<Server> servers = prov.getAllServers();
        
        for (Server server : servers) {
            upgradeZimbraContactHiddenAttributes(server);
        }
    }
}
