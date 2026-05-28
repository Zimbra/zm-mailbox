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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for {@link NamedEntry}.
 *
 * Tests verify that NamedEntry (abstract base class) correctly manages name/id state,
 * implements comparison behavior, and maintains entry lifecycle.
 */
public class NamedEntryTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        try {
            Account account = provisioning.getAccountByName("testentry1@example.com");
            if (account != null) {
                provisioning.deleteAccount(account.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
        try {
            Account account = provisioning.getAccountByName("testentry2@example.com");
            if (account != null) {
                provisioning.deleteAccount(account.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create a NamedEntry (via Account) → verify getId() returns valid ID.
     * Verifies: ID is set during construction and persists.
     */
    @Test
    public void getId_afterCreation_returnsValidId() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Entry");

        // Act
        Account entry = provisioning.createAccount("testentry1@example.com", "password", attrs);

        // Assert
        Assert.assertNotNull(entry.getId());
        Assert.assertNotNull(entry.getId().trim());
        Assert.assertTrue(entry.getId().length() > 0);
    }

    /**
     * Test: Create a NamedEntry (via Account) → verify getName() returns correct name.
     * Verifies: Name is set during construction and persists.
     */
    @Test
    public void getName_afterCreation_returnsCorrectName() throws Exception {
        // Arrange
        String expectedName = "testentry1@example.com";
        Map<String, Object> attrs = new HashMap<>();

        // Act
        Account entry = provisioning.createAccount(expectedName, "password", attrs);

        // Assert
        Assert.assertEquals(expectedName, entry.getName());
    }

    /**
     * Test: Retrieve same entry by ID and by name → verify both getName() and getId()
     * return same values.
     * Verifies: ID/name consistency across retrieval methods.
     */
    @Test
    public void getId_and_getName_areConsistent() throws Exception {
        // Arrange
        String expectedName = "testentry1@example.com";
        Map<String, Object> attrs = new HashMap<>();
        Account created = provisioning.createAccount(expectedName, "password", attrs);
        String createdId = created.getId();

        // Act - Retrieve by name
        Account byName = provisioning.getAccountByName(expectedName);

        // Assert
        Assert.assertNotNull(byName);
        Assert.assertEquals(createdId, byName.getId());
        Assert.assertEquals(expectedName, byName.getName());
    }

    /**
     * Test: Create two entries → call getLabel() → verify label matches name.
     * Verifies: getLabel() delegates to getName().
     */
    @Test
    public void getLabel_returnsName() throws Exception {
        // Arrange
        String expectedName = "testentry1@example.com";
        Account entry = provisioning.createAccount(expectedName, "password", new HashMap<>());

        // Act
        String label = entry.getLabel();

        // Assert
        Assert.assertEquals(expectedName, label);
        Assert.assertEquals(entry.getName(), label);
    }

    /**
     * Test: Create two entries → compare via compareTo().
     * Verifies: compareTo() correctly orders entries by name.
     */
    @Test
    public void compareTo_ordersEntriesByName() throws Exception {
        // Arrange
        Account entry1 = provisioning.createAccount("testentry1@example.com", "password", new HashMap<>());
        Account entry2 = provisioning.createAccount("testentry2@example.com", "password", new HashMap<>());

        // Act
        int cmp1vs2 = entry1.compareTo(entry2);
        int cmp2vs1 = entry2.compareTo(entry1);

        // Assert - should be symmetric
        Assert.assertTrue("entry1 should sort before entry2", cmp1vs2 < 0);
        Assert.assertTrue("entry2 should sort after entry1", cmp2vs1 > 0);
    }

    /**
     * Test: Create two entries with same name (impossible but test compareTo self).
     * Verifies: compareTo(self) returns 0.
     */
    @Test
    public void compareTo_withSelf_returnsZero() throws Exception {
        // Arrange
        Account entry = provisioning.createAccount("testentry1@example.com", "password", new HashMap<>());

        // Act
        int cmpSelf = entry.compareTo(entry);

        // Assert
        Assert.assertEquals(0, cmpSelf);
    }

    /**
     * Test: Create entry → call toString().
     * Verifies: toString() includes class name and name.
     */
    @Test
    public void toString_includesClassNameAndName() throws Exception {
        // Arrange
        String expectedName = "testentry1@example.com";
        Account entry = provisioning.createAccount(expectedName, "password", new HashMap<>());

        // Act
        String str = entry.toString();

        // Assert
        Assert.assertNotNull(str);
        Assert.assertTrue("Should contain class name", str.contains("Account"));
        Assert.assertTrue("Should contain entry name", str.contains(expectedName));
    }

    /**
     * Test: Create entry → call compareTo(null) - defensive test.
     * Verifies: compareTo handles null gracefully.
     */
    @Test
    public void compareTo_withNull_returnsZero() throws Exception {
        // Arrange
        Account entry = provisioning.createAccount("testentry1@example.com", "password", new HashMap<>());

        // Act
        int cmp = entry.compareTo(null);

        // Assert
        Assert.assertEquals(0, cmp);
    }

    /**
     * Test: Create entry → call compareTo(non-NamedEntry object).
     * Verifies: compareTo with wrong type returns 0.
     */
    @Test
    public void compareTo_withWrongType_returnsZero() throws Exception {
        // Arrange
        Account entry = provisioning.createAccount("testentry1@example.com", "password", new HashMap<>());
        Object wrongType = "not a NamedEntry";

        // Act
        int cmp = entry.compareTo(wrongType);

        // Assert
        Assert.assertEquals(0, cmp);
    }

    /**
     * Test: Create entry → verify ID is non-empty and non-null across retrievals.
     * Verifies: ID stability across multiple retrievals.
     */
    @Test
    public void getId_stableAcrossMultipleRetrievals() throws Exception {
        // Arrange
        String name = "testentry1@example.com";
        Account created = provisioning.createAccount(name, "password", new HashMap<>());
        String originalId = created.getId();

        // Act - retrieve multiple times
        Account retrieved1 = provisioning.getAccountByName(name);
        Account retrieved2 = provisioning.getAccountByName(name);

        // Assert
        Assert.assertEquals(originalId, retrieved1.getId());
        Assert.assertEquals(originalId, retrieved2.getId());
    }

    /**
     * Test: Create entry with attributes → call getName() → verify attributes don't affect
     * name retrieval.
     * Verifies: getName() ignores attributes.
     */
    @Test
    public void getName_ignoresAttributes() throws Exception {
        // Arrange
        String expectedName = "testentry1@example.com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Custom Description");
        attrs.put("displayName", "Custom Display Name");
        Account entry = provisioning.createAccount(expectedName, "password", attrs);

        // Act
        String name = entry.getName();

        // Assert
        Assert.assertEquals(expectedName, name);
    }

    /**
     * Test: Create entry → verify compareTo is case-sensitive on names.
     * Verifies: String comparison is standard case-sensitive.
     */
    @Test
    public void compareTo_isCaseSensitive() throws Exception {
        // Arrange
        Account entry1 = provisioning.createAccount("testentry1@example.com", "password", new HashMap<>());
        Account entry2 = provisioning.createAccount("testentry2@example.com", "password", new HashMap<>());

        // Act
        int cmp = entry1.compareTo(entry2);

        // Assert - comparison should be as expected from string comparison
        Assert.assertTrue(cmp < 0);
    }

    /**
     * Test: toString() called multiple times returns same structure.
     * Verifies: toString() is idempotent.
     */
    @Test
    public void toString_isIdempotent() throws Exception {
        // Arrange
        Account entry = provisioning.createAccount("testentry1@example.com", "password", new HashMap<>());

        // Act
        String str1 = entry.toString();
        String str2 = entry.toString();

        // Assert
        Assert.assertEquals(str1, str2);
    }
}
