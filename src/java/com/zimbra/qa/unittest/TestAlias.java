/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.ldap.LdapEntry;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.qa.unittest.TestProvisioningUtil.IDNName;

public class TestAlias extends TestCase {
    private static String TEST_NAME = "test-alias";
    private static String PASSWORD = "test123";
   
    private static Provisioning mProv;
    
    private static String BASE_DOMAIN_NAME;
    
    private static String LOCAL_DOMAIN_NAME;
    private static String ALIAS_DOMAIN_NAME;
    
    /*
     * convert underscores in inStr to hyphens
     */
    private String underscoreToHyphen(String inStr) {
        return inStr.replaceAll("_", "-");
    }
    
    public void testInit() throws Exception {
        mProv = Provisioning.getInstance();
        
        String TEST_ID = TestProvisioningUtil.genTestId();
        
        BASE_DOMAIN_NAME = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
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
        Provisioning.SearchOptions options = new Provisioning.SearchOptions();
        
        int flags = 0;
        flags = Provisioning.SA_ALIAS_FLAG;
        options.setFlags(flags);
        options.setDomain(domain);
        return mProv.searchDirectory(options);
    }
    
    /*
        Case 1:
            Alias1@localdomain.com points at account1@localdomain.com.
            Auth is attempted with alias1@aliasdomain.com. Does it work?  YES
    */
    public void testAliasDomain_Case1() throws Exception {
        String testName = getName();
        
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
    public void testAliasDomain_Case2() throws Exception {
        String testName = getName();
        
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
    public void testAliasDomain_Case3() throws Exception {
        String testName = getName();
        
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
    public void testRemoveAlias_entryExist_aliasExist_aliasPointToEntry() throws Exception {
        // Call toLowerCase to avoid the bug that we don't convert an address to lower case when it is 
        // added as a DL member, and later the address can't be removed from the DL because case does not match.
        String testName = getName().toLowerCase();  
        
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
    public void testRemoveAlias_entryExist_aliasExist_aliasPointToOtherEntry() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_mail, aliasName);
            attributes.put(Provisioning.A_zimbraMailAlias, aliasName);
            LdapEntry ldapAccount = (LdapEntry)otherAcct;
            LdapUtil.modifyAttrs(zlc, ldapAccount.getDN(), attributes, (Entry)ldapAccount);
            
            // make the attrs did get hacked in
            mProv.reload(otherAcct);
            Set<String> values;
            values = otherAcct.getMultiAttrSet(Provisioning.A_mail);
            assertTrue(values.contains(aliasName));
            values = otherAcct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
            assertTrue(values.contains(aliasName));
            
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
    public void testRemoveAlias_entryExist_aliasExist_aliasPointToNonExistEntry() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_zimbraAliasTargetId, LdapUtil.generateUUID());
            
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            LdapUtil.modifyAttrs(zlc, ldapAlias.getDN(), attributes, (Entry)ldapAlias);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
    public void testRemoveAlias_entryExist_aliasNotExist() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            String aliasDn = ldapAlias.getDN();
            zlc.unbindEntry(aliasDn);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
    public void testRemoveAlias_entryNotExist_aliasExist_aliasPointToOtherEntry() throws Exception {
        String testName = getName().toLowerCase();  
        
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
    public void testRemoveAlias_entryNotExist_aliasExist_aliasPointToNonExistEntry() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_zimbraAliasTargetId, LdapUtil.generateUUID());
            
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            LdapUtil.modifyAttrs(zlc, ldapAlias.getDN(), attributes, (Entry)ldapAlias);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
    public void testRemoveAlias_entryNotExist_aliasNotExist() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            String aliasDn = ldapAlias.getDN();
            zlc.unbindEntry(aliasDn);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
    
    public void testRemoveAlias_aliasNameExistsButIsNotAnAlias() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        mProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(CacheEntryBy.id, acct2Id)});
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
        dl = mProv.get(DistributionListBy.id, dlId);
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
    public void testCreateAlias_aliasExistAndDangling() throws Exception {
        String testName = getName().toLowerCase();  
        
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
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            LdapEntry ldapAccount = (LdapEntry)acct;
            zlc.unbindEntry(ldapAccount.getDN());
        } finally {
            ZimbraLdapContext.closeContext(zlc);
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
    
    public void testCreateAlias_aliasNameExistsButIsNotAnAlias() throws Exception {
        String testName = getName().toLowerCase();  
        
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
    
    public void testCreateAlias_aliasExists() throws Exception {
        String testName = getName().toLowerCase();  
        
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
    
    public static void main(String[] args) throws Exception {
        // TestUtil.cliSetup();
        CliUtil.toolSetup();
        TestUtil.runTest(new TestSuite(TestAlias.class));
    }

}
