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
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.qa.unittest.prov.Names;
import com.zimbra.soap.admin.type.CacheEntryType;

public class TestLdapProvSignature extends LdapTest {
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
    
    private Account createAccount(String localPart, Map<String, Object> attrs) 
    throws Exception {
        return provUtil.createAccount(localPart, domain, attrs);
    }
    
    private void deleteAccount(Account acct) throws Exception {
        provUtil.deleteAccount(acct);
    }
    
    private void deleteSignature(Account acct, Signature signature) throws Exception {
        String signatureId = signature.getId();
        prov.deleteSignature(acct, signatureId);
        signature = prov.get(acct, Key.SignatureBy.id, signatureId);
        assertNull(signature);
    }
    
    private Signature createSignatureRaw(Account acct, String signatureName) 
    throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        return prov.createSignature(acct, signatureName, attrs);
    }
    
    private Signature createSignature(Account acct, String signatureName) 
    throws Exception {
        Signature signature = prov.get(acct, Key.SignatureBy.name, signatureName);
        assertNull(signature);
        
        createSignatureRaw(acct, signatureName);
        
        signature = prov.get(acct, Key.SignatureBy.name, signatureName);
        assertNotNull(signature);
        assertEquals(signatureName, signature.getName());
        
        return signature;
    }
    
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    @Test
    public void createSignature() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String SIGNATURE_NAME = Names.makeSignatureName(genSignatureName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        assertEquals(acct.getId(), signature.getAccount().getId());
        
        deleteSignature(acct, signature);
        deleteAccount(acct);
    }
    
    @Test
    public void createSignatureAlreadyExists() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String SIGNATURE_NAME = Names.makeSignatureName(genSignatureName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        boolean caughtException = false;
        try {
            createSignatureRaw(acct, SIGNATURE_NAME);
        } catch (AccountServiceException e) {
            if (AccountServiceException.SIGNATURE_EXISTS.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteSignature(acct, signature);
        deleteAccount(acct);
    }
    
    @Test
    public void modifySignature() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String SIGNATURE_NAME = Names.makeSignatureName(genSignatureName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraPrefMailSignature;
        String MODIFIED_ATTR_VALUE = "modifySignature";
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifySignature(acct, signature.getId(), attrs);
        
        acct = getFresh(acct);
        signature = prov.get(acct, Key.SignatureBy.name, SIGNATURE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, signature.getAttr(MODIFIED_ATTR_NAME));
        
        deleteSignature(acct,signature);
        deleteAccount(acct);
    }
    
    @Test
    public void renameSignature() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String SIGNATURE_NAME_ON_ACCOUNT_ENTRY = Names.makeSignatureName(genSignatureName("sig-on-account-entry"));
        String SIGNATURE_NAME = Names.makeSignatureName(genSignatureName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Signature signatureOnAccountEntry = createSignature(acct, SIGNATURE_NAME_ON_ACCOUNT_ENTRY);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        /*
         * rename the signature on account entry
         */
        Map<String, Object> attrs = new HashMap<String, Object>();
        // modifying zimbraSignatureName will rename the signature and trigger a LDAP moddn
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraSignatureName;
        String NEW_SIGNATURE_NAME = Names.makeSignatureName(genSignatureName("sig-on-account-entry-new"));  
        String MODIFIED_ATTR_VALUE = NEW_SIGNATURE_NAME;
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifySignature(acct, signatureOnAccountEntry.getId(), attrs);
        
        acct = getFresh(acct);
        signatureOnAccountEntry = prov.get(acct, Key.SignatureBy.name, NEW_SIGNATURE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, signatureOnAccountEntry.getAttr(MODIFIED_ATTR_NAME));
        
        /*
         * rename the signature on signature entry
         */
        attrs = new HashMap<String, Object>();
        // modifying zimbraSignatureName will rename the signature and trigger a LDAP moddn
        MODIFIED_ATTR_NAME = Provisioning.A_zimbraSignatureName;
        NEW_SIGNATURE_NAME = Names.makeSignatureName(genSignatureName("new"));  
        MODIFIED_ATTR_VALUE = NEW_SIGNATURE_NAME;
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifySignature(acct, signature.getId(), attrs);
        
        acct = getFresh(acct);
        signature = prov.get(acct, Key.SignatureBy.name, NEW_SIGNATURE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, signature.getAttr(MODIFIED_ATTR_NAME));
        
        deleteSignature(acct,signatureOnAccountEntry);
        deleteSignature(acct,signature);
        deleteAccount(acct);
    }
    
    @Test
    public void getAllSignatures() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String SIGNATURE_NAME_1 = Names.makeSignatureName(genSignatureName("1"));
        String SIGNATURE_NAME_2 = Names.makeSignatureName(genSignatureName("2"));
        String SIGNATURE_NAME_3 = Names.makeSignatureName(genSignatureName("3"));
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Signature signature1 = createSignature(acct, SIGNATURE_NAME_1);
        Signature signature2 = createSignature(acct, SIGNATURE_NAME_2);
        Signature signature3 = createSignature(acct, SIGNATURE_NAME_3);
        
        acct = getFresh(acct);
        List<Signature> allSignatures = prov.getAllSignatures(acct);
        assertEquals(3, allSignatures.size());
        
        Set<String> allSignatureIds = new HashSet<String>();
        for (Signature signature : allSignatures) {
            allSignatureIds.add(signature.getId());
        }
        
        assertTrue(allSignatureIds.contains(signature1.getId()));
        assertTrue(allSignatureIds.contains(signature2.getId()));
        assertTrue(allSignatureIds.contains(signature3.getId()));
        
        deleteSignature(acct,signature1);
        deleteSignature(acct,signature2);
        deleteSignature(acct,signature3);
        deleteAccount(acct);
    }
    
    @Test
    public void getSignature() throws Exception {
        String ACCT_NAME_LOCALPART = Names.makeAccountNameLocalPart(genAcctNameLocalPart());
        String SIGNATURE_NAME = Names.makeSignatureName(genSignatureName());
        
        Account acct = createAccount(ACCT_NAME_LOCALPART);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        String signatureId = signature.getId();
        
        acct = getFresh(acct);
        signature = prov.get(acct, Key.SignatureBy.id, signatureId);
        assertNotNull(signature);
        
        acct = getFresh(acct);
        signature = prov.get(acct, Key.SignatureBy.name, SIGNATURE_NAME);
        assertNotNull(signature);
        
        deleteSignature(acct,signature);
        deleteAccount(acct);
    }
}
