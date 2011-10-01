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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.io.IOException;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.runner.JUnitCore;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.qa.unittest.LdapSuite.ConsoleListener;

public class TestLdap {
    
    // ensure assertion is enabled
    static {
        boolean assertsEnabled = false;
        assert assertsEnabled = true; // Intentional side effect!!!
        if (!assertsEnabled) {
            throw new RuntimeException("Asserts must be enabled!!!");
        }
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
        UBID(com.zimbra.cs.ldap.unboundid.UBIDLdapClient.class, com.zimbra.cs.account.ldap.LdapProvisioning.class),
        JNDI(com.zimbra.cs.ldap.jndi.JNDILdapClient.class, com.zimbra.cs.account.ldap.LdapProvisioning.class),
        LEGACY(null, com.zimbra.cs.account.ldap.legacy.LegacyLdapProvisioning.class);
        
        static private TestConfig currentTestConfig = null;
        
        private Class ldapClientClass;
        private Class ldapProvClass;
        
        private TestConfig(Class ldapClientClass, Class ldapProvClass) {
            this.ldapClientClass = ldapClientClass;
            this.ldapProvClass = ldapProvClass;
        }
        
        static synchronized void useConfig(TestConfig config) throws Exception {
            if (currentTestConfig != null) {
                fail("TestConfig.useConfig cann only be called once per JVM");
            }
            
            currentTestConfig = config;
            
            if (config.ldapClientClass != null) {
                modifyLocalConfig(LC.zimbra_class_ldap_client.key(), config.ldapClientClass.getCanonicalName());
            } else {
                // remove the key
                modifyLocalConfig(LC.zimbra_class_ldap_client.key(), "");
            }
            modifyLocalConfig(LC.zimbra_class_provisioning.key(), config.ldapProvClass.getCanonicalName());
            LC.reload();
        }
        
        static synchronized TestConfig getCurrentTestConfig() {
            if (currentTestConfig == null) {
                fail("TestConfig has not been initialized");
            }
            
            return currentTestConfig;
        }
        
    }

    
    /*
     * given a domain name like test.com, delete the entire tree under 
     * dc=com in LDAP
     */
    public static void deleteEntireBranch(String domainName) throws Exception {
        String parts[] = domainName.split("\\.");
        String[] dns = ((LdapProv) Provisioning.getInstance()).getDIT().domainToDNs(parts);
        String topMostRDN = dns[dns.length-1];
        TestLdap.deleteEntireBranchByDN(topMostRDN);
        
        cleanupAll();
    }
    
    private static void cleanupAll() throws Exception {
        deleteAllNonDefaultCoses();
        deleteAllNonDefaultServers();
        deleteAllXMPPComponents();
    }
    
    private static void deleteAllNonDefaultCoses() throws Exception {
        LdapDIT dit = ((LdapProv) Provisioning.getInstance()).getDIT();
        String cosBaseDN = dit.cosBaseDN();
        
        Set<String> defaultCosDN = new HashSet<String>();
        defaultCosDN.add(dit.cosNametoDN(Provisioning.DEFAULT_COS_NAME));
        defaultCosDN.add(dit.cosNametoDN(Provisioning.DEFAULT_EXTERNAL_COS_NAME));

        deleteAllChildrenUnderDN(cosBaseDN, defaultCosDN);
    }
    
    private static void deleteAllNonDefaultServers() throws Exception {
        LdapProv ldapProv = LdapProv.getInst();
        LdapDIT dit = ldapProv.getDIT();
        String serverBaseDN = dit.serverBaseDN();
        
        Set<String> defaultServerDN = new HashSet<String>();
        defaultServerDN.add(dit.serverNameToDN(ldapProv.getLocalServer().getName()));
        
        deleteAllChildrenUnderDN(serverBaseDN, defaultServerDN);
    }
    
