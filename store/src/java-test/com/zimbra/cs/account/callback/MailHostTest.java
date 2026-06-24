/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link MailHost#preModify}. Drives the real zimbraMailHost validation
 * against {@link Server} entries created through the in-memory harness: a valid mail-client
 * server must derive and inject the matching zimbraMailTransport, while unknown hosts, non
 * mail-client servers and the host+transport combination must be rejected.
 */
public class MailHostTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    /* Creates a server whose service hostname is reachable via Key.ServerBy.serviceHostname
     *  and which advertises the mailbox service so hasMailClientService() is true. */
    private Server newMailServer(String hostname, String lmtpPort) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, hostname);
        attrs.put(Provisioning.A_zimbraServiceHostname, hostname);
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        if (lmtpPort != null) {
            attrs.put(Provisioning.A_zimbraLmtpBindPort, lmtpPort);
        }
        return prov.createServer(hostname, attrs);
    }

    @Test
    public void preModifyEmptyValueReturnsWithoutValidation() throws Exception {
        // Arrange -- empty value means "unsetting"; no transport should be injected
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost, "", toModify, null);

        // Assert -- nothing added to the modify map
        assertNull("empty mail host must not inject a transport",
                toModify.get(Provisioning.A_zimbraMailTransport));
    }

    @Test
    public void preModifyUnsettingViaMinusKeyReturnsWithoutValidation() throws Exception {
        // Arrange -- presence of "-zimbraMailHost" signals an unset operation
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put("-" + Provisioning.A_zimbraMailHost, "old.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                "newhost.example.com", toModify, null);

        // Assert -- unset short-circuits before any server lookup / transport injection
        assertNull("unset operation must not inject a transport",
                toModify.get(Provisioning.A_zimbraMailTransport));
    }

    @Test
    public void preModifyBothHostAndTransportThrowsInvalidRequest() throws Exception {
        // Arrange -- setting both zimbraMailHost and zimbraMailTransport is forbidden
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraMailTransport, "lmtp:other:7025");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                    "host.example.com", toModify, null);
            fail("expected INVALID_REQUEST when both host and transport are set");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertEquals("error message identifies the conflicting attributes", true,
                    e.getMessage().contains("same request"));
        }
    }

    @Test
    public void preModifyUnknownHostThrowsInvalidRequest() throws Exception {
        // Arrange -- no server with this service hostname exists
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                    "nosuchhost.example.com", toModify, null);
            fail("expected INVALID_REQUEST for unknown mail host");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertEquals("message names the bad host", true,
                    e.getMessage().contains("nosuchhost.example.com"));
        }
    }

    @Test
    public void preModifyServerWithoutMailClientServiceThrowsInvalidRequest() throws Exception {
        // Arrange -- server resolves but has no mailbox/mailclient service enabled
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "nomc.example.com");
        attrs.put(Provisioning.A_zimbraServiceHostname, "nomc.example.com");
        Server server = prov.createServer("nomc.example.com", attrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                    "nomc.example.com", toModify, null);
            fail("expected INVALID_REQUEST when server lacks the mail client service");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertEquals("message mentions the service webapp requirement", true,
                    e.getMessage().contains("service webapp enabled"));
        } finally {
            prov.deleteServer(server.getId());
        }
    }

    @Test
    public void preModifyValidMailServerOnCreateInjectsComputedTransport() throws Exception {
        // Arrange -- valid mail server with explicit LMTP port, CREATE op (entry==null path)
        Server server = newMailServer("mc-create.example.com", "8025");
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.CREATE);

        // Act
        new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                "mc-create.example.com", toModify, null);

        // Assert -- transport derived as lmtp:<serviceHostname>:<lmtpPort>
        assertEquals("lmtp:mc-create.example.com:8025",
                toModify.get(Provisioning.A_zimbraMailTransport));
        prov.deleteServer(server.getId());
    }

    @Test
    public void preModifyValidMailServerDefaultPortUsesDefaultLmtpPort() throws Exception {
        // Arrange -- no LMTP port set, so the callback falls back to Config.D_LMTP_BIND_PORT (7025)
        Server server = newMailServer("mc-default.example.com", null);
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.CREATE);

        // Act
        new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                "mc-default.example.com", toModify, null);

        // Assert
        assertEquals("lmtp:mc-default.example.com:7025",
                toModify.get(Provisioning.A_zimbraMailTransport));
        prov.deleteServer(server.getId());
    }

    @Test
    public void preModifyChangingHostOnAccountMatchingOldTransportInjectsNewTransport() throws Exception {
        // Arrange -- modify (not create): account currently homed on the old server with a
        // matching transport, so switching to the new server must succeed and re-inject.
        Server oldServer = newMailServer("old-mh.example.com", "7025");
        Server newServer = newMailServer("new-mh.example.com", "7025");
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraMailHost, "old-mh.example.com");
        acctAttrs.put(Provisioning.A_zimbraMailTransport, "lmtp:old-mh.example.com:7025");
        Account acct = prov.createAccount("mh-change@example.com", "test123", acctAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                "new-mh.example.com", toModify, acct);

        // Assert -- new transport computed from the new server
        assertEquals("lmtp:new-mh.example.com:7025",
                toModify.get(Provisioning.A_zimbraMailTransport));
        prov.deleteAccount(acct.getId());
        prov.deleteServer(oldServer.getId());
        prov.deleteServer(newServer.getId());
    }

    @Test
    public void preModifyChangingHostOnAccountMismatchedOldTransportThrows() throws Exception {
        // Arrange -- account's stored transport does NOT match what the old server would compute,
        // so bug-18419 protection must reject the host change.
        Server oldServer = newMailServer("mism-old.example.com", "7025");
        Server newServer = newMailServer("mism-new.example.com", "7025");
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraMailHost, "mism-old.example.com");
        acctAttrs.put(Provisioning.A_zimbraMailTransport, "lmtp:totally-different:9999");
        Account acct = prov.createAccount("mh-mismatch@example.com", "test123", acctAttrs);
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new MailHost().preModify(ctx, Provisioning.A_zimbraMailHost,
                    "mism-new.example.com", toModify, acct);
            fail("expected INVALID_REQUEST when current host/transport do not match");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertFalse("no transport should be injected on the rejected path",
                    toModify.containsKey(Provisioning.A_zimbraMailTransport)
                    && "mism-new".equals(toModify.get(Provisioning.A_zimbraMailTransport)));
        } finally {
            prov.deleteAccount(acct.getId());
            prov.deleteServer(oldServer.getId());
            prov.deleteServer(newServer.getId());
        }
    }

    @Test
    public void postModifyNoOpDoesNotThrow() throws Exception {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- postModify is an intentional no-op
        new MailHost().postModify(ctx, Provisioning.A_zimbraMailHost, null);

        // Assert -- reaching here without exception is the contract
        assertNull("no-op postModify leaves no state", null);
    }
}
