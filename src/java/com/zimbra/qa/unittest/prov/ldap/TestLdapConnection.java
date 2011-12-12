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
package com.zimbra.qa.unittest.prov.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.unboundid.LdapConnectionPool;
import com.zimbra.cs.ldap.unboundid.UBIDLdapContext;
import com.zimbra.qa.unittest.prov.LocalconfigTestUtil;
import com.zimbra.qa.unittest.prov.ProvTest;
import com.zimbra.qa.unittest.prov.ProvTest.SkippedForInMemLdapServerException.Reason;

public class TestLdapConnection extends LdapTest {
    
    private static final boolean START_TLS_ENABLED = false;
    private static final String BIND_DN = LC.zimbra_ldap_userdn.value();
    private static final String BIND_PASSWORD = LC.zimbra_ldap_password.value();
    
    // use a different ldap url for each test so they don't get the same conneciton pool
    private static String LDAP_URL_ON_CHECKOUT;
    private static String LDAP_URL_AFTER_EXCEPTION;
    private static String LDAP_URL_BACKGROUND;

    @BeforeClass
    public static void init() throws Exception {
        LDAP_URL_ON_CHECKOUT = "ldap://" + InetAddress.getLocalHost().getHostName() + ":389";
        LDAP_URL_AFTER_EXCEPTION = "ldap://" + LC.zimbra_server_hostname.value() + ":389";
        LDAP_URL_BACKGROUND = "ldap://" + "localhost" + ":389";
    }
    
    private UBIDLdapContext getContext() throws Exception {
        return (UBIDLdapContext) LdapClient.getContext(LdapUsage.UNITTEST);
    }
    
    private UBIDLdapContext getContext(ExternalLdapConfig ldapConfig) throws Exception {
        return (UBIDLdapContext) LdapClient.getExternalContext(ldapConfig, LdapUsage.UNITTEST);
    }
    
    private void closeContext(UBIDLdapContext zlc) {
        LdapClient.closeContext(zlc);
    }
    
    private void stopLdap() throws Exception {
        List<String> STOP_LDAP_CMD = new ArrayList<String>();
        STOP_LDAP_CMD.add("/opt/zimbra/bin/ldap");
        STOP_LDAP_CMD.add("stop");
          
        // System.out.println(STOP_LDAP_CMD.toString());
        ProcessBuilder pb = new ProcessBuilder(STOP_LDAP_CMD);
        Process process = pb.start();
        int exitValue = process.waitFor();
        assertEquals(0, exitValue);
            
        // wait for 2 seconds
        Thread.sleep(2000);
    }
    
    private void startLdap() throws Exception {
        List<String> STOP_LDAP_CMD = new ArrayList<String>();
        STOP_LDAP_CMD.add("/opt/zimbra/bin/ldap");
        STOP_LDAP_CMD.add("start");
            
        // System.out.println(STOP_LDAP_CMD.toString());
        ProcessBuilder pb = new ProcessBuilder(STOP_LDAP_CMD);
        Process process = pb.start();
        int exitValue = process.waitFor();
        assertEquals(0, exitValue);
            
        // wait for 2 seconds
        Thread.sleep(2000);
    }
    
    private LDAPConnectionPool populateConnPool(ExternalLdapConfig ldapConfig, int numConns) 
    throws Exception {
        final int MAX_POOL_SIZE = LC.ldap_connect_pool_maxsize.intValue();
        assertTrue(numConns < MAX_POOL_SIZE);
        
        LDAPConnectionPool connPool = null;
        
        List<UBIDLdapContext> zlcs = Lists.newArrayList();
        for (int i = 0; i < numConns; i++) {
            UBIDLdapContext zlc = getContext(ldapConfig);
            zlcs.add(zlc);
            
            if (connPool == null) {
                connPool = zlc.getConnectionPool();
                // System.out.println("backgroundHealthCheck pool is " + connPool.getConnectionPoolName());
            } else {
                // verify all zlcs use the same conn pool
                assertTrue(connPool == zlc.getConnectionPool());
            }
        }
        
        assertEquals(MAX_POOL_SIZE, connPool.getMaximumAvailableConnections());
        
        // number of connections that are currently available for use in this connection pool
        assertEquals(0, connPool.getCurrentAvailableConnections());
        
        for (int i = 0; i < numConns; i++) {
            UBIDLdapContext zlc = zlcs.get(i);
            closeContext(zlc);
        }
        
        // There should be NUM_CONNS conns in the pool
        assertEquals(numConns, connPool.getCurrentAvailableConnections());

        return connPool;
    }
    
