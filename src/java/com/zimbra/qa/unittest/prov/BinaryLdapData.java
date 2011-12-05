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
package com.zimbra.qa.unittest.prov;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.Assert;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;

public class BinaryLdapData {

    public static final String DATA_PATH = "/opt/zimbra/unittest/ldap/binaryContent/";
    
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
            StringBuilder sb = new StringBuilder(BinaryLdapData.DATA_PATH);
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

}
