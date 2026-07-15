package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DeleteAccountTest {

    private Provisioning prov;

    private Account adminAccount;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        setupAuthTokenKey();
        prov = Provisioning.getInstance();
        Map<String, Object> adminAttrs = new HashMap<>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        adminAccount = prov.createAccount("admin@test.zimbra.com", "adminpass", adminAttrs);
        RightManager.getInstance().getAllAdminRights();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    private static void setupAuthTokenKey() throws Exception {
        // directly set the static field via reflection as absolute fallback
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        java.lang.reflect.Constructor<?> ctor =
                com.zimbra.cs.account.AuthTokenKey.class.getDeclaredConstructor(long.class, byte[].class);
        ctor.setAccessible(true);
        Object testKey = ctor.newInstance(0L, keyBytes);
        java.lang.reflect.Field field =
                com.zimbra.cs.account.AuthTokenKey.class.getDeclaredField("sLatestKey");
        field.setAccessible(true);
        field.set(null, testKey);
    }

    private Element buildDeleteAccountRequest(String accountId) {
        Element request = Element.XMLElement.mFactory.createElement(AdminConstants.DELETE_ACCOUNT_REQUEST);
        request.addNonUniqueElement(AdminConstants.E_ID).setText(accountId);
        return request;
    }

    private String getLocalServerName() throws ServiceException {
        Server localServer = prov.getLocalServer();
        return localServer.getAttr(Provisioning.A_zimbraServiceHostname, "localhost");
    }

    @Test
    public void testDeleteAccountOnLocalServer() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailHost, getLocalServerName());
        attrs.put(Provisioning.A_zimbraId, "localUserId");
        Account target = prov.createAccount("local-user@test.zimbra.com", "password", attrs);
        String targetId = target.getId();

        Assert.assertNotNull("Precondition: account must exist", prov.getAccountById(targetId));

        Element request = buildDeleteAccountRequest(targetId);
        DeleteAccount handler = new DeleteAccount();
        Map<String, Object> context = ServiceTestUtil.getRequestContext(adminAccount);
        handler.handle(request, context);

        Assert.assertNull("Account should be deleted when hosted on local server",
                prov.getAccountById(targetId));
    }

    @Test
    public void testRemoteAccountNotDeletedLocally() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraMailHost, "remote-mbox-node.example.com");
        attrs.put(Provisioning.A_zimbraId, "remoteUserId");
        Account target = prov.createAccount("remote-user@test.zimbra.com", "password", attrs);
        String targetId = target.getId();

        Assert.assertNotNull("Precondition: account must exist", prov.getAccountById(targetId));

        Element request = buildDeleteAccountRequest(targetId);
        DeleteAccount handler = new DeleteAccount();
        Map<String, Object> context = ServiceTestUtil.getRequestContext(adminAccount);
        handler.handle(request, context);

        Assert.assertNotNull("Remote server account must not be deleted locally",
                prov.getAccountById(targetId));
    }
}
