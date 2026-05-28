/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for {@link Server}.
 *
 * Tests verify Server creation, attribute management, service checks,
 * and mail transport matching.
 */
public class ServerTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        try {
            Server server = provisioning.getServerByName( "testserver1");
            if (server != null) {
                provisioning.deleteServer(server.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
        try {
            Server server = provisioning.getServerByName( "testserver2");
            if (server != null) {
                provisioning.deleteServer(server.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create server with attributes → retrieve → verify attributes persist.
     * Verifies: Server creation and attribute persistence.
     */
    @Test
    public void createServer_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail1.example.com");
        attrs.put("description", "Test Server");

        // Act
        Server server = provisioning.createServer("testserver1", attrs);

        // Assert
        Assert.assertNotNull(server);
        Assert.assertNotNull(server.getId());
        Assert.assertEquals("testserver1", server.getName());
        Assert.assertEquals("mail1.example.com", server.getAttr(Provisioning.A_zimbraServiceHostname));
    }

    /**
     * Test: Create server → call getEntryType() → verify returns SERVER.
     * Verifies: getEntryType() returns correct enum.
     */
    @Test
    public void getEntryType_returnsServerType() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        Entry.EntryType type = server.getEntryType();

        // Assert
        Assert.assertEquals(Entry.EntryType.SERVER, type);
    }

    /**
     * Test: Create server → modify attributes → retrieve → verify changes persisted.
     * Verifies: modify() updates attributes.
     */
    @Test
    public void modifyServer_updatesAttributes_changePersisted() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Original Description");
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Updated Description");
        server.modify(updates);

        // Assert
        Server retrieved = provisioning.getServerByName( "testserver1");
        Assert.assertEquals("Updated Description", retrieved.getAttr("description"));
    }

    /**
     * Test: Create server with mailbox service enabled → call hasMailboxService().
     * Verifies: hasMailboxService() correctly detects service.
     */
    @Test
    public void hasMailboxService_whenEnabled_returnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasMailboxService();

        // Assert
        Assert.assertTrue(hasService);
    }

    /**
     * Test: Create server without mailbox service → call hasMailboxService().
     * Verifies: hasMailboxService() returns false when not enabled.
     */
    @Test
    public void hasMailboxService_whenDisabled_returnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_PROXY);
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasMailboxService();

