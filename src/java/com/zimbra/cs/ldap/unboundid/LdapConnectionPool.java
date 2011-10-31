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
package com.zimbra.cs.ldap.unboundid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.GetEntryLDAPConnectionPoolHealthCheck;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PostConnectProcessor;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO.TODO;

public class LdapConnectionPool {
    
    //
    // Known connection pool names
    //
    public static final String CP_ZIMBRA_REPLICA = "ZimbraReplica";
    public static final String CP_ZIMBRA_MASTER = "ZimbraMaster";
    
    // for unittest and dump stats
    private static final Map<String, LDAPConnectionPool> connPools = 
        new HashMap<String, LDAPConnectionPool>();
    
    
    static LDAPConnectionPool createConnectionPool(String connPoolName, 
            LdapServerConfig config) throws LdapException {
        
        LDAPConnectionPool connPool = null;
        
        if (DebugConfig.useInMemoryLdapServer) {
            connPool = createConnPoolToInMemoryLdapServer(config);
        } else {
            connPool = createConnPool(config);
        }
        
        connPool.setConnectionPoolName(connPoolName);
        connPool.setMaxWaitTimeMillis(config.getConnPoolTimeoutMillis());
        
        boolean onCheckoutHealthCheckEnabled = config.isConnPoolHelathCheckOnCheckoutEnabled();
        boolean backgroundHealthCheckEnabled = !onCheckoutHealthCheckEnabled;
        
        // Set a custom health check interval only when background health check is enabled, 
        // because otherwise it has no effect anyway.
        if (backgroundHealthCheckEnabled) {
            connPool.setHealthCheckIntervalMillis(
                    config.getConnPoolHelathCheckBackgroundIntervalMillis());
        }
        
        GetEntryLDAPConnectionPoolHealthCheck healthChecker = new GetEntryLDAPConnectionPoolHealthCheck(
                null,                                                 // entryDN (null means root DSE)
                config.getConnPoolHelathCheckMaxResponseTimeMillis(), // maxResponseTime 
                false,                                                // invokeOnCreate
                onCheckoutHealthCheckEnabled,                         // invokeOnCheckout
                false,                                                // invokeOnRelease
                backgroundHealthCheckEnabled,                         // invokeForBackgroundChecks 
                false                                                 // invokeOnException
                );
        
        connPool.setHealthCheck(healthChecker);
        
        addToPoolMap(connPool);
       
        return connPool;
    }
    
    private static LDAPConnectionPool createConnPool(LdapServerConfig config) 
    throws LdapException {
        LdapServerPool serverPool = new LdapServerPool(config);
        
        ServerSet serverSet = serverPool.getServerSet();
        BindRequest bindRequest = createBindRequest(config);
    
        PostConnectProcessor postConnectProcessor = null;
        if (serverPool.getConnectionType() == LdapConnType.STARTTLS) {
            SSLContext startTLSContext = 
                LdapSSLUtil.createSSLContext(config.sslAllowUntrustedCerts());
            postConnectProcessor = new StartTLSPostConnectProcessor(startTLSContext);
        }
        
        LDAPConnectionPool connPool = null;
        try {
            connPool = new LDAPConnectionPool(serverSet, bindRequest, 
                    config.getConnPoolInitSize(), 
                    config.getConnPoolMaxSize(), postConnectProcessor);
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
        }
        
        return connPool;
    }
    
    private static LDAPConnectionPool createConnPoolToInMemoryLdapServer(LdapServerConfig config) 
    throws LdapException {
        return InMemoryLdapServer.createConnPool(InMemoryLdapServer.ZIMBRA_LDAP_SERVER, config);
    }
    
    static void closeAll() {
        for (LDAPConnectionPool connPool : connPools.values()) {
            connPool.close();
        }
    }
    
    private static synchronized void addToPoolMap(LDAPConnectionPool connPool) {
        String poolName = connPool.getConnectionPoolName();
        LDAPConnectionPool pool = connPools.get(poolName);
        assert(pool == null);
        
        connPools.put(poolName, connPool);
    }
    
    public static synchronized LDAPConnectionPool getConnPoolByName(String connPoolName) {
        return connPools.get(connPoolName);
    }
    
    private static synchronized LDAPConnectionPool getConnPool(
            String connPoolName, ExternalLdapConfig config) 
    throws LdapException{
        LDAPConnectionPool pool = connPools.get(connPoolName);
        if (pool == null) {
            // the newly created pool will be automatically put into connPools
            pool = LdapConnectionPool.createConnectionPool(connPoolName, config);
        }
        return pool;
    }
    
    static LDAPConnectionPool getConnPoolByConfig(ExternalLdapConfig config) 
    throws LdapException {
        String connPoolName = ExternalLdapConfig.ConnPoolKey.getConnPoolKey(config);
        return getConnPool(connPoolName, config);
    }
    
