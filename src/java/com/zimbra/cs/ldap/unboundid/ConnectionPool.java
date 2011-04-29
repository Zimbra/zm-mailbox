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

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPConnectionPoolStatistics;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PostConnectProcessor;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import com.zimbra.cs.ldap.LdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.TODO;

public class ConnectionPool {
    
    //
    // Connection pool names
    //
    public static final String CP_ZIMBRA_REPLICA = "ZimbraReplica";
    public static final String CP_ZIMBRA_MASTER = "ZimbraMaster";
    
    // for unittest and dump stats
    private static final Map<String, LDAPConnectionPool> connPools = 
        new HashMap<String, LDAPConnectionPool>();

    static LDAPConnectionPool createConnectionPool(String connPoolName, 
            LdapConfig config, LdapServerPool ldapHost) throws LdapException {
        
        ServerSet serverSet = ldapHost.getServerSet();
        BindRequest bindRequest = createBindRequest(config);
    
        PostConnectProcessor postConnectProcessor = null;
        if (ldapHost.getConnectionType() == LdapConnType.STARTTLS) {
            SSLContext startTLSContext = LdapSSLUtil.createSSLContext(config.sslAllowUntrustedCerts());
            postConnectProcessor = new StartTLSPostConnectProcessor(startTLSContext);
        }
        
        LDAPConnectionPool connPool;
        try {
            connPool = new LDAPConnectionPool(serverSet, bindRequest, config.getConnPoolInitSize(), 
                    config.getConnPoolMaxSize(), postConnectProcessor);
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToLdapException(e);
        }
        
        connPool.setConnectionPoolName(connPoolName);
        connPool.setMaxWaitTimeMillis(config.getConnPoolTimeoutMillis());

        addToPoolMap(connPool);
       
        return connPool;
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
    
    @TODO  // handle SASL
    private static BindRequest createBindRequest(LdapConfig config) {
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
}
