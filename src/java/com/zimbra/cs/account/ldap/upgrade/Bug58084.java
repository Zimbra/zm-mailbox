/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class Bug58084 extends LdapUpgrade {

    Bug58084() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            doGlobalConfig(zlc);
            doAllServers(zlc);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

    }
    
    private void doEntry(ZimbraLdapContext zlc, Entry entry, String entryName) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraMailEmptyFolderBatchSize;
        String oldValue = "100000";
        String newValue = "1000";
        
        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Checking " + attrName + " on " + entryName);
        
        String curValue = entry.getAttr(attrName, false);
        
        // change it if current value is not set or is the same as the old value
        if (oldValue.equals(curValue)) {
            System.out.println("    Changing " + attrName + " on " + entryName + " from " + curValue + " to " + newValue);
            
            Map<String, Object> attr = new HashMap<String, Object>();
            attr.put(attrName, newValue);
            try {
                LdapUpgrade.modifyAttrs(entry, zlc, attr);
            } catch (NamingException e) {
                // log the exception and continue
                System.out.println("Caught NamingException while modifying " + entryName + " attribute " + attr);
                e.printStackTrace();
            }
        } else {
            System.out.println("    Current value of " + attrName + " on " + entryName + " is " + curValue + " - not changed");
        }
    }
    
    private void doGlobalConfig(ZimbraLdapContext zlc) throws ServiceException {
        Config config = mProv.getConfig();
        doEntry(zlc, config, "global config");
    }
    
    private void doAllServers(ZimbraLdapContext zlc) throws ServiceException {
        List<Server> servers = mProv.getAllServers();
        
        for (Server server : servers)
            doEntry(zlc, server, "server " + server.getName());
    }
}
