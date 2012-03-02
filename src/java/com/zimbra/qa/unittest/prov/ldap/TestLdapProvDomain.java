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
package com.zimbra.qa.unittest.prov.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapHelper;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestLdapProvDomain extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private static String makeTestDomainName(String prefix) {
        String baseDomainName = baseDomainName();
        String domainName;
        if (prefix == null) {
            domainName = baseDomainName;
        } else {
            domainName = prefix.toLowerCase() + "." + baseDomainName;
        }
        
        return Names.makeDomainName(domainName);
    }
    
    private Domain createDomain(String domainName) throws Exception {
        return createDomain(domainName, null);
    }
    
    private Domain createDomain(String domainName, Map<String, Object> attrs) 
    throws Exception {
        return provUtil.createDomain(domainName, attrs);
    }
    
    private void deleteDomain(Domain domain) throws Exception {
        provUtil.deleteDomain(domain);
    }
    
    
    private void verifyAllDomains(List<Domain> allDomains) throws Exception {
        // domains created by r-t-w
        // TODO: this verification is very fragile
        Set<String> expectedDomains = new HashSet<String>();
        String defaultDomainName = prov.getInstance().getConfig().getDefaultDomainName();
        
        expectedDomains.add(defaultDomainName);
        expectedDomains.add("example.com");
        
        assertEquals(expectedDomains.size(), allDomains.size());
        
        for (Domain domain : allDomains) {
            assertTrue(expectedDomains.contains(domain.getName()));
        }
        
        //
        // another verification
        //
        LdapHelper ldapHelper = ((LdapProv) prov).getHelper();
        final List<String /* zimbraId */> domainIds = new ArrayList<String>();
        SearchLdapOptions.SearchLdapVisitor visitor = new SearchLdapOptions.SearchLdapVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
                try {
                    domainIds.add(ldapAttrs.getAttrString(Provisioning.A_zimbraId));
                } catch (ServiceException e) {
                    fail();
                }
            }
        };
        
        SearchLdapOptions searchOpts = new SearchLdapOptions(LdapConstants.DN_ROOT_DSE, 
                ZLdapFilterFactory.getInstance().fromFilterString(FilterId.UNITTEST, "(objectclass=zimbraDomain)"), 
                new String[]{Provisioning.A_zimbraId}, 
                SearchLdapOptions.SIZE_UNLIMITED, null, ZSearchScope.SEARCH_SCOPE_SUBTREE, 
                visitor);
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapUsage.UNITTEST);
            ldapHelper.searchLdap(zlc, searchOpts);
        } finally {
            LdapClient.closeContext(zlc);
        }
        
        assertEquals(domainIds.size(), allDomains.size());
        
        for (Domain domain : allDomains) {
            assertTrue(domainIds.contains(domain.getId()));
        }
    }
    
    private void getDomainById(String id) throws Exception {
        prov.flushCache(CacheEntryType.domain, null);
        Domain domain = prov.get(Key.DomainBy.id, id);
        assertEquals(id, domain.getId());
    }
    
    private void getDomainByName(String name) throws Exception {
        prov.flushCache(CacheEntryType.domain, null);
        Domain domain = prov.get(Key.DomainBy.name, name);
        assertEquals(IDNUtil.toAsciiDomainName(name), domain.getName());
    }
    
    private void getDomainByVirtualHostname(String virtualHostname, String expectedDomainId) 
    throws Exception {
        prov.flushCache(CacheEntryType.domain, null);
        Domain domain = prov.get(Key.DomainBy.virtualHostname, virtualHostname);
        assertEquals(expectedDomainId, domain.getId());
    }
    
    private void getDomainByKrb5Realm(String krb5Realm, String expectedDomainId) throws Exception {
        prov.flushCache(CacheEntryType.domain, null);
        Domain domain = prov.get(Key.DomainBy.krb5Realm, krb5Realm);
        assertEquals(expectedDomainId, domain.getId());
    }
    
    private void getDomainByForeignName(String foreignName, String expectedDomainId) throws Exception {
        prov.flushCache(CacheEntryType.domain, null);
        Domain domain = prov.get(Key.DomainBy.foreignName, foreignName);
        assertEquals(expectedDomainId, domain.getId());
    }

    @Test
    public void createTopDomain() throws Exception {
        String DOMAIN_NAME = makeTestDomainName(null);
        Domain domain = createDomain(DOMAIN_NAME);
        
        deleteDomain(domain);
    }
    
    @Test
    public void createSubDomain() throws Exception {
        String DOMAIN_NAME = makeTestDomainName(genDomainSegmentName() + ".sub1.sub2");
        Domain domain = createDomain(DOMAIN_NAME);
        
        deleteDomain(domain);
    }
    
    @Test
    public void createDomainAlreadyExists() throws Exception {
        String DOMAIN_NAME = makeTestDomainName(genDomainSegmentName());
        Domain domain = createDomain(DOMAIN_NAME);
        
        boolean caughtException = false;
        try {
            prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        } catch (AccountServiceException e) {
            if (AccountServiceException.DOMAIN_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteDomain(domain);
    }
    
    @Test
    public void deleteNonEmptyDomain() throws Exception {
        String DOMAIN_NAME = makeTestDomainName(genDomainSegmentName());
        Domain domain = createDomain(DOMAIN_NAME);
        
        String ACCT_NAME = TestUtil.getAddress("acct", DOMAIN_NAME);
        Account acct = prov.createAccount(ACCT_NAME, "test123", null);
        
        boolean caughtException = false;
        try {
            prov.deleteDomain(domain.getId());
        } catch (ServiceException e) {
            if (AccountServiceException.DOMAIN_NOT_EMPTY.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        // now should able to delete domain
        prov.deleteAccount(acct.getId());
        deleteDomain(domain);
    }
    
    @Test
    public void getAllDomain() throws Exception {
        List<Domain> allDomains = prov.getAllDomains();
        verifyAllDomains(allDomains);
    }
    
    @Test
    public void getAllDomainVisitor() throws Exception {
        final List<Domain> allDomains = new ArrayList<Domain>();
        
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                allDomains.add((Domain) entry);
            }
        };
        
        prov.getAllDomains(visitor, new String[]{Provisioning.A_zimbraId});
        
        verifyAllDomains(allDomains);
    }
    
    @Test
    public void testAliasDomain() throws Exception {
        String TARGET_DOMAIN_NAME = makeTestDomainName(genDomainSegmentName("target"));
        String ALIAS_DOMAIN_NAME = makeTestDomainName(genDomainSegmentName("alias"));
        String USER_LOCAL_PART = "user";
        
        Domain targetDomain = prov.get(Key.DomainBy.name, TARGET_DOMAIN_NAME);
        assertNull(targetDomain);
        Domain aliasDomain = prov.get(Key.DomainBy.name, ALIAS_DOMAIN_NAME);
        assertNull(aliasDomain);
        
        targetDomain = createDomain(TARGET_DOMAIN_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, ZAttrProvisioning.DomainType.alias.name());
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, targetDomain.getId());
        aliasDomain = createDomain(ALIAS_DOMAIN_NAME, attrs);
        
        String realEmail = prov.getEmailAddrByDomainAlias(TestUtil.getAddress(USER_LOCAL_PART, ALIAS_DOMAIN_NAME));
        assertEquals(IDNUtil.toAscii(TestUtil.getAddress(USER_LOCAL_PART, TARGET_DOMAIN_NAME)), 
                realEmail);
        
        deleteDomain(aliasDomain);
        deleteDomain(targetDomain);
    }
    
    @Test
    public void getEmailAddrByDomainAlias() throws Exception {
        // tested in testAliasDomain
    }

    @Test
    public void getDomain() throws Exception {
        String DOMAIN_NAME = makeTestDomainName(genDomainSegmentName());
        
        String VIRTUAL_HOSTNAME = "virtual.com";
        String KRB5_REALM = "KRB5.REALM";
        String FOREIGN_NAME = "foreignname";
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, VIRTUAL_HOSTNAME);
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, KRB5_REALM);
        attrs.put(Provisioning.A_zimbraForeignName, FOREIGN_NAME);
        
        Domain domain = createDomain(DOMAIN_NAME, attrs);
        
        String domainId = domain.getId();
        
        getDomainById(domainId);
        getDomainByName(DOMAIN_NAME);
        getDomainByVirtualHostname(VIRTUAL_HOSTNAME, domainId);
        getDomainByKrb5Realm(KRB5_REALM, domainId);
        getDomainByForeignName(FOREIGN_NAME, domainId);
        
        deleteDomain(domain);
    }
    
    @Test
    public void getDomainNotExist() throws Exception {
        String DOMAIN_NAME = makeTestDomainName(genDomainSegmentName());
        Domain domain = prov.get(Key.DomainBy.name, DOMAIN_NAME);
        assertNull(domain);
    }
}
