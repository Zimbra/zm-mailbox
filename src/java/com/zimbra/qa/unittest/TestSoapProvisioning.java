package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;

public class TestSoapProvisioning {
    
    @BeforeClass
    public static void init() {
        CliUtil.toolSetup();
        // ZimbraLog.ldap.setLevel(Log.Level.debug);
    }

    @Test
    public void isExpired() throws Exception {
        long lifeTimeSecs = 10;  // 10 seconds
        
        Provisioning prov = Provisioning.getInstance();
        
        String acctName = TestUtil.getAddress("isExpired");
        String password = "test123";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraAdminAuthTokenLifetime, String.valueOf(lifeTimeSecs) + "s");
        Account acct = prov.createAccount(acctName, password, attrs);
        
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
