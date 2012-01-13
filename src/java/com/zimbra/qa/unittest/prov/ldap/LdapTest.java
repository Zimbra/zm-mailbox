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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.BeforeClass;
import static org.junit.Assert.*;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.qa.unittest.prov.ProvTest;

public class LdapTest extends ProvTest {
    private static final String LDAP_TEST_BASE_DOMAIN = "ldaptest";
    
    // variable guarding initTest() enter only once per JVM
    // if test is triggered from ant test-ldap(-inmem), number of JVM's
    // to fork is controlled by forkmode attr in the <junit> ant element
    private static boolean perJVMInited = false;
    
    // - handy to set it to "true"/"false" when invoking a single test from inside Eclipse
    // - make sure it is always set to null in p4. 
    private static String useInMemoryLdapServerOverride = null;  // "true";
    
    // ensure assertion is enabled
    static {
        boolean assertsEnabled = false;
        assert assertsEnabled = true; // Intentional side effect!!!

        if (!assertsEnabled) {
            throw new RuntimeException("Asserts must be enabled!!!");
        }
    } 
    
    @BeforeClass  // invoked once per class loaded
    public static void beforeClass() throws Exception {
        initPerJVM();
    }
    
    static String baseDomainName() {
        StackTraceElement [] s = new RuntimeException().getStackTrace();
        return s[1].getClassName().toLowerCase() + "." + 
                LDAP_TEST_BASE_DOMAIN + "." + InMemoryLdapServer.UNITTEST_BASE_DOMAIN_SEGMENT;
    }
    
    public static String genTestId() {
        Date date = new Date();
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMdd-HHmmss");
        return fmt.format(date);
    }
    
    // invoked once per JVM
    private static synchronized void initPerJVM() throws Exception {
        if (perJVMInited) {
            return;
        }
        perJVMInited = true;
        
        CliUtil.toolSetup(Log.Level.error.name());
        ZimbraLog.test.setLevel(Log.Level.info);
        // ZimbraLog.autoprov.setLevel(Log.Level.debug);
        // ZimbraLog.account.setLevel(Log.Level.debug);
        // ZimbraLog.ldap.setLevel(Log.Level.debug);
        // ZimbraLog.soap.setLevel(Log.Level.trace);

        /*
        if (useInMemoryLdapServerProperty == null) {
            useInMemoryLdapServerProperty = 
                System.getProperty("use_in_memory_ldap_server", "false");
        }
        
        boolean useInMemoryLdapServer = 
            Boolean.parseBoolean(useInMemoryLdapServerProperty);
        
        KnownKey key = new KnownKey("debug_use_in_memory_ldap_server", 
                useInMemoryLdapServerProperty);
        assert(DebugConfig.useInMemoryLdapServer == useInMemoryLdapServer);
        useInMemoryLdapServer = InMemoryLdapServer.isOn();
        
        ZimbraLog.test.info("useInMemoryLdapServer = " + useInMemoryLdapServer);
        

        if (useInMemoryLdapServer) {
            try {
                InMemoryLdapServer.start(InMemoryLdapServer.ZIMBRA_LDAP_SERVER, 
                        new InMemoryLdapServer.ServerConfig(
                        Lists.newArrayList(LdapConstants.ATTR_DC + "=" + LDAP_TEST_BASE_DOMAIN)));
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        */
        if (useInMemoryLdapServerOverride != null) {
            boolean useInMemoryLdapServer = 
                    Boolean.parseBoolean(useInMemoryLdapServerOverride);
            
            KnownKey key = new KnownKey("debug_use_in_memory_ldap_server", 
                    useInMemoryLdapServerOverride);
            if (DebugConfig.useInMemoryLdapServer != useInMemoryLdapServer) {
                System.out.println("useInMemoryLdapServerOverride is " + useInMemoryLdapServerOverride +
                        " but LC key debug_use_in_memory_ldap_server is " + key.value() + 
                        ".  Remove the value from LC key.");
                fail();
            }
        }
        ZimbraLog.test.info("useInMemoryLdapServer = " + InMemoryLdapServer.isOn());

        RightManager.getInstance(true);
        
        Cleanup.deleteAll();
    }

}
