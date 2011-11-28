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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZMutableEntry;

public class TestLdapZMutableEntry extends LdapTest {

    @BeforeClass
    public static void init() throws Exception {
    }
    
    @Test
    public void setAttr() throws Exception {
        String KEY = "key";
        String VALUE1 = "value1";
        String VALUE2 = "value2";
        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        
        entry.setAttr(KEY, VALUE1);
        entry.setAttr(KEY, VALUE2);  // should REPLACE
        
        ZAttributes zattrs = entry.getAttributes();
        Map<String, Object> attrs = zattrs.getAttrs();
        
        Object valueGot = attrs.get(KEY);
        assertTrue(valueGot instanceof String);
    }
    
    @Test
    public void addAttr() throws Exception {
        String KEY = "key";
        String VALUE1 = "value1";
        String VALUE2 = "value2";
        String VALUE3 = "value3";
        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        
        Set<String> values = new HashSet<String>();
        values.add(VALUE1);
        values.add(VALUE2);
        entry.addAttr(KEY, values);
        
        values.clear();
        values.add(VALUE1);
        values.add(VALUE3);
        entry.addAttr(KEY, values);  // should MERGE
        
        ZAttributes zattrs = entry.getAttributes();
        Map<String, Object> attrs = zattrs.getAttrs();
        
        Object valueGot = attrs.get(KEY);
        assertTrue(valueGot instanceof String[]);
        List<String> valuesGot = Arrays.asList((String[]) valueGot);
        assertTrue(valuesGot.contains(VALUE1));
        assertTrue(valuesGot.contains(VALUE2));
        assertTrue(valuesGot.contains(VALUE3));
    }
    
    @Test
    public void getAttrString() throws Exception {
        String KEY = "key";
        String VALUE1 = "value1";
        String VALUE2 = "value2";
        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        entry.setAttr(KEY, VALUE1);
        
        // single value
        String valueGot = entry.getAttrString(KEY);
        assertEquals(VALUE1, valueGot);
        
        // multi value - should just return the first one
        Set<String> values = new HashSet<String>();
        values.add(VALUE2);
        entry.addAttr(KEY, values);
        valueGot = entry.getAttrString(KEY);
        
        // order is not guaranteed
        assertTrue(VALUE1.equals(valueGot) || VALUE2.equals(valueGot)); 
    }
    
    @Test
    public void hasAttribute() throws Exception {
        String KEY = "key";
        String VALUE1 = "value1";
        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        
        entry.setAttr(KEY, VALUE1);
        assertTrue(entry.hasAttribute(KEY));
        assertFalse(entry.hasAttribute(KEY + "-NOT"));
        
    }
    
    @Test
    public void mapToAttrsSimple() throws Exception {
        String KEY_SINGLE = "key";
        String KEY_MULTI = "key-multi";
        String VALUE1 = "value1";
        String VALUE2 = "value2";
        
        Map<String, Object> attrMap = new HashMap<String, Object>();
        
        // single value
        attrMap.put(KEY_SINGLE, VALUE1);
        
        // multi value
        attrMap.put(KEY_MULTI, new String[] {VALUE1, VALUE2});
        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        entry.mapToAttrs(attrMap);
        
        // single value
        String valueGot = entry.getAttrString(KEY_SINGLE);
        assertEquals(VALUE1, valueGot);
        
        //  multi value
        ZAttributes zattrs = entry.getAttributes();
        Map<String, Object> attrs = zattrs.getAttrs();
        Object multiValueGot = attrs.get(KEY_MULTI);
        assertTrue(multiValueGot instanceof String[]);
        List<String> valuesGot = Arrays.asList((String[]) multiValueGot);
        assertTrue(valuesGot.contains(VALUE1));
        assertTrue(valuesGot.contains(VALUE2));
    }
    
