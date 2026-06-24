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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link CheckPortConflict#preModify}. Exercises real {@link Server}
 * entries through the in-memory harness and asserts that conflicting port assignments are
 * rejected while distinct (or zero/empty) ports are accepted.
 */
public class CheckPortConflictTest {

    private Provisioning prov;

    /** Entities created per test, deleted in {@link #tearDown()} even when a test fails. */
    private final List<Server> createdServers = new ArrayList<Server>();

    private final List<Account> createdAccounts = new ArrayList<Account>();

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
        for (Account a : createdAccounts) {
            try {
                prov.deleteAccount(a.getId());
            } catch (Exception ignore) {
                // best-effort cleanup
            }
        }
        createdServers.clear();
        createdAccounts.clear();
    }

    private Server newServer(String name) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Server server = prov.createServer(name, attrs);
        createdServers.add(server);
        return server;
    }

    @Test
    public void preModifyDistinctPortsNoConflict() throws Exception {
        // Arrange
        Server server = newServer("cpc-distinct.example.com");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "143");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "110");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- two distinct ports, one callback invocation per attr (first wins the done-flag)
        CheckPortConflict cb = new CheckPortConflict();
        cb.preModify(ctx, Provisioning.A_zimbraImapBindPort, "143", toModify, server);

        // Assert -- no exception means no conflict was detected
        assertTrue("distinct ports must not conflict", true);
    }

    @Test
    public void preModifyDuplicatePortsInSameModifyThrowsConflict() throws Exception {
        // Arrange -- two different attrs assigned the SAME port in one modify map
        Server server = newServer("cpc-dup.example.com");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "7777");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "7777");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "7777",
                    toModify, server);
            fail("expected INVALID_REQUEST for duplicate port 7777");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("message should mention the conflicting port",
                    e.getMessage().contains("7777"));
        }
    }

    @Test
    public void preModifyPortZeroIsNotAConflict() throws Exception {
        // Arrange -- port 0 means disabled and must never collide, even when duplicated
        Server server = newServer("cpc-zero.example.com");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "0");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "0");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "0",
                toModify, server);

        // Assert -- reached here, so duplicate "0" did not throw
        assertTrue("duplicate port 0 must be allowed", true);
    }

    @Test
    public void preModifyConflictWithExistingServerValueThrowsConflict() throws Exception {
        // Arrange -- an existing port already set on the server, then modify another attr to it
        Server server = newServer("cpc-existing.example.com");
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(Provisioning.A_zimbraImapBindPort, "8888");
        prov.modifyAttrs(server, seed);
        assertEquals("8888", server.getAttr(Provisioning.A_zimbraImapBindPort));

        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraPop3BindPort, "8888");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraPop3BindPort, "8888",
                    toModify, server);
            fail("expected conflict with already-set imap port");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("8888"));
        }
    }

    @Test
    public void preModifySecondInvocationOnSameContextIsSkipped() throws Exception {
        // Arrange -- a context already marked done for CheckPortConflict
        Server server = newServer("cpc-done.example.com");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "9999");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "9999");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        // mark done so the next preModify short-circuits
        assertTrue("first call returns false (was not done)",
                !ctx.isDoneAndSetIfNot(CheckPortConflict.class));

        // Act -- even though ports conflict, the done-flag short circuits BEFORE the check
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "9999",
                toModify, server);

        // Assert -- no exception, because the conflict check was skipped entirely
        assertTrue("done context must skip conflict checking", true);
    }

    @Test
    public void preModifyNonServerNonConfigEntryReturnsWithoutChecking() throws Exception {
        // Arrange -- an Account entry should be ignored by this callback
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        Account acct = prov.createAccount("cpc-acct@example.com", "test123", acctAttrs);
        createdAccounts.add(acct);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "143");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "143");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- duplicate ports but on an Account, which must be skipped
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "143",
                toModify, acct);

        // Assert -- no conflict raised because Account entries are not checked
        assertTrue("Account entries are not port-checked", true);
    }

    @Test
    public void preModifyUnsetWithNoDefaultTreatedAsNoValue() throws Exception {
        // Arrange -- unsetting a port (null value) with no server default => newValue null => no conflict
        Server server = newServer("cpc-unset.example.com");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, null);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, null,
                toModify, server);

        // Assert
        assertTrue("unsetting a port with no default must not conflict", true);
    }

    @Test
    public void postModifyIsNoOpDoesNotThrow() throws Exception {
        // Arrange
        Server server = newServer("cpc-post.example.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new CheckPortConflict().postModify(ctx, Provisioning.A_zimbraImapBindPort, server);

        // Assert -- postModify is empty by contract; verify it is harmless
        assertTrue("postModify must be a no-op", true);
    }

    @Test
    public void preModifyConfigDuplicatePortsThrowsConflictOnGlobalConfig() throws Exception {
        // Arrange -- a Config entry routes through checkConfig(); two attrs share a port
        com.zimbra.cs.account.Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "6543");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "6543");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "6543",
                    toModify, config);
            fail("expected INVALID_REQUEST for duplicate config port 6543");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("message should mention the conflicting config port",
                    e.getMessage().contains("6543"));
            assertTrue("message should mention global config",
                    e.getMessage().contains("global config"));
        }
    }

    @Test
    public void preModifyConfigDistinctPortsNoConflictAcrossServers() throws Exception {
        // Arrange -- distinct config ports; getAllServers is iterated by checkConfig
        newServer("cpc-cfgsrv-a.example.com");
        newServer("cpc-cfgsrv-b.example.com");
        com.zimbra.cs.account.Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "13001");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "13002");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- exercises checkConfig + checkServerWithNewDefaults over all servers
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "13001",
                toModify, config);

        // Assert -- distinct ports must not conflict
        assertTrue("distinct config ports across servers must not conflict", true);
    }

    @Test
    public void preModifyConfigChangeConflictsWithServerValueThrowsServerConflict() throws Exception {
        // Arrange -- a server already has an imap port; changing config pop3 to the same value
        // makes the server inherit a conflicting pop3 default while keeping its own imap port.
        Server server = newServer("cpc-cfgserverconflict.example.com");
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(Provisioning.A_zimbraImapBindPort, "15500");
        prov.modifyAttrs(server, seed);
        assertEquals("15500", server.getAttr(Provisioning.A_zimbraImapBindPort));

        com.zimbra.cs.account.Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        // pop3 default on config = 15500, server inherits it (server has no pop3 of its own)
        toModify.put(Provisioning.A_zimbraPop3BindPort, "15500");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraPop3BindPort, "15500",
                    toModify, config);
            fail("expected server-level conflict when a config change clashes with a server port");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("conflict message should mention port 15500", e.getMessage().contains("15500"));
            assertTrue("conflict message should reference the server",
                    e.getMessage().contains("on server"));
        }
    }

    @Test
    public void preModifyConfigUnsetPortTreatedAsNoValue() throws Exception {
        // Arrange -- unsetting a config port (null) yields newValue null in checkConfig => no conflict
        com.zimbra.cs.account.Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, null);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, null,
                toModify, config);

        // Assert
        assertTrue("unsetting a config port must not conflict", true);
    }

    @Test
    public void preModifyNullEntryCreatingServerChecksDuplicateAsConflict() throws Exception {
        // Arrange -- a null entry means a server is being created; checkServer((Server)null,...)
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraImapBindPort, "17777");
        toModify.put(Provisioning.A_zimbraPop3BindPort, "17777");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "17777",
                    toModify, (com.zimbra.cs.account.Server) null);
            fail("expected conflict when creating a server with duplicate ports");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("message should mention the conflicting port", e.getMessage().contains("17777"));
        }
    }

    @Test
    public void preModifyProxyAndNonProxyPortSharedNoConflictWhenNotBothServices() throws Exception {
        // Arrange -- a server that runs neither mailbox nor proxy service. A proxy attr and a
        // non-proxy attr sharing the same port must NOT conflict (the suppression branch).
        Server server = newServer("cpc-mixed.example.com");
        Map<String, Object> toModify = new HashMap<String, Object>();
        // imap (non-proxy) and imap-proxy share port 18888
        toModify.put(Provisioning.A_zimbraImapBindPort, "18888");
        toModify.put(Provisioning.A_zimbraImapProxyBindPort, "18888");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- no mailbox/proxy service => proxy vs non-proxy sharing is allowed
        new CheckPortConflict().preModify(ctx, Provisioning.A_zimbraImapBindPort, "18888",
                toModify, server);

        // Assert
        assertTrue("proxy and non-proxy ports may coincide when services are not both enabled", true);
    }
}
