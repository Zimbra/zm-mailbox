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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.io.File;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.common.account.Key.AccountBy;

public class TestLdapBinary extends TestLdap {
    
    /* 
     * To run this unit test:
     * 
     * (to get the two test attrs added in the schema)
     * cp -f /Users/pshao/p4/main/ZimbraServer/data/unittest/ldap/attrs-unittest.xml /opt/zimbra/conf/attrs
     * ant refresh-ldap-schema
     * ant generate-getters
     * ant init-unittest
     * 
    <attr id="10000" name="zimbraUnittestBinary" type="binary" max="5000" cardinality="single" optionalIn="account" since="8.0.0">
      <desc>binary data</desc>
    </attr>
    
    <attr id="10001" name="zimbraUnittestCertificate" type="certificate" cardinality="single" optionalIn="account" since="8.0.0">
      <desc>binary data</desc>
    </attr>

    */
    
    private static LdapProv prov;
    private static Domain domain;
    private static final String USER = "test-ldap-binary";
    
    private static final String DATA_PATH = "/opt/zimbra/unittest/ldap/binaryContent/";
    private static final String CONTENT_NAME = DATA_PATH + "zimbra.jpeg";
    
    private static final String CONTENT_NAME_1 = DATA_PATH + "1.jpeg";
    private static final String CONTENT_NAME_2 = DATA_PATH + "2.jpeg";
    private static final String CONTENT_NAME_3 = DATA_PATH + "3.jpeg";
    
    private static final String CERT_NAME_1 = DATA_PATH + "user1_primary.DER.crt";
    private static final String CERT_NAME_2 = DATA_PATH + "user1_alias.DER.crt";
    
    private static final String CONTENT_NAME_TOO_LARGE = DATA_PATH + "too_large.txt";

    // private static final String ATTR_SINGLE = "zimbraBinary";
    // private static final String ATTR_MULTI = "zimbraBinaryMulti";
    
    private static SingleValuedTestData ZIMBRA_BINARY_SINGLE = new SingleValuedTestData(
            "zimbraUnittestBinary", CONTENT_NAME);
    
    private static SingleValuedTestData ZIMBRA_CERTIFICATE_SINGLE = new SingleValuedTestData(
            "zimbraUnittestCertificate", CERT_NAME_1);
    
    private static SingleValuedTestData ZIMBRA_BINARY_SINGLE_TEST_TOO_LARGE = new SingleValuedTestData(
            "zimbraUnittestBinary", CONTENT_NAME_TOO_LARGE);
    
    private static MultiValuedTestData ZIMBRA_BINARY_MULTI = new MultiValuedTestData(
            Provisioning.A_zimbraPrefMailSMIMECertificate,
            new String[]{CONTENT_NAME_1, CONTENT_NAME_2, CONTENT_NAME_3},
            new String[]{CONTENT_NAME_1, CONTENT_NAME_2},
            new String[]{CONTENT_NAME_3}
            );
    
    private static MultiValuedTestData CERTIFICATE_MULTI = new MultiValuedTestData(
            Provisioning.A_userCertificate,
            new String[]{CERT_NAME_1, CERT_NAME_2},
            new String[]{CERT_NAME_1},
            new String[]{CERT_NAME_2}
            );
    
    private static MultiValuedTestData RFC2252_BINARY_MULTI = new MultiValuedTestData(
            Provisioning.A_userSMIMECertificate,
            new String[]{CONTENT_NAME_1, CONTENT_NAME_2, CONTENT_NAME_3},
            new String[]{CONTENT_NAME_1, CONTENT_NAME_2},
            new String[]{CONTENT_NAME_3}
            );
    
    
    private static class SingleValuedTestData {
        String attrName;
        String content;
        
        SingleValuedTestData(String attrName, String content) {
            this.attrName = attrName;
            this.content = content;
        }
    };
    
    private static class MultiValuedTestData {
        String attrName;
        String[] contents;
        String[] contentsToRemove;
        String[] contentsRemaining;
        
        MultiValuedTestData(String attrName, String[] contents, String[] contentsToRemove, String[] contentsRemaining) {
            this.attrName = attrName;
            this.contents = contents;
            this.contentsToRemove = contentsToRemove;
            this.contentsRemaining = contentsRemaining;
        }
    }
    
