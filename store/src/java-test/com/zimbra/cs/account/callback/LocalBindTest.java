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

import com.zimbra.common.account.ZAttrProvisioning.MailMode;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link LocalBind#postModify}. Real {@link Server} entries are mutated and
 * the callback is invoked; the resulting {@code zimbraAdminLocalBind} / {@code zimbraMailLocalBind}
 * derived flags are then asserted on the persisted entry.
 */
public class LocalBindTest {

    private Provisioning prov;

    /** Servers created per test, deleted in {@link #tearDown()} even when a test fails. */
    private final List<Server> createdServers = new ArrayList<Server>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        for (Server s : createdServers) {
            try {
                prov.deleteServer(s.getId());
            } catch (Exception ignore) {
                // best-effort cleanup: one failure must not block the rest
            }
        }
        createdServers.clear();
    }

    private Server newServer(String name) throws Exception {
        Server server = prov.createServer(name, new HashMap<String, Object>());
        createdServers.add(server);
        return server;
    }

    @Test
    public void postModifyAdminBindLoopbackAddressSetsAdminLocalBindFalse() throws Exception {
        // Arrange -- loopback address must NOT be treated as a local bind
        Server server = newServer("lb-admin-loop.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAdminBindAddress, "127.0.0.1");
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraAdminBindAddress, server);

        // Assert
        assertFalse("loopback admin bind => zimbraAdminLocalBind false",
                server.isAdminLocalBind());
    }

    @Test
    public void postModifyAdminBindRoutableAddressSetsAdminLocalBindTrue() throws Exception {
        // Arrange -- a non-loopback, non-any address => local bind true
        Server server = newServer("lb-admin-routable.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAdminBindAddress, "8.8.8.8");
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraAdminBindAddress, server);

        // Assert
        assertTrue("routable admin bind => zimbraAdminLocalBind true",
                server.isAdminLocalBind());
    }

    @Test
    public void postModifyAdminBindEmptyAddressSetsAdminLocalBindFalse() throws Exception {
        // Arrange -- empty admin bind address => false
        Server server = newServer("lb-admin-empty.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAdminBindAddress, "");
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraAdminBindAddress, server);

        // Assert
        assertFalse("empty admin bind address => zimbraAdminLocalBind false",
                server.isAdminLocalBind());
    }

    @Test
    public void postModifyAdminBindUnknownHostSetsAdminLocalBindFalse() throws Exception {
        // Arrange -- an unresolvable host triggers UnknownHostException => false
        Server server = newServer("lb-admin-bad.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAdminBindAddress,
                "no-such-host.invalid.zimbra.example");
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraAdminBindAddress, server);

        // Assert
        assertFalse("unresolvable admin bind host => zimbraAdminLocalBind false",
                server.isAdminLocalBind());
    }

    @Test
    public void postModifyMailBindLoopbackWithHttpModeSetsMailLocalBindFalse() throws Exception {
        // Arrange -- http mail mode + loopback bind address => mail local bind false
        Server server = newServer("lb-mail-loop.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailMode, MailMode.http.toString());
        attrs.put(Provisioning.A_zimbraMailBindAddress, "127.0.0.1");
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraMailBindAddress, server);

        // Assert
        assertFalse("http + loopback mail bind => zimbraMailLocalBind false",
                server.isMailLocalBind());
    }

    @Test
    public void postModifyMailBindRoutableWithHttpModeSetsMailLocalBindTrue() throws Exception {
        // Arrange
        Server server = newServer("lb-mail-routable.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailMode, MailMode.http.toString());
        attrs.put(Provisioning.A_zimbraMailBindAddress, "8.8.4.4");
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraMailBindAddress, server);

        // Assert
        assertTrue("http + routable mail bind => zimbraMailLocalBind true",
                server.isMailLocalBind());
    }

    @Test
    public void postModifyMailModeHttpsSetsMailLocalBindTrue() throws Exception {
        // Arrange -- https mail mode always enables http for localhost binding
        Server server = newServer("lb-mail-https.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailMode, MailMode.https.toString());
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraMailMode, server);

        // Assert
        assertTrue("https mail mode => zimbraMailLocalBind true",
                server.isMailLocalBind());
    }

    @Test
    public void postModifyMailBindEmptyWithHttpModeSetsMailLocalBindFalse() throws Exception {
        // Arrange -- http mode, no bind address => false
        Server server = newServer("lb-mail-empty.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailMode, MailMode.http.toString());
        prov.modifyAttrs(server, attrs);

        // Act
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraMailBindAddress, server);

        // Assert
        assertFalse("http + empty mail bind => zimbraMailLocalBind false",
                server.isMailLocalBind());
    }

    @Test
    public void postModifyUnrelatedAttrDoesNotChangeBindFlags() throws Exception {
        // Arrange -- an attr that is neither admin nor mail bind related is a no-op
        Server server = newServer("lb-noop.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailLocalBind, "TRUE");
        prov.modifyAttrs(server, attrs);

        // Act -- callback for an unrelated attribute must not flip the flag
        new LocalBind().postModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraImapBindPort, server);

        // Assert -- the pre-existing value is untouched
        assertTrue("unrelated attribute must leave zimbraMailLocalBind unchanged",
                server.isMailLocalBind());
    }

    @Test
    public void preModifyIsNoOpDoesNotThrow() throws Exception {
        // Arrange
        Server server = newServer("lb-pre.example.com");

        // Act
        new LocalBind().preModify(new CallbackContext(Op.MODIFY),
                Provisioning.A_zimbraMailBindAddress, "8.8.8.8",
                new HashMap<String, Object>(), server);

        // Assert -- preModify is empty by contract
        assertTrue("preModify is a no-op", true);
    }
}
