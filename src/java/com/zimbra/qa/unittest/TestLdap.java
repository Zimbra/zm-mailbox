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
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.io.IOException;

import org.junit.runner.JUnitCore;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.prov.ldap.LdapHelper;
import com.zimbra.cs.prov.ldap.LdapProv;
import com.zimbra.cs.prov.ldap.entry.LdapCos;
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
        junit.run(TestLdapProvDomain.class);
        junit.run(TestLdapProvEntry.class);
        junit.run(TestLdapProvGlobalConfig.class);
        junit.run(TestLdapUtil.class);
        junit.run(TestLdapZMutableEntry.class);
    }
    
    // so tests can be called directly, without running from TestLdap.
    static void manualInit() throws Exception {
        
        CliUtil.toolSetup();
        // TestConfig.useConfig(TestConfig.UBID);
        // TestConfig.useConfig(TestConfig.JNDI);
        // TestConfig.useConfig(TestConfig.LEGACY);
    }
    
    static void deleteEntireBranchInDIT(String dn) throws Exception {
        ZLdapContext zlc = null;
        
        try {
            zlc = LdapClient.getContext();
            deleteEntireBranch(zlc, dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private static void deleteEntireBranch(ZLdapContext zlc, String dn) throws Exception {
        
        if (isLeaf(zlc, dn)) {
            deleteEntry(dn);
            return;
        }
        
        List<String> childrenDNs = getDirectChildrenDNs(zlc, dn);
        for (String childDN : childrenDNs) {
            deleteEntireBranch(zlc, childDN);
        }
        deleteEntry(dn);
    }
    
    private static void deleteEntry(String dn) throws Exception {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER);
            zlc.unbindEntry(dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private static boolean isLeaf(ZLdapContext zlc, String dn) throws Exception {
        return getDirectChildrenDNs(zlc, dn).size() == 0;
    }
    
    private static List<String> getDirectChildrenDNs(ZLdapContext zlc, String dn) throws Exception {
        final List<String> childrenDNs = new ArrayList<String>();

        String query = "(objectClass=*)";
        
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_ONELEVEL, 
                ZSearchControls.SIZE_UNLIMITED, new String[]{"objectClass"});
        
        ZSearchResultEnumeration sr = zlc.searchDir(dn, "(objectClass=*)", searchControls);
        while (sr.hasMore()) {
            ZSearchResultEntry entry = sr.next();
            childrenDNs.add(entry.getDN());
        }
        sr.close();
        
        return childrenDNs;
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
