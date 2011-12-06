package com.zimbra.qa.unittest.prov.soap;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;

import static org.junit.Assert.*;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.qa.unittest.TestUtil;

public class TestSoapProvisioning extends SoapTest {
    
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
    
    @AfterClass 
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }

    @Test
    public void isExpired() throws Exception {
        long lifeTimeSecs = 5;  // 5 seconds
        
        String acctName = TestUtil.getAddress("isExpired", domain.getName());
        String password = "test123";
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraAdminAuthTokenLifetime, String.valueOf(lifeTimeSecs) + "s");
        Account acct = provUtil.createAccount(acctName, password, attrs);
        
        SoapProvisioning soapProv = new SoapProvisioning();
        
        Server server = prov.getLocalServer();
        soapProv.soapSetURI(URLUtil.getAdminURL(server));
        
        assertTrue(soapProv.isExpired());
        
        soapProv.soapAdminAuthenticate(acctName, password);
        
        assertFalse(soapProv.isExpired());
        
        System.out.println("Waiting for " + lifeTimeSecs + " seconds");
        Thread.sleep((lifeTimeSecs+1)*1000);
        
        assertTrue(soapProv.isExpired());
        
        prov.deleteAccount(acct.getId());
    }
}
