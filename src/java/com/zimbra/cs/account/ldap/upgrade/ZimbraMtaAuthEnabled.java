/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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

        ZAttrProvisioning.MtaTlsSecurityLevel value = null;
        
        if (Provisioning.TRUE.equals(zimbraMtaAuthEnabled) && 
                Provisioning.TRUE.equals(zimbraMtaTlsAuthOnly)) {
                // set to "may" if both are TRUE
                value = ZAttrProvisioning.MtaTlsSecurityLevel.may;
        } else {
            // set to "none" for all other cases
            value = ZAttrProvisioning.MtaTlsSecurityLevel.none;
        }
        
        if (value != null)
            attrValues.put(Provisioning.A_zimbraMtaTlsSecurityLevel, value.toString());
    }
    
    /*       
    * On a server entry:
    * 4 scenarios:
    *     (A) set to "may" if both are TRUE
    *     (B) set to "none" if either one is FALSE
    *     (C) do not set it if both are not set, the new value will inherit from global config
    *     (D) the only remaining scenario is one of them is TRUE; and the other is not set
    *         for this scenario, we should:
    *           get the "non-set" one from global config
    *               - if there is a value on global config, good, use that value and either A) or B) will apply
    *               - if there is not a value on global config, do not set any value on the server entry, the
    *                 new value should just inherit from global config.
    */
    private void doMtaTlsSecurityLevelOnServer(Entry entry, Map<String, Object> attrValues) {
        
        String zimbraMtaAuthEnabled      = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
        String zimbraMtaTlsAuthOnly      = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);
        
        ZAttrProvisioning.MtaTlsSecurityLevel value = null;
        
        if (Provisioning.TRUE.equals(zimbraMtaAuthEnabled) && 
            Provisioning.TRUE.equals(zimbraMtaTlsAuthOnly)) {
            // (A) set to "may" if both are TRUE
            value = ZAttrProvisioning.MtaTlsSecurityLevel.may;
        } else if (Provisioning.FALSE.equals(zimbraMtaAuthEnabled) ||
                   Provisioning.FALSE.equals(zimbraMtaTlsAuthOnly)) {
            // (B) set to "none" if either one is FALSE
            value = ZAttrProvisioning.MtaTlsSecurityLevel.none;
        } else if (zimbraMtaAuthEnabled == null &&
                   zimbraMtaTlsAuthOnly == null){
            // (C) do not set it if both are not set, the new value will inherit from global config
            value = null;
        } else {
            // (D) one of them is TRUE; and "the other" is not set
            String theOther = null;
            
            
            if (zimbraMtaAuthEnabled == null) {
                // get inherited value(if any) for the non-set one
                theOther = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled);
                
                // now, "the other" get to decide what we should set
                if (Provisioning.TRUE.equals(theOther))
                    value = ZAttrProvisioning.MtaTlsSecurityLevel.may;
                else if (Provisioning.FALSE.equals(theOther))
                    value = ZAttrProvisioning.MtaTlsSecurityLevel.none;
                else
                    value = null;
            }
            
            if (zimbraMtaTlsAuthOnly == null) {
                // get inherited value(if any) for the non-set one
                theOther = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly);
                
                // now, "the other" get to decide what we should set
                if (Provisioning.TRUE.equals(theOther))
                    value = ZAttrProvisioning.MtaTlsSecurityLevel.may;
                else if (Provisioning.FALSE.equals(theOther))
                    value = ZAttrProvisioning.MtaTlsSecurityLevel.none;
                else
                    value = null;
            }
        }
            
        if (value != null)
            attrValues.put(Provisioning.A_zimbraMtaTlsSecurityLevel, value.toString());
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

            // new attrs
            String zimbraMtaTlsSecurityLevel = entry.getAttr(Provisioning.A_zimbraMtaTlsSecurityLevel, false);
            String zimbraMtaSaslAuthEnable   = entry.getAttr(Provisioning.A_zimbraMtaSaslAuthEnable, false);
            
            // set it only if it does not already have a value
            if (zimbraMtaTlsSecurityLevel == null) {
                if (entry instanceof Server)
                    doMtaTlsSecurityLevelOnServer(entry, attrValues);
                else
                    doMtaTlsSecurityLevelOnGlobalConfig(entry, attrValues);
            }
            
            // set it only if it does not already have a value
            if (zimbraMtaSaslAuthEnable == null) {
                if (zimbraMtaAuthEnabled != null)
                    attrValues.put(Provisioning.A_zimbraMtaSaslAuthEnable, zimbraMtaAuthEnabled);
            }
            
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

    
    /////////////////////////////////////////////////////////////////
    //
    // Below are unittest code
    //
    /////////////////////////////////////////////////////////////////
    private static Map<String, Object> setupAttrs(Boolean zimbraMtaAuthEnabled, Boolean zimbraMtaTlsAuthOnly) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        if (zimbraMtaAuthEnabled != null)
            attrs.put(Provisioning.A_zimbraMtaAuthEnabled, zimbraMtaAuthEnabled.toString().toUpperCase());
        else
            attrs.put(Provisioning.A_zimbraMtaAuthEnabled, null);
        
        if (zimbraMtaTlsAuthOnly != null)
            attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, zimbraMtaTlsAuthOnly.toString().toUpperCase());
        else
            attrs.put(Provisioning.A_zimbraMtaTlsAuthOnly, null);
        
        return attrs;
    }
    
    private static void setupGlobalConfig(Boolean zimbraMtaAuthEnabled, Boolean zimbraMtaTlsAuthOnly) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        prov.modifyAttrs(config, setupAttrs(zimbraMtaAuthEnabled, zimbraMtaTlsAuthOnly));
    }
    
    private static Server setupTestServer(String serverName, Boolean zimbraMtaAuthEnabled, Boolean zimbraMtaTlsAuthOnly) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        Server server = prov.get(Provisioning.ServerBy.name, serverName);
        Map<String, Object> attrs = setupAttrs(zimbraMtaAuthEnabled, zimbraMtaTlsAuthOnly);
        
        if (server == null)
            server = prov.createServer(serverName, attrs);
        else
            prov.modifyAttrs(server, attrs);
        
        return server;
    }
    
    private static void cleanServer(Server server) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMtaTlsSecurityLevel, null);
        
        Provisioning prov = Provisioning.getInstance();
        prov.modifyAttrs(server, attrs);
    }
    
    private static Server refreshServer(String serverName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        prov.flushCache(Provisioning.CacheEntryType.server, null);
        return prov.get(Provisioning.ServerBy.name, serverName);
    }
    
    private static void doTest(ZimbraMtaAuthEnabled upgrade, String serverName, 
                               ZAttrProvisioning.MtaTlsSecurityLevel expected) throws ServiceException {
        
        System.out.println();
        System.out.println("=========================");
        System.out.println("Testing " + serverName);
        System.out.println("=========================");
        
        Server server = refreshServer(serverName);
        
        // clean value from previous test
        cleanServer(server);
        server = refreshServer(serverName);
        
        // do the upgrade
        upgrade.doUpgrade();
        server = refreshServer(serverName);
        // verify
        
        String mtaTlsSecurityLevel = server.getAttr(Provisioning.A_zimbraMtaTlsSecurityLevel, false);
        
        if (mtaTlsSecurityLevel == null) {
            if (expected != null)
                throw ServiceException.FAILURE("FAILED: server=" + server.getName() + ", expected=" + expected + ", actual=null", null);
        } else {
            ZAttrProvisioning.MtaTlsSecurityLevel actual = ZAttrProvisioning.MtaTlsSecurityLevel.fromString(mtaTlsSecurityLevel);
            if (actual != expected)
                throw ServiceException.FAILURE("FAILED: server=" + server.getName() + ", expected=" + expected + ", actual=" + actual.name(), null);
        }
    }
    
    public static void main(String[] args) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        String zimbraMtaAuthEnabled_config = config.getAttr(Provisioning.A_zimbraMtaAuthEnabled);
        String zimbraMtaTlsAuthOnly_config = config.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly);
        
         // <server>  <expected value after after upgrade>
        Map<String, ZAttrProvisioning.MtaTlsSecurityLevel> servers = new HashMap<String, ZAttrProvisioning.MtaTlsSecurityLevel>();
        
        servers.put(setupTestServer("trueTrue",     Boolean.TRUE,  Boolean.TRUE).getName(),  ZAttrProvisioning.MtaTlsSecurityLevel.may);
        servers.put(setupTestServer("trueFalse",    Boolean.TRUE,  Boolean.FALSE).getName(), ZAttrProvisioning.MtaTlsSecurityLevel.none);
        servers.put(setupTestServer("falseTrue",    Boolean.FALSE, Boolean.TRUE).getName(),  ZAttrProvisioning.MtaTlsSecurityLevel.none);
        servers.put(setupTestServer("falseFalse",   Boolean.FALSE, Boolean.FALSE).getName(), ZAttrProvisioning.MtaTlsSecurityLevel.none);
        servers.put(setupTestServer("falseNotset",  Boolean.FALSE, null).getName(),          ZAttrProvisioning.MtaTlsSecurityLevel.none);
        servers.put(setupTestServer("notsetFalse",  null,          Boolean.FALSE).getName(), ZAttrProvisioning.MtaTlsSecurityLevel.none);
        servers.put(setupTestServer("notsetNotset", null,          null).getName(),          null);
        
        String trueNotset = setupTestServer("trueNotset", Boolean.TRUE, null).getName();
        String notsetTrue = setupTestServer("notsetTrue",   null, Boolean.TRUE).getName();
        
        ZimbraMtaAuthEnabled upgrade = new ZimbraMtaAuthEnabled();
        upgrade.setBug("test");
        upgrade.setVerbose(true);
        
        for (Map.Entry<String, ZAttrProvisioning.MtaTlsSecurityLevel> entry : servers.entrySet()) {
            doTest(upgrade, entry.getKey(), entry.getValue());
        }
        
        setupGlobalConfig(null, Boolean.TRUE);
        doTest(upgrade, trueNotset, ZAttrProvisioning.MtaTlsSecurityLevel.may);
        setupGlobalConfig(null, Boolean.FALSE);
        doTest(upgrade, trueNotset, ZAttrProvisioning.MtaTlsSecurityLevel.none);
        setupGlobalConfig(null, null);
        doTest(upgrade, trueNotset, null);

        setupGlobalConfig(Boolean.TRUE, null);
        doTest(upgrade, notsetTrue, ZAttrProvisioning.MtaTlsSecurityLevel.may);
        setupGlobalConfig(Boolean.FALSE, null);
        doTest(upgrade, notsetTrue, ZAttrProvisioning.MtaTlsSecurityLevel.none);
        setupGlobalConfig(null, null);
        doTest(upgrade, notsetTrue, null);

        System.out.println("\n\nAll is well!");
    }
}
