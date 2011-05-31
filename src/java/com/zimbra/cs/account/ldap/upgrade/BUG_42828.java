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
import java.util.List;
import java.util.Map;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class BUG_42828 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        upgrade_zimbraGalLdapAttrMap();
        upgrade_zimbraContactHiddenAttributes();
    }
    
    void upgrade_zimbraGalLdapAttrMap() throws ServiceException {
        
        String attrName = Provisioning.A_zimbraGalLdapAttrMap;
        
        Config config = prov.getConfig();
        
        printer.println();
        printer.println("Checking " + config.getLabel() + " for " + attrName);
        
        String oldCalResType = "zimbraCalResType=zimbraCalResType";
        String newCalResType = "zimbraCalResType,msExchResourceSearchProperties=zimbraCalResType";
        
        String oldCalResLocationDisplayName = "zimbraCalResLocationDisplayName=zimbraCalResLocationDisplayName";
        String newCalResLocationDisplayName = "zimbraCalResLocationDisplayName,displayName=zimbraCalResLocationDisplayName";
        
        String oldMailForwardingAddress = "zimbraMailForwardingAddress=zimbraMailForwardingAddress";
        String newMailForwardingAddress = "zimbraMailForwardingAddress=member";
        
        String zimbraCalResBuilding = "zimbraCalResBuilding=zimbraCalResBuilding";
        String zimbraCalResCapacity = "zimbraCalResCapacity,msExchResourceCapacity=zimbraCalResCapacity";
        String zimbraCalResFloor = "zimbraCalResFloor=zimbraCalResFloor";
        String zimbraCalResSite = "zimbraCalResSite=zimbraCalResSite";
        String zimbraCalResContactEmail = "zimbraCalResContactEmail=zimbraCalResContactEmail";
        String zimbraAccountCalendarUserType = "msExchResourceSearchProperties=zimbraAccountCalendarUserType";
        
        String[] curValues = config.getMultiAttr(attrName);
        
        Map<String, Object> attrs = new HashMap<String, Object>(); 
        for (String curValue : curValues) {
            replaceIfNeeded(attrs, attrName, curValue, oldCalResType, newCalResType);
            replaceIfNeeded(attrs, attrName, curValue, oldCalResLocationDisplayName, newCalResLocationDisplayName);
            replaceIfNeeded(attrs, attrName, curValue, oldMailForwardingAddress, newMailForwardingAddress);
        }

        addValue(attrs, attrName, zimbraCalResBuilding);
        addValue(attrs, attrName, zimbraCalResCapacity);
        addValue(attrs, attrName, zimbraCalResFloor);
        addValue(attrs, attrName, zimbraCalResSite);
        addValue(attrs, attrName, zimbraCalResContactEmail);
        addValue(attrs, attrName, zimbraAccountCalendarUserType);
        
        if (attrs.size() > 0) {
            printer.println("Modifying " + attrName + " on " + config.getLabel());
            prov.modifyAttrs(config, attrs);
        }
    }

    private void upgrade_zimbraContactHiddenAttributes(Entry entry, String entryName) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraContactHiddenAttributes;
        
        printer.println();
        printer.println("Checking " + entryName + " for " + attrName);
        
        String curValue = entry.getAttr(attrName);
        
        // remove zimbraCalResType,zimbraCalResLocationDisplayName,zimbraCalResCapacity,zimbraCalResContactEmail 
        // add member
        String newHiddenAttrs = "";
        if (curValue != null) {
            String[] curHiddenAttrs = curValue.split(",");
            boolean seenMember = false;
            boolean first = true;
            for (String hiddenAttr : curHiddenAttrs) {
                if (!Provisioning.A_zimbraCalResType.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraCalResLocationDisplayName.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraCalResCapacity.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraCalResContactEmail.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraAccountCalendarUserType.equalsIgnoreCase(hiddenAttr)) {
                    if (!first)
                        newHiddenAttrs += ",";
                    else
                        first = false;
                    newHiddenAttrs += hiddenAttr;
                }
                
                if (ContactConstants.A_member.equalsIgnoreCase(hiddenAttr))
                    seenMember = true;
                        
            }
            
            // add member if not seen
            if (!seenMember) {
                if (!first)
                    newHiddenAttrs += ",";
                newHiddenAttrs += ContactConstants.A_member;
            }
        }
        if (newHiddenAttrs.length() == 0)
            newHiddenAttrs = "dn,zimbraAccountCalendarUserType,vcardUID,vcardURL,vcardXProps" + ContactConstants.A_member;
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraContactHiddenAttributes, newHiddenAttrs);
        
        if (attrs.size() > 0) {
            printer.println("Modifying " + attrName + " on " + entryName + " from " + curValue + " to " + newHiddenAttrs);
            prov.modifyAttrs(entry, attrs);
        }
    }
    
    private void upgrade_zimbraContactHiddenAttributes() throws ServiceException {
        Config config = prov.getConfig();
        upgrade_zimbraContactHiddenAttributes(config, config.getLabel());
        
        List<Server> servers = prov.getAllServers();
        
        for (Server server : servers) {
            upgrade_zimbraContactHiddenAttributes(server, "server " + server.getLabel());
        }
    }
    
    private void replaceIfNeeded(Map<String, Object> attrs, String attrName, String curValue, String oldValue, String newValue) {
        if (curValue.equalsIgnoreCase(oldValue)) {
            printer.println("    removing value: " + oldValue);
            printer.println("    adding value: " + newValue);
            
            StringUtil.addToMultiMap(attrs, "-" + attrName, oldValue);
            StringUtil.addToMultiMap(attrs, "+" + attrName, newValue);
        }
    }
    
    private void addValue(Map<String, Object> attrs, String attrName, String value) {
        printer.println("    adding value: " + value);
        StringUtil.addToMultiMap(attrs, "+" + attrName, value);
    }

}
