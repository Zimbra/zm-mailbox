/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

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
import com.zimbra.soap.admin.message.GetAllServersRequest;
import com.zimbra.soap.admin.message.GetAllServersResponse;

public class TestSoapProvisioning {
    
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private String ADMIN_NAME = "TestSoapProvisioningAdmin";
    private String ADMIN_PASS = TestUtil.DEFAULT_PASSWORD;
    private int LIFETIME = 5;

    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
    }

    @Before
    public void setUp() throws Exception {
        cleanup();
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraAdminAuthTokenLifetime, String.valueOf(LIFETIME) + "s");
        TestUtil.createAccount(ADMIN_NAME, attrs);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }

    private void cleanup() throws Exception {
        if(TestUtil.accountExists(ADMIN_NAME)) {
            TestUtil.deleteAccount(ADMIN_NAME);
        }
    }

    @Test
    public void isExpired() throws Exception {
        long lifeTimeSecs = 5;  // 5 seconds
        
        SoapProvisioning soapProv = new SoapProvisioning();
        
        Server server = prov.getLocalServer();
        soapProv.soapSetURI(URLUtil.getAdminURL(server));
        
        assertTrue(soapProv.isExpired());
        
        soapProv.soapAdminAuthenticate(ADMIN_NAME, ADMIN_PASS);
        
        assertFalse(soapProv.isExpired());
        
        System.out.println("Waiting for " + lifeTimeSecs + " seconds");
        Thread.sleep((lifeTimeSecs+1)*1000);
        
        assertTrue(soapProv.isExpired());
    }

    @Test
    public void testInvokeJaxbAsAdminWithRetry() throws Exception {
        long lifeTimeSecs = 5;  // 5 seconds
        SoapProvisioning soapProv = new SoapProvisioning();
        Server server = prov.getLocalServer();
        soapProv.soapSetURI(URLUtil.getAdminURL(server));
        soapProv.soapZimbraAdminAuthenticate();
        assertFalse("SoapProvisioning should have a valid token after authenticating as cn=zimbra", soapProv.isExpired());
        GetAllServersRequest req = new GetAllServersRequest(Provisioning.SERVICE_MAILBOX, false);
        GetAllServersResponse resp = soapProv.invokeJaxbAsAdminWithRetry(req);
        assertNotNull("GetAllServersResponse should not be null when executed normally", resp);
        assertFalse(soapProv.isExpired());

        soapProv.soapAdminAuthenticate(ADMIN_NAME, ADMIN_PASS);
        assertFalse("SoapProvisioning should have a valid token after authenticating", soapProv.isExpired());
        req = new GetAllServersRequest(Provisioning.SERVICE_MAILBOX, false);
        resp = soapProv.invokeJaxbAsAdminWithRetry(req);
        assertNotNull("GetAllServersResponse should not be null when executed by an admin account", resp);
        assertFalse("SoapProvisioning should have a valid token before waiting", soapProv.isExpired());

        Thread.sleep((lifeTimeSecs+1)*1000);

        assertTrue("SoapProvisioning should have an expired token after waiting", soapProv.isExpired());
        resp = soapProv.invokeJaxbAsAdminWithRetry(req);
        assertNotNull("GetAllServersResponse should not be null after retrying", resp);
        assertFalse(soapProv.isExpired());
    }
}
