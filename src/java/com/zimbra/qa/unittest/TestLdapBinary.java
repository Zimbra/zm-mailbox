/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class TestLdapBinary extends TestCase {
    
    /* 
     * To run this unit test, paste these attributes to zimbra-attrs.xml and run:
     * ant refresh-ldap-schema
     * ant generate-getters
     * ant init-unittest
     * 
    <attr id="10000" name="zimbraBinary" type="binary" max="5000" cardinality="single" optionalIn="globalConfig" since="8.0.0">
      <desc>binary data</desc>
    </attr>
    
    <attr id="10001" name="zimbraBinaryMulti" type="binary" max="5000" cardinality="multi" optionalIn="globalConfig" since="8.0.0">
      <desc>binary data</desc>
    </attr>
    */
    
    private static final String DATA_PATH = "/opt/zimbra/unittest/ldap/binaryContent/";
    private static final String CONTENT_NAME = DATA_PATH + "zimbra.jpeg";
    private static final String CONTENT_NAME_1 = DATA_PATH + "1.jpeg";
    private static final String CONTENT_NAME_2 = DATA_PATH + "2.jpeg";
    private static final String CONTENT_NAME_3 = DATA_PATH + "3.jpeg";
    private static final String CONTENT_NAME_TOO_LARGE = DATA_PATH + "too_large.txt";

    private static final String ATTR_SINGLE = "zimbraBinary";
    private static final String ATTR_MULTI = "zimbraBinaryMulti";
    
    // private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    static class Content {
        private String string;
        private byte[] binary;
        
        private Content(byte[] content) {
            this.binary = content;
            this.string = ByteUtil.encodeLDAPBase64(content);
        }
        
        String getString() {
            return string;
        }
        
        byte[] getBinary() {
            return binary;
        }
        
        static Content getContentByFileName(String contentFileName) throws IOException {
            File inFile = new File(contentFileName);
            return new Content(ByteUtil.getContent(inFile));
        }
        
        static Content generateContent(int numBytes) {
            byte[] content = new byte[numBytes];
            Random random = new Random();
            random.nextBytes(content);
            return new Content(content);
        }
        
        boolean equals(String str) {
            return string.equals(str);
        }
        
        boolean equals(byte[] bin) {
            if (bin == null) {
                return false;
            }
            
            if (binary.length != bin.length) {
                return false;
            }
            
            for (int i = 0; i < binary.length; i++) {
                if (binary[i] != bin[i]) {
                    return false;
                }
            }
            
            return true;
        }
    }

    private Config getConfig() throws Exception {
        return Provisioning.getInstance().getConfig();
    }
    
    private void delete(String attrName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, null); 
        prov.modifyAttrs(config, attrs);
        
        byte[] value = config.getBinaryAttr(attrName);
        assertNull(value);
    }
    
    // testing the code path when value is a String 
    private void modify(String attrName, String contentName) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, Content.getContentByFileName(contentName).getString());
        prov.modifyAttrs(config, attrs);
    }
    
    // testing the code path when value is a String[]
    private void modify(String attrName, String[] contentNames) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (String contentName : contentNames) {
            StringUtil.addToMultiMap(attrs, attrName, Content.getContentByFileName(contentName).getString());
        }
        prov.modifyAttrs(config, attrs);
    }
    
    
    private void replaceAttr(String attrName, String contentName) throws Exception {
        modify(attrName, contentName);
    }
    
    private void addAttr(String attrName, String contentName) throws Exception {
        modify("+" + attrName, contentName);
    }
    
    private void removeAttr(String attrName, String contentName) throws Exception {
        modify("-" + attrName, contentName);
    }
    
    private void replaceAttr(String attrName, String[] contentNames) throws Exception {
        modify(attrName, contentNames);
    }
    
    private void addAttr(String attrName, String[] contentNames) throws Exception {
        modify("+" + attrName, contentNames);
    }
    
    private void removeAttr(String attrName, String[] contentNames) throws Exception {
        modify("-" + attrName, contentNames);
    }
    
    private void verify(String attrName, String contentName) throws Exception  {
        Config config = getConfig();
        String stringValue = config.getAttr(attrName);
        byte[] binaryValue = config.getBinaryAttr(attrName);
        
        Content content = Content.getContentByFileName(contentName);
        assertTrue(content.equals(stringValue));
        assertTrue(content.equals(binaryValue));
    }
    
    private void verify(String attrName, String[] contentNames) throws Exception{
        int numContents = contentNames.length;
        
        Config config = getConfig();
        String[] stringValues = config.getMultiAttr(attrName);
        List<byte[]> binaryValues = config.getMultiBinaryAttr(attrName);
        
        assertEquals(numContents, stringValues.length);
        assertEquals(numContents, binaryValues.size());
        
        for (int i = 0; i < numContents; i++) {
            Content content = Content.getContentByFileName(contentNames[i]);
            
            boolean found = false;
            for (int j = 0; j < stringValues.length && !found; j++) {
                if (content.equals(stringValues[j])) {
                    found = true;
                }
            }
            assertTrue(found);
            
            found = false;
            for (int j = 0; j < binaryValues.size() && !found; j++) {
                if (content.equals(binaryValues.get(j))) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }
    
    private void verifyIsEmpty(Config config, String attrName) {
        String value = config.getAttr(attrName);
        assertNull(value);
    }
   
    private void remove(boolean array) throws Exception {
        String attrName = ATTR_MULTI;
        String[] contentNames = new String[]{CONTENT_NAME_1, CONTENT_NAME_2, CONTENT_NAME_3};
        
        delete(attrName);
        
        replaceAttr(attrName, contentNames);
        verify(attrName, contentNames);  
        
        String[] removedContentNames = new String[]{CONTENT_NAME_1, CONTENT_NAME_2};
        String[] remainingContentNames = new String[]{CONTENT_NAME_3};
        
        if (array) {
            removeAttr(attrName, removedContentNames);
        } else {
            for (String contentName : removedContentNames) {
                removeAttr(attrName, contentName);
            }
        }

        verify(attrName, remainingContentNames);
    }

    private void add(boolean array) throws Exception {
        String attrName = ATTR_MULTI;
        String[] contentNames = new String[]{CONTENT_NAME_1, CONTENT_NAME_2, CONTENT_NAME_3};
        
        delete(attrName);
        
        if (array) {
            addAttr(attrName, contentNames);
        } else {
            for (String contentName : contentNames) {
                addAttr(attrName, contentName);
            }
        }
        
        verify(attrName, contentNames);
    }
    
    @Test
    public void testTooLarge() throws Exception {
        String attrName = ATTR_SINGLE;
        String contentName = CONTENT_NAME_TOO_LARGE;
        
        delete(attrName);

        boolean caught = false;
        try {
            replaceAttr(attrName, contentName);
        } catch (ServiceException e) {
            if (AccountServiceException.INVALID_ATTR_VALUE.equals(e.getCode())) {
                caught = true;
            }
        }
        
        assertTrue(caught);
    }
    
    @Test
    public void testReplaceSingle() throws Exception {
        String attrName = ATTR_SINGLE;
        String contentName = CONTENT_NAME;
        
        delete(attrName);

        replaceAttr(attrName, contentName);
        verify(attrName, contentName);
    }
    
    @Test
    public void testReplaceMulti() throws Exception {
        String attrName = ATTR_MULTI;
        String[] contentNames = new String[]{CONTENT_NAME_1, CONTENT_NAME_2, CONTENT_NAME_3};
        
        delete(attrName);
        
        replaceAttr(attrName, contentNames);
        verify(attrName, contentNames);
    }
    
    @Test
    public void testRemove() throws Exception {
        remove(false);
        remove(true);
    }
    
    @Test
    public void testAdd() throws Exception {
        add(false);
        add(true);
    }
    
    //
    // START uncomment after running "ant generate-getters" with the test binary attributes
    //
    /* 
    public void verifyWithGetters(Config config, Content content) {
        byte[] binary = config.getBinary();
        assertTrue(content.equals(binary));
        
        String string = config.getBinaryAsString();
        assertTrue(content.equals(string));
    }
    
    public void verifyWithGettersMulti(Config config, Content[] contents) {
        // just return the first value
        byte[] binary = config.getBinaryMulti();
        assertTrue(contents[0].equals(binary));
        
        String[] strings = config.getBinaryMultiAsString();
        assertEquals(strings.length, contents.length);
        for (int i = 0; i < contents.length; i++) {
            assertTrue(contents[i].equals(strings[i]));
        }
    }
    
    @Test
    public void testGettersSettersSingle() throws Exception {
        String attrName = ATTR_SINGLE;
        String contentName = CONTENT_NAME;
        
        delete(attrName);
        
        Config config = getConfig();
        Content content = Content.getContentByFileName(CONTENT_NAME);
        Map<String,Object> attrs = null;
        Object value = null;
        
        // test typed setter
        config.setBinary(content.getBinary());
        verifyWithGetters(config, content);
        
        // test setter no-commit
        attrs = config.setBinary(content.getBinary(), null);
        value = attrs.get(attrName);
        assertTrue(value instanceof String);
        assertEquals(content.getString(), (String)value);
        
        // test unsetter no-commit
        attrs = config.unsetBinary(null);
        value = attrs.get(attrName);
        assertTrue(value instanceof String);
        assertEquals(0, ((String)value).length());
        
        // test unsetter do-commit
        config.unsetBinary();
        verifyIsEmpty(config, attrName);
    }
    
    @Test
    public void testGettersSettersMulti() throws Exception {
        String attrName = ATTR_MULTI;
        String[] contentNames = new String[]{CONTENT_NAME_1, CONTENT_NAME_2, CONTENT_NAME_3};
        
        delete(attrName);
        
        Config config = getConfig();
        Content content = Content.getContentByFileName(CONTENT_NAME);
        Map<String,Object> attrs = null;
        Object value = null;
        
        // test typed setter
        config.setBinaryMulti(content.getBinary());
        verifyWithGettersMulti(config, new Content[]{content});
        
        // test setter no-commit
        attrs = config.setBinaryMulti(content.getBinary(), null);
        value = attrs.get(attrName);
        assertTrue(value instanceof String);
        assertEquals(content.getString(), (String)value);
        
        // test unsetter no-commit
        attrs = config.unsetBinaryMulti(null);
        value = attrs.get(attrName);
        assertTrue(value instanceof String);
        assertEquals(0, ((String)value).length());
        
        // test unsetter do-commit
        config.unsetBinaryMulti();
        verifyIsEmpty(config, attrName);
    }
    */
    //
    // END uncomment after running "ant generate-getters" with the test binary attributes
    //
    
    
    public static void main(String[] args) throws Exception  {
        TestUtil.runTest(TestLdapBinary.class);
    }

}