    @Test
    public void mapToAttrsBinarySingle() throws Exception {
        int NUM_BYTES_IN_BINARY_DATA = 64;
        
        String KEY_SINGLE_BINARY = Provisioning.A_userSMIMECertificate;
        String KEY_SINGLE_BINARY_TRANSFER = Provisioning.A_userCertificate;
        
        TestLdapBinary.Content VALUE_BINARY = 
            TestLdapBinary.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
        TestLdapBinary.Content VALUE_BINARY_TRANSFER = 
            TestLdapBinary.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
        
        Map<String, Object> attrMap = new HashMap<String, Object>();
        
        attrMap.put(KEY_SINGLE_BINARY, VALUE_BINARY.getString());
        attrMap.put(KEY_SINGLE_BINARY_TRANSFER, VALUE_BINARY_TRANSFER.getString());
                        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        entry.mapToAttrs(attrMap);
        
        /*
         * this will fail.  We do not handle binary data in this path,
         * nor is there a use case for it.
         * 
         * Note: ZMutableEntry and ZAttributes encapsulate a 
         * SDK(UBID or JNDI) native Entry/Attributes.  Binary data
         * are always stored in byte[] in those.  When the SDK object 
         * is "mapped" to a Map<String, Object>, used in Provisioning,
         * it is then base64 encoded, and appear as a String.
         */
        // String valueGot = entry.getAttrString(KEY_SINGLE_BINARY);
        // assertEquals(VALUE1.getString(), valueGot); 
        
        // this will work, because the ZAttribute.getAttrs() base64 encode 
        // the binary data when putting them in the Map<String, Object>.
        
        // contains binary data
        Map<String, Object> attrMapGot = entry.getAttributes().getAttrs();
        Object valueGot = attrMapGot.get(KEY_SINGLE_BINARY);
        assertTrue(valueGot instanceof String);
        assertTrue(((String) valueGot).equals(VALUE_BINARY.getString()));

        // is binary transfer
        attrMapGot = entry.getAttributes().getAttrs();
        valueGot = attrMapGot.get(KEY_SINGLE_BINARY_TRANSFER);
        assertTrue(valueGot instanceof String);
        assertTrue(((String) valueGot).equals(VALUE_BINARY_TRANSFER.getString()));
    }
    
    @Test
    public void mapToAttrsBinaryMulti() throws Exception {
        int NUM_BYTES_IN_BINARY_DATA = 64;
        
        String KEY_MULTI_BINARY = Provisioning.A_userSMIMECertificate;
        String KEY_MULTI_BINARY_TRANSFER = Provisioning.A_userCertificate;
        
        TestLdapBinary.Content VALUE_BINARY_1 = 
            TestLdapBinary.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
        TestLdapBinary.Content VALUE_BINARY_2 = 
            TestLdapBinary.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
        TestLdapBinary.Content VALUE_BINARY_TRANSFER_1 = 
            TestLdapBinary.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
        TestLdapBinary.Content VALUE_BINARY_TRANSFER_2 = 
            TestLdapBinary.Content.generateContent(NUM_BYTES_IN_BINARY_DATA);
        
        Map<String, Object> attrMap = new HashMap<String, Object>();
        
        attrMap.put(KEY_MULTI_BINARY, 
                new String[]{VALUE_BINARY_1.getString(), VALUE_BINARY_2.getString()});
        attrMap.put(KEY_MULTI_BINARY_TRANSFER,
                new String[]{VALUE_BINARY_TRANSFER_1.getString(), 
                VALUE_BINARY_TRANSFER_2.getString()});
                        
        ZMutableEntry entry = LdapClient.createMutableEntry();
        entry.mapToAttrs(attrMap);
        
        // contains binary data
        Map<String, Object> attrMapGot = entry.getAttributes().getAttrs();
        Object valueGot = attrMapGot.get(KEY_MULTI_BINARY);
        assertTrue(valueGot instanceof String[]);
        List<String> valuesGot = Arrays.asList((String[]) valueGot);
        assertTrue(valuesGot.contains(VALUE_BINARY_1.getString()));
        assertTrue(valuesGot.contains(VALUE_BINARY_2.getString()));
        
        // is binary transfer
        attrMapGot = entry.getAttributes().getAttrs();
        valueGot = attrMapGot.get(KEY_MULTI_BINARY_TRANSFER);
        assertTrue(valueGot instanceof String[]);
        valuesGot = Arrays.asList((String[]) valueGot);
        assertTrue(valuesGot.contains(VALUE_BINARY_TRANSFER_1.getString()));
        assertTrue(valuesGot.contains(VALUE_BINARY_TRANSFER_2.getString()));
    }
    
    @Test
    public void setDN() throws Exception {
        String DN = "cn=zimbra";
        ZMutableEntry entry = LdapClient.createMutableEntry();
        entry.setDN(DN);
        assertEquals(DN, entry.getDN());
    }
    
}
