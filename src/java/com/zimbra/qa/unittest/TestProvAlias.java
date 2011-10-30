/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;


public class TestProvAlias extends TestLdap {
    private static String TEST_ID;
    private static String TEST_NAME = "test-alias";
    private static String PASSWORD = "test123";
   
    private static Provisioning mProv;
    
    private static String BASE_DOMAIN_NAME;
    
    private static String LOCAL_DOMAIN_NAME;
    private static String ALIAS_DOMAIN_NAME;
    
    private static String origDefaultDomainName;
    
    /*
     * convert underscores in inStr to hyphens
     */
    private String underscoreToHyphen(String inStr) {
        return inStr.replaceAll("_", "-");
    }
    
    
    @BeforeClass
    public static void init() throws Exception {
        TEST_ID = TestProvisioningUtil.genTestId();
        
        mProv = Provisioning.getInstance();
        
        Config config = mProv.getConfig();
        origDefaultDomainName = config.getAttr(Provisioning.A_zimbraDefaultDomainName);
        
        initTest();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Config config = mProv.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, origDefaultDomainName);
        mProv.modifyAttrs(config, attrs);
        
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
    }
    
    private static void initTest() throws Exception {
        
        BASE_DOMAIN_NAME = baseDomainName();
        LOCAL_DOMAIN_NAME = "local." + BASE_DOMAIN_NAME;
        ALIAS_DOMAIN_NAME = "alias." + BASE_DOMAIN_NAME;
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        // create the local domain
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain localDomain = mProv.createDomain(LOCAL_DOMAIN_NAME, attrs);
        
        // create the alias domain
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_ALIAS);
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, localDomain.getId());
        Domain aliasDomain = mProv.createDomain(ALIAS_DOMAIN_NAME, attrs);
    }
    
    private String getEmail(String localPart, String domainName, String testName) {
        return getLocalPart(localPart, testName) + "@" + domainName;
    }
    
    private String getEmail(String localPart, String domainName) {
        return localPart + "@" + domainName;
    }
    
    private String getLocalPart(String localPart, String testName) {
        return localPart + "-" + testName;
    }
    
    private List<NamedEntry> searchAliasesInDomain(Domain domain) throws ServiceException {
        SearchDirectoryOptions options = new SearchDirectoryOptions();
        options.setTypes(SearchDirectoryOptions.ObjectType.aliases);
        options.setDomain(domain);
        options.setFilterString(FilterId.UNITTEST, null);
        return mProv.searchDirectory(options);
    }
    
    /*
        Case 1:
            Alias1@localdomain.com points at account1@localdomain.com.
            Auth is attempted with alias1@aliasdomain.com. Does it work?  YES
    */
    @Test
    public void testAliasDomain_Case1() throws Exception {
        String testName = "testAliasDomain_Case1"; // getName();
        
        String acctLocalPart = "account1";
        String acctName = getEmail(acctLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        String aliasLocalPart = "alias1";
        String aliasName = getEmail(aliasLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        Account acct = mProv.createAccount(acctName, PASSWORD, null);
        mProv.addAlias(acct, aliasName);
        
        String authAs = getEmail(aliasLocalPart, ALIAS_DOMAIN_NAME, testName);
        Account acctGot = mProv.get(AccountBy.name, authAs);
        
        TestProvisioningUtil.verifySameEntry(acct, acctGot);
        mProv.authAccount(acctGot, PASSWORD, AuthContext.Protocol.test);
    }

    /*
        Case 2:
            Alias1@aliasdomain.com points at account1@localdomain.com.  (there is no alias1@localdomain.com alias)
            Auth is attempted with alias1@localdomain.com.    Does it work?  NO
            Auth is attempted with account1@aliasdomain.com.  Does it work?  YES
    */
    @Test
    public void testAliasDomain_Case2() throws Exception {
        String testName = "testAliasDomain_Case2"; // getName();
        
        String acctLocalPart = "account1";
        String acctName = getEmail(acctLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        String aliasLocalPart = "alias1";
        String aliasName = getEmail(aliasLocalPart, ALIAS_DOMAIN_NAME, testName);
        
        Account acct = mProv.createAccount(acctName, PASSWORD, null);
        mProv.addAlias(acct, aliasName);
        
        String authAs = getEmail(aliasLocalPart, LOCAL_DOMAIN_NAME, testName);
        Account acctGot = mProv.get(AccountBy.name, authAs);
        assertNull(acctGot);
        
        authAs = getEmail(acctLocalPart, ALIAS_DOMAIN_NAME, testName);
        acctGot = mProv.get(AccountBy.name, authAs);
        TestProvisioningUtil.verifySameEntry(acct, acctGot);
        mProv.authAccount(acctGot, PASSWORD, AuthContext.Protocol.test);
    }
    
    /*
        Case 3:
            Alias1@aliasdomain.com points at account1@localdomain.com.  (there is no alias1@localdomain.com alias)
            Global config zimbra default domain is set to localdomain.com.
            Auth is attempted with "alias1".  Does it work?  NO
            
    */
    @Test
    public void testAliasDomain_Case3() throws Exception {
        String testName = "testAliasDomain_Case3"; // getName();
        
        String acctLocalPart = "account1";
        String acctName = getEmail(acctLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        String aliasLocalPart = "alias1";
        String aliasName = getEmail(aliasLocalPart, ALIAS_DOMAIN_NAME, testName);
        
        Config config = mProv.getConfig();
        String origDefaltDomainName = config.getAttr(Provisioning.A_zimbraDefaultDomainName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, LOCAL_DOMAIN_NAME);
        mProv.modifyAttrs(config, attrs);
        
        Account acct = mProv.createAccount(acctName, PASSWORD, null);
        mProv.addAlias(acct, aliasName);
        
        String authAs = getLocalPart(aliasLocalPart, testName);
        Account acctGot = mProv.get(AccountBy.name, authAs);
        assertNull(acctGot);
        
        // put the orig back
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, "");
        mProv.modifyAttrs(config, attrs);
    }
    
    
    /*
     * test removing alias
     * 
     * 1. remove alias from mail and zimbraMailAlias attributes of the entry
     * 2. remove alias from all distribution lists
     * 3. delete the alias entry 
     * 
     * A. entry exists, alias exists
     *    - if alias points to the entry:            do 1, 2, 3
     *    - if alias points to other existing entry: do 1, and then throw NO_SUCH_ALIAS
     *    - if alias points to a non-existing entry: do 1, 2, 3, and then throw NO_SUCH_ALIAS
     *  
     * B. entry exists, alias does not exist:  do 1, 2, and then throw NO_SUCH_ALIAS
     * 
     * C. entry does not exist, alias exists:
     *    - if alias points to other existing entry: do nothing (and then throw NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST in ProvUtil)
     *    - if alias points to a non-existing entry: do 2, 3 (and then throw NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST in ProvUtil)
     * 
     * D. entry does not exist, alias does not exist:  do 2 (and then throw NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST in ProvUtil)
     * 
     * 
     */
    
    //
    // A - alias points to the entry
    // 
    @Test
    public void testRemoveAlias_entryExist_aliasExist_aliasPointToEntry() throws Exception {
        // Call toLowerCase to avoid the bug that we don't convert an address to lower case when it is 
        // added as a DL member, and later the address can't be removed from the DL because case does not match.
        String testName = "testRemoveAlias_entryExist_aliasExist_aliasPointToEntry".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = "EE-AE-aliasPointToEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // remove the alias
        mProv.removeAlias(acct, aliasName);
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is removed from the account's mail/zimbraMailAlias attrs
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertFalse(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias is removed from all the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias entry is removed
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 0);
    }
    
    //
    // A - alias points to other existing entry
    //
    @Test
    public void testRemoveAlias_entryExist_aliasExist_aliasPointToOtherEntry() throws Exception {
        String testName = "testRemoveAlias_entryExist_aliasExist_aliasPointToOtherEntry".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = "EE-AE-aliasPointToOtherEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account the alias points to
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // create another account
        String otherAcctName = getEmail("acct-other", domainName);
        Account otherAcct = mProv.createAccount(otherAcctName, PASSWORD, new HashMap<String, Object>());
        
        // and hack the other account to also contain the alias in it's mail/zimbraMailAlias attrs
        // the hacked attrs should be removed after the removeAlais call
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_mail, aliasName);
            attributes.put(Provisioning.A_zimbraMailAlias, aliasName);
            LdapEntry ldapAccount = (LdapEntry)otherAcct;
            ((LdapProv) mProv).getHelper().modifyEntry(ldapAccount.getDN(), attributes, 
                    (Entry)ldapAccount, LdapUsage.UNITTEST);
            
            // make the attrs did get hacked in
            mProv.reload(otherAcct);
            Set<String> values;
            values = otherAcct.getMultiAttrSet(Provisioning.A_mail);
            assertTrue(values.contains(aliasName));
            values = otherAcct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
            assertTrue(values.contains(aliasName));
            
        }
        
        // remove the alias, on the "other" account, which is *not* the target for the alias we are removing
        // ensure we *do* get a NO_SUCH_ALIAS exception
        boolean good = false;
        try {
            mProv.removeAlias(otherAcct, aliasName);
        } catch (ServiceException e) {
            assertEquals(e.getCode(), (AccountServiceException.NO_SUCH_ALIAS));
            good = true;
        }
        assertTrue(good);
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(otherAcct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is still on the account
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertTrue(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertTrue(values.contains(aliasName));
        
        // ensure the hacked in attrs are removed from the other account
        values = otherAcct.getMultiAttrSet(Provisioning.A_mail);
        assertFalse(values.contains(aliasName));
        values = otherAcct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias is *not* removed from any the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertTrue(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertTrue(values.contains(aliasName));
        
        // ensure the alias entry is *not* removed
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 1);
        assertTrue(aliases.get(0).getName().equals(aliasName));
    }
    
    //
    // A - alias points to a non-existing entry
    // 
    @Test
    public void testRemoveAlias_entryExist_aliasExist_aliasPointToNonExistEntry() throws Exception {
        String testName = "testRemoveAlias_entryExist_aliasExist_aliasPointToNonExistEntry".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = "EE-AE-aliasPointToNonExistEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it so the alias points to a non-existing entry
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_zimbraAliasTargetId, LdapUtil.generateUUID());
            
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            ((LdapProv) mProv).getHelper().modifyEntry(ldapAlias.getDN(), attributes, 
                    (Entry)ldapAlias, LdapUsage.UNITTEST);
        }
        
        // remove the alias
        // ensure we *do* get a NO_SUCH_ALIAS exception
        boolean good = false;
        try {
            mProv.removeAlias(acct, aliasName);
        } catch (ServiceException e) {
            assertEquals(e.getCode(), (AccountServiceException.NO_SUCH_ALIAS));
            good = true;
        }
        assertTrue(good);
       
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is removed from the account's mail/zimbraMailAlias attrs
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertFalse(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias is removed from all the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias entry is removed
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 0);
    }
    
    //
    // B
    //
    @Test
    public void testRemoveAlias_entryExist_aliasNotExist() throws Exception {
        String testName = "testRemoveAlias_entryExist_aliasNotExist".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = "EE-AN" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it to delete the alias entry
        {
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            String aliasDn = ldapAlias.getDN();
            ((LdapProv) mProv).getHelper().deleteEntry(aliasDn, LdapUsage.UNITTEST);
        }
        
        // remove the alias
        // ensure we *do* get a NO_SUCH_ALIAS exception
        boolean good = false;
        try {
            mProv.removeAlias(acct, aliasName);
        } catch (ServiceException e) {
            assertEquals(e.getCode(), (AccountServiceException.NO_SUCH_ALIAS));
            good = true;
        }
        assertTrue(good);
       
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is removed from the account's mail/zimbraMailAlias attrs
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertFalse(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias is removed from all the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias entry is removed (should have been removed when we hacked to unbind it)
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 0);
    }
    
    //
    // C - alias points to other existing entry
    // 
    @Test
    public void testRemoveAlias_entryNotExist_aliasExist_aliasPointToOtherEntry() throws Exception {
        String testName = "testRemoveAlias_entryNotExist_aliasExist_aliasPointToOtherEntry".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = "EN-AE-aliasPointToOtherEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account the alias points to
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // non-existing entry account
        Account nonExistingAcct = null;
        
        // remove the alias, on a "not found" account, and the alias is pointing to another existing target
        // we should *not* get the NO_SUCH_ALIAS exception
        mProv.removeAlias(nonExistingAcct, aliasName);
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is still on the account
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertTrue(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertTrue(values.contains(aliasName));
        
        // ensure the alias is *not* removed from any the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertTrue(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertTrue(values.contains(aliasName));
        
        // ensure the alias entry is *not* removed
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 1);
        assertTrue(aliases.get(0).getName().equals(aliasName));
    }
    
    //
    // C - alias points to a non-existing entry
    //
    @Test
    public void testRemoveAlias_entryNotExist_aliasExist_aliasPointToNonExistEntry() throws Exception {
        String testName = "testRemoveAlias_entryNotExist_aliasExist_aliasPointToNonExistEntry".toLowerCase(); //  getName().toLowerCase();  
        
        // create the domain
        String domainName = "EN-AE-aliasPointToNonExistEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it so the alias points to a non-existing entry
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_zimbraAliasTargetId, LdapUtil.generateUUID());
            
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            ((LdapProv) mProv).getHelper().modifyEntry(ldapAlias.getDN(), attributes, 
                    (Entry)ldapAlias, LdapUsage.UNITTEST);
        }
        
        Account nonExistingAcct = null;
        
        // remove the alias, on a "not found" account, and the alias is pointing to a non-existing entry
        // we should *not* get the NO_SUCH_ALIAS exception
        mProv.removeAlias(nonExistingAcct, aliasName);
        
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is still on the account's mail/zimbraMailAlias attrs
        // because there is no ref to this account so there is no way to remove them
        // (note, to remove them, A - aliasPointToNonExistEntry is the test for this)
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertTrue(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertTrue(values.contains(aliasName));
        
        // ensure the alias is removed from all the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias entry is removed
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 0);
    }
    
    //
    // D
    //
    @Test
    public void testRemoveAlias_entryNotExist_aliasNotExist() throws Exception {
        String testName = "testRemoveAlias_entryNotExist_aliasNotExist".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = "EN-AN" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it to delete the alias entry
        {
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            String aliasDn = ldapAlias.getDN();
            ((LdapProv) mProv).getHelper().deleteEntry(aliasDn, LdapUsage.UNITTEST);
        }
        
        Account nonExistingAcct = null;
        
        // remove the alias
        // we should *not* get a NO_SUCH_ALIAS exception
        mProv.removeAlias(nonExistingAcct, aliasName);
        
        // reload all entries
        mProv.reload(acct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is still on the account's mail/zimbraMailAlias attrs
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertTrue(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertTrue(values.contains(aliasName));
        
        // ensure the alias is removed from all the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias entry is removed (should have been removed when we hacked to unbind it)
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 0);
    }
    
    @Test
    public void testRemoveAlias_aliasNameExistsButIsNotAnAlias() throws Exception {
        String testName = "testRemoveAlias_aliasNameExistsButIsNotAnAlias".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // create another account
        String acct2Name = getEmail("acct-2", domainName);
        Account acct2 = mProv.createAccount(acct2Name, PASSWORD, new HashMap<String, Object>());
        String acct2Id = acct2.getId();
        
        // create a distribution list
        String dlName = getEmail("dl", domainName);
        DistributionList dl = mProv.createDistributionList(dlName, new HashMap<String, Object>());
        String dlId = dl.getId();
        
        boolean good = false;
        try {
            mProv.removeAlias(acct, acct2Name);
        } catch (ServiceException e) {
            if (AccountServiceException.NO_SUCH_ALIAS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        // make sure the account is not touched
        mProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(Key.CacheEntryBy.id, acct2Id)});
        acct2 = mProv.get(AccountBy.id, acct2Id);
        assertNotNull(acct2);
        assertEquals(acct2Id, acct2.getId());
        assertEquals(acct2Name, acct2.getName());
        
        try {
            mProv.removeAlias(acct, dlName);
        } catch (ServiceException e) {
            if (AccountServiceException.NO_SUCH_ALIAS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        // make sure the dl is not touched
        // mProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(CacheEntryBy.id, acct2Id)});
        dl = mProv.get(Key.DistributionListBy.id, dlId);
        assertNotNull(dl);
        assertEquals(dlId, dl.getId());
        assertEquals(dlName, dl.getName());
    }
    
    /*
     * test adding an alias to account but the alias is "dangling"
     * i.e. the alias entry exists but points to a non-existing entry
     * 
     * The dangling alias should be removed then recreated and then added to the account
     */
    @Test
    public void testCreateAlias_aliasExistAndDangling() throws Exception {
        String testName = "testCreateAlias_aliasExistAndDangling".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        mProv.addAlias(acct, aliasName);
        
        // remember the zimbraId of the alias entry
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 1);
        String origZimbraIdOfAlias = aliases.get(0).getId();
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = mProv.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = mProv.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        mProv.addMembers(dl1, new String[]{aliasName});
        mProv.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it to delete the orig account entry
        {
            LdapEntry ldapAccount = (LdapEntry)acct;
            ((LdapProv) mProv).getHelper().deleteEntry(ldapAccount.getDN(), LdapUsage.UNITTEST);
        }
        
        // now , try to add the alias to another account
        String otherAcctName = getEmail("acct-other", domainName);
        Account otherAcct = mProv.createAccount(otherAcctName, PASSWORD, new HashMap<String, Object>());
        mProv.addAlias(otherAcct, aliasName);
        
        // reload all entries
        // mProv.reload(acct); this account should be gone already
        mProv.reload(otherAcct);
        mProv.reload(dl1);
        mProv.reload(dl2);

        Set<String> values;
        
        // ensure the alias is added to the other account
        values = acct.getMultiAttrSet(Provisioning.A_mail);
        assertTrue(values.contains(aliasName));
        values = acct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
        assertTrue(values.contains(aliasName));
        
        // ensure the alias is removed from all the DLs
        values = dl1.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        values = dl2.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        assertFalse(values.contains(aliasName));
        
        // ensure the alias entry is is recreated (by verifing that it's got a diff zimbraId)
        aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 1);
        assertFalse(aliases.get(0).getId().equals(origZimbraIdOfAlias));
    }
    
    @Test
    public void testCreateAlias_aliasNameExistsButIsNotAnAlias() throws Exception {
        String testName = "testCreateAlias_aliasNameExistsButIsNotAnAlias".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = mProv.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // create another account
        String acct2Name = getEmail("acct-2", domainName);
        Account acct2 = mProv.createAccount(acct2Name, PASSWORD, new HashMap<String, Object>());
        
        // create a distribution list
        String dlName = getEmail("dl", domainName);
        DistributionList dl = mProv.createDistributionList(dlName, new HashMap<String, Object>());
        
        boolean good = false;
        try {
            mProv.addAlias(acct, acct2Name);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        try {
            mProv.addAlias(acct, dlName);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
    }
    
    @Test
    public void testCreateAlias_aliasExists() throws Exception {
        String testName = "testCreateAlias_aliasExists".toLowerCase(); // getName().toLowerCase();  
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
        Domain domain  = mProv.createDomain(domainName, attrs);
        
        // create the account
        String acct1Name = getEmail("acct-1", domainName);
        Account acct1 = mProv.createAccount(acct1Name, PASSWORD, new HashMap<String, Object>());
        
        // create another account
        String acct2Name = getEmail("acct-2", domainName);
        Account acct2 = mProv.createAccount(acct2Name, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to acct1
        String aliasName = getEmail("alias", domainName);
        mProv.addAlias(acct1, aliasName);
        
        // add the same alias to acct2, should get error
        boolean good = false;
        try {
            mProv.addAlias(acct2, aliasName);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
    }
    
    /*
     * To repro:
     *    - create two domains
     *      zmporv -l cd main.com
     *      zmporv -l cd other.com
     *      
     *    - create an account in the main domain
     *      zmprov -l ca junk@main.com test123
     *      
     *    - add two aliases to the account, one in the same domain, the other in the other domain
     *      zmprov -l aaa junk@main.com phoebe@main.com  (this is required to repro)
     *      zmprov -l aaa junk@main.com phoebe@other.com (can be skipped - A)
     *      
     *    - remove the alias in the other domain
     *      zmprov -l raa junk@main.com phoebe@other.com (can be skipped if A is skipped) 
     *      
     *    - now, rename the account to the alias just removed
     *      zmprov -v -l ra junk@main.com phoebe@other.com 
[] WARN: unable to move alias from uid=phoebe,ou=people,dc=main,dc=com to uid=phoebe,ou=people,dc=other,dc=com
javax.naming.NameAlreadyBoundException: [LDAP: error code 68 - Entry Already Exists]; remaining name 'uid=phoebe,ou=people,dc=main,dc=com'
        at com.sun.jndi.ldap.LdapCtx.mapErrorCode(LdapCtx.java:3012)
        at com.sun.jndi.ldap.LdapCtx.processReturnCode(LdapCtx.java:2963)
        at com.sun.jndi.ldap.LdapCtx.processReturnCode(LdapCtx.java:2769)
        at com.sun.jndi.ldap.LdapCtx.c_rename(LdapCtx.java:699)
        at com.sun.jndi.toolkit.ctx.ComponentContext.p_rename(ComponentContext.java:693)
        at com.sun.jndi.toolkit.ctx.PartialCompositeContext.rename(PartialCompositeContext.java:251)
        at javax.naming.InitialContext.rename(InitialContext.java:389)
        at com.zimbra.cs.account.ldap.ZimbraLdapContext.renameEntry(ZimbraLdapContext.java:756)
        at com.zimbra.cs.account.ldap.LdapProvisioning.moveAliases(LdapProvisioning.java:5398)
        at com.zimbra.cs.account.ldap.LdapProvisioning.renameAccount(LdapProvisioning.java:2297)
        at com.zimbra.cs.account.ProvUtil.execute(ProvUtil.java:934)
        at com.zimbra.cs.account.ProvUtil.main(ProvUtil.java:2810)
        
        This only happens if 
        - domain for the account is also changed for the renameAccount
        - when the account is being renamed, there is an alias named as:
          {same localpart as the account's new localpart}@{same domain as the account's old domain}
     
        This is because when we do validation to see if there is any clash with new alias names, 
        the account has not been renamed yet, therefore it is not caught.
        
        After the fix, it should throw ACCOUNT_EXISTS (com.zimbra.cs.account.AccountServiceException: email address already exists: phoebe@other.com)
        i.e. the renameAccount should not be allowed
      */
    @Test
    public void testBug41884() throws Exception {
        String OLD_DOMAIN_NAME = "main." + BASE_DOMAIN_NAME;
        String NEW_DOMAIN_NAME = "other." + BASE_DOMAIN_NAME;
        String OLD_LOCALPART = "junk";
        String NEW_LOCALPART = "phoebe";
        String OLD_ACCT_NAME = OLD_LOCALPART + "@" + OLD_DOMAIN_NAME;
        String NEW_ACCT_NAME = NEW_LOCALPART + "@" + NEW_DOMAIN_NAME;
        String ALIAS_NAME = NEW_LOCALPART + "@" + OLD_DOMAIN_NAME;
        
        Domain oldDomain = mProv.createDomain(OLD_DOMAIN_NAME, new HashMap<String, Object>());
        Domain newDomain = mProv.createDomain(NEW_DOMAIN_NAME, new HashMap<String, Object>());
        
        Account acct = mProv.createAccount(OLD_ACCT_NAME, "test123", new HashMap<String, Object>());
        mProv.addAlias(acct, ALIAS_NAME);
        
        boolean good = false;
        try {
            mProv.renameAccount(acct.getId(), NEW_ACCT_NAME);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode())) {
                good = true;
            }
        }
        assertTrue(good);
    }


}
