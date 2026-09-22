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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Functional tests for {@link MailTarget}, the abstract mail-addressable base of
 * {@link Account}. Exercised through a real Account from the in-memory
 * {@link MockProvisioning} harness: domain parsing, unicode name derivation, and
 * the cached domain-id lookup for both domain-bearing and admin (no-domain) targets.
 */
public class MailTargetTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        if (prov.get(DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
        // Overwrite-on-duplicate contract: safe to recreate per-method.
        prov.createAccount("user@example.com", "secret", new HashMap<String, Object>());
    }

    @After
    public void cleanUp() throws Exception {
        Account admin = prov.get(AccountBy.name, "admin");
        if (admin != null) {
            prov.deleteAccount(admin.getId());
        }
    }

    @Test
    public void getDomainNameEmailWithDomainReturnsDomainPart() throws Exception {
        // Arrange
        MailTarget target = prov.get(AccountBy.name, "user@example.com");

        // Act / Assert
        assertEquals("example.com", target.getDomainName());
    }

    @Test
    public void getDomainNameNameWithoutAtReturnsNull() throws Exception {
        // Arrange — admin-style account with no domain part.
        prov.createAccount("admin", "secret", new HashMap<String, Object>());
        MailTarget target = prov.get(AccountBy.name, "admin");

        // Act / Assert — no '@' means no domain.
        assertNull(target.getDomainName());
    }

    @Test
    public void getUnicodeNameEmailWithDomainCombinesLocalAndUnicodeDomain() throws Exception {
        // Arrange
        MailTarget target = prov.get(AccountBy.name, "user@example.com");

        // Act / Assert — ASCII domain round-trips unchanged.
        assertEquals("user@example.com", target.getUnicodeName());
    }

    @Test
    public void getUnicodeNameNameWithoutAtReturnsRawName() throws Exception {
        // Arrange
        prov.createAccount("admin", "secret", new HashMap<String, Object>());
        MailTarget target = prov.get(AccountBy.name, "admin");

        // Act / Assert — no domain branch keeps the name verbatim.
        assertEquals("admin", target.getUnicodeName());
    }

    @Test
    public void getUnicodeDomainNameEmailWithDomainReturnsUnicodeDomain() throws Exception {
        // Arrange
        MailTarget target = prov.get(AccountBy.name, "user@example.com");

        // Act / Assert
        assertEquals("example.com", target.getUnicodeDomainName());
    }

    @Test
    public void getUnicodeDomainNameNameWithoutAtReturnsNull() throws Exception {
        // Arrange
        prov.createAccount("admin", "secret", new HashMap<String, Object>());
        MailTarget target = prov.get(AccountBy.name, "admin");

        // Act / Assert — domain field never set for domain-less names.
        assertNull(target.getUnicodeDomainName());
    }

    @Test
    public void getDomainIdExistingDomainReturnsDomainIdAndCaches() throws Exception {
        // Arrange
        Domain domain = prov.get(DomainBy.name, "example.com");
        MailTarget target = prov.get(AccountBy.name, "user@example.com");

        // Act — first call resolves and caches the id.
        String first = target.getDomainId();
        String second = target.getDomainId();

        // Assert — id matches the real domain and is stable across calls (cache hit).
        assertNotNull(first);
        assertEquals(domain.getId(), first);
        assertEquals(first, second);
    }

    @Test
    public void getDomainIdNameWithoutDomainReturnsNull() throws Exception {
        // Arrange — admin account has no domain to resolve.
        prov.createAccount("admin", "secret", new HashMap<String, Object>());
        MailTarget target = prov.get(AccountBy.name, "admin");

        // Act / Assert — null domain name short-circuits to null id.
        assertNull(target.getDomainId());
    }

    @Test
    public void getDomainIdDomainNotProvisionedReturnsNull() throws Exception {
        // Arrange — account whose domain was never created.
        prov.createAccount("ghost@nowhere.test", "secret", new HashMap<String, Object>());
        MailTarget target = prov.get(AccountBy.name, "ghost@nowhere.test");

        // Act / Assert — unresolved domain caches the sentinel and surfaces as null.
        assertNull(target.getDomainId());

        // Cleanup the extra account.
        prov.deleteAccount(target.getId());
    }
}
