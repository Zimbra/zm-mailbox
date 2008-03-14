/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.account.callback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class CheckPortConflict extends AttributeCallback {

    private static final String KEY = CheckPortConflict.class.getName();
    private static final Set<String> sPortAttrs = new HashSet<String>();
        
    static {
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
	// sPortAttrs.add(Provisioning.A_zimbraMailSSLProxyPort);
    }
        
       
    /**
     * check port conflict
     * 
     */
    public void preModify(Map context, String attrName, Object value,
                          Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        Object done = context.get(KEY);
        if (done == null)
            context.put(KEY, KEY);
        else
            return;
        
        String newPortValue; 
        
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(attrName + " is a single-valued attribute", null);
        else
            newPortValue = (String)value;
            
        if (!((entry instanceof Server)||(entry instanceof Config))) return;
            
        // sanity check, zimbra-attrs.xml and the sPortAttrsToCheck map has to be in sync
        if (!sPortAttrs.contains(attrName) ||
            !AttributeManager.getInstance().isServerInherited(attrName))
            assert(false);
           
        if (entry instanceof Server)
            checkServer((Server)entry, attrsToModify);
        else 
            checkConfig((Config)entry, attrsToModify);
    }

    private void checkServer(Server server, Map<String, Object> serverAttrsToModify) throws ServiceException {
           
        Map<String, String> ports = new HashMap<String, String>();
        Map<String, Object> serverDefaults = Provisioning.getInstance().getConfig().getServerDefaults();
            
        for (String attrName : sPortAttrs) {
            String newValue = null;
                
            if (serverAttrsToModify.containsKey(attrName)) {
                Object obj = serverAttrsToModify.get(attrName);
                if (obj instanceof String) {
                    String newPort = (String)obj;
                    if (newPort.length() > 0) {
                        // setting
                        newValue = newPort;
                    } else {
                        // unsetting, get default, which would become the new value
                        Object defValue = serverDefaults.get(attrName);
                        if (defValue != null) {
                            if (defValue instanceof String) {
                                newValue = (String)defValue;
                            } else {
                                // huh??
                                ZimbraLog.misc.info("default value for " + attrName + " should be a single-valued attribute, invalid default value ignored");
                            }
                        } 
                    }
                } else 
                    throw ServiceException.INVALID_REQUEST(attrName + " is a single-valued attribute", null);
            } else {
                if (server != null)
                    newValue = server.getAttr(attrName);
            }
            
            if (!StringUtil.isNullOrEmpty(newValue)) {
                if (ports.containsKey(newValue))
                    throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                                                           attrName + " and " + ports.get(newValue) + " on server " + 
                                                           (server == null?"":server.getName()), null);
                else
                    ports.put(newValue, attrName);
            }
        }
    }
        
    private void checkConfig(Config config, Map<String, Object> configAttrsToModify) throws ServiceException {
        DualHashBidiMap newDefaults = new DualHashBidiMap();
            
        /*
         * First, make sure there is no conflict in the Config entry, even
         * if the value on the config entry might not be effective on a server.
         */ 
        for (String attrName : sPortAttrs) {
            String newValue = null;
                
            if (configAttrsToModify.containsKey(attrName)) {
                Object obj = configAttrsToModify.get(attrName);
                if (obj instanceof String) {
                    String newPort = (String)obj;
                    newValue = newPort;
                } else
                    throw ServiceException.INVALID_REQUEST(attrName + " is a single-valued attribute", null);
            } else {
                if (config != null)
                    newValue = config.getAttr(attrName);
            }
                
            if (!StringUtil.isNullOrEmpty(newValue)) {
                if (newDefaults.containsKey(newValue))
                    throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                                                           attrName + " and " + newDefaults.get(newValue) + " on global config", null);
                else
                    newDefaults.put(newValue, attrName);
            }
        }
            
        /* 
         * Then, iterate through all servers see if this port change on the Config
         * entry has impact on a server.  It has impact on a server only when the
         * attr is not present on a server.  
         */
        List<Server> servers = Provisioning.getInstance().getAllServers();
        for (Server server : servers) {
            checkServerWithNewDefaults(server, newDefaults);
        }
    }
    
    private void checkServerWithNewDefaults(Server server, DualHashBidiMap newDefaults) throws ServiceException {
        Map<String, String> ports = new HashMap<String, String>();
        for (String attrName : sPortAttrs) {
            String newValue = null;
            String curValue = server.getAttr(attrName, false); // value on the server entry
            if (!StringUtil.isNullOrEmpty(curValue)) {
                if (newDefaults.containsValue(attrName))
                    newValue = (String)newDefaults.getKey(attrName);
                else
                    newValue = server.getAttr(attrName);
            } else
                newValue = curValue;
            
            if (!StringUtil.isNullOrEmpty(newValue)) {
                if (ports.containsKey(newValue))
                    throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                            attrName + " and " + ports.get(newValue) + " on server " + server.getName(), null);
                else
                    ports.put(newValue, attrName);
            }
        }
    }
        
        
    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
