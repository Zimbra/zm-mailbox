/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class Bug57425 extends LdapUpgrade {
	Bug57425() throws ServiceException {
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
        
        String attrName = Provisioning.A_zimbraContactHiddenAttributes;
        
        System.out.println();
        System.out.println("Checking " + entryName);
        
        String curValue = entry.getAttr(attrName, false);
        
        boolean needsUpdate;
        
        if (curValue == null) {
        	if (entry instanceof Config) {
        		needsUpdate = true;
        	} else {
        		return;
        	}
        } else {
        	needsUpdate = !curValue.contains(ContactConstants.A_SMIMECertificate);
        }
        
        if (needsUpdate) {
            String newValue;
            
            if (curValue == null) {
            	newValue = ContactConstants.A_SMIMECertificate;
            } else {
            	newValue = curValue + "," + ContactConstants.A_SMIMECertificate;
            }
            
            Map<String, Object> attrs = new HashMap<String, Object>();
            StringUtil.addToMultiMap(attrs, attrName, newValue);
            modifyAttrs(entry, attrs);
        } else {
            System.out.println("    " + attrName + " already has an effective value: [" + curValue + "] on entry " + entryName + " - skipping"); 
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