    @TODO  // handle SASL
    private static BindRequest createBindRequest(LdapServerConfig config) {
        String bindDN = config.getAdminBindDN();
        
        if (bindDN != null) {
            String bindPassword = config.getAdminBindPassword();
            return new SimpleBindRequest(bindDN, bindPassword);
        } else {
            return null;
        }
        
        /*
        if (bindDN != null) {
            return new SimpleBindRequest(bindDN, pw);
        } else if (saslMechanism != null) {
            return createSASLBindRequest();
        } else {
            return null;
        }
        */
    }
    
    
    //
    // static wrappers for DebugConnPool
    //
    static void debugCheckOut(LDAPConnectionPool connPool, LDAPConnection conn) {
        if (!DebugConnPool.enabled()) {
            return;
        }
        DebugConnPool.checkOut(connPool, conn);
    }
    
    static void debugCheckIn(LDAPConnectionPool connPool, LDAPConnection conn) {
        if (!DebugConnPool.enabled()) {
            return;
        }
        LdapConnectionPool.DebugConnPool.checkIn(connPool, conn);
    }
    
    @TODO  // have a way to trigger dumping the DebugConnPool
    public static void dump() {
        if (!DebugConnPool.enabled()) {
            return;
        }
        LdapConnectionPool.DebugConnPool.dump();
    }
    
    private static class DebugConnPool {
        
        private static final Map<String /* connection pool name */, DebugConnPool> 
            checkedOutByPoolName = new HashMap<String, DebugConnPool>();
        
        private List<CheckedOutInfo> checkedOutConns = new ArrayList<CheckedOutInfo>();
       
        static boolean enabled() {
            return LC.ldap_connect_pool_debug.booleanValue();
        }
        
        @TODO // where to output to?
        private static void output(String msg) {
            System.out.println(msg);
        }
        
        private void dumpDebugConnPool(long now) {
            DebugConnPool.output("Number of checked out connections: " + checkedOutConns.size() + "\n");
            
            for (CheckedOutInfo checkedOutConn : checkedOutConns) {
                checkedOutConn.dump(now);
            }
        }
        
        private static class CheckedOutInfo {
            
            private CheckedOutInfo(LDAPConnection conn) {
                connId = conn.getConnectionID();
                connPoolName = conn.getConnectionPoolName();
                checkedOutTimestamp = System.currentTimeMillis();
                
                Thread currentThread = Thread.currentThread();
                stackTrace = currentThread.getStackTrace();
            }
            
            private void dump(long now) {
                StringBuilder sb = new StringBuilder();
                sb.append("connId: " + connId + " (" + connPoolName + ")" + "\n");
                sb.append("elapsed milli secs: " + (now - checkedOutTimestamp) + "\n");
                sb.append("\n");
                for (StackTraceElement element : stackTrace) {
                    sb.append(element.toString() + "\n");
                }
                sb.append("\n");
                
                DebugConnPool.output("--------------------");
                DebugConnPool.output(sb.toString());
                
            }
            
            long connId;
            String connPoolName;
            long checkedOutTimestamp;
            StackTraceElement[] stackTrace;
        }
        
        private static synchronized void checkOut(LDAPConnectionPool connPool, LDAPConnection conn) {
            String connPoolName = connPool.getConnectionPoolName();
            DebugConnPool checkedOutFromPool = checkedOutByPoolName.get(connPoolName);
            
            if (checkedOutFromPool == null) {
                checkedOutFromPool = new DebugConnPool();
                checkedOutByPoolName.put(connPoolName, checkedOutFromPool);
            } else {
                // sanity check, see if the connection is already checked out
                for (CheckedOutInfo checkedOutConn : checkedOutFromPool.checkedOutConns) {
                    long connId = conn.getConnectionID();
                    if (connId == checkedOutConn.connId) {
                        DebugConnPool.output("connection " + connId + " is already checked out.");
                        checkedOutConn.dump(System.currentTimeMillis());
                        assert(false);
                    }
                }
            }
            
            CheckedOutInfo checkedOutConn = new CheckedOutInfo(conn);
            checkedOutFromPool.checkedOutConns.add(checkedOutConn);
        }
        
        private static synchronized void checkIn(LDAPConnectionPool connPool, LDAPConnection conn) {
            String connPoolName = connPool.getConnectionPoolName();
            DebugConnPool checkedOutFromPool = checkedOutByPoolName.get(connPoolName);
            
            assert(checkedOutFromPool != null);
            
            boolean checkedIn = false;
            for (Iterator<CheckedOutInfo> it = checkedOutFromPool.checkedOutConns.iterator(); it.hasNext();) {
                CheckedOutInfo checkedOutConn = it.next();
                long connId = conn.getConnectionID();
                if (connId == checkedOutConn.connId) {
                    checkedOutFromPool.checkedOutConns.remove(checkedOutConn);
                    checkedIn = true;
                    break;
                }
            }
            
            assert(checkedIn);
        }
        
        private static synchronized void dump() {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, DebugConnPool> checkedOutFromPool : checkedOutByPoolName.entrySet()) {
                String poolName = checkedOutFromPool.getKey();
                DebugConnPool checkedOutConns = checkedOutFromPool.getValue();
                DebugConnPool.output("====================");
                DebugConnPool.output("Pool " + poolName);
                DebugConnPool.output("====================");
                checkedOutConns.dumpDebugConnPool(now);
            }
        }
    }
}
