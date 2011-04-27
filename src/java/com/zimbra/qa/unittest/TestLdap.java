package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.runner.JUnitCore;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.qa.unittest.LdapSuite.ConsoleListener;

public class TestLdap {
    
    // ensure assertion is enabled
    static {
        boolean assertsEnabled = false;
        assert assertsEnabled = true; // Intentional side effect!!!
        if (!assertsEnabled)
            throw new RuntimeException("Asserts must be enabled!!!");
    } 
    
    static void modifyLocalConfig(String key, String value) throws Exception {
        Process process = null;
        try {
            String command = "/opt/zimbra/bin/zmlocalconfig -e " + key + "=" + value;
            System.out.println(command);
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } 
        
        int exitCode;
        try {
            exitCode = process.waitFor();
            assertEquals(0, exitCode);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw e;
        } 
        
    }

    enum TestConfig {
        UBID(com.zimbra.cs.ldap.unboundid.UBIDLdapClient.class, com.zimbra.cs.prov.ldap.LdapProvisioning.class),
        JNDI(com.zimbra.cs.ldap.jndi.JNDILdapClient.class, com.zimbra.cs.prov.ldap.LdapProvisioning.class),
        LEGACY(null, com.zimbra.cs.account.ldap.LdapProvisioning.class);
        
        private Class ldapClientClass;
        private Class ldapProvClass;
        
        private TestConfig(Class ldapClientClass, Class ldapProvClass) {
            this.ldapClientClass = ldapClientClass;
            this.ldapProvClass = ldapProvClass;
        }
        
        static void useConfig(TestConfig config) throws Exception {
            if (config.ldapClientClass != null) {
                modifyLocalConfig(LC.zimbra_class_ldap_client.key(), config.ldapClientClass.getCanonicalName());
            } else {
                // remove the key
                modifyLocalConfig(LC.zimbra_class_ldap_client.key(), "");
            }
            modifyLocalConfig(LC.zimbra_class_provisioning.key(), config.ldapProvClass.getCanonicalName());
            LC.reload();
        }
    }

    //
    // TODO: merge with LdapSuite
    //
    private static void runTests(JUnitCore junit, TestConfig testConfig) throws Exception {
        TestConfig.useConfig(testConfig);
        
        if (testConfig == TestConfig.UBID) {
            junit.run(TestLdapSDK.class);
        }
        junit.run(TestLdapHelper.class);
        junit.run(TestLdapProvEntry.class);
        junit.run(TestLdapProvGlobalConfig.class);
        junit.run(TestLdapZMutableEntry.class);
    }
    
    // so tests can be called directly, without running from TestLdap.
    static void manualInit() throws Exception {
        CliUtil.toolSetup();
        // TestConfig.useConfig(TestConfig.UBID);
        // TestConfig.useConfig(TestConfig.JNDI);
        // TestConfig.useConfig(TestConfig.LEGACY);
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        
        JUnitCore junit = new JUnitCore();
        junit.addListener(new ConsoleListener());
        
        // runTests(junit, TestConfig.UBID);
        // runTests(junit, TestConfig.JNDI);
        // runTests(junit, TestConfig.LEGACY);
    }
}
