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
package com.zimbra.qa.unittest.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Server;

public class TestLdapProvServer extends LdapTest {
    private static ProvTestUtil provUtil;
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new ProvTestUtil();
        prov = provUtil.getProv();
    }
    
    private Server createServer(String serverName) throws Exception {
        return provUtil.createServer(serverName, null);
    }
    
    private Server createServer(String serverName, Map<String, Object> attrs) throws Exception {
        return provUtil.createServer(serverName, attrs);
    }
    
    private void deleteServer(Server server) throws Exception {
        provUtil.deleteServer(server);
    }
    
    @Test
    public void createServer() throws Exception {
        String SERVER_NAME = Names.makeServerName("createServer");
        Server server = createServer(SERVER_NAME);
        deleteServer(server);
    }
    
    @Test
    public void createServerAlreadyExists() throws Exception {
        String SERVER_NAME = Names.makeServerName("createServerAlreadyExists");
        Server server = createServer(SERVER_NAME);
        
        boolean caughtException = false;
        try {
            prov.createServer(SERVER_NAME, new HashMap<String, Object>());
        } catch (AccountServiceException e) {
            if (AccountServiceException.SERVER_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteServer(server);
    }
    
    @Test
    public void localServer() throws Exception {
        Server localServer = prov.getLocalServer();
        assertNotNull(localServer);
    }
    
    @Test
    public void getAllServers() throws Exception {
        String SERVER_NAME_1 = Names.makeServerName("getAllServers-1");
        
        Map<String, Object> server1Attrs = new HashMap<String, Object>();
        server1Attrs.put(Provisioning.A_zimbraServiceEnabled, 
                new String[]{Provisioning.SERVICE_MEMCACHED, Provisioning.SERVICE_MAILBOX});
        Server server1 = createServer(SERVER_NAME_1, server1Attrs);
        
        String SERVER_NAME_2 = Names.makeServerName("getAllServers-2");
        Server server2 = createServer(SERVER_NAME_2);
        
        List<Server> allServers = prov.getAllServers();
        assertEquals(3, allServers.size());
        
        Set<String> allServerIds = new HashSet<String>();
        for (Server server : allServers) {
            allServerIds.add(server.getId());
        }
        assertTrue(allServerIds.contains(prov.getLocalServer().getId()));
        assertTrue(allServerIds.contains(server1.getId()));
        assertTrue(allServerIds.contains(server2.getId()));
        
        List<Server> allServersByService = prov.getAllServers(Provisioning.SERVICE_MEMCACHED);
        assertEquals(1, allServersByService.size());
        assertEquals(server1.getId(), allServersByService.get(0).getId());
        
        deleteServer(server1);
        deleteServer(server2);
    }
    
    @Test
    public void getServer() throws Exception {
        String SERVER_NAME = Names.makeServerName("getServer");
        Server server = createServer(SERVER_NAME);
        String serverId = server.getId();
        
        prov.flushCache(CacheEntryType.server, null);
        server = prov.get(Key.ServerBy.id, serverId);
        assertEquals(serverId, server.getId());
        
        prov.flushCache(CacheEntryType.server, null);
        server = prov.get(Key.ServerBy.name, SERVER_NAME);
        assertEquals(serverId, server.getId());
        
        deleteServer(server);
    }

    @Test
    public void getServerNotExist() throws Exception {
        String SERVER_NAME = Names.makeServerName("getServer");
        Server server = prov.get(Key.ServerBy.name, SERVER_NAME);
        assertNull(server);
    }
}
