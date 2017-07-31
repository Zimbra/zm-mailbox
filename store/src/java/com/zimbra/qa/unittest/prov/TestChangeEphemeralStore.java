package com.zimbra.qa.unittest.prov;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.qa.unittest.TestUtil;

public class TestChangeEphemeralStore {

    private static final Provisioning prov = Provisioning.getInstance();
    private static final SoapProvisioning soapProv = new SoapProvisioning();
    private static List<Server> servers;
    private static String originalEphemeralURL;
    private static String originalPrevEphemeralURL;
    private static String originalMigrationInfo;
    private static final String testURL = "ldap://test";

    @BeforeClass
    public static void beforeClass() throws Exception {
        servers = prov.getAllMailClientServers();
        Config config = prov.getConfig();
        originalEphemeralURL = config.getEphemeralBackendURL();
        originalPrevEphemeralURL = config.getPreviousEphemeralBackendURL();
        originalMigrationInfo = config.getAttributeMigrationInfo();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Config config = prov.getConfig();
        config.setEphemeralBackendURL(originalEphemeralURL);
        if (Strings.isNullOrEmpty(originalPrevEphemeralURL)) {
            config.unsetPreviousEphemeralBackendURL();
        } else {
            config.setPreviousEphemeralBackendURL(originalPrevEphemeralURL);
        }
        if (Strings.isNullOrEmpty(originalMigrationInfo)) {
            config.unsetAttributeMigrationInfo();
        } else {
            config.setAttributeMigrationInfo(originalMigrationInfo);
        }
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

    @Test
    public void testPreviousEphemeralBackendURL() throws Exception {
        String testUrl1 = "ldap://1";
        String testUrl2 = "ldap://2";
        String testUrl3 = "ldap://3";
        Config config = prov.getConfig();
        config.setEphemeralBackendURL(testUrl1);
        config.setEphemeralBackendURL(testUrl2);
        assertEquals("previous URL should be " + testUrl1, config.getPreviousEphemeralBackendURL(), testUrl1);
        config.setEphemeralBackendURL(testUrl3);
        assertEquals("previous URL should be " + testUrl2, config.getPreviousEphemeralBackendURL(), testUrl2);
    }

    @Test
    public void testChangeURLDuringMigration() throws Exception {
        Config config = prov.getConfig();
        MigrationInfo info = MigrationInfo.getFactory().getInfo();
        info.setURL("ldap://test");
        info.beginMigration();
        try {
            config.setEphemeralBackendURL("ldap://test");
            fail("should not be able to change ephemeral backend URL while migration is in progress");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("attribute migration to ldap://test is currently in progress"));
        } finally {
            info.clearData();
        }
    }
}
