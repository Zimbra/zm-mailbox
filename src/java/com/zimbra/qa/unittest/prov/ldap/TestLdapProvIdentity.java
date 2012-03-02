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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestLdapProvIdentity extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        return provUtil.createAccount(localPart, domain, attrs);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }
    
    private void deleteIdentity(Account acct, Identity identity) throws Exception {
        String identityId = identity.getId();
        String identityName = identity.getName();
        prov.deleteIdentity(acct, identityName);
        identity = prov.get(acct, Key.IdentityBy.id, identityId);
        assertNull(identity);
    }
    
    private Identity createIdentityRaw(Account acct, String identityName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        return prov.createIdentity(acct, identityName, attrs);
    }
    
    private Identity createIdentity(Account acct, String identityName) throws Exception {
        Identity identity = prov.get(acct, Key.IdentityBy.name, identityName);
        assertNull(identity);
        
        createIdentityRaw(acct, identityName);
        
        identity = prov.get(acct, Key.IdentityBy.name, identityName);
        assertNotNull(identity);
        assertEquals(identityName, identity.getName());
        
        return identity;
    }
    
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    @Test
    public void createIdentity() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String IDENTITY_NAME = Names.makeIdentityName(genIdentityName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Identity identity = createIdentity(acct, IDENTITY_NAME);
        
        assertEquals(acct.getId(), identity.getAccount().getId());
        
        deleteIdentity(acct, identity);
        deleteAccount(acct);
    }
    
    @Test
    public void createIdentityAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String IDENTITY_NAME = Names.makeIdentityName(genIdentityName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Identity identity = createIdentity(acct, IDENTITY_NAME);
        
        boolean caughtException = false;
        try {
            createIdentityRaw(acct, IDENTITY_NAME);
        } catch (AccountServiceException e) {
            if (AccountServiceException.IDENTITY_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteIdentity(acct, identity);
        deleteAccount(acct);
    }
    
    @Test
    public void modifyIdentity() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String IDENTITYE_NAME = Names.makeIdentityName(genIdentityName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Identity identity = createIdentity(acct, IDENTITYE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraPrefFromDisplay;
        String MODIFIED_ATTR_VALUE = "modifyIdentity";
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifyIdentity(acct, identity.getName(), attrs);
        
        acct = getFresh(acct);
        identity = prov.get(acct, Key.IdentityBy.name, IDENTITYE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, identity.getAttr(MODIFIED_ATTR_NAME));
        
        deleteIdentity(acct,identity);
        deleteAccount(acct);
    }
    
    @Test
    public void renameIdentity() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String IDENTITYE_NAME = Names.makeIdentityName(genIdentityName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Identity identity = createIdentity(acct, IDENTITYE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        // modifying zimbraPrefIdentityName will rename the identity and trigger a LDAP moddn
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraPrefIdentityName;
        String NEW_IDENTITY_NAME = genIdentityName("new");  
        String MODIFIED_ATTR_VALUE = NEW_IDENTITY_NAME;
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifyIdentity(acct, identity.getName(), attrs);
        
        acct = getFresh(acct);
        identity = prov.get(acct, Key.IdentityBy.name, NEW_IDENTITY_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, identity.getAttr(MODIFIED_ATTR_NAME));
        
        deleteIdentity(acct,identity);
        deleteAccount(acct);
    }
    
    @Test
    public void getAllIdentitys() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String IDENTITYE_NAME_1 = Names.makeIdentityName(genIdentityName("1"));
        String IDENTITYE_NAME_2 = Names.makeIdentityName(genIdentityName("2"));
        String IDENTITYE_NAME_3 = Names.makeIdentityName(genIdentityName("3"));
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Identity identity1 = createIdentity(acct, IDENTITYE_NAME_1);
        Identity identity2 = createIdentity(acct, IDENTITYE_NAME_2);
        Identity identity3 = createIdentity(acct, IDENTITYE_NAME_3);
        
        acct = getFresh(acct);
        List<Identity> allIdentitys = prov.getAllIdentities(acct);
        assertEquals(4, allIdentitys.size()); // 3 + the account identity
        
        Set<String> allIdentityIds = new HashSet<String>();
        for (Identity identity : allIdentitys) {
            allIdentityIds.add(identity.getId());
        }
        
        assertTrue(allIdentityIds.contains(identity1.getId()));
        assertTrue(allIdentityIds.contains(identity2.getId()));
        assertTrue(allIdentityIds.contains(identity3.getId()));
        
        deleteIdentity(acct,identity1);
        deleteIdentity(acct,identity2);
        deleteIdentity(acct,identity3);
        deleteAccount(acct);
    }
    
    @Test
    public void getIdentity() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String IDENTITYE_NAME = Names.makeIdentityName(genIdentityName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Identity identity = createIdentity(acct, IDENTITYE_NAME);
        String identityId = identity.getId();
        
        acct = getFresh(acct);
        identity = prov.get(acct, Key.IdentityBy.id, identityId);
        assertNotNull(identity);
        
        acct = getFresh(acct);
        identity = prov.get(acct, Key.IdentityBy.name, IDENTITYE_NAME);
        assertNotNull(identity);
        
        deleteIdentity(acct,identity);
        deleteAccount(acct);
    }
}
