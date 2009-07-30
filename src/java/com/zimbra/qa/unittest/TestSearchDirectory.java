/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
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
    
    private class Bug39514TestData {
        String mAdminName;
        String mAdminPassword;
        Integer mExpectedNumEntriesFound;
        
        Bug39514TestData(String adminName, String adminPassword, Integer expectedNumEntriesFound) {
            mAdminName = adminName;
            mAdminPassword = adminPassword;
            mExpectedNumEntriesFound = expectedNumEntriesFound;
        }
    }
    
    private List<Bug39514TestData> setupBug39514() throws Exception {
        Provisioning prov = TestProvisioningUtil.getSoapProvisioning();
        
        // 1. create 1000 accounts (use LdapProvisioning)
        // createAccounts(Provisioning.getInstance());
        
        int numAcctsInDomain1 = 10;
        int numAcctsInDomain2 = 3000;
        
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
        Bug39514TestData test11 = new Bug39514TestData(adminAcct11Name, PASSWORD, numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2);  
        Bug39514TestData test12 = new Bug39514TestData(adminAcct12Name, PASSWORD, numAcctsInDomain1 + 2);
        Bug39514TestData test21 = new Bug39514TestData(adminAcct21Name, PASSWORD, numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2);
        Bug39514TestData test22 = new Bug39514TestData(adminAcct22Name, PASSWORD, numAcctsInDomain2 + 2);
        
        // system admin
        Bug39514TestData systemAdmin = new Bug39514TestData(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value(),
                numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2);
        
        List<Bug39514TestData> tests = new ArrayList<Bug39514TestData>();
        // tests.add(test11);
        tests.add(test12);
        // tests.add(test21);
        // tests.add(test22);
        // tests.add(systemAdmin);
        
        if (adminAcct11 != null && adminAcct12 != null && adminAcct21 != null && adminAcct12 != null)
            return tests;
        
        //
        // 1. create two domains, one has 10 accounts, the other has 3000 accounts
        //
        Domain domain1 = prov.createDomain(domain1Name, new HashMap<String, Object>());
        Domain domain2 = prov.createDomain(domain2Name, new HashMap<String, Object>());
        
        //
        // 2. create 10 accounts in domain1, 3000 accounts in domain2
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
                GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroup1.getId(), 
                right, null);
        
        prov.grantRight(TargetType.domain.getCode(), TargetBy.id, domain2.getId(), 
                GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroup2.getId(), 
                right, null);
        
        //
        // 5. create 4 delegated admin accounts, two in each of the two domains
        //
        Map<String, Object> adminAcct11Attrs = new HashMap<String, Object>();
        adminAcct11Attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        adminAcct11 = prov.createAccount(adminAcct11Name, "test123", adminAcct11Attrs);
        
        Map<String, Object> adminAcct12Attrs = new HashMap<String, Object>();
        adminAcct12Attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        adminAcct12 = prov.createAccount(adminAcct12Name, "test123", adminAcct12Attrs);
        
        Map<String, Object> adminAcct21Attrs = new HashMap<String, Object>();
        adminAcct21Attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        adminAcct21 = prov.createAccount(adminAcct21Name, "test123", adminAcct21Attrs);
        
        Map<String, Object> adminAcct22Attrs = new HashMap<String, Object>();
        adminAcct22Attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        adminAcct22 = prov.createAccount(adminAcct22Name, "test123", adminAcct22Attrs);
        
        //
        // 6. add the admin accounts to admin groups
        //
        prov.addMembers(adminGroup1, new String[]{adminAcct11Name, adminAcct12Name});
        prov.addMembers(adminGroup2, new String[]{adminAcct21Name, adminAcct22Name});
        
        prov.addMembers(adminGroup1, new String[]{adminAcct21Name});
        prov.addMembers(adminGroup2, new String[]{adminAcct11Name});
        
        return tests;
    }
    
    private List<Bug39514TestData> setupBug39514_qa62() throws Exception {
        Provisioning prov = TestProvisioningUtil.getSoapProvisioning();
        
        // 1. create 1000 accounts (use LdapProvisioning)
        // createAccounts(Provisioning.getInstance());
        
        int numAcctsInDomain1 = 10;
        int numAcctsInDomain2 = 3000;
        
        String domain1Name = "domain-1." + DOMAIN_NAME;
        String domain2Name = "domain-2." + DOMAIN_NAME;
        
        String adminAcctName = "domainadmin-1" + "@" + domain1Name;
        
        Account adminAcct = prov.get(AccountBy.name, adminAcctName);
        
        // 2 ==> two admin accounts in each domain
        Bug39514TestData test = new Bug39514TestData(adminAcctName, PASSWORD, numAcctsInDomain1 + 2);  
        
        Bug39514TestData systemAdmin = new Bug39514TestData(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value(),
                numAcctsInDomain1 + 2 + numAcctsInDomain2 + 2);
        
        List<Bug39514TestData> tests = new ArrayList<Bug39514TestData>();
        // tests.add(test);
        tests.add(systemAdmin);
        
        if (adminAcct != null)
            return tests;
        
        //
        // 3. create two admin groups in domain 1, one DL amdin and one domain admin
        //
        Map<String, Object> adminGroupDLAttrs = new HashMap<String, Object>();
        adminGroupDLAttrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
        DistributionList adminGroupDL = prov.createDistributionList("adminGroup-DL" + "@" + domain1Name, adminGroupDLAttrs);
        
        Map<String, Object> adminGroupDomainAttrs = new HashMap<String, Object>();
        adminGroupDomainAttrs.put(Provisioning.A_zimbraIsAdminGroup, Provisioning.TRUE);
        DistributionList adminGroupDomain = prov.createDistributionList("adminGroup-DOMAIN" + "@" + domain1Name, adminGroupDomainAttrs);
        
        //
        // 4. grant rights to the admin groups, the same way as QA did on qa62
        //
        // Right right = AdminRight.RT_adminConsoleDomainRights;
        // String right = AdminRight.RT_domainAdminConsoleRights;
        
        // for the DL admin group
        prov.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroupDL.getId(), 
                AdminRight.RT_listAccount, null);
        
        prov.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroupDL.getId(), 
                AdminRight.RT_adminConsoleDLRights, null);
        
        prov.grantRight(TargetType.global.getCode(), null, null, 
                GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroupDomain.getId(), 
                AdminRight.RT_domainAdminZimletRights, null);
        
        prov.grantRight(TargetType.domain.getCode(), TargetBy.name, domain1Name, 
                GranteeType.GT_GROUP.getCode(), GranteeBy.id, adminGroupDomain.getId(), 
                AdminRight.RT_domainAdminConsoleRights, null);
        
        //
        // 5. create a delegated admin accounts in the adminGroupDomain group
        //
        Map<String, Object> adminAcctAttrs = new HashMap<String, Object>();
        adminAcctAttrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, Provisioning.TRUE);
        adminAcct = prov.createAccount(adminAcctName, "test123", adminAcctAttrs);
        
        //
        // 6. add the admin account to admin group
        //
        prov.addMembers(adminGroupDomain, new String[]{adminAcctName});
        
        return tests;
    }
    
    public void testBug39514() throws Exception {
       
        List<Bug39514TestData> tests = setupBug39514_qa62();
        
        for (Bug39514TestData test : tests) {
            doTestBug39514(test.mAdminName, test.mAdminPassword, test.mExpectedNumEntriesFound);
        }
    }    
        
    public void doTestBug39514(String adminName, String adminPassword, int expectedNumEntries) throws Exception {
        
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
        
        Integer offset = 0;
        Integer limit = 25;
        
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
        options.setFlags(Provisioning.SA_ACCOUNT_FLAG);
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
                
                System.out.println();
                for (NamedEntry entry : results) {
                    System.out.println(entry.getName());
                }
              
            }
            
            if (expectedNumEntries > limit.intValue())
                expectedNumEntries = limit.intValue();
            assertEquals(expectedNumEntries, results.size());
        }
    }
    

    /*
    public List<NamedEntry> searchDirectory(SearchOptions options, Integer offset, Integer limit) throws ServiceException {
        List<NamedEntry> result = new ArrayList<NamedEntry>();
        XMLElement req = new XMLElement(AdminConstants.SEARCH_DIRECTORY_REQUEST);
        req.addElement(AdminConstants.E_QUERY).setText(options.getQuery());
        if (options.getMaxResults() != 0) req.addAttribute(AdminConstants.A_MAX_RESULTS, options.getMaxResults());
        if (options.getDomain() != null) req.addAttribute(AdminConstants.A_DOMAIN, options.getDomain().getName());
        if (options.getSortAttr() != null) req.addAttribute(AdminConstants.A_SORT_BY, options.getSortAttr());
        if (options.getFlags() != 0) req.addAttribute(AdminConstants.A_TYPES, Provisioning.searchAccountMaskToString(options.getFlags()));
        req.addAttribute(AdminConstants.A_SORT_ASCENDING, options.isSortAscending() ? "1" : "0");
        if (options.getReturnAttrs() != null) {
            req.addAttribute(AdminConstants.A_ATTRS, StringUtil.join(",", options.getReturnAttrs()));
        }
        
        if (offset != null)
            req.addAttribute(AdminConstants.A_OFFSET, offset.intValue());
        
        if (limit != null)
            req.addAttribute(AdminConstants.A_LIMIT, limit.intValue());
        
        
        // TODO: handle ApplyCos, limit, offset?
        Element resp = invoke(req);
        for (Element e: resp.listElements(AdminConstants.E_DL))
            result.add(new SoapDistributionList(e, this));

        for (Element e: resp.listElements(AdminConstants.E_ALIAS))
            result.add(new SoapAlias(e, this));

        for (Element e: resp.listElements(AdminConstants.E_ACCOUNT))
            result.add(new SoapAccount(e, this));

        for (Element e: resp.listElements(AdminConstants.E_DOMAIN))
            result.add(new SoapDomain(e, this));
        return result;
    }
    */
    
    public static void main(String[] args) throws Exception {
        // TestUtil.cliSetup();  // to use SoapProvisioning
        // CliUtil.toolSetup("DEBUG");
        CliUtil.toolSetup();
        
        // createAccounts(null);
        
        TestUtil.runTest(TestSearchDirectory.class);
    }
}