    public static class Content {
        private String string;
        private byte[] binary;
        
        private static int SEQUENCE = 0;
        
        public Content(byte[] content) {
            this.binary = content;
            this.string = ByteUtil.encodeLDAPBase64(content);
        }
        
        public String getString() {
            return string;
        }
        
        public byte[] getBinary() {
            return binary;
        }
        
        public static Content getContentByFileName(String contentFileName) throws IOException {
            File inFile = new File(contentFileName);
            return new Content(ByteUtil.getContent(inFile));
        }
        
        public static Content generateContent(int numBytes) {
            byte[] content = new byte[numBytes];
            Random random = new Random();
            random.nextBytes(content);
            return new Content(content);
        }
        
        private static synchronized int nextSequence() {
            return ++SEQUENCE;
        }
        
        public static Content generateCert() throws Exception {
            StringBuilder sb = new StringBuilder(DATA_PATH);
            sb.append(File.separator).append("temp");
            
            File dir = new File(sb.toString());
            FileUtil.deleteDir(dir);
            
            // create the temp directory
            FileUtil.ensureDirExists(dir);
            
            String keystoreFileName = dir.getAbsolutePath() + File.separator + "keystore.unittest";
            String aliasName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String certFileName = dir.getAbsolutePath() + File.separator + aliasName + ".crt";
            
            File keyStoreFile = new File(keystoreFileName);
            if (keyStoreFile.exists()) {
                keyStoreFile.delete();
            }
            
            String cn = aliasName + "-" + nextSequence();  // make sure it is a unique one
            
            /*
            String GEN_CERT_CMD = "keytool -genkey -alias " + aliasName +
            " -keystore " + keystoreFileName + " -storepass test123 -keypass test123" + 
            " -dname 'CN=cName, OU=orgUnit, O=org, L=city, S=state, C=countryCode'";
            */
            
            List<String> GEN_CERT_CMD = new ArrayList<String>();
            GEN_CERT_CMD.add("keytool");
            GEN_CERT_CMD.add("-genkey");
            GEN_CERT_CMD.add("-keystore");
            GEN_CERT_CMD.add(keystoreFileName);
            GEN_CERT_CMD.add("-storepass");
            GEN_CERT_CMD.add("test123");
            GEN_CERT_CMD.add("-alias");
            GEN_CERT_CMD.add("aliasName");
            GEN_CERT_CMD.add("-keypass");
            GEN_CERT_CMD.add("test123");
            GEN_CERT_CMD.add("-dname");
            GEN_CERT_CMD.add("CN= " + cn + ", OU=zimbra, O=VMWare, L=Palo Alto, S=California, C=US");
            
            StringBuilder GEN_CERT_CMD_STR = new StringBuilder();
            for (String str : GEN_CERT_CMD) {
                GEN_CERT_CMD_STR.append(str).append(" ");
            }
            
            /*
            String EXPORT_CERT_CMD = "keytool -export" +
            " -keystore " + keystoreFileName + " -storepass test123 -keypass test123" + 
            " -alias " + aliasName + " -file " + certFileName;
            System.out.println(GEN_CERT_CMD);
            System.out.println(EXPORT_CERT_CMD);
            */
            
            List<String> EXPORT_CERT_CMD = new ArrayList<String>();
            EXPORT_CERT_CMD.add("keytool");
            EXPORT_CERT_CMD.add("-export");
            EXPORT_CERT_CMD.add("-keystore");
            EXPORT_CERT_CMD.add(keystoreFileName);
            EXPORT_CERT_CMD.add("-storepass");
            EXPORT_CERT_CMD.add("test123");
            EXPORT_CERT_CMD.add("-alias");
            EXPORT_CERT_CMD.add("aliasName");
            EXPORT_CERT_CMD.add("-keypass");
            EXPORT_CERT_CMD.add("test123");
            EXPORT_CERT_CMD.add("-file");
            EXPORT_CERT_CMD.add(certFileName);
            
            StringBuilder EXPORT_CERT_CMD_STR = new StringBuilder();
            for (String str : EXPORT_CERT_CMD) {
                EXPORT_CERT_CMD_STR.append(str).append(" ");
            }
            
            ProcessBuilder pb;
            Process process;
            int exitValue;
            
            // System.out.println(GEN_CERT_CMD_STR.toString());
            pb = new ProcessBuilder(GEN_CERT_CMD);
            pb.directory(dir);
            process = pb.start();
            exitValue = process.waitFor();
            if (exitValue != 0) {
                String error = new String(ByteUtil.getContent(process.getErrorStream(), -1));
                System.out.println(error);
            }
            Assert.assertEquals(0, exitValue);
            
            // System.out.println(EXPORT_CERT_CMD_STR.toString());
            pb = new ProcessBuilder(EXPORT_CERT_CMD);
            pb.directory(dir);
            process = pb.start();
            exitValue = process.waitFor();
            if (exitValue != 0) {
                String error = new String(ByteUtil.getContent(process.getErrorStream(), -1));
                System.out.println(error);
            }
            Assert.assertEquals(0, exitValue);
            
            File certFile = new File(certFileName);
            Assert.assertTrue(certFile.exists());
            
            Content content = getContentByFileName(certFileName);
            
            // delete the temp directory and all files under it
            FileUtil.deleteDir(dir);
            
            // content = getContentByFileName("/Users/pshao/p4/main/ZimbraServer/data/unittest/ldap/binaryContent/user1_primary.DER.crt");
            return content;
        }
        
