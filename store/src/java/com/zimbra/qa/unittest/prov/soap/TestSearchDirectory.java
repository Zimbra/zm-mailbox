/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.qa.QA.Bug;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;

public class TestSearchDirectory extends SoapTest {

    private static final String PASSWORD = "test123";

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }


    private static void createAccounts(String domainName, int numAccounts)
    throws Exception {
        for (int i = 0; i < numAccounts; i++) {
            String acctName = "user-" + (i+1) + "@" + domainName;

            Account acct = provUtil.createAccount(acctName, PASSWORD);

            if ((i+1) % 100 == 0) {
                System.out.println("Created " + (i+1) + " accounts");
            }
        }
    }

    /*
     * these are what passed from admin console
     */

    private String expandFilter(String filter, String key) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("n", key);
        return LdapUtil.expandStr(filter, vars);
    }

    private List searchDirectory(String key, boolean expectTooMany) throws Exception {
        final String FILTER_1 = "(|(uid=*%n*)(cn=*%n*)(sn=*%n*)(gn=*%n*)(displayName=*%n*)(zimbraId=%n)(mail=*%n*)(zimbraMailAlias=*%n*)(zimbraMailDeliveryAddress=*%n*)(zimbraDomainName=*%n*))";
        final String[] ATTRS = new String[]{
                "displayName", "zimbraId", "zimbraMailHost", "uid", "zimbraAccountStatus",
                "zimbraLastLogonTimestamp", "description", "zimbraMailStatus",
                "zimbraCalResType", "zimbraDomainType", "zimbraDomainName"};

        /*
        int flags = 0;
        flags |= Provisioning.SD_ACCOUNT_FLAG;
        flags |= Provisioning.SD_ALIAS_FLAG;
        flags |= Provisioning.SD_DISTRIBUTION_LIST_FLAG;
        flags |= Provisioning.SD_CALENDAR_RESOURCE_FLAG;
        flags |= Provisioning.SD_DOMAIN_FLAG;
        */

        String query = expandFilter(FILTER_1, key);

        SearchDirectoryOptions options = new SearchDirectoryOptions();
        options.setDomain(null);
        options.setMaxResults(5000);
        options.setFilterString(null, query);
        options.setReturnAttrs(ATTRS);
        options.setSortOpt(SortOpt.SORT_ASCENDING);
        options.setSortAttr("name");
        options.setConvertIDNToAscii(true);

        List results = null;
        boolean good = false;
        try {
            results = prov.searchDirectory(options);
            good = true;
        } catch (AccountServiceException e) {
            if (expectTooMany &&
                e.getCode().equals(AccountServiceException.TOO_MANY_SEARCH_RESULTS)) {
                good = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(good);
        return results;
    }

    @Test
    @Ignore  // takes too long to run, must create at least ldap page size (1000) accounts
    @Bug(bug=24168)
    public void bug24168() throws Exception {
        final int NUM_ACCTS = 1000;
        List results;

        String DOMAIN_NAME = ("bug24168." + TestSearchDirectory.domain.getName()).toLowerCase();
        Domain domain = provUtil.createDomain(DOMAIN_NAME);
        createAccounts(DOMAIN_NAME, NUM_ACCTS);

        System.out.flush();
        for (int i = 0; i < 10; i++) {
            System.out.flush();
            System.out.println("iteration " + (i+1));
            System.out.flush();

            System.out.println("search all");  System.out.flush();
            results = searchDirectory("user", true);

            System.out.println("search one");  System.out.flush();
            results = searchDirectory("user-1000", false);
            assertEquals(1, results.size());
            Account acct = (Account)results.get(0);
            assertEquals("user-1000", acct.getName().substring(0, 9));
        }
    }

    private static class Bug39514 {
        private static String BUG_39514_TEST_DOMAIN = "bug39514-test-";

        private static final int numAcctsInDomain1 = 5;
        private static final int numAcctsInDomain2 = 10;
        private static final int numAcctsInDomain3 = 3;

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

            String DOMAIN_NAME = ("Bug39514." + TestSearchDirectory.domain.getName()).toLowerCase();

            String domain1Name = BUG_39514_TEST_DOMAIN + "1." + DOMAIN_NAME;
            String domain2Name = BUG_39514_TEST_DOMAIN + "2." + DOMAIN_NAME;
            String domain3Name = BUG_39514_TEST_DOMAIN + "3." + DOMAIN_NAME;

            String adminAcct11Name = "admin-1-1" + "@" + domain1Name;
            String adminAcct12Name = "admin-1-2" + "@" + domain1Name;
            String adminAcct21Name = "admin-2-1" + "@" + domain2Name;
            String adminAcct22Name = "admin-2-2" + "@" + domain2Name;
            String globalAdminName = "admin-global" + "@" + domain3Name;

            // 2 ==> two admin accounts in each domain
            TestData test11 = new TestData(adminAcct11Name, PASSWORD,
                    numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2); // in both admin groups
            TestData test12 = new TestData(adminAcct12Name, PASSWORD,
                    numAcctsInDomain1 + 2);                         // in admin group 1
            TestData test21 = new TestData(adminAcct21Name, PASSWORD,
                    numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2); // in both admin groups
            TestData test22 = new TestData(adminAcct22Name, PASSWORD,
                    numAcctsInDomain2 + 2);                         // in admin group 2
            TestData globalAdmin = new TestData(globalAdminName, PASSWORD,
                    numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2 + numAcctsInDomain3 + 1);

            List<TestData> tests = new ArrayList<TestData>();
            tests.add(test11);
            tests.add(test12);
            tests.add(test21);
            tests.add(test22);
            tests.add(globalAdmin);

            //
            // 1. create domains
            //
            Domain domain1 = provUtil.createDomain(domain1Name);
            Domain domain2 = provUtil.createDomain(domain2Name);
            Domain domain3 = provUtil.createDomain(domain3Name);

            //
            // 2. create accounts in domains
            //
            createAccounts(domain1Name, numAcctsInDomain1);
            createAccounts(domain2Name, numAcctsInDomain2);
            createAccounts(domain3Name, numAcctsInDomain3);

            //
            // 3. create two admin groups, one in domain 1, one in domain 2
            //
            Map<String, Object> adminGroup1Attrs = new HashMap<String, Object>();
            adminGroup1Attrs.put(Provisioning.A_zimbraIsAdminGroup, ProvisioningConstants.TRUE);
            DistributionList adminGroup1 = prov.createDistributionList("adminGroup-1" + "@" + domain1Name, adminGroup1Attrs);

            Map<String, Object> adminGroup2Attrs = new HashMap<String, Object>();
            adminGroup2Attrs.put(Provisioning.A_zimbraIsAdminGroup, ProvisioningConstants.TRUE);
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
            //    create a global admin account
            //
            Account adminAcct11 = provUtil.createDelegatedAdmin(adminAcct11Name, PASSWORD);
            Account adminAcct12 = provUtil.createDelegatedAdmin(adminAcct12Name, PASSWORD);
            Account adminAcct21 = provUtil.createDelegatedAdmin(adminAcct21Name, PASSWORD);
            Account adminAcct22 = provUtil.createDelegatedAdmin(adminAcct22Name, PASSWORD);
            Account globalAdminAcct = provUtil.createGlobalAdmin(globalAdminName, PASSWORD);

            //
            // 6. add the admin accounts to admin groups
            //
            prov.addMembers(adminGroup1, new String[]{adminAcct11Name, adminAcct12Name});
            prov.addMembers(adminGroup2, new String[]{adminAcct21Name, adminAcct22Name});

            prov.addMembers(adminGroup1, new String[]{adminAcct21Name});
            prov.addMembers(adminGroup2, new String[]{adminAcct11Name});

            return tests;
        }

        private static void doTest(Bug39514.TestData testData) throws Exception {
            String adminName = testData.mAdminName;
            String adminPassword = testData.mAdminPassword;
            int expectedNumEntries = testData.mExpectedNumEntriesFound;

            System.out.println("\n\n=========================");
            System.out.println("Testing as " + adminName);

            SoapProvisioning prov = SoapProvTestUtil.getSoapProvisioning(adminName, adminPassword);

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

            SearchDirectoryOptions options = new SearchDirectoryOptions();
            options.setConvertIDNToAscii(true);
            options.setDomain(null);
            options.setTypes(SearchDirectoryOptions.ObjectType.accounts, SearchDirectoryOptions.ObjectType.aliases);
            options.setMaxResults(5000);
            options.setFilterString(null, null);
            options.setReturnAttrs(attrs);
            options.setSortOpt(SortOpt.SORT_ASCENDING);
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

                /*
                if (expectedNumEntries > limit.intValue())
                    expectedNumEntries = limit.intValue();
                */

                // throw away irrelevant domains created outside of this test
                List<NamedEntry> verifyResults = Lists.newArrayList();
                for (NamedEntry entry : results) {
                    if (entry.getName().contains(BUG_39514_TEST_DOMAIN)) {
                        verifyResults.add(entry);
                    }
                }

                assertEquals(expectedNumEntries, verifyResults.size());
            }
        }
    }

    @Test
    @Bug(bug=39514)
    public void bug39514() throws Exception {

        List<Bug39514.TestData> tests = Bug39514.setup();

        for (Bug39514.TestData test : tests) {
            Bug39514.doTest(test);
        }
    }

    private static class Bug40499 {

        private static String BUG_40499_TEST_DOMAIN = "bug40499-test-";

        private static class TestData {
            String mAdminName;
            String mAdminPassword;
            Set<String> mExpected;

            private TestData(String adminName, String adminPassword, String[] expected) {
                mAdminName = adminName;
                mAdminPassword = adminPassword;
                mExpected = new HashSet<String>(Arrays.asList(expected));
            }
        }

        private static List<TestData> setup() throws Exception {
            String DOMAIN_NAME = ("Bug40499." + TestSearchDirectory.domain.getName()).toLowerCase();

            // create 2 domains
            String denyDomainName = BUG_40499_TEST_DOMAIN + "deny." + DOMAIN_NAME;
            String allowDomainName = BUG_40499_TEST_DOMAIN + "allow." + DOMAIN_NAME;

            Domain denyDomain = prov.createDomain(denyDomainName, new HashMap<String, Object>());
            Domain allowDomain = prov.createDomain(allowDomainName, new HashMap<String, Object>());

            // create two delegated admins, one in each domain
            String denyAdminName = "da" + "@" + denyDomainName;
            String allowAdminName = "da" + "@" + allowDomainName;

            Account denyAdmin = provUtil.createDelegatedAdmin(denyAdminName, PASSWORD);
            Account allowAdmin = provUtil.createDelegatedAdmin(allowAdminName, PASSWORD);

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


            TestData test1 = new TestData(denyAdminName, PASSWORD, new String[]  {allowDomainName});
            TestData test2 = new TestData(allowAdminName, PASSWORD, new String[] {allowDomainName, denyDomainName});

            List<TestData> tests = new ArrayList<TestData>();
            tests.add(test1);
            tests.add(test2);

            return tests;
        }

        private static void doTest(TestData testData) throws Exception {

            System.out.println("\n\n=========================");
            System.out.println("Testing as " + testData.mAdminName);

            SoapProvisioning prov = SoapProvTestUtil.getSoapProvisioning(
                    testData.mAdminName, testData.mAdminPassword);

            String[] attrs = new String[]{"zimbraId",
                                          "zimbraDomainname"
                                         };

            SearchDirectoryOptions options = new SearchDirectoryOptions();
            options.setConvertIDNToAscii(true);
            options.setDomain(null);
            options.setTypes(SearchDirectoryOptions.ObjectType.domains);
            options.setMaxResults(5000);
            options.setFilterString(null, null);
            options.setReturnAttrs(attrs);
            options.setSortOpt(SortOpt.SORT_ASCENDING);
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

                    System.out.println();
                    for (NamedEntry entry : results) {
                        System.out.println(entry.getName());
                    }
                }

                // throw away irrelevant domains created outside of this test
                List<NamedEntry> verifyResults = Lists.newArrayList();
                for (NamedEntry entry : results) {
                    if (entry.getName().contains(BUG_40499_TEST_DOMAIN)) {
                        verifyResults.add(entry);
                    }
                }

                Verify.verifyEquals(testData.mExpected, verifyResults);
            }
        }

    }

    @Test
    @Bug(bug=40499)
    public void bug40499() throws Exception {
        List<Bug40499.TestData> tests = Bug40499.setup();

        for (Bug40499.TestData test : tests) {
            Bug40499.doTest(test);
        }
    }

    @Test
    public void testResponsContainsEphemeralAttrs() throws Exception {
        String acctName = String.format("user1@%s", domain.getName());
        Account acct = provUtil.createAccount(acctName, PASSWORD);
        Date date = new Date();
        acct.setLastLogonTimestamp(date);
        List<NamedEntry> results = searchDirectory(acct.getName(), false);
        acct = (Account) results.get(0);
        String timestamp = acct.getAttr(Provisioning.A_zimbraLastLogonTimestamp);
        assertEquals(LdapDateUtil.toGeneralizedTime(date), timestamp);
    }
}