    private Map<KnownKey, String> setLocalConfig(Map<KnownKey, String> keyValues) {
        Map<KnownKey, String> curValues = new HashMap<KnownKey, String>();
        
        for (Map.Entry<KnownKey, String> keyValue : keyValues.entrySet()) {
            KnownKey key = keyValue.getKey();
            String value = keyValue.getValue();
            
            // save the current value
            String curValue = key.value();
            curValues.put(key, curValue);
            
            // set to the new value
            LocalconfigTestUtil.modifyLocalConfigTransient(key, value);
            assertEquals(value, key.value());
        }
        
        return curValues;
    }
    
    @Test
    public void onCheckoutHealthCheck() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(Reason.CONNECTION_POOL_HEALTH_CHECK);
        
        Map<KnownKey, String> lcKeysToModify = new HashMap<KnownKey, String>();
        lcKeysToModify.put(LC.ldap_connect_pool_health_check_on_checkout_enabled, "true");
        // lcKeysToModify.put(LC.ldap_connect_pool_health_check_after_exception_enabled, "false");
        // lcKeysToModify.put(LC.ldap_connect_pool_health_check_background_enabled, "false");
        
        Map<KnownKey, String> origLCKeyValues = setLocalConfig(lcKeysToModify);
                
        final int NUM_CONNS = 10;
        
        ExternalLdapConfig ldapConfig = new ExternalLdapConfig(
                LDAP_URL_ON_CHECKOUT, START_TLS_ENABLED, null, BIND_DN, BIND_PASSWORD, null, null);
        
        LDAPConnectionPool connPool = populateConnPool(ldapConfig, NUM_CONNS);
        
        // stop ldap server here
        System.out.println("Before health check, availConns = " + connPool.getCurrentAvailableConnections());
        stopLdap();
        
        // try to get a connection from the pool to trigger health check
        boolean caughtException = false;
        try {
            UBIDLdapContext zlc = getContext(ldapConfig);
        } catch (ServiceException e) {
            caughtException = true;
        }
        assertTrue(caughtException);
        
        System.out.println("After health check, availConns = " + connPool.getCurrentAvailableConnections());
        assertEquals(0, connPool.getCurrentAvailableConnections());
        
        // put the config key back
        setLocalConfig(origLCKeyValues);
        
        startLdap();
        