        public boolean equals(String str) {
            return string.equals(str);
        }
        
        public boolean equals(byte[] bin) {
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

    private Entry getEntry() throws Exception {
        String entryName = TestUtil.getAddress(USER, domain.getName());
        return prov.get(AccountBy.name, entryName);
    }
    
    private void delete(String attrName) throws Exception {
        Entry entry = getEntry();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, null); 
        prov.modifyAttrs(entry, attrs);
        
        byte[] value = entry.getBinaryAttr(attrName);
        Assert.assertNull(value);
    }
    
    // testing the code path when value is a String 
    private void modify(String attrName, String contentName) throws Exception {
        Entry entry = getEntry();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, Content.getContentByFileName(contentName).getString());
        prov.modifyAttrs(entry, attrs);
    }
    
    // testing the code path when value is a String[]
    private void modify(String attrName, String[] contentNames) throws Exception {
        Entry entry = getEntry();
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (String contentName : contentNames) {
            StringUtil.addToMultiMap(attrs, attrName, Content.getContentByFileName(contentName).getString());
        }
        prov.modifyAttrs(entry, attrs);
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
        Entry entry = getEntry();
        String stringValue = entry.getAttr(attrName);
        byte[] binaryValue = entry.getBinaryAttr(attrName);
        
        Content content = Content.getContentByFileName(contentName);
        Assert.assertTrue(content.equals(stringValue));
        Assert.assertTrue(content.equals(binaryValue));
    }
    
    private void verify(String attrName, String[] contentNames) throws Exception{
        int numContents = contentNames.length;
        
        Entry entry = getEntry();
        String[] stringValues = entry.getMultiAttr(attrName);
        List<byte[]> binaryValues = entry.getMultiBinaryAttr(attrName);
        
        Assert.assertEquals(numContents, stringValues.length);
        Assert.assertEquals(numContents, binaryValues.size());
        
        for (int i = 0; i < numContents; i++) {
            Content content = Content.getContentByFileName(contentNames[i]);
            
            boolean found = false;
            for (int j = 0; j < stringValues.length && !found; j++) {
                if (content.equals(stringValues[j])) {
                    found = true;
                }
            }
            Assert.assertTrue(found);
            
            found = false;
            for (int j = 0; j < binaryValues.size() && !found; j++) {
                if (content.equals(binaryValues.get(j))) {
                    found = true;
                }
            }
            Assert.assertTrue(found);
        }
    }
    
    private void verifyIsEmpty(Entry entry, String attrName) {
        String value = entry.getAttr(attrName);
        Assert.assertNull(value);
    }
    
