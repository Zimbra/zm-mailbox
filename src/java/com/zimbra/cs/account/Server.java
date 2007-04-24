/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Server extends NamedEntry {
    
    public Server(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults) {
        super(name, id, attrs, defaults);
    }
    
    private static final Set<String> sPortAttrsToCheck = new HashSet<String>();
    
    static {
        sPortAttrsToCheck.add(Provisioning.A_zimbraPop3BindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraPop3SSLBindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraPop3ProxyBindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraPop3SSLProxyBindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraImapBindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraImapSSLBindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraImapProxyBindPort);
        sPortAttrsToCheck.add(Provisioning.A_zimbraImapSSLProxyBindPort);
    }
        
    public void checkPortConflict(Map<String, Object> serverAttrsToModify) throws ServiceException {
        
        Map<String, String> ports = new HashMap<String, String>();
        Map<String, Object> serverDefaults = null; 
        
        for (String attrName : sPortAttrsToCheck) {
            String newValue;
            
            if (serverAttrsToModify.containsKey(attrName)) {
                Object obj = serverAttrsToModify.get(attrName);
                if (obj instanceof String) {
                    String newPort = (String)obj;
                    if (newPort.length() > 0) {
                        // setting
                        newValue = newPort;
                    } else {
                        // unsetting, get default, which would become the new value
                        if (serverDefaults == null)
                            serverDefaults = Provisioning.getInstance().getConfig().getServerDefaults();
                        Object defValue = serverDefaults.get(attrName);
                        if (defValue != null) {
                            if ((defValue instanceof String)) {
                                newValue = (String)defValue;
                            } else {
                                // huh??
                                ZimbraLog.misc.info("default value for " + attrName + " should be a single-valued attribute, invalid default value ignored");
                                newValue = newPort; // which is an empty string
                            }
                        } else
                            newValue = "";
                    }
                } else 
                    throw ServiceException.INVALID_REQUEST(attrName + " is a single-valued attribute", null);
            } else
                newValue = getAttr(attrName);
            
            if (!StringUtil.isNullOrEmpty(newValue)) {
                if (ports.containsKey(newValue))
                    throw ServiceException.INVALID_REQUEST("port " + newValue + " conflict between " + 
                                                           attrName + " and " + ports.get(newValue) + " on server " + getName(), null);

                else
                    ports.put(newValue, attrName);
            }
        }
    }
    
    /*
     * called from Config
     */
    public static void checkPortConflict(Config config, Map<String, Object> configAttrsToModify) throws ServiceException {
        DualHashBidiMap newDefaults = new DualHashBidiMap();
        
        /*
         * First, make sure there is no conflict in the Config entry.  Even
         * if the value in the config entry might not be effective on a server.
         */ 
        for (String attrName : sPortAttrsToCheck) {
            String newValue;
            
            if (configAttrsToModify.containsKey(attrName)) {
                Object obj = configAttrsToModify.get(attrName);
                if (obj instanceof String) {
                    String newPort = (String)obj;
                    newValue = newPort;
                } else {
                    throw ServiceException.INVALID_REQUEST(attrName + " is a single-valued attribute", null);
                }
            } else
                newValue = config.getAttr(attrName);
            
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
            checkPortConflict(server, newDefaults);
        }
    }
    
    private static void checkPortConflict(Server server, DualHashBidiMap newDefaults) throws ServiceException {
        Map<String, String> ports = new HashMap<String, String>();
        for (String attrName : sPortAttrsToCheck) {
            String newValue;
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
    
}
