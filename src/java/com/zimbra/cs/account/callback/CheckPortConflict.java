/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.callback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class CheckPortConflict extends AttributeCallback {

    private static final String KEY = CheckPortConflict.class.getName();
    private static final Set<String> sPortAttrs = new HashSet<String>();
        
    static {
        // TODO: use a flag in imbra-attrs.xml and generate this map automatically
        sPortAttrs.add(Provisioning.A_zimbraAdminPort);
            
        sPortAttrs.add(Provisioning.A_zimbraImapBindPort);
        sPortAttrs.add(Provisioning.A_zimbraImapSSLBindPort);
        sPortAttrs.add(Provisioning.A_zimbraImapProxyBindPort);
        sPortAttrs.add(Provisioning.A_zimbraImapSSLProxyBindPort);

        sPortAttrs.add(Provisioning.A_zimbraLmtpBindPort);
            
        sPortAttrs.add(Provisioning.A_zimbraMailPort);
        sPortAttrs.add(Provisioning.A_zimbraMailSSLPort);
            
        sPortAttrs.add(Provisioning.A_zimbraPop3BindPort);
        sPortAttrs.add(Provisioning.A_zimbraPop3SSLBindPort);
        sPortAttrs.add(Provisioning.A_zimbraPop3ProxyBindPort);
        sPortAttrs.add(Provisioning.A_zimbraPop3SSLProxyBindPort);
            
        sPortAttrs.add(Provisioning.A_zimbraRemoteManagementPort);
        
        sPortAttrs.add(Provisioning.A_zimbraMemcachedBindPort);

        sPortAttrs.add(Provisioning.A_zimbraMailProxyPort);
        sPortAttrs.add(Provisioning.A_zimbraMailSSLProxyPort);
    }
        
       
    /**
     * check port conflict
     * 
     */
    public void preModify(Map context, String attrName, Object attrValue,
                          Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        if (entry != null && !(entry instanceof Server) && !(entry instanceof Config)) return;
        
        Object done = context.get(KEY);
        if (done == null)
            context.put(KEY, KEY);
        else
            return;
            
        // sanity check, zimbra-attrs.xml and the sPortAttrsToCheck map has to be in sync
        if (!sPortAttrs.contains(attrName) ||
            !AttributeManager.getInstance().isServerInherited(attrName))
            assert(false);
        
        // server == null means the server entry is being created
        if (entry == null || entry instanceof Server)
            checkServer((Server)entry, attrsToModify);
        else 
            checkConfig((Config)entry, attrsToModify);
    }
    
    private void checkServer(Server server, Map<String, Object> serverAttrsToModify) throws ServiceException {
        Map<String, String> ports = new HashMap<String, String>();
        Map<String, Object> defaults = Provisioning.getInstance().getConfig().getServerDefaults();
        
        // collect current port values
        if (server != null) {
            for (String attrName : sPortAttrs) {
                if (!serverAttrsToModify.containsKey(attrName))
                    ports.put(server.getAttr(attrName), attrName);
            }
        }
        
        // check conflict for attrs being changed 
        for (Map.Entry<String, Object> attrToModify : serverAttrsToModify.entrySet()) {
            String attrName = attrToModify.getKey();
            
            if (!sPortAttrs.contains(attrName))
                continue;
            
            SingleValueMod mod = singleValueMod(serverAttrsToModify, attrName);
            String newValue = null;
            if (mod.setting())
                newValue = mod.value();
            else {
                // unsetting, get default, which would become the new value
                Object defValue = defaults.get(attrName);
                if (defValue != null) {
                    if (defValue instanceof String)
                        newValue = (String)defValue;
                    else
                        ZimbraLog.misc.info("default value for " + attrName + " should be a single-valued attribute, invalid default value ignored");
                } 
            }
            
            if (conflict(ports, newValue)) {
                String serverInfo = (server == null) ? "" : " on server " + server.getName();
                throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                                                       attrName + " and " + ports.get(newValue) + serverInfo, null);
            } else
                ports.put(newValue, attrName);
        }
    }
    
    private void checkConfig(Config config, Map<String, Object> configAttrsToModify) throws ServiceException {
        BiMap<String, String> newDefaults = HashBiMap.create();
            
        /*
         * First, make sure there is no conflict in the Config entry, even
         * if the value on the config entry might not be effective on a server.
         */ 
        for (String attrName : sPortAttrs) {
            if (!configAttrsToModify.containsKey(attrName))
                newDefaults.put(config.getAttr(attrName), attrName);
        }
        
        // check conflict for attrs being changed 
        for (Map.Entry<String, Object> attrToModify : configAttrsToModify.entrySet()) {
            String attrName = attrToModify.getKey();
            
            if (!sPortAttrs.contains(attrName))
                continue;
            
            SingleValueMod mod = singleValueMod(configAttrsToModify, attrName);
            String newValue = null;
            if (mod.setting())
                newValue = mod.value();
                        
            if (conflict(newDefaults, newValue)) {
                throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                        attrName + " and " + newDefaults.get(newValue) + " on global config", null);
            } else
                newDefaults.put(newValue, attrName);
        }
        
        /* 
         * Then, iterate through all servers see if this port change on the Config
         * entry has impact on a server.  
         */
        List<Server> servers = Provisioning.getInstance().getAllServers();
        for (Server server : servers) {
            checkServerWithNewDefaults(server, newDefaults, configAttrsToModify);
        }
    }
    
    private void checkServerWithNewDefaults(Server server, BiMap<String, String> newDefaults, Map<String, Object> configAttrsToModify) throws ServiceException {
        Map<String, String> ports = new HashMap<String, String>();
        
        for (String attrName : sPortAttrs) {
            String newValue = null;
            String curValue = server.getAttr(attrName, false); // value on the server entry
            if (curValue == null)
                newValue = newDefaults.inverse().get(attrName);  // will inherit from new default
            else
                newValue = curValue;
            
            if (conflict(ports, newValue)) {
                String conflictWith = ports.get(newValue);
                // throw only when the attr is one of the attrs being modified, otherwise, just let it pass.
                if (configAttrsToModify.containsKey(attrName) || configAttrsToModify.containsKey(conflictWith))
                    throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                        attrName + " and " + ports.get(newValue) + " on server " + server.getName(), null);
            } else
                ports.put(newValue, attrName);
        }
    }
        
    private boolean conflict(Map<String, String> ports, String port) {
        
        if (StringUtil.isNullOrEmpty(port))
            return false;
        else if (port.equals("0"))
            return false;
        else
            return ports.containsKey(port);
    }
        
    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
