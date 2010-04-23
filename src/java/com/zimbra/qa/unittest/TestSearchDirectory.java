/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.SearchOptions;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestSearchDirectory extends TestCase {
    
    private static String TEST_NAME = "test-sd";
    private static String DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, null);
    
    private static final String PASSWORD = "test123";

    /*
     * these are what passed from admin console
     */
    private static final String FILTER_1 = "(|(uid=*%n*)(cn=*%n*)(sn=*%n*)(gn=*%n*)(displayName=*%n*)(zimbraId=%n)(mail=*%n*)(zimbraMailAlias=*%n*)(zimbraMailDeliveryAddress=*%n*)(zimbraDomainName=*%n*))";
    private static final String[] ATTRS = new String[]{"displayName", "zimbraId", "zimbraMailHost", "uid", "zimbraAccountStatus", "zimbraLastLogonTimestamp", "description", "zimbraMailStatus", "zimbraCalResType", "zimbraDomainType", "zimbraDomainName"};

    private static void createAccounts(Provisioning prov, String domainName, int numAccounts) throws Exception {
        
        if (prov == null)
            prov = Provisioning.getInstance();
        
        Domain domain = prov.get(DomainBy.name, domainName);
        if (domain == null)
            domain = prov.createDomain(domainName, new HashMap<String, Object>());
        
        for (int i = 0; i < numAccounts; i++) {
            String acctName = "user-" + (i+1) + "@" + domainName;
            
            Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
            assertNotNull(acct);
            
            if ((i+1) % 100 == 0)
                System.out.println("Created " + (i+1) + " accounts");
        }
    }
    
    private static Account createDelegatedAdmin(Provisioning prov, String acctName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        return prov.createAccount(acctName, PASSWORD, attrs);
    }
    
    
    private static void assertEquals(Set<String> expected, List<NamedEntry> actual) {
        assertEquals(expected.size(), actual.size());
        
        for (NamedEntry entry : actual) {
            assertTrue(expected.contains(entry.getName()));
        }
    }
    
    private String expandFilter(String filter, String key) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("n", key); 
        return LdapProvisioning.expandStr(filter, vars);
    }
    
    private List searchDirectory(String filter, String key, boolean expectTooMany) throws Exception {
        int flags = 0;
        flags |= Provisioning.SA_ACCOUNT_FLAG;
        flags |= Provisioning.SA_ALIAS_FLAG;
        flags |= Provisioning.SA_DISTRIBUTION_LIST_FLAG;
        flags |= Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        flags |= Provisioning.SA_DOMAIN_FLAG;
        
        String query = expandFilter(filter, key);
        
        SearchOptions options = new SearchOptions();
        options.setDomain(null);
        options.setFlags(flags);
        options.setMaxResults(5000);
        options.setQuery(query);
        options.setReturnAttrs(ATTRS);
        options.setSortAscending(true);
        options.setSortAttr("name");
        options.setConvertIDNToAscii(true);
        
        List results = null;
        boolean good = false;
        try {
            results = Provisioning.getInstance().searchDirectory(options);
            good = true;
        } catch (AccountServiceException e) {
            if (expectTooMany && e.getCode().equals(AccountServiceException.TOO_MANY_SEARCH_RESULTS))
                good = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        assertTrue(good);
        return results;
    }
    
    public void disable_testSearchDirectory() throws Exception {
        List results;
        
        /*
         * bring in cos and domain so they don't need to be read during searchDirectory
         * and flood our connection pool debug
         */
        
        Domain domain1 = Provisioning.getInstance().get(Provisioning.DomainBy.name, "test-gal.ldaptest");
        Domain domain2 = Provisioning.getInstance().get(Provisioning.DomainBy.name, "goodbyewhen-lm-corp-yahoo-com-2.local");
        Cos cos = Provisioning.getInstance().get(Provisioning.CosBy.name, "default");
        
        System.out.flush();
        for (int i=0; i<10; i++) {
            System.out.flush();
            System.out.println("iteration " + (i+1));
            System.out.flush();
            
            System.out.println("search all");  System.out.flush();
            results = searchDirectory(FILTER_1, "user", true);
            
            System.out.println("search one");  System.out.flush();
            results = searchDirectory(FILTER_1, "user-10001", false);
            assertEquals(1, results.size());
            Account acct = (Account)results.get(0);
            assertEquals("user-10001", acct.getName().substring(0, 10));
        }
    }
    
    private static class Bug39514 {
        
        private static final int numAcctsInDomain1 = 5;
        private static final int numAcctsInDomain2 = 10;
        
        private static boolean verbose = false;
        
        private static class TestData {
            String mAdminName;
            String mAdminPassword;
            Integer mExpectedNumEntriesFound;
            
            TestData(String adminName, String adminPassword, Integer expectedNumEntriesFound) {
                mAdminName = adminName;
                mAdminPassword = adminPassword;
                mExpectedNumEntriesFound = expectedNumEntriesFound;
            }
        }
        
        private static List<TestData> setup() throws Exception {
            Provisioning prov = TestProvisioningUtil.getSoapProvisioning();
            
            String domain1Name = "domain-1." + DOMAIN_NAME;
            String domain2Name = "domain-2." + DOMAIN_NAME;
            
            String adminAcct11Name = "admin-1-1" + "@" + domain1Name;
            String adminAcct12Name = "admin-1-2" + "@" + domain1Name;
            String adminAcct21Name = "admin-2-1" + "@" + domain2Name;
            String adminAcct22Name = "admin-2-2" + "@" + domain2Name;
            
            Account adminAcct11 = prov.get(AccountBy.name, adminAcct11Name);
            Account adminAcct12 = prov.get(AccountBy.name, adminAcct12Name);
            Account adminAcct21 = prov.get(AccountBy.name, adminAcct21Name);
            Account adminAcct22 = prov.get(AccountBy.name, adminAcct22Name);
            
            // 2 ==> two admin accounts in each domain
            TestData test11 = new TestData(adminAcct11Name, PASSWORD, numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2); // in both admin groups 
            TestData test12 = new TestData(adminAcct12Name, PASSWORD, numAcctsInDomain1 + 2);                         // in admin group 1
            TestData test21 = new TestData(adminAcct21Name, PASSWORD, numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2); // in both admin groups 
            TestData test22 = new TestData(adminAcct22Name, PASSWORD, numAcctsInDomain2 + 2);                         // in admin group 2
            
            /*
             * urg, after r-t-w there are 9 accounts in the default domain:
                   admin
                   domainadmin
                   ham-sink
                   spam-sink
                   user1
                   user2
                   user3
                   user4
                   wiki
             */
            TestData globalAdmin = new TestData(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value(),
                    numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2 + 9);
            
            List<TestData> tests = new ArrayList<TestData>();
            tests.add(test11);
            tests.add(test12);
            tests.add(test21);
            tests.add(test22);
            tests.add(globalAdmin);
            
            if (adminAcct11 != null && adminAcct12 != null && adminAcct21 != null && adminAcct12 != null)
                return tests;
            
            //
            // 1. create two domains, one has 10(numAcctsInDomain1) accounts, the other has 3000(numAcctsInDomain2) accounts
            //
            Domain domain1 = prov.createDomain(domain1Name, new HashMap<String, Object>());
            Domain domain2 = prov.createDomain(domain2Name, new HashMap<String, Object>());
            
            //
            // 2. create 10(numAcctsInDomain1) accounts in domain1, 3000(numAcctsInDomain2) accounts in domain2
            //
            createAccounts(prov, domain1Name, numAcctsInDomain1);
            createAccounts(prov, domain2Name, numAcctsInDomain2);
            
            //
            // 3. create two admin groups, one in each of the two domains
            //
            Map<String, Object> adminGroup1Attrs = new HashMap<String, Object>();
            adminGroup1Attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
            DistributionList adminGroup1 = prov.createDistributionList("adminGroup-1" + "@" + domain1Name, adminGroup1Attrs);
            
            Map<String, Object> adminGroup2Attrs = new HashMap<String, Object>();
            adminGroup2Attrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
            DistributionList adminGroup2 = prov.createDistributionList("adminGroup-2" + "@" + domain2Name, adminGroup2Attrs);
            
            //
            // 4. grant rights to the admin groups on the domain
            //
            // Right right = AdminRight.RT_adminConsoleDomainRights;
            String right = AdminRight.RT_domainAdminConsoleRights;
            prov.grantRight(TargetType.domain.getCode(), TargetBy.id, domain1.getId(), 
                    GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroup1.getId(), null,
                    right, null);
            
            prov.grantRight(TargetType.domain.getCode(), TargetBy.id, domain2.getId(), 
                    GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroup2.getId(), null,
                    right, null);
            
            //
            // 5. create 4 delegated admin accounts, two in each of the two domains
            //
            adminAcct11 = createDelegatedAdmin(prov, adminAcct11Name);
            adminAcct12 = createDelegatedAdmin(prov, adminAcct12Name);
            adminAcct21 = createDelegatedAdmin(prov, adminAcct21Name);
            adminAcct22 = createDelegatedAdmin(prov, adminAcct22Name);
            
            //
            // 6. add the admin accounts to admin groups
            //
            prov.addMembers(adminGroup1, new String[]{adminAcct11Name, adminAcct12Name});
            prov.addMembers(adminGroup2, new String[]{adminAcct21Name, adminAcct22Name});
            
            prov.addMembers(adminGroup1, new String[]{adminAcct21Name});
            prov.addMembers(adminGroup2, new String[]{adminAcct11Name});
            
            return tests;
        }
        
        private static void teardown() throws Exception {
            
        }
        
        private static void doTest(String adminName, String adminPassword, int expectedNumEntries) throws Exception {
            
            System.out.println("\n\n=========================");
            System.out.println("Testing as " + adminName);
            
            SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning(adminName, adminPassword);
            
            String[] attrs = new String[]{"displayName",
                                          "zimbraId",
                                          "zimbraMailHost",
                                          "uid",
                                          "zimbraCOSId",
                                          "zimbraAccountStatus",
                                          "zimbraLastLogonTimestamp",
                                          "description",
                                          "zimbraIsDelegatedAdminAccount",
                                          "zimbraIsAdminAccount",
                                          "zimbraMailStatus",
                                          "zimbraIsAdminGroup",
                                          "zimbraCalResType",
                                          "zimbraDomainType",
                                          "zimbraDomainName",
                                          "zimbraIsDelegatedAdminAccount",
                                          "zimbraIsAdminGroup"
                                         };
            
            /*
            Integer offset = 0;
            Integer limit = 25;
            */
            
            /*
             * mBase              null
             * mConvertIDNToAscii true
             * mDomain            null
             * mFlags             1
             * mMaxResults        5000
             * mQuery             ""
             * mReturnAttrs       (17 of them)
             * mSortAscending     true
             * mSortAttr          "name"
             */
            
            SearchOptions options = new SearchOptions();
            options.setBase(null);
            options.setConvertIDNToAscii(true);
            options.setDomain(null);
            options.setFlags(Provisioning.SA_ACCOUNT_FLAG | Provisioning.SA_ALIAS_FLAG);
            options.setMaxResults(5000);
            options.setQuery("");
            options.setReturnAttrs(attrs);
            options.setSortAscending(true);
            options.setSortAttr("name");
            
            List<NamedEntry> results = null;
            long startTime = System.currentTimeMillis();
            try {
                // results = prov.searchDirectory(options, offset, limit);
                results = prov.searchDirectory(options);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                System.out.println("Finished, elapsedTime=" + elapsedTime + " milli seconds");
                
                if (results != null) {
                    System.out.println("Found " + results.size() + " entries");
                    
                    if (verbose) {
                        System.out.println();
                        for (NamedEntry entry : results) {
                            System.out.println(entry.getName());
                        }
                    }
                }
                
                /*
                if (expectedNumEntries > limit.intValue())
                    expectedNumEntries = limit.intValue();
                */
                assertEquals(expectedNumEntries, results.size());
            }
        }
    }
    
    public void disable_testBug39514() throws Exception {
       
        List<Bug39514.TestData> tests = Bug39514.setup();
        
        for (Bug39514.TestData test : tests) {
            Bug39514.doTest(test.mAdminName, test.mAdminPassword, test.mExpectedNumEntriesFound);
        }
        
         // teardown does nothign for now, just go to ldap and delete the two domains
        Bug39514.teardown();
    }    
        
    private static class Bug40499 {
        private static boolean verbose = true;
        
        private static class TestData {
            String mAdminName;
            String mAdminPassword;
            Set<String> mExpected;
            
            TestData(String adminName, String adminPassword, String[] expected) {
                mAdminName = adminName;
                mAdminPassword = adminPassword;
                mExpected = new HashSet<String>(Arrays.asList(expected));
            }
        }
        
        private static List<TestData> setup() throws Exception {
            
            String DEFAULT_DOMAIN_NAME = TestUtil.getDomain();
            
            Provisioning prov = TestProvisioningUtil.getSoapProvisioning();
            
            // create 2 domains
            String denyDomainName = "bug40499-deny." + DOMAIN_NAME;
            String allowDomainName = "bug40499-allow." + DOMAIN_NAME;
            
            Domain denyDomain = prov.createDomain(denyDomainName, new HashMap<String, Object>());
            Domain allowDomain = prov.createDomain(allowDomainName, new HashMap<String, Object>());
            
            // create two delegated admins, one in each doamin
            String denyAdminName = "da" + "@" + denyDomainName;
            String allowAdminName = "da" + "@" + allowDomainName;
            
            Account denyAdmin = createDelegatedAdmin(prov, denyAdminName);
            Account allowAdmin = createDelegatedAdmin(prov, allowAdminName);
            
            // grant positive adminConsoleDomainRights to both da on globalgrant
            String right = AdminRight.RT_adminConsoleDomainRights;
            prov.grantRight(TargetType.global.getCode(), null, null, 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, denyAdmin.getId(), null,
                    right, null);
            prov.grantRight(TargetType.global.getCode(), null, null, 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, allowAdmin.getId(), null,
                    right, null);
            
            // grant negative adminConsoleDomainRights to one the the da's on a domain
            prov.grantRight(TargetType.domain.getCode(), TargetBy.id, denyDomain.getId(), 
                    GranteeType.GT_USER.getCode(), GranteeBy.id, denyAdmin.getId(), null,
                    right, RightModifier.RM_DENY);
            
            
            TestData test1 = new TestData(denyAdminName, PASSWORD, new String[]  {DEFAULT_DOMAIN_NAME, allowDomainName});
            TestData test2 = new TestData(allowAdminName, PASSWORD, new String[] {DEFAULT_DOMAIN_NAME, allowDomainName, denyDomainName});
            
            List<TestData> tests = new ArrayList<TestData>();
            tests.add(test1);
            tests.add(test2);
            
            return tests;
        }
        
        private static void teardown() throws Exception {
        }
        
        private static void doTest(TestData testData) throws Exception {
            
            System.out.println("\n\n=========================");
            System.out.println("Testing as " + testData.mAdminName);
            
            SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning(testData.mAdminName, testData.mAdminPassword);
            
            String[] attrs = new String[]{"zimbraId",
                                          "zimbraDomainname"
                                         };
            
            SearchOptions options = new SearchOptions();
            options.setBase(null);
            options.setConvertIDNToAscii(true);
            options.setDomain(null);
            options.setFlags(Provisioning.SA_DOMAIN_FLAG);
            options.setMaxResults(5000);
            options.setQuery("");
            options.setReturnAttrs(attrs);
            options.setSortAscending(true);
            options.setSortAttr("name");
            
            List<NamedEntry> results = null;
            long startTime = System.currentTimeMillis();
            try {
                results = prov.searchDirectory(options);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                System.out.println("Finished, elapsedTime=" + elapsedTime + " milli seconds");
                
                if (results != null) {
                    System.out.println("Found " + results.size() + " entries");
                    
                    if (verbose) {
                        System.out.println();
                        for (NamedEntry entry : results) {
                            System.out.println(entry.getName());
                        }
                    }
                }
                
                assertEquals(testData.mExpected, results);
            }
        }

    }
    
    
    public void testBug40499() throws Exception {
        List<Bug40499.TestData> tests = Bug40499.setup();
        
        for (Bug40499.TestData test : tests) {
            Bug40499.doTest(test);
        }
        
         // teardown does nothign for now, just go to ldap and delete the two domains
        Bug39514.teardown();
    }
    
    public static void main(String[] args) throws Exception {
        // TestUtil.cliSetup();  // to use SoapProvisioning
        // CliUtil.toolSetup("DEBUG");
        CliUtil.toolSetup();

        TestUtil.runTest(TestSearchDirectory.class);
    }
}

