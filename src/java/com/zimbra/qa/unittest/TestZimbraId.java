package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.servlet.ZimbraServlet;

public class TestZimbraId extends TestCase {
    
    private static final String TEST_NAME = "test-zimbraid";
    private static final String TEST_ID = TestProvisioningUtil.genTestId();
    private static final String USER = "user1";
    private static final String PASSWORD = "test123";
    private static final String DOMAIN = TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
    private static final String ZIMBRA_ID = "1234567890@" + TEST_ID;
    
    private static LdapProvisioning sLdapProv;
    private static SoapProvisioning sSoapProv;
    
    static {
        // create test domain
        try {
            Map<String, Object> attrs = new HashMap<String, Object>();
            Domain domain = Provisioning.getInstance().createDomain(DOMAIN, attrs);
        } catch (ServiceException e) {
            fail();
        }
        
        // init LdapPovisioning instance
        Provisioning prov = Provisioning.getInstance();
        assertTrue(prov instanceof LdapProvisioning);
        sLdapProv = (LdapProvisioning)prov;
        
        // init logs and ssl
        CliUtil.toolSetup();
        
        // init SoapPovisioning instance
        sSoapProv = new SoapProvisioning();
        sSoapProv.soapSetURI("https://localhost:7071" + ZimbraServlet.ADMIN_SERVICE_URI);
        try {
            sSoapProv.soapZimbraAdminAuthenticate();
        } catch (Exception e) {
            System.out.println("soapZimbraAdminAuthenticate failed");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void setUp() throws Exception {
    }
    
    public void tearDown() throws Exception {
    }
    
    private String authToken(Account acct) throws Exception {
        AuthToken at = new AuthToken(acct);
        return at.getEncoded();
    }
    
    public void createAccountWithZimbraId(Provisioning prov, String note) throws Exception {
        String userName = "acct-with-zimbra-id-" + note + "@" + DOMAIN;
        String zimbraId = ZIMBRA_ID + "-" + note;
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, zimbraId);
        Account acct = prov.createAccount(userName, PASSWORD, attrs);
        assertNotNull(acct);
        assertEquals(acct.getId(), zimbraId);
        
        // try get a ccount by id
        Account acctById = prov.get(Provisioning.AccountBy.id, zimbraId);
        assertNotNull(acctById);
    }
    
    public void testCreateAccountWithZimbraId() throws Exception {
        createAccountWithZimbraId(sLdapProv, "ldap");
        createAccountWithZimbraId(sSoapProv, "soap");
    }
    
    public void createAccountWithInvalidZimbraId(Provisioning prov) throws Exception {
        String userName = "acct-with-invalid-zimbra-id" + "@" + DOMAIN;
        String zimbraId = "containing:colon";
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, zimbraId);
        
        try {
            Account acct = prov.createAccount(userName, PASSWORD, attrs);
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            return;
        }
        fail();
    }
    
    public void testCreateAccountWithInvalidZimbraId() throws Exception {
        createAccountWithInvalidZimbraId(sLdapProv);
        createAccountWithInvalidZimbraId(sSoapProv);
    }
    
    public void createAccountWithCosName(Provisioning prov, String note) throws Exception {
        // create a COS
        String cosName = "cos-testCreateAccountWithCosName-" + note + "-" + TEST_ID;
        Cos cos = prov.createCos(cosName, new HashMap<String, Object>());
        
        String userName = "acct-with-cos-name-" + note + "@" + DOMAIN;
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraCOSId, cosName);
        Account acct = prov.createAccount(userName, PASSWORD, attrs);
        
        Cos acctCos = prov.getCOS(acct);
        assertEquals(cos.getName(), acctCos.getName());
        assertEquals(cos.getId(), acctCos.getId());
    }
    
    public void testCreateAccountWithCosName() throws Exception {
        createAccountWithCosName(sLdapProv, "ldap");
        createAccountWithCosName(sSoapProv, "soap");
    }
    
    public void createAccountWithCosId(Provisioning prov, String note) throws Exception {
        // create a COS
        String cosName = "cos-testCreateAccountWithCosId-" + note + "-" + TEST_ID;
        Cos cos = prov.createCos(cosName, new HashMap<String, Object>());
        
        String userName = "acct-with-cos-id-" + note + "@" + DOMAIN;
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        Account acct = prov.createAccount(userName, PASSWORD, attrs);
        
        Cos acctCos = prov.getCOS(acct);
        assertEquals(cos.getName(), acctCos.getName());
        assertEquals(cos.getId(), acctCos.getId());
    }
    
    public void testCreateAccountWithCosId() throws Exception {
        createAccountWithCosId(sLdapProv, "ldap");
        createAccountWithCosId(sSoapProv, "soap");
    }
    
    public void testFileUpload() throws Exception {
        Account acct = TestUtil.getAccount(USER);
        
        int bodyLen = 128;
        byte[] body = new byte[bodyLen];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(body);
        
        Upload ulSaved = FileUploadServlet.saveUpload(new ByteArrayInputStream(body), "zimbraId-test", "text/plain", acct.getId());
        // System.out.println("Upload id is: " + ulSaved.getId());
        
        String authToken = authToken(acct);
        Upload ulFetched = FileUploadServlet.fetchUpload(acct.getId(), ulSaved.getId(), authToken);
        
        assertEquals(ulSaved.getId(), ulFetched.getId());
        assertEquals(ulSaved.getName(), ulFetched.getName());
        assertEquals(ulSaved.getSize(), ulFetched.getSize());
        assertEquals(ulSaved.getContentType(), ulFetched.getContentType());
        assertEquals(ulSaved.toString(), ulFetched.toString());
        
        byte[] bytesUploaded = ByteUtil.getContent(ulFetched.getInputStream(), -1);
        assertTrue(Arrays.equals(body, bytesUploaded));
    }
    
    public static void main(String[] args) throws Exception {
        TestUtil.runTest(new TestSuite(TestZimbraId.class));        
    }
}
