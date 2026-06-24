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

package com.zimbra.cs.account;

import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link Server}, exercising real {@link Server} domain objects created
 * through the in-memory MockProvisioning harness.
 */
public class ServerFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Server newServer(Map<String, Object> attrs) {
        return new Server("srv.zimbra.com", "server-id-1", attrs, new HashMap<String, Object>(), prov);
    }

    @Test
    public void getEntryTypeAnyServerReturnsServer() throws Exception {
        // Arrange
        Server server = newServer(new HashMap<String, Object>());

        // Act
        EntryType type = server.getEntryType();

        // Assert
        assertEquals(EntryType.SERVER, type);
    }

    @Test
    public void constructorWithoutAlwaysOnClusterInitializesWithoutOverrides() throws Exception {
        // Arrange -- Server.getId() (via ZAttrServer) reads the zimbraId attribute, not the
        // constructor id parameter, so the id must be supplied as an attribute.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "srv.zimbra.com");
        attrs.put(Provisioning.A_zimbraId, "server-id-1");

        // Act
        Server server = newServer(attrs);

        // Assert
        assertNotNull(server);
        assertEquals("srv.zimbra.com", server.getName());
        assertEquals("server-id-1", server.getId());
        assertEquals("srv.zimbra.com", server.getAttr(Provisioning.A_zimbraServiceHostname, null));
    }

    @Test
    public void mailTransportMatchesNullTransportReturnsTrue() throws Exception {
        // Arrange
        Server server = newServer(new HashMap<String, Object>());

        // Act / Assert
        assertTrue(server.mailTransportMatches(null));
    }

    @Test
    public void mailTransportMatchesLmtpMatchingHostnameReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.zimbra.com");
        Server server = newServer(attrs);

        // Act
        boolean matches = server.mailTransportMatches("lmtp:mail.zimbra.com:7025");

        // Assert
        assertTrue(matches);
    }

    @Test
    public void mailTransportMatchesDifferentHostnameReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.zimbra.com");
        Server server = newServer(attrs);

        // Act
        boolean matches = server.mailTransportMatches("lmtp:other.zimbra.com:7025");

        // Assert
        assertFalse(matches);
    }

    @Test
    public void mailTransportMatchesNonLmtpProtocolReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.zimbra.com");
        Server server = newServer(attrs);

        // Act
        boolean matches = server.mailTransportMatches("smtp:mail.zimbra.com:25");

        // Assert
        assertFalse(matches);
    }

    @Test
    public void mailTransportMatchesMalformedTransportReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceHostname, "mail.zimbra.com");
        Server server = newServer(attrs);

        // Act -- only two parts, not three
        boolean matches = server.mailTransportMatches("lmtp:mail.zimbra.com");

        // Assert
        assertFalse(matches);
    }

    @Test
    public void hasMailboxServiceServiceEnabledReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasMailboxService());
    }

    @Test
    public void hasMailboxServiceServiceNotEnabledReturnsFalse() throws Exception {
        // Arrange
        Server server = newServer(new HashMap<String, Object>());

        // Act / Assert
        assertFalse(server.hasMailboxService());
    }

    @Test
    public void hasProxyServiceServiceEnabledReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_PROXY);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasProxyService());
    }

    @Test
    public void hasWebClientServicePreVersionServerWithMailboxReturnsTrue() throws Exception {
        // Arrange -- no zimbraServerVersion (pre 8.5), mailbox service stands in
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasWebClientService());
    }

    @Test
    public void hasWebClientServiceVersionedServerWithWebclientReturnsTrue() throws Exception {
        // Arrange -- versioned server checks the explicit webclient service
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServerVersion, "8.5.0");
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_WEBCLIENT);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasWebClientService());
    }

    @Test
    public void hasWebClientServiceVersionedServerWithoutWebclientReturnsFalse() throws Exception {
        // Arrange -- versioned server with mailbox only does NOT imply webclient
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServerVersion, "8.5.0");
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server = newServer(attrs);

        // Act / Assert
        assertFalse(server.hasWebClientService());
    }

    @Test
    public void hasAdminClientServiceVersionedServerWithAdminclientReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServerVersion, "8.5.0");
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_ADMINCLIENT);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasAdminClientService());
    }

    @Test
    public void hasMailClientServicePreVersionServerWithMailboxReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_MAILBOX);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasMailClientService());
    }

    @Test
    public void hasZimletServiceVersionedServerWithZimletReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServerVersion, "8.5.0");
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_ZIMLET);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasZimletService());
    }

    @Test
    public void hasOnlyOfficeServiceServiceEnabledReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraServiceEnabled, Provisioning.SERVICE_ONLYOFFICE);
        Server server = newServer(attrs);

        // Act / Assert
        assertTrue(server.hasOnlyOfficeService());
    }

    @Test
    public void hasOnlyOfficeServiceServiceNotEnabledReturnsFalse() throws Exception {
        // Arrange
        Server server = newServer(new HashMap<String, Object>());

        // Act / Assert
        assertFalse(server.hasOnlyOfficeService());
    }

    @Test
    public void isLocalServerDifferentIdFromLocalReturnsFalse() throws Exception {
        // Arrange -- our server id differs from the harness local server. Server.getId() reads
        // the zimbraId attribute, so it is supplied through attrs.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "server-id-1");
        Server server = newServer(attrs);
        Server local = prov.getLocalServer();

        // Act
        boolean isLocal = server.isLocalServer();

        // Assert -- distinct ids means not the local server
        assertEquals("server-id-1", server.getId());
        assertFalse("distinct server id must not match local", server.getId().equals(local.getId()));
        assertFalse(isLocal);
    }

    @Test
    public void isLocalServerSameIdAsLocalReturnsTrue() throws Exception {
        // Arrange -- build a server reusing the local server's id. Server.getId() reads the
        // zimbraId attribute, so the local id must be placed in attrs (the constructor id
        // parameter is ignored by getId()).
        Server local = prov.getLocalServer();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, local.getId());
        Server sameAsLocal = new Server("localish", local.getId(),
                attrs, new HashMap<String, Object>(), prov);

        // Act / Assert
        assertEquals(local.getId(), sameAsLocal.getId());
        assertTrue(sameAsLocal.isLocalServer());
    }
}
