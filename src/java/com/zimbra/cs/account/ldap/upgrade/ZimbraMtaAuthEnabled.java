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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZAttrProvisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.util.BuildInfo;

public class ZimbraMtaAuthEnabled extends LdapUpgrade {

    private static final String TLSLEVEL_ENCRYPT = ZAttrProvisioning.MtaTlsSecurityLevel.may.toString(); // we don't support encryp yet, see http://bugzilla.zimbra.com/show_bug.cgi?id=33814#c12
    private static final String TLSLEVEL_MAY     = ZAttrProvisioning.MtaTlsSecurityLevel.may.toString();
    private static final String TLSLEVEL_NONE    = ZAttrProvisioning.MtaTlsSecurityLevel.none.toString();
    
    ZimbraMtaAuthEnabled() throws ServiceException {
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
    
    /*
     * On global config:
     *     - set to "may" if both are TRUE
     *     - set to "none" for all other cases, including the cases when
     *       one or both of them are not set
     */
    private void doMtaTlsSecurityLevelOnGlobalConfig(Entry entry, Map<String, Object> attrValues) {
        
        String zimbraMtaAuthEnabled      = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
        String zimbraMtaTlsAuthOnly      = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);

        String value = null;
        
        if (Provisioning.TRUE.equals(zimbraMtaAuthEnabled)) {
            if (Provisioning.TRUE.equals(zimbraMtaTlsAuthOnly))
                value = TLSLEVEL_ENCRYPT;
            else
                value = TLSLEVEL_MAY;
        } else
            value = TLSLEVEL_NONE;
        
        attrValues.put(Provisioning.A_zimbraMtaTlsSecurityLevel, value);
    }
    
    /*
                                           zimbraMtaTlsAuthOnly
                                            TRUE        FALSE
           ---------------------------------------------------------------
           zimbraMtaAuthEnabled TRUE       may(see *)    may
           zimbraMtaAuthEnabled FALSE      none          none

    */
    private void doMtaTlsSecurityLevelOnServer(Entry entry, Map<String, Object> attrValues) {
        
        // value on server entry
        String zimbraMtaAuthEnabledOnServer = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
        String zimbraMtaTlsAuthOnlyOnServer = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);
        
        // value on server entry, or if not set on server, value on global config 
        String zimbraMtaAuthEnabled = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled);
        String zimbraMtaTlsAuthOnly = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly);

        String value = null;
        
        if (Provisioning.TRUE.equals(zimbraMtaAuthEnabledOnServer)) {
            if (Provisioning.TRUE.equals(zimbraMtaTlsAuthOnly))
                value = TLSLEVEL_ENCRYPT;
            else
                value = TLSLEVEL_MAY;
                
        } else if (Provisioning.FALSE.equals(zimbraMtaAuthEnabledOnServer)) {
            value = TLSLEVEL_NONE;
        } else {
            // zimbraMtaAuthEnabled is not set on server
            
            // see what's on global config
            if (Provisioning.TRUE.equals(zimbraMtaAuthEnabled)) {
                if (Provisioning.TRUE.equals(zimbraMtaTlsAuthOnlyOnServer))
                    value = TLSLEVEL_ENCRYPT;
                else if (Provisioning.FALSE.equals(zimbraMtaTlsAuthOnlyOnServer))
                    value = TLSLEVEL_MAY;
                // else zimbraMtaTlsAuthOnly is also not set on server, do not 
                // set zimbraMtaTlsSecurityLevel and just let it inherit from global config
            } else {
                // zimbraMtaAuthEnabled on global config is FALSE or is not set
                // in this case zimbraMtaTlsSecurityLevel must be NONE on global config
                // do not set zimbraMtaTlsSecurityLevel on server regardless what
                // zimbraMtaTlsAuthOnly is(TRUE, FALSE, or not set), 
                // just let it inherit from global config, which will be NONE
            }
        }
            
        if (value != null)
            attrValues.put(Provisioning.A_zimbraMtaTlsSecurityLevel, value);
    }
    
    private void doEntry(ZimbraLdapContext zlc, Entry entry, String entryName, AttributeClass klass) throws ServiceException {
        
        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Checking " + entryName + ": ");
        
        StringBuilder msg = new StringBuilder();
        try {
            Map<String, Object> attrValues = new HashMap<String, Object>();
            
            // old attrs
            // get value on the entry, do not pull in inherited value 
            String zimbraMtaAuthEnabled      = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
            String zimbraMtaTlsAuthOnly      = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);
            
            System.out.println("zimbraMtaAuthEnabled: " + zimbraMtaAuthEnabled);
            System.out.println("zimbraMtaTlsAuthOnly: " + zimbraMtaTlsAuthOnly);
            System.out.println();
            
            // new attrs
            String zimbraMtaTlsSecurityLevel = entry.getAttr(Provisioning.A_zimbraMtaTlsSecurityLevel, false);
            String zimbraMtaSaslAuthEnable   = entry.getAttr(Provisioning.A_zimbraMtaSaslAuthEnable, false);
            
            // upgrade zimbraMtaTlsSecurityLevel
            // set it only if it does not already have a value
            if (zimbraMtaTlsSecurityLevel == null) {
                if (entry instanceof Server)
                    doMtaTlsSecurityLevelOnServer(entry, attrValues);
                else
                    doMtaTlsSecurityLevelOnGlobalConfig(entry, attrValues);
            } else 
                System.out.println("Not updating zimbraMtaTlsSecurityLevel because there is already a value: " + zimbraMtaTlsSecurityLevel);
            
            // upgrade zimbraMtaSaslAuthEnable
            // set it only if it does not already have a value
            if (zimbraMtaSaslAuthEnable == null) {
                if (zimbraMtaAuthEnabled != null)
                    attrValues.put(Provisioning.A_zimbraMtaSaslAuthEnable, zimbraMtaAuthEnabled);
            } else
                System.out.println("Not updating zimbraMtaSaslAuthEnable because there is already a value: " + zimbraMtaSaslAuthEnable);
            
            if (!attrValues.isEmpty()) {
                boolean first = true;
                for (Map.Entry<String, Object> attr : attrValues.entrySet()) {
                    if (!first)
                        msg.append(", ");
                    msg.append(attr.getKey() + "=>" + (String)attr.getValue());
                    first = false;
                }
                
                System.out.println("Updating " + entryName + ": " + msg.toString());
                LdapUpgrade.modifyAttrs(entry, zlc, attrValues);
            }
        } catch (ServiceException e) {
            // log the exception and continue
            System.out.println("Caught ServiceException while modifying " + entryName + ": " + msg.toString());
            e.printStackTrace();
        } catch (NamingException e) {
            // log the exception and continue
            System.out.println("Caught NamingException while modifying " + entryName + ": " + msg.toString());
            e.printStackTrace();
        }
    }

    private void doGlobalConfig(ZimbraLdapContext zlc) throws ServiceException {
        Config config = mProv.getConfig();
        doEntry(zlc, config, "global config", AttributeClass.globalConfig);
    }
    
    private void doAllServers(ZimbraLdapContext zlc) throws ServiceException {
        List<Server> servers = mProv.getAllServers();
        
        for (Server server : servers)
            doEntry(zlc, server, "server " + server.getName(), AttributeClass.server);
    }


}
