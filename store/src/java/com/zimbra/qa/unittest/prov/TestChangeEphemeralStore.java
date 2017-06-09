package com.zimbra.qa.unittest.prov;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.qa.unittest.TestUtil;

public class TestChangeEphemeralStore {

    private static final Provisioning prov = Provisioning.getInstance();
    private static final SoapProvisioning soapProv = new SoapProvisioning();
    private static List<Server> servers;
    private static String originalEphemeralURL;
    private static final String testURL = "ldap://test";

    @BeforeClass
    public static void beforeClass() throws Exception {
        servers = prov.getAllMailClientServers();
        originalEphemeralURL = prov.getConfig().getEphemeralBackendURL();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prov.getConfig().setEphemeralBackendURL(originalEphemeralURL);
    }

    @Test
    public void testChangeEphemeralBackend() throws Exception {
        TestUtil.assumeTrue(String.format("Number of servers=%d needs to be > 1", servers.size()), servers.size() > 1);
        prov.getConfig().setEphemeralBackendURL(testURL);
        int maxWaitMillis = 5000;
        for (Server server: servers) {
            soapProv.soapSetURI(URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI, true));
            soapProv.soapZimbraAdminAuthenticate();
            int waited = 0;
            String newUrl = null;
            while (waited < maxWaitMillis) {
                newUrl = soapProv.getConfig().getEphemeralBackendURL();
                if (newUrl.equals(testURL)) {
                    break;
                } else {
                    Thread.sleep(100);
                    waited+=100;
                }
            }
            assertEquals(testURL, newUrl);
        }
    }
}
