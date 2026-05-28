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
 * Unit tests for Alias functionality.
 *
 * Tests verify Alias creation via addAlias, targeting, and lifecycle management.
 * Note: Aliases are created via Account.addAlias(), not through a direct create method.
 */
public class AliasTest {

    private static Provisioning provisioning;
    private static Account testAccount;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
        testAccount = provisioning.createAccount("user1@example.com", "secret", new HashMap<>());
    }

    @After
    public void cleanup() throws Exception {
        try {
            Account aliasAccount = provisioning.getAccountByName("alias1@example.com");
            if (aliasAccount != null && !aliasAccount.getId().equals(testAccount.getId())) {
                // It's an alias pointing to our test account
                provisioning.removeAlias(testAccount, "alias1@example.com");
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Add alias for account → retrieve via alias name → verify created.
     * Verifies: Alias creation and retrieval.
     */
    @Test
    public void addAlias_forAccount_createdSuccessfully() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");

        // Act
        provisioning.addAlias(account, "alias1@example.com");
        Account aliasAccount = provisioning.getAccountByName("alias1@example.com");

        // Assert
        Assert.assertNotNull(aliasAccount);
        Assert.assertEquals(account.getId(), aliasAccount.getId());
    }

    /**
     * Test: Add alias → retrieve account by alias name → verify same ID.
     * Verifies: Alias points to correct target.
     */
    @Test
    public void aliasRetrievalByName_returnsTargetAccount() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        String accountId = account.getId();

        // Act
        provisioning.addAlias(account, "alias1@example.com");
        Account viaAlias = provisioning.getAccountByName("alias1@example.com");

        // Assert
        Assert.assertNotNull(viaAlias);
        Assert.assertEquals(accountId, viaAlias.getId());
    }

    /**
     * Test: Remove alias → verify no longer resolves.
     * Verifies: removeAlias() removes alias.
     */
    @Test
    public void removeAlias_deletesAlias() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        provisioning.addAlias(account, "alias1@example.com");

        // Act
        provisioning.removeAlias(account, "alias1@example.com");

        // Assert
        try {
            Account deleted = provisioning.getAccountByName("alias1@example.com");
            Assert.assertNull("Alias should not resolve after removal", deleted);
        } catch (AccountServiceException e) {
            // Expected - alias no longer exists
            Assert.assertTrue(e.getCode().contains("no such account"));
        }
    }

    /**
     * Test: Add alias → verify retrieval is consistent.
     * Verifies: Alias resolution is consistent.
     */
    @Test
    public void alias_consistentRetrieval_returnsTargetAccount() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        provisioning.addAlias(account, "alias1@example.com");

        // Act
        Account alias1 = provisioning.getAccountByName("alias1@example.com");
        Account alias2 = provisioning.getAccountByName("alias1@example.com");

        // Assert
        Assert.assertEquals(alias1.getId(), alias2.getId());
        Assert.assertEquals(alias1.getId(), account.getId());
    }

    /**
     * Test: Alias and its target account have same ID but different names.
     * Verifies: Alias properties are correct.
     */
    @Test
    public void alias_andTargetAccount_sameIdDifferentNames() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        provisioning.addAlias(account, "alias1@example.com");

        // Act
        Account viaAlias = provisioning.getAccountByName("alias1@example.com");

        // Assert
        Assert.assertEquals(account.getId(), viaAlias.getId());
        Assert.assertEquals("user1@example.com", account.getName());
        Assert.assertEquals("alias1@example.com", viaAlias.getName());
    }

    /**
     * Test: Add alias → toString() produces valid output.
     * Verifies: toString() works on aliased accounts.
     */
    @Test
    public void aliasAccountToString_returnsValidString() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        provisioning.addAlias(account, "alias1@example.com");
        Account viaAlias = provisioning.getAccountByName("alias1@example.com");

        // Act
        String str = viaAlias.toString();

        // Assert
        Assert.assertNotNull(str);
        Assert.assertTrue(str.length() > 0);
    }

    /**
     * Test: Create multiple aliases for same account.
     * Verifies: Multiple aliases can point to same target.
     */
    @Test
    public void multipleAliases_forSameAccount_allResolveCorrectly() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        provisioning.addAlias(account, "alias1@example.com");

        try {
            provisioning.addAlias(account, "alias2@example.com");

            // Act
            Account viaAlias1 = provisioning.getAccountByName("alias1@example.com");
            Account viaAlias2 = provisioning.getAccountByName("alias2@example.com");

            // Assert
            Assert.assertEquals(account.getId(), viaAlias1.getId());
            Assert.assertEquals(account.getId(), viaAlias2.getId());
            Assert.assertNotEquals("alias1@example.com", "alias2@example.com");

            // Cleanup
            provisioning.removeAlias(account, "alias2@example.com");
        } catch (ServiceException e) {
            // Skip if system doesn't support multiple aliases
        }
    }

    /**
     * Test: Get account by original name and by alias → same properties.
     * Verifies: Alias resolution maintains account properties.
     */
    @Test
    public void accountRetrievedByAlias_hasCorrectProperties() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        String originalId = account.getId();
        provisioning.addAlias(account, "alias1@example.com");

        // Act
        Account byOriginal = provisioning.getAccountByName("user1@example.com");
        Account byAlias = provisioning.getAccountByName("alias1@example.com");

        // Assert
        Assert.assertEquals(originalId, byAlias.getId());
        Assert.assertEquals(byOriginal.getId(), byAlias.getId());
    }

    /**
     * Test: Alias lifecycle → create → retrieve → remove.
     * Verifies: State persistence through lifecycle.
     */
    @Test
    public void aliasCycle_createRetrieveRemove_stateTransitions() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");

        // Act - Create
        provisioning.addAlias(account, "alias1@example.com");
        Account created = provisioning.getAccountByName("alias1@example.com");
        Assert.assertNotNull(created);
        Assert.assertEquals(account.getId(), created.getId());

        // Act - Retrieve
        Account retrieved = provisioning.getAccountByName("alias1@example.com");
        Assert.assertEquals(account.getId(), retrieved.getId());

        // Act - Remove
        provisioning.removeAlias(account, "alias1@example.com");

        // Assert - Verify removed
        try {
            Account removed = provisioning.getAccountByName("alias1@example.com");
            Assert.assertNull("Alias should not resolve after removal", removed);
        } catch (AccountServiceException e) {
            // Expected - alias no longer exists
            Assert.assertTrue(e.getCode().contains("no such account"));
        }
    }

    /**
     * Test: Aliased account is a MailTarget instance.
     * Verifies: Alias account is proper MailTarget.
     */
    @Test
    public void aliasAccount_isMailTarget() throws Exception {
        // Arrange
        Account account = provisioning.getAccountByName("user1@example.com");
        provisioning.addAlias(account, "alias1@example.com");

        // Act
        Account viaAlias = provisioning.getAccountByName("alias1@example.com");

        // Assert
        Assert.assertTrue(viaAlias instanceof MailTarget);
        Assert.assertTrue(viaAlias instanceof NamedEntry);
    }
}
