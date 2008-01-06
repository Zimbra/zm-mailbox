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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeType;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.SearchGalResult;

public class TestGal extends TestCase {
    private String TEST_ID = TestProvisioningUtil.genTestId();
    private static String TEST_NAME = "test-gal";
    private static String PASSWORD = "test123";
    private static String ACCT_NAME_PREFIX = "user";
    private static String QUERY = ACCT_NAME_PREFIX;
    private static int NUM_ACCOUNTS = 1000;
    
    private static int MAX_PAGE_SIZE = 1000; // defined in zimbra-attrs.xml
    private static int UNLIMITED = 0;
    private static int LIMITED = 100;
    // sizelimit in /opt/zimbra/conf/slapd.conf
    // set LDAP_SERVER_SIZE_LIMIT to either UNLIMITED or LIMITED and set slapd.conf accordingly, then restart ldap server
    private static int LDAP_SERVER_SIZE_LIMIT = UNLIMITED; 
    private String DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, null);
    private Provisioning mProv;
    private Domain mDomain;
    
    private boolean DEBUG = true;
    private boolean SKIP_ACCT_CHECKING = false;  // to save time if we are sure accounts are OK
    
    // too bad SyncGal does not work with SoapProvisioning, because it calls searchGal but that does not return token.  :(
    // don't run SOAP for now.
    private static final boolean SOAP_PROV = false;
    static {
        if (SOAP_PROV) {
            try { 
                // comment for LdapProvisioning, uncomment for SoapProvisioning
                TestUtil.cliSetup();  // which will invoke CliUtil.toolSetup();
            } catch (ServiceException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else
            CliUtil.toolSetup();
    }
    
    enum GalOp {
        GOP_AUTOCOMPLETE,
        GOP_SEARCH,
        GOP_SYNC
    };
    
    interface PageSize {
        public int pageSize();
    }
    
    /*
     * page size for testing when ldap server size is unlimited
     */
    enum PageSizeLdapServerUnlimited implements PageSize {
        PS_UNLIMITED(0),
        PS_MORE_ACCOUNTS(more(NUM_ACCOUNTS)),
        PS_LESS_ACOUNTS(less(NUM_ACCOUNTS));
            
        int mPs;
        
        PageSizeLdapServerUnlimited(int ps) {
            if (ps > MAX_PAGE_SIZE)
                ps = MAX_PAGE_SIZE;
            mPs = ps;
        }
            
        public int pageSize() {
            return mPs;
        }
    }
    
    /*
     * page size for testing when ldap server size is unlimited
     */
    enum PageSizeLdapServerLimited implements PageSize {
        PS_UNLIMITED(0),
        PS_MORE_LDAP_SERVER_LIMIT(more(LDAP_SERVER_SIZE_LIMIT)),
        PS_LESS_LDAP_SERVER_LIMIT(less(LDAP_SERVER_SIZE_LIMIT));
        
        int mPs;
    
        PageSizeLdapServerLimited(int ps) {
            if (ps > MAX_PAGE_SIZE)
                ps = MAX_PAGE_SIZE;
            mPs = ps;
        }
        
        public int pageSize() {
            return mPs;
        }
    }
    
    class PageSizeEnum {
        int mIdx;
        PageSize[] mEnum;
        
        PageSizeEnum() {
            mIdx = 0;
            if (LDAP_SERVER_SIZE_LIMIT == UNLIMITED) 
                mEnum = PageSizeLdapServerUnlimited.values();
            else
                mEnum = PageSizeLdapServerLimited.values();
        }
        
        public boolean hasNext() {
            return (mIdx < mEnum.length);
        }
        
        public int next() {
            assert(mIdx < mEnum.length);
            return mEnum[mIdx++].pageSize();
        }
    }
    
    private String acctName(int index) {
        return ACCT_NAME_PREFIX + "-" + index + "@" + DOMAIN_NAME;
    }
    
    public TestGal() throws Exception {
        mProv = Provisioning.getInstance();
        
        // create the domain and accounts if they have not been created yet
        mDomain = mProv.get(DomainBy.name, DOMAIN_NAME);
        if (mDomain == null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            // setup zimbra OpenLDAP for external GAL
            attrs.put(Provisioning.A_zimbraGalLdapURL, "ldap://localhost:389");
            attrs.put(Provisioning.A_zimbraGalLdapBindDn, LC.zimbra_ldap_userdn.value());
            attrs.put(Provisioning.A_zimbraGalLdapBindPassword, LC.zimbra_ldap_password.value());
            attrs.put(Provisioning.A_zimbraGalLdapSearchBase, "dc=test-gal,dc=ldaptest");
            
            mDomain = mProv.createDomain(DOMAIN_NAME, attrs);
        }
        assertNotNull(mDomain);
        assertEquals(mDomain.getAttr(Provisioning.A_zimbraGalLdapURL), "ldap://localhost:389");
        assertEquals(mDomain.getAttr(Provisioning.A_zimbraGalLdapBindDn), LC.zimbra_ldap_userdn.value());
        assertEquals(mDomain.getAttr(Provisioning.A_zimbraGalLdapBindPassword), LC.zimbra_ldap_password.value());
        assertEquals(mDomain.getAttr(Provisioning.A_zimbraGalLdapSearchBase), "dc=test-gal,dc=ldaptest");
        
        if (SKIP_ACCT_CHECKING)
            return;
        
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            String acctName = acctName(i);
            Account acct = mProv.get(AccountBy.name, acctName);
            if (acct == null)
                acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
            assertNotNull(acct);
        }
        
        /* following won't work because we set LDAP_SERVER_SIZE_LIMIT to too small when it is LIMITED
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();
        options.setMaxResults(0);
        options.setFlags(Provisioning.SA_ACCOUNT_FLAG);
        options.setDomain(mDomain);
        List<NamedEntry> list = mProv.searchDirectory(options);
        if (list.size() == 0) {
            for (int i=0; i<NUM_ACCOUNTS; i++) {
                String acctName = ACCT_NAME_PREFIX + "-" + i + "@" + DOMAIN_NAME;
                Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
                assertNotNull(acct);
            }
        } else 
            assertEquals(NUM_ACCOUNTS, list.size());
        */
    }
  
    private void setupAutoComplete(String galMode, int pageSize) throws Exception {
        Domain domain = mProv.get(DomainBy.name, DOMAIN_NAME);
        assertNotNull(domain);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraGalMode, galMode);
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, ""+pageSize);
        
        // set domain limit to be larger than total number of accounts so test parameters 
        // in autoComplete tests can be effective instead of being limited by the domain limit.
        attrs.put(Provisioning.A_zimbraGalMaxResults, ""+more(NUM_ACCOUNTS));
        attrs.put(Provisioning.A_zimbraGalLdapFilter, "zimbraAccounts");
        mProv.modifyAttrs(domain, attrs);
    }
    
    private void setupSearch(String galMode, int pageSize, int domainLimit) throws Exception {
        Domain domain = mProv.get(DomainBy.name, DOMAIN_NAME);
        assertNotNull(domain);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraGalMode, galMode);
        attrs.put(Provisioning.A_zimbraGalLdapPageSize, ""+pageSize);
        attrs.put(Provisioning.A_zimbraGalMaxResults, ""+domainLimit);
        attrs.put(Provisioning.A_zimbraGalLdapFilter, "zimbraAccounts");
        mProv.modifyAttrs(domain, attrs);
    }
    
    private static int less(int number) {
        assertFalse(UNLIMITED==number);
        return number/2;
    }
    
    private static int more(int number) {
        assertFalse(UNLIMITED==number);
        return number*2;
    }
    
    private void dumpResult(SearchGalResult galResult) {
        for (GalContact contact : galResult.matches) {
            System.out.println(contact.getId());
        }
    }
    private void autoCompleteGal(int numResultsExpected, int maxWanted) throws Exception {
        SearchGalResult galResult = mProv.autoCompleteGal(mDomain, 
                                                          QUERY,
                                                          Provisioning.GAL_SEARCH_TYPE.ALL, 
                                                          maxWanted);
        if (numResultsExpected != galResult.matches.size())
            dumpResult(galResult);
        
        assertEquals(numResultsExpected, galResult.matches.size());
        boolean expectedHasMore = numResultsExpected < NUM_ACCOUNTS;
        assertEquals(expectedHasMore, galResult.hadMore);
    }
    
    private void searchGal(int numResultsExpected) throws Exception {
        SearchGalResult galResult = mProv.searchGal(mDomain, 
                                                    QUERY,
                                                    Provisioning.GAL_SEARCH_TYPE.ALL, 
                                                    null);
        assertEquals(numResultsExpected, galResult.matches.size());
        boolean expectedHasMore = numResultsExpected < NUM_ACCOUNTS;
        assertEquals(expectedHasMore, galResult.hadMore);
    }
    
    private String syncGal(int numResultsExpected, int numTotal, String token) throws Exception {
        SearchGalResult galResult = mProv.searchGal(mDomain, 
                                                    QUERY,
                                                    Provisioning.GAL_SEARCH_TYPE.ALL, 
                                                    token);
        assertEquals(numResultsExpected, galResult.matches.size());
        boolean expectedHasMore = numResultsExpected < numTotal;
        
        // SyncGal SOAP response does not encode the more flag. 
        if (!SOAP_PROV)
            assertEquals(expectedHasMore, galResult.hadMore);
        
        return galResult.token;
    }
    
    private void syncGal(int numResultsExpected) throws Exception {
        // sync all
        String token = syncGal(numResultsExpected, NUM_ACCOUNTS, null);
        assertNotNull(token);
        
        // wait 1 second
        Thread.sleep(1000);
        
        // modify some accounts
        int numDelta = NUM_ACCOUNTS/10;
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraNotes, "blah");
        
        for (int i=0; i<numDelta; i++) {
            String acctName = acctName(i);
            Account acct = mProv.get(AccountBy.name, acctName);
            assertNotNull(acct);
            
            mProv.modifyAttrs(acct, attrs);
        }
        
        // sync again, with a token so should return only the delta
        syncGal(numDelta, numDelta, token);
    }
    
    /*
     * verifies that page size or if paging is enabled does NOT affect the result, 
     * and that is true regardless whether the ldap server has a size limit.
     * 
     * when ldap server does not have a limit:
     *      it returns the number of entries wanted
     *      
     * when ldap server has a limit:
     *      it is capped by the ldap server limit     
     */
    private void autoCompleteGal(String galMode, int pageSize) throws Exception {
        if (DEBUG)
            System.out.format("autoCompleteGal: %s, %d\n", galMode, pageSize);
        
        setupAutoComplete(galMode, pageSize);
        
        if (LDAP_SERVER_SIZE_LIMIT == UNLIMITED) {
            autoCompleteGal(NUM_ACCOUNTS, UNLIMITED);
            autoCompleteGal(NUM_ACCOUNTS, more(NUM_ACCOUNTS));
            autoCompleteGal(less(NUM_ACCOUNTS), less(NUM_ACCOUNTS));
        } else {
            autoCompleteGal(LDAP_SERVER_SIZE_LIMIT, UNLIMITED);
            autoCompleteGal(LDAP_SERVER_SIZE_LIMIT, more(LDAP_SERVER_SIZE_LIMIT));
            autoCompleteGal(less(LDAP_SERVER_SIZE_LIMIT), less(LDAP_SERVER_SIZE_LIMIT));
        }
    }
    
    /*
     * verifies that page size or if paging is enabled does NOT affect the result, 
     * and that is true regardless whether the ldap server has a size limit.
     * 
     * when ldap server does not have a limit:
     *      it is capped by the domain limit 
     *      
     * when ldap server has a limit:
     *      it is capped by the lower of ldap server limit and domain limit
     */
    private void searchGal(String galMode, int pageSize) throws Exception {
        if (DEBUG)
            System.out.format("searchGal: %s, %d\n", galMode, pageSize);
        
        if (LDAP_SERVER_SIZE_LIMIT == UNLIMITED) {
            setupSearch(galMode, pageSize, UNLIMITED);
            searchGal(NUM_ACCOUNTS);
            
            setupSearch(galMode, pageSize, more(NUM_ACCOUNTS));
            searchGal(NUM_ACCOUNTS);
            
            setupSearch(galMode, pageSize, less(NUM_ACCOUNTS));
            searchGal(less(NUM_ACCOUNTS));
        } else {
            setupSearch(galMode, pageSize, LDAP_SERVER_SIZE_LIMIT);
            searchGal(LDAP_SERVER_SIZE_LIMIT);
            
            setupSearch(galMode, pageSize, more(LDAP_SERVER_SIZE_LIMIT));
            searchGal(LDAP_SERVER_SIZE_LIMIT);
            
            setupSearch(galMode, pageSize, less(LDAP_SERVER_SIZE_LIMIT));
            searchGal(less(LDAP_SERVER_SIZE_LIMIT));
        }
    }
    
    private void syncGal(String galMode, int pageSize) throws Exception {
        if (DEBUG)
            System.out.format("syncGal: %s, %d\n", galMode, pageSize);
        
        if (LDAP_SERVER_SIZE_LIMIT == UNLIMITED) {
            setupSearch(galMode, pageSize, UNLIMITED);
            syncGal(NUM_ACCOUNTS);
            
            setupSearch(galMode, pageSize, more(NUM_ACCOUNTS));
            syncGal(NUM_ACCOUNTS);
            
            setupSearch(galMode, pageSize, less(NUM_ACCOUNTS));
            syncGal(less(NUM_ACCOUNTS));
        } else {
            setupSearch(galMode, pageSize, LDAP_SERVER_SIZE_LIMIT);
            syncGal(LDAP_SERVER_SIZE_LIMIT);
            
            setupSearch(galMode, pageSize, more(LDAP_SERVER_SIZE_LIMIT));
            syncGal(LDAP_SERVER_SIZE_LIMIT);
            
            setupSearch(galMode, pageSize, less(LDAP_SERVER_SIZE_LIMIT));
            syncGal(less(LDAP_SERVER_SIZE_LIMIT));
        }
    }
    
    public void autoCompleteGal(String galMode) throws Exception {
        PageSizeEnum pse = new PageSizeEnum();
        while (pse.hasNext())
            autoCompleteGal(galMode, pse.next());
    }
    
    public void searchGal(String galMode) throws Exception {
        PageSizeEnum pse = new PageSizeEnum();
        while (pse.hasNext())
            searchGal(galMode, pse.next());
    }
    
    public void syncGal(String galMode) throws Exception {
        PageSizeEnum pse = new PageSizeEnum();
        while (pse.hasNext())
            syncGal(galMode, pse.next());
    }
    
    public void testAutoCompleteGal() throws Exception {
        autoCompleteGal(Provisioning.GM_ZIMBRA);
        autoCompleteGal(Provisioning.GM_LDAP);
    }
    
    public void testSearchGal() throws Exception {
        searchGal(Provisioning.GM_ZIMBRA);
        searchGal(Provisioning.GM_LDAP);
    }
    
    public void testSyncGal() throws Exception {
        syncGal(Provisioning.GM_ZIMBRA);
        syncGal(Provisioning.GM_LDAP);
    }
    
    public void disable_testPageSizeEnum() throws Exception {
       
        PageSizeEnum pse = null;
        LDAP_SERVER_SIZE_LIMIT = LIMITED;
        System.out.println("limited");
        pse = new PageSizeEnum();
        while (pse.hasNext()) {
            System.out.println(pse.next());
        }
        
        LDAP_SERVER_SIZE_LIMIT = UNLIMITED;
        System.out.println("unlimited");
        pse = new PageSizeEnum();
        while (pse.hasNext()) {
            System.out.println(pse.next());
        }
    }
 
    public static void main(String[] args) throws Exception {
        try {
            TestUtil.runTest(new TestSuite(TestGal.class));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