        // get a connection now, should be successful
        UBIDLdapContext zlc = getContext(ldapConfig);
        closeContext(zlc);
    }
    
    @Test
    @Ignore   // after-exception health check is not supported.
    public void afterExceptionHealthCheck() throws Exception {
        Map<KnownKey, String> lcKeysToModify = new HashMap<KnownKey, String>();
        lcKeysToModify.put(LC.ldap_connect_pool_health_check_on_checkout_enabled, "false");
        // lcKeysToModify.put(LC.ldap_connect_pool_health_check_after_exception_enabled, "true");
        // lcKeysToModify.put(LC.ldap_connect_pool_health_check_background_enabled, "false");
        
        Map<KnownKey, String> origLCKeyValues = setLocalConfig(lcKeysToModify);
                
        final int NUM_CONNS = 10;
        
        ExternalLdapConfig ldapConfig = new ExternalLdapConfig(
                LDAP_URL_AFTER_EXCEPTION, START_TLS_ENABLED, null, BIND_DN, BIND_PASSWORD, null, null);
        
        LDAPConnectionPool connPool = populateConnPool(ldapConfig, NUM_CONNS);
        
        // stop ldap server here
        System.out.println("Before health check, availConns = " + connPool.getCurrentAvailableConnections());
        stopLdap();
        
        // try to get a connection from the pool
        // unlike on checkout health check, this will NOT trigger a health check
        // it will just return one connection from the pool
        UBIDLdapContext zlcTest = getContext(ldapConfig);
        
        // use the connection - now we should get an exception, and a health check 
        // should be triggered.
        boolean caughtException = false;
        try {
            zlcTest.getAttributes(LdapConstants.DN_ROOT_DSE, null);
        } catch (ServiceException e) {
            caughtException = true;
            // e.printStackTrace();
        } finally {
            // if this is called, it somehow increments the CurrentAvailableConnections count
            // in the connection pool - it should not, because the connection is already defunced
            // (LDAPConnectionPool.releaseConnectionAfterException() was called).
            // 
            // The CurrentAvailableConnections count drop back to NUM_CONNS - 1 after one minute.
            // closeContext(zlcTest);
        }
        assertTrue(caughtException);
        
        System.out.println("After health check, availConns = " + connPool.getCurrentAvailableConnections());
        
        int secs = 0;
        while (true) {
            Thread.sleep(1000);
            secs++;
            int junk = connPool.getCurrentAvailableConnections();
            System.out.println("After health check, availConns = " + junk + " " + secs);
            if (junk < NUM_CONNS) {
                break;
            }
        }
        // unlink on-checkout and beckground modes, the after-exception mode removes only 
        // the bad connection.   To support this, we need to call 
        // LDAPConnectionPool.releaseConnectionAfterException(LDAPConnection connection, LDAPException exception) 
        // after an exception is caught.
        // For some reason this only work in Eclipse because of timing issue.
        assertEquals(NUM_CONNS - 1, connPool.getCurrentAvailableConnections());
        
        // put the config key back
        setLocalConfig(origLCKeyValues);
        
        startLdap();
        
        // get a connection now, should be successful
        UBIDLdapContext zlc = getContext(ldapConfig);
        closeContext(zlc);
    }
    
    
    @Test
    public void backgroundHealthCheck() throws Exception {
        SKIP_FOR_INMEM_LDAP_SERVER(Reason.CONNECTION_POOL_HEALTH_CHECK);
        
        final long BACKGROUND_HEALTH_CHECK_INTERVAL = 5000; // 5 secs
        
        Map<KnownKey, String> lcKeysToModify = new HashMap<KnownKey, String>();
        lcKeysToModify.put(LC.ldap_connect_pool_health_check_on_checkout_enabled, "false");
        // lcKeysToModify.put(LC.ldap_connect_pool_health_check_after_exception_enabled, "false");
        // lcKeysToModify.put(LC.ldap_connect_pool_health_check_background_enabled, "true");
        lcKeysToModify.put(LC.ldap_connect_pool_health_check_background_interval_millis, 
                Long.valueOf(BACKGROUND_HEALTH_CHECK_INTERVAL).toString());
        
        Map<KnownKey, String> origLCKeyValues = setLocalConfig(lcKeysToModify);
        
        final int MAX_POOL_SIZE = LC.ldap_connect_pool_maxsize.intValue();
        final int NUM_CONNS = 10;
        
        ExternalLdapConfig ldapConfig = new ExternalLdapConfig(
                LDAP_URL_BACKGROUND, START_TLS_ENABLED, null, BIND_DN, BIND_PASSWORD, null, null);
        
        LDAPConnectionPool connPool = populateConnPool(ldapConfig, NUM_CONNS);

        // stop ldap server here
        System.out.println("Before health check, availConns = " + connPool.getCurrentAvailableConnections());
        stopLdap();
        
        // wait for the health check interval to trigger health check
        long waitFor = BACKGROUND_HEALTH_CHECK_INTERVAL + 1000;
        System.out.println("Waiting for " + waitFor + " msecs");
        Thread.sleep(waitFor);
        
        System.out.println("After health check, availConns = " + connPool.getCurrentAvailableConnections());
        assertEquals(0, connPool.getCurrentAvailableConnections());
        
        // put the config key back
        setLocalConfig(origLCKeyValues);
        
        startLdap();
        
        // get a connection now, should be successful
        UBIDLdapContext zlc = getContext(ldapConfig);
        closeContext(zlc);
    }
    
    @Test
    // @Ignore  // TODO: must be the first test to run
    public void testConnPoolNumAvailConns() throws Exception {
        
        int INIT_POOL_SIZE = LC.ldap_connect_pool_initsize.intValue();
        int MAX_POOL_SIZE = LC.ldap_connect_pool_maxsize.intValue();
        
        LDAPConnectionPool connPool = LdapConnectionPool.getConnPoolByName(
                LdapConnectionPool.CP_ZIMBRA_REPLICA);
        
        assertEquals(INIT_POOL_SIZE, connPool.getCurrentAvailableConnections());
        assertEquals(MAX_POOL_SIZE, connPool.getMaximumAvailableConnections());
        
        UBIDLdapContext zlc = getContext();
        String poolName = connPool.getConnectionPoolName();
        closeContext(zlc);
        
        assertEquals(LdapConnectionPool.CP_ZIMBRA_REPLICA, poolName);
        
        //
        // available connections: 
        //   connections that are connected and is available to be checked out.
        //
        
        // get a connection and close it, num available connections in the pool 
        // should not change.
        for (int i = 0; i < 10; i++) {
            UBIDLdapContext conn = getContext();
            closeContext(conn);
            assertEquals(INIT_POOL_SIZE, connPool.getCurrentAvailableConnections());
        }
        
        int numOpen = 20;
        // make sure numOpen is a good number to test
        assertTrue(numOpen > INIT_POOL_SIZE);
        assertTrue(numOpen < MAX_POOL_SIZE);
        
        // get connections, not closing them, num available connections in the pool 
        // should keep decreasing until there is no more.
        UBIDLdapContext[] conns = new UBIDLdapContext[numOpen];
        for (int i = 0; i < numOpen; i++) {
            conns[i] = getContext();
            int expected = Math.max(0, INIT_POOL_SIZE - (i + 1));
            assertEquals(expected, connPool.getCurrentAvailableConnections());
        }
        
        // now, release all the open connections, num available connections in the pool 
        // should keep increasing.
        for (int i = 0; i < numOpen; i++) {
            closeContext(conns[i]);
            int expected = i + 1;
            assertEquals(expected, connPool.getCurrentAvailableConnections());
        }
        
        // dumpConnPool(connPool);
    }
}
