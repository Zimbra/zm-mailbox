/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchOptions;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class TestSearchDirectory extends TestCase {
    
    private static String TEST_NAME = "test-sd";
    private static String DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, null);
    private static int NUM_ACCTS = 10000;
    /*
     * these are what passed from admin console
     */
    private static final String FILTER_1 = "(|(uid=*%n*)(cn=*%n*)(sn=*%n*)(gn=*%n*)(displayName=*%n*)(zimbraId=%n)(mail=*%n*)(zimbraMailAlias=*%n*)(zimbraMailDeliveryAddress=*%n*)(zimbraDomainName=*%n*))";
    private static final String[] ATTRS = new String[]{"displayName", "zimbraId", "zimbraMailHost", "uid", "zimbraAccountStatus", "zimbraLastLogonTimestamp", "description", "zimbraMailStatus", "zimbraCalResType", "zimbraDomainType", "zimbraDomainName"};

    private static void createAccounts() throws Exception{
        Provisioning prov = Provisioning.getInstance();
        
        Domain domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        
        for (int i = 0; i < NUM_ACCTS; i++) {
            String acctName = "user-" + (i+1) + "@" + DOMAIN_NAME;
            
            Account acct = prov.createAccount(acctName, "test123", new HashMap<String, Object>());
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
    
    public void testSearchDirectory() throws Exception {
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
 
    public static void main(String[] args) throws Exception {
        // TestUtil.cliSetup();
        // CliUtil.toolSetup("DEBUG");
        CliUtil.toolSetup();
        createAccounts();
        
        // TestUtil.runTest(new TestSuite(TestSearchDirectory.class));
    }
}

