package com.zimbra.cs.imap;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
public class ImapServerListenerPoolTest {
    private Provisioning prov = Provisioning.getInstance();
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    @Test
    public void testInstanceManagement() throws ServiceException {
        String serverName1 = "example1.zimbra.com";
        String serverName2 = "example2.zimbra.com";
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "example1.zimbra.com");
        attrs.put(Provisioning.A_zimbraAdminPort, 7071);
        prov.createServer(serverName1, attrs);

        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "example2.zimbra.com");
        attrs.put(Provisioning.A_zimbraAdminPort, 7071);
        prov.createServer(serverName2, new HashMap<String, Object>());

        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraMailHost,serverName1);
        Account user1 = prov.createAccount("user1@example1.zimbra.com", "test123", attrs);

        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraMailHost,serverName1);
        Account user1_2 = prov.createAccount("user1_2@example1.zimbra.com", "test123", attrs);

        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraMailHost,serverName2);
        Account user2 = prov.createAccount("user2@example2.zimbra.com", "test123", attrs);

        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraMailHost,serverName2);
        Account user2_2 = prov.createAccount("user2_2@example2.zimbra.com", "test123", attrs);
        
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(user1.getId());
        options.setAccountBy(Key.AccountBy.id);
        options.setPassword("test123");
        options.setAuthAuthToken(false);
        options.setAuthToken("sometoken");
        options.setUri("https://" + serverName1);
        ZMailbox testMbox1 = new MockZMailbox(options);

        options = new ZMailbox.Options();
        options.setAccount(user1_2.getId());
        options.setAccountBy(Key.AccountBy.id);
        options.setPassword("test123");
        options.setAuthAuthToken(false);
        options.setAuthToken("sometoken");
        options.setUri("https://" + serverName1);
        ZMailbox testMbox1_2 = new MockZMailbox(options);

        options = new ZMailbox.Options();
        options.setAccount(user2.getId());
        options.setAccountBy(Key.AccountBy.id);
        options.setPassword("test123");
        options.setAuthAuthToken(false);
        options.setAuthToken("sometoken");
        options.setUri("https://" + serverName2);
        ZMailbox testMbox2 = new MockZMailbox(options);

        options = new ZMailbox.Options();
        options.setAccount(user2_2.getId());
        options.setAccountBy(Key.AccountBy.id);
        options.setPassword("test123");
        options.setAuthAuthToken(false);
        options.setAuthToken("sometoken");
        options.setUri("https://" + serverName2);
        ZMailbox testMbox2_2 = new MockZMailbox(options);

        ImapServerListenerPool pool = ImapServerListenerPool.getInstance();
        ImapServerListener listener1 = pool.get(testMbox1);
        ImapServerListener listener2 = pool.get(testMbox2);
        ImapServerListener listener1_2 = pool.get(testMbox1_2);
        ImapServerListener listener2_2 = pool.get(testMbox2_2);
        assertNotSame("listener 1 should be different from listener 2", listener1, listener2);
        assertNotSame("listener 1_2 should be different from listener 2_2", listener1_2, listener2_2);
        assertSame("listener 1 should be the same as listener 1_2", listener1, listener1_2);
        assertSame("listener 2 should be the same as listener 2_2", listener2, listener2_2);
    }

    class MockZMailbox extends ZMailbox {
        private String accountId = "";
        public MockZMailbox(Options options) throws ServiceException {
            super(options);
            accountId = options.getAccount();
        }

        @Override
        public String getAccountId() throws ServiceException {
            return accountId;
        }
    }
}