    private void replaceSingle(SingleValuedTestData data) throws Exception {
        String attrName = data.attrName;
        String contentName = data.content;
        
        delete(attrName);

        replaceAttr(attrName, contentName);
        verify(attrName, contentName);
    }
    
    private void replaceMulti(MultiValuedTestData data) throws Exception {
        String attrName = data.attrName;
        String[] contentNames = data.contents;
        
        delete(attrName);
        
        replaceAttr(attrName, contentNames);
        verify(attrName, contentNames);
    }
   
    private void remove(MultiValuedTestData data, boolean array) throws Exception {
        String attrName = data.attrName;
        String[] contentNames = data.contents;
        
        delete(attrName);
        
        replaceAttr(attrName, contentNames);
        verify(attrName, contentNames);  
        
        String[] contentNamesToRemove = data.contentsToRemove;
        String[] contentNamesRemaining = data.contentsRemaining;
        
        if (array) {
            removeAttr(attrName, contentNamesToRemove);
        } else {
            for (String contentName : contentNamesToRemove) {
                removeAttr(attrName, contentName);
            }
        }

        verify(attrName, contentNamesRemaining);
    }

    private void add(MultiValuedTestData data, boolean array) throws Exception {
        String attrName = data.attrName;
        String[] contentNames = data.contents;
        
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
    
    @BeforeClass
    public static void init() throws Exception {
        prov = LdapProv.getInst();
        domain = TestLdapProvDomain.createDomain(prov, baseDomainName(), null);
        
        String entryName = TestUtil.getAddress(USER, domain.getName());
        Account acct = prov.get(AccountBy.name, entryName);
        Assert.assertNull(acct);
        
        acct = prov.createAccount(entryName, "test123", null);
        Assert.assertNotNull(acct);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String entryName = TestUtil.getAddress(USER, domain.getName());
        Account acct = prov.get(AccountBy.name, entryName);
        Assert.assertNotNull(acct);
        
        prov.deleteAccount(acct.getId());
        
        String baseDomainName = baseDomainName();
        TestLdap.deleteEntireBranch(baseDomainName);
    }
    
    private static String baseDomainName() {
        return baseDomainName(TestLdapBinary.class);
    }
    
    @Test
    public void testTooLarge() throws Exception {
        SingleValuedTestData data = ZIMBRA_BINARY_SINGLE_TEST_TOO_LARGE;
        
        String attrName = data.attrName;
        String contentName = data.content;
        
        delete(attrName);

        boolean caught = false;
        try {
            replaceAttr(attrName, contentName);
        } catch (ServiceException e) {
            if (AccountServiceException.INVALID_ATTR_VALUE.equals(e.getCode())) {
                caught = true;
            }
        }
        
        Assert.assertTrue(caught);
    }
    
    @Test
    public void testReplaceSingle() throws Exception {
        replaceSingle(ZIMBRA_BINARY_SINGLE);
        replaceSingle(ZIMBRA_CERTIFICATE_SINGLE);
    }
    
    @Test
    public void testReplaceMulti() throws Exception {
        replaceMulti(ZIMBRA_BINARY_MULTI);
        replaceMulti(CERTIFICATE_MULTI);
        replaceMulti(RFC2252_BINARY_MULTI);
    }
    
    @Test
    public void testRemove() throws Exception {
        remove(ZIMBRA_BINARY_MULTI, false);
        remove(ZIMBRA_BINARY_MULTI, true);
        
        remove(CERTIFICATE_MULTI, false);
        remove(CERTIFICATE_MULTI, true);
        
        // RFC 2252 binary does not support adding/deleting individual values
        // remove(RFC2252_BINARY_MULTI, false);
        // remove(RFC2252_BINARY_MULTI, true);
    }
    
    @Test
    public void testAdd() throws Exception {
        add(ZIMBRA_BINARY_MULTI, false);
        add(ZIMBRA_BINARY_MULTI, true);
        
        add(CERTIFICATE_MULTI, false);
        add(CERTIFICATE_MULTI, true);
        
        // RFC 2252 binary does not support adding/deleting individual values
        // add(RFC2252_BINARY_MULTI, false);
        // add(RFC2252_BINARY_MULTI, true);
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

}