    private static void deleteAllXMPPComponents() throws Exception {
        String xmppBaseDN = ((LdapProv) Provisioning.getInstance()).getDIT().xmppcomponentBaseDN();
        deleteAllChildrenUnderDN(xmppBaseDN, null);
    }

    
    // dn itself will also be deleted
    private static void deleteEntireBranchByDN(String dn) throws Exception {
        ZLdapContext zlc = null;
        
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            deleteEntireBranch(zlc, dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    // dn itself will not be deleted
    private static void deleteAllChildrenUnderDN(String dn, Set<String> ignoreDNs) throws Exception {
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            List<String> childrenDNs = getDirectChildrenDNs(zlc, dn);
            for (String childDN : childrenDNs) {
                if (ignoreDNs == null || !ignoreDNs.contains(childDN)) {
                    deleteEntireBranch(zlc, childDN);
                }
            }
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
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
            zlc.deleteEntry(dn);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private static boolean isLeaf(ZLdapContext zlc, String dn) throws Exception {
        return getDirectChildrenDNs(zlc, dn).size() == 0;
    }
    
    private static List<String> getDirectChildrenDNs(ZLdapContext zlc, String dn) throws Exception {
        final List<String> childrenDNs = new ArrayList<String>();

        ZLdapFilter filter = ZLdapFilterFactory.getInstance().anyEntry();
        
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_ONELEVEL, 
                ZSearchControls.SIZE_UNLIMITED, new String[]{"objectClass"});
        
        ZSearchResultEnumeration sr = zlc.searchDir(dn, filter, searchControls);
        while (sr.hasMore()) {
            ZSearchResultEntry entry = sr.next();
            childrenDNs.add(entry.getDN());
        }
        sr.close();
        
        return childrenDNs;
    }
    
    /**
     * Given a name (which is to be turn into a DN), mix in chars 
     * defined in rfc2253.txt that need to be escaped in RDN value.
     * 
     * http://www.ietf.org/rfc/rfc2253.txt?number=2253
     * 
     * - a space or "#" character occurring at the beginning of the
     *   string
     *
     * - a space character occurring at the end of the string
     *
     * - one of the characters ",", "+", """, "\", "<", ">" or ";"
     * 
     * Implementations MAY escape other characters.
     *
     * If a character to be escaped is one of the list shown above, then it
     * is prefixed by a backslash ('\' ASCII 92).
     *
     * Otherwise the character to be escaped is replaced by a backslash and
     * two hex digits, which form a single byte in the code of the
     * character.
     * 
     * @param name
     * @return
     */    
    private static String makeRFC2253Name(String name, boolean wantTrailingBlank) {
        String LEADING_CHARS = "#";
        String TRAILING_CHARS = " ";
        String BACKSLASH_ESCAPED_CHARS = "# ,+\"\\<>;";
        String UNICODE_CHARS = "\u4e2d\u6587";
        
        if (wantTrailingBlank) {
            return LEADING_CHARS + BACKSLASH_ESCAPED_CHARS + DOT_ATOM_CHARS + UNICODE_CHARS + "---" + name + TRAILING_CHARS;
        } else {
            return LEADING_CHARS + BACKSLASH_ESCAPED_CHARS + DOT_ATOM_CHARS + UNICODE_CHARS + "---" + name;
        }
    }
    
    // RFC 2822
    private static final String ATOM_CHARS = "!#$%&'*+-/=?^_`{|}~";   
    private static final String DOT_ATOM_CHARS = "." + ATOM_CHARS;
    
    private static String makeRFC2253NameEmailLocalPart(String name) {
        String LEADING_CHAR = "#";
        return LEADING_CHAR + DOT_ATOM_CHARS + "---" + name;
    }
    
    private static String makeRFC2253NameDomainName(String name) {
        String UNICODE_CHARS = "\u4e2d\u6587";
        
        // hmm, javamail does not like any of the ATOM_CHARS 
        return /* ATOM_CHARS + */ UNICODE_CHARS + "---" + name;
    }

    static String makeAccountNameLocalPart(String localPart) {
        return makeRFC2253NameEmailLocalPart(localPart);
    }

    static String makeAliasNameLocalPart(String localPart) {
        return makeRFC2253NameEmailLocalPart(localPart);
    }

    static String makeCosName(String name) {
        return makeRFC2253Name(name, false);
    }

    static String makeDataSourceName(String name) {
        // historically we allow trailing blank in data source name
        // should probably make it consistent across the board.
        return makeRFC2253Name(name, true);
    }
    
    static String makeDLNameLocalPart(String localPart) {
        return makeRFC2253NameEmailLocalPart(localPart);
    }
    
    static String makeDomainName(String name) {
        return TestLdap.makeRFC2253NameDomainName(name);
    }
    
    static String makeIdentityName(String name) {
        // historically we allow trailing blank in identity name
        // should probably make it consistent across the board.
        return makeRFC2253Name(name, true);
    }
    
    static String makeServerName(String name) {
        return makeRFC2253Name(name, false);
    }
    
    static String makeSignatureName(String name) {
        return makeRFC2253Name(name, false);
    }
    
    static String makeXMPPName(String name) {
        return makeRFC2253Name(name, false);
    }
    
    static String makeZimletName(String name) {
        return makeRFC2253Name(name, false);
    }
    

    private static boolean batchMode = false;
    
    //
    // TODO: merge with LdapSuite
    //
    private static void runTests(JUnitCore junit, TestConfig testConfig) throws Exception {
        batchMode = true;
         
        initTest(testConfig);
        
        Date startTime = new Date();
        SimpleDateFormat dateFmt = new SimpleDateFormat("HH:mm:ss");
        System.out.println("TestLdap started at: " + dateFmt.format(startTime));
        
        if (testConfig == TestConfig.UBID) {
            // junit.run(TestLdapSDK.class);
        }
        
        junit.run(TestLdapHelper.class);
        junit.run(TestLdapProvAccount.class);
        junit.run(TestLdapProvAlias.class);
        junit.run(TestLdapProvAutoProvision.class);
        junit.run(TestLdapProvCos.class);
        junit.run(TestLdapProvDataSource.class);
        junit.run(TestLdapProvDistributionList.class);
        junit.run(TestLdapProvDIT.class);
        junit.run(TestLdapProvDomain.class);
        junit.run(TestLdapProvEntry.class);
        junit.run(TestLdapProvExternalLdapAuth.class);
        junit.run(TestLdapProvGal.class);
        junit.run(TestLdapProvGlobalConfig.class);
        junit.run(TestLdapProvGlobalGrant.class);
        junit.run(TestLdapProvIdentity.class);
        junit.run(TestLdapProvMimeType.class);
        junit.run(TestLdapProvMisc.class);
        junit.run(TestLdapProvModifyAttrs.class);
        junit.run(TestLdapProvRenameDomain.class);
        junit.run(TestLdapProvServer.class);
        junit.run(TestLdapProvSignature.class);
        junit.run(TestLdapProvXMPPComponent.class);
        junit.run(TestLdapProvZimlet.class);
        junit.run(TestLdapUtil.class);
        junit.run(TestLdapUpgrade.class);
        junit.run(TestLdapZLdapContext.class);
        junit.run(TestLdapZLdapFilter.class);
        junit.run(TestLdapZMutableEntry.class);
        junit.run(TestProvAttrCallback.class);
        
        // old tests, TODO: convert them 
        junit.run(TestAccountLockout.class);
        junit.run(TestACPermissionCache.class);
        junit.run(TestACUserRights.class);
        junit.run(TestBuildInfo.class);
        junit.run(TestLdapBinary.class);
        junit.run(TestLdapUtil.class);
        junit.run(TestProvAlias.class);
        junit.run(TestProvAttr.class);
        junit.run(TestProvCallbackAvailableZimlets.class);
        junit.run(TestProvCos.class);
        junit.run(TestProvGroup.class);
        junit.run(TestProvIDN.class);
        junit.run(TestProvValidator.class);
        junit.run(TestProvZimbraId.class);
        junit.run(TestSearchCalendarResources.class);
        junit.run(TestSearchGal.class);
        
        /*
         * tests in extensions - don't forget to run them:
         * 
         * NginxLookupExtensionTest
         * TestGetSMIMEPublicCerts
         * TestLdapSMIMELookup
         */
        
        /*
         * ACL tests
         * 
         * TestACBasic
         * TestACEffectiveRights
         * TestACLAttrRight
         * TestACLGrant
         * TestACLGrantee
         * TestACLPrecedence
         * TestACLRight
         * TestACLTarget
         * TestACPermissionCache
         * TestACUserRights
         * 
         */
        /*  // tests need fixing
        junit.run(TestCreateAccount.class);
        junit.run(TestProvDomainStatus.class);
        */
        
        Date endTime = new Date();
        System.out.println("TestLdap ended at:   " + dateFmt.format(endTime));
        
        long elapsedMills = endTime.getTime() - startTime.getTime();
        long minutes = elapsedMills / Constants.MILLIS_PER_MINUTE;
        long seconds = elapsedMills / Constants.MILLIS_PER_SECOND;
        long secs = seconds - (minutes * Constants.SECONDS_PER_MINUTE);
        System.out.println();
        System.out.println("Elapsed time: " + minutes + " mins " + secs + " secs");
    }
    
    // invoked once per JVM
    private static void initTest(TestConfig testConfig) throws Exception {
        CliUtil.toolSetup();
        // ZimbraLog.account.setLevel(Log.Level.debug);
        ZimbraLog.ldap.setLevel(Log.Level.debug);
        // ZimbraLog.soap.setLevel(Log.Level.trace);
        
        RightManager.getInstance(true);
        TestConfig.useConfig(testConfig);
        cleanupAll();
    }
    
    @BeforeClass  // invoked once per class loaded
    public static void beforeClass() throws Exception {
        if (batchMode) {
            return;
        }
        
        TestConfig testConfig = TestConfig.UBID;
        // TestConfig testConfig = TestConfig.JNDI;
        // TestConfig testConfig = TestConfig.LEGACY;
        
        initTest(testConfig);
    }
    
    static TestConfig getCurrentTestConfig() {
        return TestConfig.getCurrentTestConfig();
    }
    
    /*
     * zmjava -ea com.zimbra.qa.unittest.TestLdap > ~/temp/out.txt
     * 
     * cp -f /Users/pshao/p4/main/ZimbraServer/data/unittest/ldap/attrs-unittest.xml /Users/pshao/p4/main/ZimbraServer/conf/attrs
     * ant refresh-ldap-schema
     * 
     * cp -f /Users/pshao/p4/main/ZimbraServer/data/unittest/ldap/rights-unittest.xml /opt/zimbra/conf/rights
     * 
     */
    public static void main(String[] args) throws Exception {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new ConsoleListener());
        
        TestConfig.useConfig(TestConfig.LEGACY);
        
        // runTests(junit, TestConfig.UBID);
        // runTests(junit, TestConfig.JNDI);
        // runTests(junit, TestConfig.LEGACY);
        
        System.out.println();
        System.out.println("=== Finished ===");
    }

}