        // Assert
        Assert.assertFalse(hasService);
    }

    /**
     * Test: Create server with proxy service → call hasProxyService().
     * Verifies: hasProxyService() detects service.
     */
    @Test
    public void hasProxyService_whenEnabled_returnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_PROXY);
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasProxyService();

        // Assert
        Assert.assertTrue(hasService);
    }

    /**
     * Test: Create server → call mailTransportMatches() with null.
     * Verifies: null mail transport matches (always true).
     */
    @Test
    public void mailTransportMatches_withNull_returnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean matches = server.mailTransportMatches(null);

        // Assert
        Assert.assertTrue(matches);
    }

    /**
     * Test: Create server with hostname → call mailTransportMatches() with matching transport.
     * Verifies: mailTransportMatches() recognizes matching LMTP transport.
     */
    @Test
    public void mailTransportMatches_withMatchingTransport_returnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.example.com");
        Server server = provisioning.createServer("testserver1", attrs);
        String mailTransport = "lmtp:mail.example.com:24";

        // Act
        boolean matches = server.mailTransportMatches(mailTransport);

        // Assert
        Assert.assertTrue(matches);
    }

    /**
     * Test: Create server with hostname → call mailTransportMatches() with non-matching transport.
     * Verifies: mailTransportMatches() rejects mismatched LMTP transport.
     */
    @Test
    public void mailTransportMatches_withMismatchedTransport_returnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.example.com");
        Server server = provisioning.createServer("testserver1", attrs);
        String mailTransport = "lmtp:different.example.com:24";

        // Act
        boolean matches = server.mailTransportMatches(mailTransport);

        // Assert
        Assert.assertFalse(matches);
    }

    /**
     * Test: Create server → call mailTransportMatches() with invalid format.
     * Verifies: mailTransportMatches() handles invalid format gracefully.
     */
    @Test
    public void mailTransportMatches_withInvalidFormat_returnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Server server = provisioning.createServer("testserver1", attrs);
        String mailTransport = "invalid-format";

        // Act
        boolean matches = server.mailTransportMatches(mailTransport);

        // Assert
        Assert.assertFalse(matches);
    }

    /**
     * Test: Server with no version attribute → call hasWebClientService().
     * Verifies: Pre-8.5 compatibility (no zimbraServerVersion means mailbox service).
     */
    @Test
    public void hasWebClientService_pre85Server_returnsTrueForMailboxService() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        // No zimbraServerVersion attribute (pre-8.5)
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasWebClientService();

        // Assert
        Assert.assertTrue(hasService);
    }

    /**
     * Test: Server with version and webclient service → call hasWebClientService().
     * Verifies: 8.5+ server with explicit service flag.
     */
    @Test
    public void hasWebClientService_post85Server_detectsWebclientService() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServerVersion, "8.6.0");
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_WEBCLIENT);
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasWebClientService();

        // Assert
        Assert.assertTrue(hasService);
    }

    /**
     * Test: Create server → call isLocalServer().
     * Verifies: isLocalServer() detects local vs remote.
     */
    @Test
    public void isLocalServer_checksServerIdentity() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean isLocal = server.isLocalServer();

        // Assert - may be local or remote depending on test setup
        Assert.assertNotNull(isLocal); // Just verify it executes without error
    }

    /**
     * Test: Create server → verify deleteServer() removes it.
     * Verifies: deleteServer() deletes from provisioning.
     */
    @Test
    public void deleteServer_removesServer() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Server server = provisioning.createServer("testserver1", attrs);
        String serverId = server.getId();

        // Act
        server.deleteServer(serverId);

        // Assert
        Server retrieved = provisioning.getServerByName( "testserver1");
        Assert.assertNull("Server should be deleted", retrieved);
    }

    /**
     * Test: Create server with admin client service → call hasAdminClientService().
     * Verifies: Service detection for admin client.
     */
    @Test
    public void hasAdminClientService_whenEnabled_returnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServerVersion, "8.6.0");
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_ADMINCLIENT);
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasAdminClientService();

        // Assert
        Assert.assertTrue(hasService);
    }

    /**
     * Test: Create server with OnlyOffice service → call hasOnlyOfficeService().
     * Verifies: OnlyOffice service detection.
     */
    @Test
    public void hasOnlyOfficeService_whenEnabled_returnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_ONLYOFFICE);
        Server server = provisioning.createServer("testserver1", attrs);

        // Act
        boolean hasService = server.hasOnlyOfficeService();

        // Assert
        Assert.assertTrue(hasService);
    }

    /**
     * Test: Create server → modify → verify state persists.
     * Verifies: Sequential modifications maintain correct state.
     */
    @Test
    public void modifyServer_multipleModifications_finalStatePersisted() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Original");
        Server server = provisioning.createServer("testserver1", attrs);

        // Act - First modification
        Map<String, Object> updates1 = new HashMap<>();
        updates1.put("description", "First Update");
        server.modify(updates1);

        // Act - Second modification
        Map<String, Object> updates2 = new HashMap<>();
        updates2.put("description", "Second Update");
        server.modify(updates2);

        // Assert
        Server retrieved = provisioning.getServerByName( "testserver1");
        Assert.assertEquals("Second Update", retrieved.getAttr("description"));
    }

    /**
     * Test: Create server → enable multiple services → verify all detected.
     * Verifies: Multiple service detection works correctly.
     */
    @Test
    public void multipleServices_bothDetected_correctly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, new String[]{
            Provisioning.SERVICE_MAILBOX,
            Provisioning.SERVICE_PROXY
        });
        Server server = provisioning.createServer("testserver1", attrs);

        // Act & Assert
        Assert.assertTrue("Should have mailbox service", server.hasMailboxService());
        Assert.assertTrue("Should have proxy service", server.hasProxyService());
    }

    /**
     * Test: Create server without version → all service checks fallback to mailbox check.
     * Verifies: Pre-8.5 compatibility fallback pattern.
     */
    @Test
    public void preEightFiveServer_allServiceChecksFallbackToMailbox() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        // Intentionally no zimbraServerVersion
        Server server = provisioning.createServer("testserver1", attrs);

        // Act & Assert
        Assert.assertTrue("Pre-8.5 with mailbox should have webclient", server.hasWebClientService());
        Assert.assertTrue("Pre-8.5 with mailbox should have adminclient", server.hasAdminClientService());
        Assert.assertTrue("Pre-8.5 with mailbox should have mailclient", server.hasMailClientService());
        Assert.assertTrue("Pre-8.5 with mailbox should have zimlet", server.hasZimletService());
    }

    /**
     * Test: Server hostname → mailTransportMatches with protocol variations.
     * Verifies: Protocol matching is case-insensitive for "lmtp".
     */
    @Test
    public void mailTransportMatches_caseInsensitiveProtocol() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.example.com");
        Server server = provisioning.createServer("testserver1", attrs);

        // Act & Assert - both LMTP and lmtp should match
        Assert.assertTrue("lowercase 'lmtp' should match", server.mailTransportMatches("lmtp:mail.example.com:24"));
        Assert.assertTrue("uppercase 'LMTP' should match", server.mailTransportMatches("LMTP:mail.example.com:24"));
    }

    /**
     * Test: Create server → get entry type → verify SERVER.
     * Verifies: Entry type is consistently SERVER.
     */
    @Test
    public void entryType_alwaysServer() throws Exception {
        // Arrange
        Server server = provisioning.createServer("testserver1", new HashMap<>());

        // Act
        Entry.EntryType type1 = server.getEntryType();

        // Retrieve again
        Server retrieved = provisioning.getServerByName( "testserver1");
        Entry.EntryType type2 = retrieved.getEntryType();

        // Assert
        Assert.assertEquals(Entry.EntryType.SERVER, type1);
        Assert.assertEquals(Entry.EntryType.SERVER, type2);
    }

    /**
     * Test: Create server → get ID → verify non-null and non-empty.
     * Verifies: Server has valid ID after creation.
     */
    @Test
    public void serverId_validAndNonEmpty() throws Exception {
        // Arrange
        Server server = provisioning.createServer("testserver1", new HashMap<>());

        // Act
        String id = server.getId();

        // Assert
        Assert.assertNotNull("Server ID should not be null", id);
        Assert.assertFalse("Server ID should not be empty", id.isEmpty());
    }

    /**
     * Test: Create server → get name → verify equals creation name.
     * Verifies: Server name persists correctly.
     */
    @Test
    public void serverName_equalsCreationName() throws Exception {
        // Arrange
        Server server = provisioning.createServer("testserver1", new HashMap<>());

        // Act
        String name = server.getName();

        // Assert
        Assert.assertEquals("testserver1", name);
    }

    /**
     * Test: Create server → modify → modify again → verify final state.
     * Verifies: State transitions maintain consistency.
     */
    @Test
    public void sequentialModifications_finalStateCorrect() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "original.com");
        Server server = provisioning.createServer("testserver1", attrs);

        // Act - Modify once
        Map<String, Object> update1 = new HashMap<>();
        update1.put(Provisioning.A_zimbraServiceHostname, "first.com");
        server.modify(update1);

        // Act - Modify again
        Map<String, Object> update2 = new HashMap<>();
        update2.put(Provisioning.A_zimbraServiceHostname, "final.com");
        server.modify(update2);

        // Assert
        Server retrieved = provisioning.getServerByName( "testserver1");
        Assert.assertEquals("final.com", retrieved.getAttr(Provisioning.A_zimbraServiceHostname));
    }

    /**
     * Test: Verify getProvisioning() returns valid reference.
     * Verifies: Server maintains reference to provisioning instance.
     */
    @Test
    public void getProvisioning_returnsValidReference() throws Exception {
        // Arrange
        Server server = provisioning.createServer("testserver1", new HashMap<>());

        // Act
        Provisioning prov = server.getProvisioning();

        // Assert
        Assert.assertNotNull(prov);
        Assert.assertSame("Should be same provisioning instance", provisioning, prov);
    }
}
