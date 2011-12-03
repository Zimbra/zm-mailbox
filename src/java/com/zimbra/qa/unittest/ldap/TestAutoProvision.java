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

import com.google.common.collect.Maps;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DirectoryEntryVisitor;
import com.zimbra.cs.account.Provisioning.EagerAutoProvisionScheduler;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.ldap.AutoProvisionListener;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.type.AutoProvPrincipalBy;

public class TestAutoProvision extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
    private static Domain extDomain;
    private static String extDomainDn;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        extDomain = provUtil.createDomain("external." + baseDomainName());
        extDomainDn = LdapUtil.domainToDN(extDomain.getName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private String getTestName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
    
    private String getZimbraDomainName(String testName) {
        return testName + "." + baseDomainName();
    }
    
    private Domain createZimbraDomain(String testName, Map<String, Object> zimbraDomainAttrs) 
    throws Exception {
        return provUtil.createDomain(getZimbraDomainName(testName), zimbraDomainAttrs);
    }
    
    private String createExternalAcctEntry(String localPart) throws Exception {
        return createExternalAcctEntry(localPart, null);
    }
    
    private String createExternalAcctEntry(String localPart, Map<String, Object> attrs) throws Exception {
        return createExternalAcctEntry(localPart, null, attrs);
    }
    
    private String createExternalAcctEntry(String localPart, String externalPassword, 
            Map<String, Object> attrs) throws Exception {
        String extAcctName = TestUtil.getAddress(localPart, extDomain.getName());
        
        Map<String, Object> extAcctAttrs = attrs == null ? new HashMap<String, Object>() : attrs;
        
        extAcctAttrs.put(Provisioning.A_displayName, "display name");
        extAcctAttrs.put(Provisioning.A_sn, "last name");
        Account extAcct = prov.createAccount(extAcctName, externalPassword, extAcctAttrs);
        return extAcctName;
    }
    
    private static void modifyExternalAcctEntry(String externalDN, Map<String, Object> extAcctAttrs) 
    throws Exception {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UNITTEST);
        try {
            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(extAcctAttrs);
            zlc.replaceAttributes(externalDN, entry.getAttributes());
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private String externalEntryDN(String externalEntryLocalPart) {
        return String.format("uid=%s,ou=people,%s", externalEntryLocalPart, extDomainDn);
    }
    
    
    /* ================
     * LAZY mode tests
     * ================
     */
    
    @Test
    public void lazyModeGetExternalEntryByDNTemplate() throws Exception {
        String testName = getTestName();
        
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // uid=bydntemplate,ou=people,dc=external,dc=com,dc=zimbra,dc=qa,dc=unittest,dc=testldapprovautoprovision
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapBindDn, "uid=%u,ou=people," + extDomainDn);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
               
        String loginName = extAcctLocalPart;
        Account acct = prov.autoProvAccountLazy(zimbraDomain, loginName, null, AutoProvAuthMech.LDAP);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct);
    }
    
    @Test
    public void lazyModeGetExternalEntryBySearch() throws Exception {
        String testName = getTestName();
        
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        String loginName = extAcctLocalPart;
        Account acct = prov.autoProvAccountLazy(zimbraDomain, loginName, null, AutoProvAuthMech.LDAP);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct);
    }
    
    public static class TestListener implements AutoProvisionListener {
        private static TestListener instance;
        
        private Account acct;
        private String externalDN;
        
        public TestListener() {
            assertNull(instance);
            instance = this;  // remember the instance created from auto provision engine
        }
        
        @Override
        public void postCreate(Domain domain, Account acct, String externalDN) {
            // rememebr the acct and external DN for verification
            this.acct = acct;
            this.externalDN = externalDN;
            
        }
        
        private static TestListener getInstance() {
            return instance;
        }
    }
    
    /**
     * A AutoProvisionListener that marks entry "provisioned" in the external directory
     *
     */
    public static class MarkEntryProvisionedListener implements AutoProvisionListener {
        private static final String PROVED_INDICATOR_ATTR = Provisioning.A_zimbraNotes;
        private static final String PROVED_NOTE = "PROVISIONIN IN ZIMBRA";
        private static final String NOT_PROVED_FILTER = "(!(" + PROVED_INDICATOR_ATTR + "=" + PROVED_NOTE + "))";
        
        public MarkEntryProvisionedListener() {
        }
        
        @Override
        public void postCreate(Domain domain, Account acct, String externalDN) {
            Map<String, Object> attrs = Maps.newHashMap();
            attrs.put(PROVED_INDICATOR_ATTR, PROVED_NOTE);
            try {
                modifyExternalAcctEntry(externalDN, attrs);
            } catch (Exception e) {
                fail();
            }
        }
    }

    
    @Test
    public void lazyModeListener() throws Exception {
        String testName = getTestName();
        
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        
        // com.zimbra.qa.unittest.ldap.TestAutoProvision$TestListener
        String className = TestListener.class.getName();
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvListenerClass, className);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        String loginName = extAcctLocalPart;
        Account acct = prov.autoProvAccountLazy(zimbraDomain, loginName, null, AutoProvAuthMech.LDAP);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct);
        
        TestListener listener = TestListener.getInstance();
        assertNotNull(listener);
        assertEquals(acct.getId(), listener.acct.getId());
        assertEquals(externalEntryDN(extAcctLocalPart).toLowerCase(), listener.externalDN);
    }
    
    
    @Test
    public void lazyModeAccountLocalpartMap() throws Exception {
        String testName = getTestName();
        
        String extAcctLocalPart = testName;
        String zimbraAcctLocalpart = "myzimbraname";
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_description, zimbraAcctLocalpart);  // going to be the local part of the zimrba account name
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, acctAttrs);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, Provisioning.A_description);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        String loginName = extAcctLocalPart;
        Account acct = prov.autoProvAccountLazy(zimbraDomain, loginName, null, AutoProvAuthMech.LDAP);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct, zimbraAcctLocalpart + "@" + zimbraDomain.getName());
    }
    
    @Test
    public void lazyModeAutoProvNotEnabledByAuthMech() throws Exception {
        String testName = getTestName();
        
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapBindDn, "uid=%u,ou=people," + extDomainDn);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
               
        String loginName = extAcctLocalPart;
        Account acct = prov.autoProvAccountLazy(zimbraDomain, loginName, null, AutoProvAuthMech.PREAUTH);
        assertNull(acct);
    }
    
    
    @Test
    public void lazyModeExternalLdapAuth() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        // setup external LDAP auth
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthMech, AuthMech.ldap.name());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapURL, "ldap://localhost:389");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAuthLdapBindDn, "uid=%u,ou=people," + extDomainDn);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        // try auto provisioning with bad password
        String loginName = extAcctLocalPart;
        Account acct = prov.autoProvAccountLazy(zimbraDomain, loginName, externalPassword+"bad", null);
        assertNull(acct);
        
        // try again with correct password
        acct = prov.autoProvAccountLazy(zimbraDomain, loginName, externalPassword, null);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct);
    }
    
    
    /* ==================
     * MANUAL mode tests
     * ==================
     */
    
    @Test
    public void manualModeByName() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart = testName;
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        String principal = extAcctLocalPart;
        String password = "test123";
        Account acct = prov.autoProvAccountManual(zimbraDomain, AutoProvPrincipalBy.name, 
                principal, password);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct);
        
        // make sure the provided password, instead of the external password is used
        prov.authAccount(acct, password, AuthContext.Protocol.test);
        
        boolean caughtException = false;
        try {
            prov.authAccount(acct, externalPassword, AuthContext.Protocol.test);
        } catch (ServiceException e) {
            if (AccountServiceException.AUTH_FAILED.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
    }
    
    @Test
    public void manualModeByDN() throws Exception {
        String testName = getTestName();
        
        String extAcctLocalPart = testName;
        String zimbraAcctLocalpart = "myzimbraname";
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_description, zimbraAcctLocalpart);  // going to be the local part of the zimrba account name
        String extAcctName = createExternalAcctEntry(extAcctLocalPart, acctAttrs);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, Provisioning.A_description);
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        String principal = externalEntryDN(extAcctLocalPart);
        String password = null;
        Account acct = prov.autoProvAccountManual(zimbraDomain, AutoProvPrincipalBy.dn, 
                principal, password);
        AutoProvisionTestUtil.verifyAcctAutoProvisioned(acct, zimbraAcctLocalpart + "@" + zimbraDomain.getName());
    }
    
    @Test 
    public void searchAutoProvDirectoryByName() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart1 = testName + "_1";
        String extAcctLocalPart2 = testName + "_2";
        String extAcctLocalPart3 = testName + "_3";
        createExternalAcctEntry(extAcctLocalPart1, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart2, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart3, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        final Set<String> entriesFound = new HashSet<String>();
        DirectoryEntryVisitor visitor = new DirectoryEntryVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs) {
                entriesFound.add(dn);
            }
        };
        
        prov.searchAutoProvDirectory(zimbraDomain, null, extAcctLocalPart2, 
                null, 0, visitor);
        
        assertEquals(1, entriesFound.size());
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart2).toLowerCase()));
    }
    
    @Test 
    public void searchAutoProvDirectoryByProvidedFilter() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart1 = testName + "_1";
        String extAcctLocalPart2 = testName + "_2";
        String extAcctLocalPart3 = testName + "_3";
        createExternalAcctEntry(extAcctLocalPart1, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart2, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart3, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov - no need
        // zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        // zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(uid=%u)");
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        final Set<String> entriesFound = new HashSet<String>();
        DirectoryEntryVisitor visitor = new DirectoryEntryVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs) {
                entriesFound.add(dn);
            }
        };
        
        prov.searchAutoProvDirectory(zimbraDomain, 
                String.format("(mail=%s*)", testName),
                null, null, 0, visitor);
        
        assertEquals(3, entriesFound.size());
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart1).toLowerCase()));
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart2).toLowerCase()));
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart3).toLowerCase()));
    }
    
    @Test 
    public void searchAutoProvDirectoryByConfiguredSearchFilter() throws Exception {
        String testName = getTestName();
        
        String externalPassword = "test456";
        String extAcctLocalPart1 = testName + "_1";
        String extAcctLocalPart2 = testName + "_2";
        String extAcctLocalPart3 = testName + "_3";
        createExternalAcctEntry(extAcctLocalPart1, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart2, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart3, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, "(&(uid=%u)(mail=searchAutoProvDirectoryByConfiguredSearchFilter*))");
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
        
        final Set<String> entriesFound = new HashSet<String>();
        DirectoryEntryVisitor visitor = new DirectoryEntryVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs) {
                entriesFound.add(dn);
            }
        };
        
        prov.searchAutoProvDirectory(zimbraDomain, null, null, 
                null, 0, visitor);
        
        assertEquals(3, entriesFound.size());
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart1).toLowerCase()));
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart2).toLowerCase()));
        assertTrue(entriesFound.contains(externalEntryDN(extAcctLocalPart3).toLowerCase()));
    }
    
    
    /* =================
     * EAGER mode tests
     * =================
     */
    @Test
    public void eagerMode() throws Exception {
        String testName = getTestName();
        
        final String externalPassword = "test456";
        
        
        int totalAccts = 4;
        String extAcctLocalPart1 = testName + "_1";
        String extAcctLocalPart2 = testName + "_2";
        String extAcctLocalPart3 = testName + "_3";
        String extAcctLocalPart4 = testName + "_4";
        createExternalAcctEntry(extAcctLocalPart1, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart2, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart3, externalPassword, null);
        createExternalAcctEntry(extAcctLocalPart4, externalPassword, null);
        
        Map<String, Object> zimbraDomainAttrs = AutoProvisionTestUtil.commonZimbraDomainAttrs();
        // setup auto prov
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchBase, extDomainDn);
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvLdapSearchFilter, 
                "(&(uid=%u)(mail=eagerMode*)" + MarkEntryProvisionedListener.NOT_PROVED_FILTER + ")");
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvAccountNameMap, Provisioning.A_uid);
        
        // set batch size to a smaller number then num account matching the filter, 
        // so we hit the TOO_MANY_SEARCH_RESULTS (bug 66605)
        int batchSize = totalAccts - 1;
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvBatchSize, Integer.valueOf(batchSize).toString());
        zimbraDomainAttrs.put(Provisioning.A_zimbraAutoProvListenerClass, 
                MarkEntryProvisionedListener.class.getName());
        Domain zimbraDomain = createZimbraDomain(testName, zimbraDomainAttrs);
                
        // schedule the domain on local server
        prov.getLocalServer().addAutoProvScheduledDomains(zimbraDomain.getName());
        
        EagerAutoProvisionScheduler scheduler = new EagerAutoProvisionScheduler() {
            @Override
            public boolean isShutDownRequested() {
                return false;
            }
        };
        
        prov.autoProvAccountEager(scheduler);

        List<Account> zimbraAccts = prov.getAllAccounts(zimbraDomain);
        assertEquals(batchSize, zimbraAccts.size());
        
        // do it again, this time the 4th account should be provisioned
        prov.autoProvAccountEager(scheduler);
        zimbraAccts = prov.getAllAccounts(zimbraDomain);
        assertEquals(totalAccts, zimbraAccts.size());
        
        
        Set<String> acctNames = new HashSet<String>();
        for (Account acct : zimbraAccts) {
            acctNames.add(acct.getName());
        }
        
        assertTrue(acctNames.contains(TestUtil.getAddress(extAcctLocalPart1, zimbraDomain.getName()).toLowerCase()));
        assertTrue(acctNames.contains(TestUtil.getAddress(extAcctLocalPart2, zimbraDomain.getName()).toLowerCase()));
        assertTrue(acctNames.contains(TestUtil.getAddress(extAcctLocalPart3, zimbraDomain.getName()).toLowerCase()));
        assertTrue(acctNames.contains(TestUtil.getAddress(extAcctLocalPart4, zimbraDomain.getName()).toLowerCase()));
        
        // clear scheduled domains on the local server
        prov.getLocalServer().unsetAutoProvScheduledDomains();
    }
   
}
