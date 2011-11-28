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
package com.zimbra.qa.unittest.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.qa.unittest.TestUtil;

public class TestLdapProvModifyAttrs extends LdapTest {
    private static ProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new ProvTestUtil();
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
    
    // read the entry from ldap 
    private Account getFresh(Account acct) throws Exception {
        prov.flushCache(CacheEntryType.account, null);
        return prov.get(AccountBy.id, acct.getId());
    }
    
    private void modifySingleValue(Account acct, String attrName, String attrValue) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(acct, attrs);
        acct = getFresh(acct);
        
        // modifying an attr value to "" will unset the attr
        String expectedValue = (attrValue == "") ? null : attrValue;
        assertEquals(expectedValue, acct.getAttr(attrName));
    }
    
    private void modifyMultiValue(Account acct, String attrName, String[] attrValues) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValues);
        prov.modifyAttrs(acct, attrs);
    }
    
    @Test
    public void setSingleValue() throws Exception {
        String ATTR_NAME = Provisioning.A_displayName;
        String ATTR_VALUE = "setSingleValue";
        
        Account acct = createAccount("setSingleValue");
        
        // run twice, ensure it is handled correctly
        modifySingleValue(acct, ATTR_NAME, ATTR_VALUE);
        modifySingleValue(acct, ATTR_NAME, ATTR_VALUE);
        
        deleteAccount(acct);
    }
    
    @Test
    public void setSingleValueBinary() throws Exception {
        String ATTR_NAME = Provisioning.A_userSMIMECertificate;
        String ATTR_VALUE = TestLdapBinary.Content.generateContent(32).getString();
        
        Account acct = createAccount("setSingleValueBinary");
        
        // run twice, ensure it is handled correctly
        modifySingleValue(acct, ATTR_NAME, ATTR_VALUE);
        modifySingleValue(acct, ATTR_NAME, ATTR_VALUE);
        
        deleteAccount(acct);
    }
    
    @Test
    public void unsetSingleValue() throws Exception {
        String ATTR_NAME = Provisioning.A_displayName;
        String ATTR_VALUE = "unsetSingleValue";
        
        Account acct = createAccount("unsetSingleValue");
        modifySingleValue(acct, ATTR_NAME, ATTR_VALUE);
        
        // run twice, ensure it is handled correctly
        modifySingleValue(acct, ATTR_NAME, null);
        modifySingleValue(acct, ATTR_NAME, null);
        modifySingleValue(acct, ATTR_NAME, "");
        modifySingleValue(acct, ATTR_NAME, "");
        
        deleteAccount(acct);
    }
    
    @Test
    public void addMultiValue() throws Exception {
        String ATTR_NAME = Provisioning.A_zimbraACE;
        String ATTR_VALUE_1 = "addMultiValue-1";
        String ATTR_VALUE_2 = "addMultiValue-2";
        String ATTR_VALUE_3 = "addMultiValue-3";
        String ATTR_VALUE_4 = "addMultiValue-4";
        
        Account acct = createAccount("addMultiValue");
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_1, ATTR_VALUE_2});
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_3, ATTR_VALUE_4});
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_1, ATTR_VALUE_2});
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_3, ATTR_VALUE_4});
        
        acct = getFresh(acct);
        Set<String> values = acct.getMultiAttrSet(ATTR_NAME);
        assertEquals(4, values.size());
        assertTrue(values.contains(ATTR_VALUE_1));
        assertTrue(values.contains(ATTR_VALUE_2));
        assertTrue(values.contains(ATTR_VALUE_3));
        assertTrue(values.contains(ATTR_VALUE_4));
        
        deleteAccount(acct);
    }
    
    @Test
    public void addMultiValueBinary() throws Exception {
        String ATTR_NAME = Provisioning.A_userSMIMECertificate;
        String ATTR_VALUE_1 = TestLdapBinary.Content.generateContent(32).getString();
        String ATTR_VALUE_2 = TestLdapBinary.Content.generateContent(32).getString();
        String ATTR_VALUE_3 = TestLdapBinary.Content.generateContent(32).getString();
        String ATTR_VALUE_4 = TestLdapBinary.Content.generateContent(32).getString();
        
        Account acct = createAccount("addMultiValueBinary");
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_1, ATTR_VALUE_2});
        
        acct = getFresh(acct);
        Set<String> values = acct.getMultiAttrSet(ATTR_NAME);
        assertEquals(2, values.size());
        assertTrue(values.contains(ATTR_VALUE_1));
        assertTrue(values.contains(ATTR_VALUE_2));
        
        deleteAccount(acct);
    }
    
    @Test
    public void removeMultiValue() throws Exception {
        String ATTR_NAME = Provisioning.A_zimbraACE;
        String ATTR_VALUE_1 = "removeMultiValue-1";
        String ATTR_VALUE_2 = "removeMultiValue-2";
        String ATTR_VALUE_3 = "removeMultiValue-3";
        String ATTR_VALUE_4 = "removeMultiValue-4";
        
        Account acct = createAccount("removeMultiValue");
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_1, ATTR_VALUE_2, ATTR_VALUE_3, ATTR_VALUE_4});
        
        modifyMultiValue(acct, "-" + ATTR_NAME, new String[]{ATTR_VALUE_3, ATTR_VALUE_4});
        
        acct = getFresh(acct);
        Set<String> values = acct.getMultiAttrSet(ATTR_NAME);
        assertEquals(2, values.size());
        assertTrue(values.contains(ATTR_VALUE_1));
        assertTrue(values.contains(ATTR_VALUE_2));
        
        deleteAccount(acct);
    }
    
    @Test
    public void unsetMultiValue() throws Exception {
        String ATTR_NAME = Provisioning.A_zimbraACE;
        String ATTR_VALUE_1 = "removeMultiValue-1";
        String ATTR_VALUE_2 = "removeMultiValue-2";
        String ATTR_VALUE_3 = "removeMultiValue-3";
        String ATTR_VALUE_4 = "removeMultiValue-4";
        
        Account acct = createAccount("removeMultiValue");
        modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_1, ATTR_VALUE_2, ATTR_VALUE_3, ATTR_VALUE_4});
        
        modifyMultiValue(acct, ATTR_NAME, null);
        
        acct = getFresh(acct);
        Set<String> values = acct.getMultiAttrSet(ATTR_NAME);
        assertEquals(0, values.size());
        
        deleteAccount(acct);
    }
    
    @Test
    public void invalidAttrValue() throws Exception {
        // set multiple values to a single-valued attr
        String ATTR_NAME = Provisioning.A_zimbraPrefSkin;
        String ATTR_VALUE_1 = "invalidAttrValue-1";
        String ATTR_VALUE_2 = "invalidAttrValue-2";
        
        Account acct = createAccount("invalidAttrValue");
        
        boolean caughtException= false;
        try {
            modifyMultiValue(acct, "+" + ATTR_NAME, new String[]{ATTR_VALUE_1, ATTR_VALUE_2});
        } catch (AccountServiceException e) {
            if (AccountServiceException.INVALID_ATTR_VALUE.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteAccount(acct);
    }

    @Test
    public void invalidAttrName() throws Exception {
        // settting muliple values to a single-valued attr
        String ATTR_NAME = "bogus";
        String ATTR_VALUE = "invalidAttrValue";
        
        Account acct = createAccount("invalidAttrValue");
        
        boolean caughtException= false;
        try {
            modifySingleValue(acct, "+" + ATTR_NAME, ATTR_VALUE);
        } catch (AccountServiceException e) {
            if (AccountServiceException.INVALID_ATTR_NAME.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        deleteAccount(acct);
    }
}
