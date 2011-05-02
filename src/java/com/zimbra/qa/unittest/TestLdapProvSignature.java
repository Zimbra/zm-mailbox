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
package com.zimbra.qa.unittest;

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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Provisioning.SignatureBy;

public class TestLdapProvSignature {
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        TestLdap.manualInit();
        
        prov = Provisioning.getInstance();
        domain = prov.createDomain(baseDomainName(), new HashMap<String, Object>());
        assertNotNull(domain);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return TestLdapProvSignature.class.getName().toLowerCase();
    }
    
    private Account createAccount(String localPart) throws Exception {
        return createAccount(localPart, null);
    }
    
    private Account createAccount(String localPart, Map<String, Object> attrs) throws Exception {
        String acctName = TestUtil.getAddress(localPart, domain.getName());
        Account acct = prov.get(AccountBy.name, acctName);
        assertNull(acct);
                
        acct = prov.createAccount(acctName, "test123", attrs);
        assertNotNull(acct);
        return acct;
    }
    
    private void deleteAccount(Account acct) throws Exception {
        String acctId = acct.getId();
        prov.deleteAccount(acctId);
        acct = prov.get(AccountBy.id, acctId);
        assertNull(acct);
    }
    
    private void deleteSignature(Account acct, Signature signature) throws Exception {
        String signatureId = signature.getId();
        prov.deleteSignature(acct, signatureId);
        signature = prov.get(acct, SignatureBy.id, signatureId);
        assertNull(signature);
    }
    
    private Signature createSignatureRaw(Account acct, String signatureName) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        return prov.createSignature(acct, signatureName, attrs);
    }
    
    private Signature createSignature(Account acct, String signatureName) throws Exception {
        Signature signature = prov.get(acct, SignatureBy.name, signatureName);
        assertNull(signature);
        
        createSignatureRaw(acct, signatureName);
        
        signature = prov.get(acct, SignatureBy.name, signatureName);
        assertNotNull(signature);
        
        return signature;
    }
    
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    @Test
    public void createSignature() throws Exception {
        String ACCT_NAME = "createSignature";
        String SIGNATURE_NAME = "createSignature";
        
        Account acct = createAccount(ACCT_NAME);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        assertEquals(acct.getId(), signature.getAccount().getId());
        
        deleteSignature(acct, signature);
        deleteAccount(acct);
    }
    
    @Test
    public void createSignatureAlreadyExists() throws Exception {
        String ACCT_NAME = "createSignatureAlreadyExists";
        String SIGNATURE_NAME = "createSignatureAlreadyExists";
        
        Account acct = createAccount(ACCT_NAME);
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
        String ACCT_NAME = "modifySignature";
        String SIGNATURE_NAME = "modifySignature";
        
        Account acct = createAccount(ACCT_NAME);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraPrefMailSignature;
        String MODIFIED_ATTR_VALUE = "modifySignature";
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifySignature(acct, signature.getId(), attrs);
        
        acct = getFresh(acct);
        signature = prov.get(acct, SignatureBy.name, SIGNATURE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, signature.getAttr(MODIFIED_ATTR_NAME));
        
        deleteSignature(acct,signature);
        deleteAccount(acct);
    }
    
    @Test
    public void renameSignature() throws Exception {
        String ACCT_NAME = "renameSignature";
        String SIGNATURE_NAME = "renameSignature";
        
        Account acct = createAccount(ACCT_NAME);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        // modifying zimbraSignatureName will rename the signature and trigger a LDAP moddn
        String MODIFIED_ATTR_NAME = Provisioning.A_zimbraSignatureName;
        String NEW_SIGNATURE_NAME = "renameSignature-new";  
        String MODIFIED_ATTR_VALUE = NEW_SIGNATURE_NAME;
        attrs.put(MODIFIED_ATTR_NAME, MODIFIED_ATTR_VALUE);
        prov.modifySignature(acct, signature.getId(), attrs);
        
        acct = getFresh(acct);
        signature = prov.get(acct, SignatureBy.name, NEW_SIGNATURE_NAME);
        assertEquals(MODIFIED_ATTR_VALUE, signature.getAttr(MODIFIED_ATTR_NAME));
        
        deleteSignature(acct,signature);
        deleteAccount(acct);
    }
    
    @Test
    public void getAllSignatures() throws Exception {
        String ACCT_NAME = "getAllSignatures";
        String SIGNATURE_NAME_1 = "getAllSignatures-1";
        String SIGNATURE_NAME_2 = "getAllSignatures-2";
        String SIGNATURE_NAME_3 = "getAllSignatures-3";
        
        Account acct = createAccount(ACCT_NAME);
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
        String ACCT_NAME = "getSignature";
        String SIGNATURE_NAME = "getSignature";
        
        Account acct = createAccount(ACCT_NAME);
        Signature signature = createSignature(acct, SIGNATURE_NAME);
        String signatureId = signature.getId();
        
        acct = getFresh(acct);
        signature = prov.get(acct, SignatureBy.id, signatureId);
        assertNotNull(signature);
        
        acct = getFresh(acct);
        signature = prov.get(acct, SignatureBy.name, SIGNATURE_NAME);
        assertNotNull(signature);
        
        deleteSignature(acct,signature);
        deleteAccount(acct);
    }
}
