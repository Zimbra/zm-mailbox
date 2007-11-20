package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;

public class TestZimbraId extends TestCase {
    
    private static final String USER = "user1";
            
    public void setUp() throws Exception {
    }
    
    public void tearDown() throws Exception {
    }
    
    private String authToken(Account acct) throws Exception {
        AuthToken at = new AuthToken(acct);
        return at.getEncoded();
    }

    private void assertEquals(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i=0; i<expected.length; i++) {
            if (expected[i] != actual[i])
                fail();
        }
    }
    
    public void testFileUpload() throws Exception {
        Account acct = TestUtil.getAccount(USER);
        
        int bodyLen = 128;
        byte[] body = new byte[bodyLen];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(body);
        
        Upload ulSaved = FileUploadServlet.saveUpload(new ByteArrayInputStream(body), "zimbraId-test", "text/plain", acct.getId());
        System.out.println("Upload id is: " + ulSaved.getId());
        
        String authToken = authToken(acct);
        Upload ulFetched = FileUploadServlet.fetchUpload(acct.getId(), ulSaved.getId(), authToken);
        
        assertEquals(ulSaved.getId(), ulFetched.getId());
        assertEquals(ulSaved.getName(), ulFetched.getName());
        assertEquals(ulSaved.getSize(), ulFetched.getSize());
        assertEquals(ulSaved.getContentType(), ulFetched.getContentType());
        assertEquals(ulSaved.toString(), ulFetched.toString());
        
        byte[] bytesUploaded = ByteUtil.getContent(ulFetched.getInputStream(), -1);
        assertEquals(body, bytesUploaded);
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        TestUtil.runTest(new TestSuite(TestZimbraId.class));        
    }
}
