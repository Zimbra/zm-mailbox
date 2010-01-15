/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.util.Constants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class ZimbraHsmPolicy extends LdapUpgrade {

    ZimbraHsmPolicy() throws ServiceException {
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
        
        String oldAttr = Provisioning.A_zimbraHsmAge;
        String newAttr = Provisioning.A_zimbraHsmPolicy;
        
        System.out.println();
        System.out.println("Checking " + entryName);
        
        String oldValue = entry.getAttr(oldAttr, false);
        String newValue = entry.getAttr(newAttr, false);
        if (oldValue != null) {
            if (newValue == null) {
                newValue = String.format("message,document:before:-%dminutes", 
                        entry.getTimeInterval(oldAttr, 0) / Constants.MILLIS_PER_MINUTE);
                
                System.out.println("    Setting " + newAttr + " on " + entryName + 
                        " from " + oldAttr + " value: [" + oldValue + "]" + 
                        " to [" + newValue + "]");
                
                Map<String, Object> attr = new HashMap<String, Object>();
                attr.put(newAttr, newValue);
                mProv.modifyAttrs(entry, attr);
            } else
                System.out.println("    " + newAttr + " already has a value: [" + newValue + "], skipping"); 
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
