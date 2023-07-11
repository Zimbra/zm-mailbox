/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest.prov.ldap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
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
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.qa.QA.Bug;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestProvAlias extends LdapTest {
    private static String PASSWORD = "test123";
    
    private static String BASE_DOMAIN_NAME;
    
    private static String LOCAL_DOMAIN_NAME;
    private static String ALIAS_DOMAIN_NAME;
    
    private static String origDefaultDomainName;
    
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
        
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        
        Config config = prov.getConfig();
        origDefaultDomainName = config.getAttr(Provisioning.A_zimbraDefaultDomainName);
        
        initTest();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, origDefaultDomainName);
        prov.modifyAttrs(config, attrs);
        
        Cleanup.deleteAll(baseDomainName());
    }
    
    /*
     * convert underscores in inStr to hyphens
     */
    private String underscoreToHyphen(String inStr) {
        return inStr.replaceAll("_", "-");
    }
    
    private static void initTest() throws Exception {
        
        BASE_DOMAIN_NAME = baseDomainName();
        LOCAL_DOMAIN_NAME = "local." + BASE_DOMAIN_NAME;
        ALIAS_DOMAIN_NAME = "alias." + BASE_DOMAIN_NAME;
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        // create the local domain
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain localDomain = provUtil.createDomain(LOCAL_DOMAIN_NAME, attrs);
        
        // create the alias domain
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.alias.name());
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, localDomain.getId());
        Domain aliasDomain = provUtil.createDomain(ALIAS_DOMAIN_NAME, attrs);
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
        return prov.searchDirectory(options);
    }
    
    /*
        Case 1:
            Alias1@localdomain.com points at account1@localdomain.com.
            Auth is attempted with alias1@aliasdomain.com. Does it work?  YES
    */
    @Test
    public void testAliasDomain_Case1() throws Exception {
        String testName = getTestName();
        
        String acctLocalPart = "account1";
        String acctName = getEmail(acctLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        String aliasLocalPart = "alias1";
        String aliasName = getEmail(aliasLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        Account acct = prov.createAccount(acctName, PASSWORD, null);
        prov.addAlias(acct, aliasName);
        
        String authAs = getEmail(aliasLocalPart, ALIAS_DOMAIN_NAME, testName);
        Account acctGot = prov.get(AccountBy.name, authAs);
        assertEquals(acct.getId(), acctGot.getId());
        assertEquals(acct.getName(), acctGot.getName());
        
        prov.authAccount(acctGot, PASSWORD, AuthContext.Protocol.test);
    }

    /*
        Case 2:
            Alias1@aliasdomain.com points at account1@localdomain.com.  (there is no alias1@localdomain.com alias)
            Auth is attempted with alias1@localdomain.com.    Does it work?  NO
            Auth is attempted with account1@aliasdomain.com.  Does it work?  YES
    */
    @Test
    public void testAliasDomain_Case2() throws Exception {
        String testName = getTestName();
        
        String acctLocalPart = "account1";
        String acctName = getEmail(acctLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        String aliasLocalPart = "alias1";
        String aliasName = getEmail(aliasLocalPart, ALIAS_DOMAIN_NAME, testName);
        
        Account acct = prov.createAccount(acctName, PASSWORD, null);
        prov.addAlias(acct, aliasName);
        
        String authAs = getEmail(aliasLocalPart, LOCAL_DOMAIN_NAME, testName);
        Account acctGot = prov.get(AccountBy.name, authAs);
        assertNull(acctGot);
        
        authAs = getEmail(acctLocalPart, ALIAS_DOMAIN_NAME, testName);
        acctGot = prov.get(AccountBy.name, authAs);
        assertEquals(acct.getId(), acctGot.getId());
        assertEquals(acct.getName(), acctGot.getName());
        
        prov.authAccount(acctGot, PASSWORD, AuthContext.Protocol.test);
    }
    
    /*
        Case 3:
            Alias1@aliasdomain.com points at account1@localdomain.com.  (there is no alias1@localdomain.com alias)
            Global config zimbra default domain is set to localdomain.com.
            Auth is attempted with "alias1".  Does it work?  NO
            
    */
    @Test
    public void testAliasDomain_Case3() throws Exception {
        String testName = getTestName();
        
        String acctLocalPart = "account1";
        String acctName = getEmail(acctLocalPart, LOCAL_DOMAIN_NAME, testName);
        
        String aliasLocalPart = "alias1";
        String aliasName = getEmail(aliasLocalPart, ALIAS_DOMAIN_NAME, testName);
        
        Config config = prov.getConfig();
        String origDefaltDomainName = config.getAttr(Provisioning.A_zimbraDefaultDomainName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, LOCAL_DOMAIN_NAME);
        prov.modifyAttrs(config, attrs);
        
        Account acct = prov.createAccount(acctName, PASSWORD, null);
        prov.addAlias(acct, aliasName);
        
        String authAs = getLocalPart(aliasLocalPart, testName);
        Account acctGot = prov.get(AccountBy.name, authAs);
        assertNull(acctGot);
        
        // put the orig back
        attrs.clear();
        attrs.put(Provisioning.A_zimbraDefaultDomainName, "");
        prov.modifyAttrs(config, attrs);
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
        // getTestName retusn all lower case.  This avoids the bug that we don't convert 
        // an address to lower case when it is added as a DL member, and later the address 
        // can't be removed from the DL because case does not match.
        String testName = getTestName();
        
        // create the domain
        String domainName = "EE-AE-aliasPointToEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // remove the alias
        prov.removeAlias(acct, aliasName);
        
        // reload all entries
        prov.reload(acct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = "EE-AE-aliasPointToOtherEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account the alias points to
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // create another account
        String otherAcctName = getEmail("acct-other", domainName);
        Account otherAcct = prov.createAccount(otherAcctName, PASSWORD, new HashMap<String, Object>());
        
        // and hack the other account to also contain the alias in it's mail/zimbraMailAlias attrs
        // the hacked attrs should be removed after the removeAlais call
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            
            // can no long do this, we now have an unique constraint on mail
            // attributes.put(Provisioning.A_mail, aliasName);
            
            attributes.put(Provisioning.A_zimbraMailAlias, aliasName);
            LdapEntry ldapAccount = (LdapEntry)otherAcct;
            ((LdapProv) prov).getHelper().modifyEntry(ldapAccount.getDN(), attributes, 
                    (Entry)ldapAccount, LdapUsage.UNITTEST);
            
            // make sure the attrs did get hacked in
            prov.reload(otherAcct);
            Set<String> values;
            // values = otherAcct.getMultiAttrSet(Provisioning.A_mail);
            // assertTrue(values.contains(aliasName));
            values = otherAcct.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
            assertTrue(values.contains(aliasName));
            
        }
        
        // remove the alias, on the "other" account, which is *not* the target for the alias we are removing
        // ensure we *do* get a NO_SUCH_ALIAS exception
        boolean good = false;
        try {
            prov.removeAlias(otherAcct, aliasName);
        } catch (ServiceException e) {
            assertEquals(e.getCode(), (AccountServiceException.NO_SUCH_ALIAS));
            good = true;
        }
        assertTrue(good);
        
        // reload all entries
        prov.reload(acct);
        prov.reload(otherAcct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = "EE-AE-aliasPointToNonExistEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it so the alias points to a non-existing entry
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_zimbraAliasTargetId, LdapUtil.generateUUID());
            
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            ((LdapProv) prov).getHelper().modifyEntry(ldapAlias.getDN(), attributes, 
                    (Entry)ldapAlias, LdapUsage.UNITTEST);
        }
        
        // remove the alias
        // ensure we *do* get a NO_SUCH_ALIAS exception
        boolean good = false;
        try {
            prov.removeAlias(acct, aliasName);
        } catch (ServiceException e) {
            assertEquals(e.getCode(), (AccountServiceException.NO_SUCH_ALIAS));
            good = true;
        }
        assertTrue(good);
       
        
        // reload all entries
        prov.reload(acct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = "EE-AN" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it to delete the alias entry
        {
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            String aliasDn = ldapAlias.getDN();
            ((LdapProv) prov).getHelper().deleteEntry(aliasDn, LdapUsage.UNITTEST);
        }
        
        // remove the alias
        // ensure we *do* get a NO_SUCH_ALIAS exception
        boolean good = false;
        try {
            prov.removeAlias(acct, aliasName);
        } catch (ServiceException e) {
            assertEquals(e.getCode(), (AccountServiceException.NO_SUCH_ALIAS));
            good = true;
        }
        assertTrue(good);
       
        
        // reload all entries
        prov.reload(acct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = "EN-AE-aliasPointToOtherEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account the alias points to
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // non-existing entry account
        Account nonExistingAcct = null;
        
        // remove the alias, on a "not found" account, and the alias is pointing to another existing target
        // we should *not* get the NO_SUCH_ALIAS exception
        prov.removeAlias(nonExistingAcct, aliasName);
        
        // reload all entries
        prov.reload(acct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName(); 
        
        // create the domain
        String domainName = "EN-AE-aliasPointToNonExistEntry" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it so the alias points to a non-existing entry
        {
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put(Provisioning.A_zimbraAliasTargetId, LdapUtil.generateUUID());
            
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            ((LdapProv) prov).getHelper().modifyEntry(ldapAlias.getDN(), attributes, 
                    (Entry)ldapAlias, LdapUsage.UNITTEST);
        }
        
        Account nonExistingAcct = null;
        
        // remove the alias, on a "not found" account, and the alias is pointing to a non-existing entry
        // we should *not* get the NO_SUCH_ALIAS exception
        prov.removeAlias(nonExistingAcct, aliasName);
        
        
        // reload all entries
        prov.reload(acct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = "EN-AN" + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it to delete the alias entry
        {
            List<NamedEntry> aliases = searchAliasesInDomain(domain);
            assertEquals(aliases.size(), 1);
            LdapEntry ldapAlias = (LdapEntry)aliases.get(0);
            String aliasDn = ldapAlias.getDN();
            ((LdapProv) prov).getHelper().deleteEntry(aliasDn, LdapUsage.UNITTEST);
        }
        
        Account nonExistingAcct = null;
        
        // remove the alias
        // we should *not* get a NO_SUCH_ALIAS exception
        prov.removeAlias(nonExistingAcct, aliasName);
        
        // reload all entries
        prov.reload(acct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // create another account
        String acct2Name = getEmail("acct-2", domainName);
        Account acct2 = prov.createAccount(acct2Name, PASSWORD, new HashMap<String, Object>());
        String acct2Id = acct2.getId();
        
        // create a distribution list
        String dlName = getEmail("dl", domainName);
        DistributionList dl = prov.createDistributionList(dlName, new HashMap<String, Object>());
        String dlId = dl.getId();
        
        boolean good = false;
        try {
            prov.removeAlias(acct, acct2Name);
        } catch (ServiceException e) {
            if (AccountServiceException.NO_SUCH_ALIAS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        // make sure the account is not touched
        prov.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(Key.CacheEntryBy.id, acct2Id)});
        acct2 = prov.get(AccountBy.id, acct2Id);
        assertNotNull(acct2);
        assertEquals(acct2Id, acct2.getId());
        assertEquals(acct2Name, acct2.getName());
        
        try {
            prov.removeAlias(acct, dlName);
        } catch (ServiceException e) {
            if (AccountServiceException.NO_SUCH_ALIAS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        // make sure the dl is not touched
        // mProv.flushCache(CacheEntryType.account, new CacheEntry[]{new CacheEntry(CacheEntryBy.id, acct2Id)});
        dl = prov.get(Key.DistributionListBy.id, dlId);
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
        String testName = getTestName();
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to the account
        String aliasName = getEmail("alias-1", domainName);
        prov.addAlias(acct, aliasName);
        
        // remember the zimbraId of the alias entry
        List<NamedEntry> aliases = searchAliasesInDomain(domain);
        assertEquals(aliases.size(), 1);
        String origZimbraIdOfAlias = aliases.get(0).getId();
        
        // create 2 DLs
        String dl1Name = getEmail("dl-1", domainName);
        DistributionList dl1 = prov.createDistributionList(dl1Name, new HashMap<String, Object>());
        
        String dl2Name = getEmail("dl-2", domainName);
        DistributionList dl2 = prov.createDistributionList(dl2Name, new HashMap<String, Object>());
        
        // add the alias to the two DLs
        prov.addMembers(dl1, new String[]{aliasName});
        prov.addMembers(dl2, new String[]{aliasName});
        
        // now, hack it to delete the orig account entry
        {
            LdapEntry ldapAccount = (LdapEntry)acct;
            ((LdapProv) prov).getHelper().deleteEntry(ldapAccount.getDN(), LdapUsage.UNITTEST);
        }
        
        // now , try to add the alias to another account
        String otherAcctName = getEmail("acct-other", domainName);
        Account otherAcct = prov.createAccount(otherAcctName, PASSWORD, new HashMap<String, Object>());
        prov.addAlias(otherAcct, aliasName);
        
        // reload all entries
        // mProv.reload(acct); this account should be gone already
        prov.reload(otherAcct);
        prov.reload(dl1);
        prov.reload(dl2);

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
        String testName = getTestName();
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acctName = getEmail("acct-1", domainName);
        Account acct = prov.createAccount(acctName, PASSWORD, new HashMap<String, Object>());
        
        // create another account
        String acct2Name = getEmail("acct-2", domainName);
        Account acct2 = prov.createAccount(acct2Name, PASSWORD, new HashMap<String, Object>());
        
        // create a distribution list
        String dlName = getEmail("dl", domainName);
        DistributionList dl = prov.createDistributionList(dlName, new HashMap<String, Object>());
        
        boolean good = false;
        try {
            prov.addAlias(acct, acct2Name);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
        
        try {
            prov.addAlias(acct, dlName, false);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode()))
                good = true;
        }
        assertTrue(good);
    }
    
    @Test
    public void testCreateAlias_aliasExists() throws Exception {
        String testName = getTestName();
        
        // create the domain
        String domainName = underscoreToHyphen(testName) + "." + BASE_DOMAIN_NAME;
        domainName = domainName.toLowerCase();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, Provisioning.DomainType.local.name());
        Domain domain  = prov.createDomain(domainName, attrs);
        
        // create the account
        String acct1Name = getEmail("acct-1", domainName);
        Account acct1 = prov.createAccount(acct1Name, PASSWORD, new HashMap<String, Object>());
        
        // create another account
        String acct2Name = getEmail("acct-2", domainName);
        Account acct2 = prov.createAccount(acct2Name, PASSWORD, new HashMap<String, Object>());
        
        // add an alias to acct1
        String aliasName = getEmail("alias", domainName);
        prov.addAlias(acct1, aliasName);
        
        // add the same alias to acct2, should get error
        boolean good = false;
        try {
            prov.addAlias(acct2, aliasName);
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
    @Bug(bug=41884)
    public void testBug41884() throws Exception {
        String OLD_DOMAIN_NAME = "main." + BASE_DOMAIN_NAME;
        String NEW_DOMAIN_NAME = "other." + BASE_DOMAIN_NAME;
        String OLD_LOCALPART = "junk";
        String NEW_LOCALPART = "phoebe";
        String OLD_ACCT_NAME = OLD_LOCALPART + "@" + OLD_DOMAIN_NAME;
        String NEW_ACCT_NAME = NEW_LOCALPART + "@" + NEW_DOMAIN_NAME;
        String ALIAS_NAME = NEW_LOCALPART + "@" + OLD_DOMAIN_NAME;
        
        Domain oldDomain = prov.createDomain(OLD_DOMAIN_NAME, new HashMap<String, Object>());
        Domain newDomain = prov.createDomain(NEW_DOMAIN_NAME, new HashMap<String, Object>());
        
        Account acct = prov.createAccount(OLD_ACCT_NAME, "test123", new HashMap<String, Object>());
        prov.addAlias(acct, ALIAS_NAME);
        
        boolean good = false;
        try {
            prov.renameAccount(acct.getId(), NEW_ACCT_NAME);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode())) {
                good = true;
            }
        }
        assertTrue(good);
    }

}
