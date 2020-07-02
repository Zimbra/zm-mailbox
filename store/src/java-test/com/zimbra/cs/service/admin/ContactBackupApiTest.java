package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import org.junit.Ignore;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ContactBackupRequest;
import com.zimbra.soap.admin.message.ContactBackupRequest.Operation;
import com.zimbra.soap.admin.message.ContactBackupResponse;
import com.zimbra.soap.admin.type.ContactBackupServer;
import com.zimbra.soap.admin.type.ContactBackupServer.ContactBackupStatus;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.admin.type.ServerSelector.ServerBy;

import junit.framework.Assert;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ContactBackupApiTest {
    private static Provisioning prov = null;
    private static final String DOMAIN_NAME = "zimbra.com";
    private static final String BUG_NUMBER = "zcs3594";
    private static final String ADMIN = "admin_" + BUG_NUMBER + "@" + DOMAIN_NAME;
    private static Account admin = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraIsAdminAccount, true); // set admin account
        prov.createAccount(ADMIN, "secret", attrs);
        admin = prov.getAccountByName(ADMIN);
        MailboxManager.getInstance().getMailboxByAccount(admin);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testContactBackupApiWithStart() throws Exception {
        ContactBackupRequest cbReq = new ContactBackupRequest();
        cbReq.setOp(Operation.start);
        cbReq.addServer(new ServerSelector(ServerBy.name, "test1.com"));
        cbReq.addServer(new ServerSelector(ServerBy.name, "test2.com"));
        cbReq.addServer(new ServerSelector(ServerBy.name, "test3.com"));
        Element request = JaxbUtil.jaxbToElement(cbReq);
        Element response  = null;

        try {
            response = new MockContactBackup().handle(request, ServiceTestUtil.getRequestContext(admin));
        } catch (ServiceException se) {
            Assert.fail("ServiceException must not be thrown.");
        }
        if (response == null) {
            Assert.fail("Response must be received.");
        }
        ContactBackupResponse cbResp = JaxbUtil.elementToJaxb(response);
        Assert.assertNotNull(cbResp.getServers());
        List<ContactBackupServer> servers = cbResp.getServers();
        for (ContactBackupServer server : servers) {
            Assert.assertEquals(server.getStatus(), ContactBackupStatus.started);
        }
    }

    @Test
    public void testContactBackupApiWithStop() throws Exception {
        ContactBackupRequest cbReq = new ContactBackupRequest();
        cbReq.setOp(Operation.stop);
        cbReq.addServer(new ServerSelector(ServerBy.name, "test1.com"));
        cbReq.addServer(new ServerSelector(ServerBy.name, "test2.com"));
        cbReq.addServer(new ServerSelector(ServerBy.name, "test3.com"));
        Element request = JaxbUtil.jaxbToElement(cbReq);
        Element response  = null;

        try {
            response = new MockContactBackup().handle(request, ServiceTestUtil.getRequestContext(admin));
        } catch (ServiceException se) {
            Assert.fail("ServiceException must not be thrown.");
        }
        if (response == null) {
            Assert.fail("Response must be received.");
        }
        ContactBackupResponse cbResp = JaxbUtil.elementToJaxb(response);
        Assert.assertNotNull(cbResp.getServers());
        List<ContactBackupServer> servers = cbResp.getServers();
        for (ContactBackupServer server : servers) {
            Assert.assertEquals(server.getStatus(), ContactBackupStatus.stopped);
        }
    }

    public class MockContactBackup extends ContactBackup {
        @Override
        protected List<ContactBackupServer> startContactBackup(List<ServerSelector> selectors, Map<String, Object> context, ZimbraSoapContext zsc) throws ServiceException {
            List<ContactBackupServer> servers = new ArrayList<ContactBackupServer>();
            for (ServerSelector serverSelector : selectors) {
                servers.add(new ContactBackupServer(serverSelector.getKey(), ContactBackupStatus.started));
            }
            return servers;
        }

        @Override
        protected List<ContactBackupServer> stopContactBackup(List<ServerSelector> selectors, Map<String, Object> context, ZimbraSoapContext zsc) throws ServiceException {
            List<ContactBackupServer> servers = new ArrayList<ContactBackupServer>();
            for (ServerSelector serverSelector : selectors) {
                servers.add(new ContactBackupServer(serverSelector.getKey(), ContactBackupStatus.stopped));
            }
            return servers;
        }
    }
}
