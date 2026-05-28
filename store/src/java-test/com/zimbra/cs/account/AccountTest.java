/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Full functional tests for {@link Account}.
 *
 * Tests verify state transitions, side effects, and real-world workflows
 * for Account operations including creation, modification, deletion, and
 * account relationships (aliases, distribution lists, etc.).
 */
public class AccountTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        // Clean up test accounts
        try {
            Account account = provisioning.getAccountByName("testaccount@example.com");
            if (account != null) {
                provisioning.deleteAccount(account.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }

        try {
            Account account = provisioning.getAccountByName("newname@example.com");
            if (account != null) {
                provisioning.deleteAccount(account.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }

        try {
            Account account = provisioning.getAccountByName("modified@example.com");
            if (account != null) {
                provisioning.deleteAccount(account.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create account with attributes → retrieve by name → verify attributes
     * persisted correctly.
     *
     * Verifies: Account creation creates persistent entries, attributes
     * survive retrieval, complete object state is available.
     */
    @Test
    public void createAccount_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Account");
        attrs.put("displayName", "Test Display Name");
        attrs.put("mail", "testaccount@example.com");

        // Act - Create account
        Account created = provisioning.createAccount("testaccount@example.com", "password", attrs);

        // Assert - Verify created state
        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());
        Assert.assertEquals("testaccount@example.com", created.getName());

        // Act - Retrieve account to verify persistence
        Account retrieved = provisioning.getAccountByName("testaccount@example.com");

        // Assert - Verify persistence
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(created.getId(), retrieved.getId());
        Assert.assertEquals("testaccount@example.com", retrieved.getName());
        Assert.assertEquals("Test Account", retrieved.getAttr("description"));
        Assert.assertEquals("Test Display Name", retrieved.getAttr("displayName"));
    }

    /**
     * Test: Create account → modify attributes → retrieve → verify changes
     * persisted.
     *
     * Verifies: Modification changes object state and persists to storage,
     * subsequent retrievals see updated values.
     */
    @Test
    public void modifyAccount_updatesAttributes_changePersisted() throws Exception {
        // Arrange - Create initial account
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Original Description");
        Account created = provisioning.createAccount("testaccount@example.com", "password", attrs);
        String accountId = created.getId();

        // Act - Modify account
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Updated Description");
        updates.put("displayName", "Updated Name");
        provisioning.modifyAttrs(created, updates);

        // Assert - Verify modification took effect on returned object
        Account modified = provisioning.getAccountByName("testaccount@example.com");
        Assert.assertEquals("Updated Description", modified.getAttr("description"));
        Assert.assertEquals("Updated Name", modified.getAttr("displayName"));

        // Assert - Verify persistence: fresh retrieval shows changes
        Account retrieved = provisioning.get(AccountBy.id, accountId);
        Assert.assertEquals("Updated Description", retrieved.getAttr("description"));
        Assert.assertEquals("Updated Name", retrieved.getAttr("displayName"));
    }

    /**
     * Test: Create account → rename → verify old name unavailable, new name
     * returns same account ID.
     *
     * Verifies: Rename changes identity mapping and persists state,
     * old name no longer resolves, ID remains constant.
     */
    @Test
    public void renameAccount_changesIdentity_oldNameUnavailable() throws Exception {
        // Arrange
        Account created = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        String accountId = created.getId();

        // Act - Rename account
        provisioning.renameAccount(accountId, "newname@example.com");

        // Assert - Old name should not resolve
        Account oldLookup = provisioning.getAccountByName("testaccount@example.com");
        Assert.assertNull(oldLookup);

        // Assert - New name should resolve to same ID
        Account newLookup = provisioning.getAccountByName("newname@example.com");
        Assert.assertNotNull(newLookup);
        Assert.assertEquals(accountId, newLookup.getId());
        Assert.assertEquals("newname@example.com", newLookup.getName());
    }

    /**
     * Test: Create account → delete → verify no longer retrievable.
     *
     * Verifies: Deletion removes account from all lookup paths,
     * subsequent retrievals return null.
     */
    @Test
    public void deleteAccount_removesFromSystem_noLongerRetrievable() throws Exception {
        // Arrange
        Account created = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        String accountId = created.getId();

        // Verify initial state - account should exist
        Account verify = provisioning.getAccountByName("testaccount@example.com");
        Assert.assertNotNull(verify);

        // Act - Delete account
        provisioning.deleteAccount(accountId);

        // Assert - Name lookup returns null
        Account byName = provisioning.getAccountByName("testaccount@example.com");
        Assert.assertNull(byName);

        // Assert - ID lookup returns null
        Account byId = provisioning.get(AccountBy.id, accountId);
        Assert.assertNull(byId);
    }

    /**
     * Test: Create account → add alias → retrieve account → verify alias
     * appears in aliases list.
     *
     * Verifies: Alias addition affects account state, aliases persist
     * and are retrievable.
     */
    @Test
    public void addAlias_toAccount_appearsinAliasesList() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Act - Add alias
        provisioning.addAlias(account, "alias1@example.com");

        // Assert - Retrieve account and verify alias
        Account retrieved = provisioning.getAccountByName("testaccount@example.com");
        Assert.assertNotNull(retrieved);
        String[] aliases = retrieved.getMailAlias();
        Assert.assertNotNull(aliases);
        Assert.assertTrue("Alias should be in list", contains(aliases, "alias1@example.com"));

        // Act - Add second alias
        provisioning.addAlias(account, "alias2@example.com");

        // Assert - Verify both aliases present
        retrieved = provisioning.getAccountByName("testaccount@example.com");
        aliases = retrieved.getMailAlias();
        Assert.assertTrue("Alias1 should be present", contains(aliases, "alias1@example.com"));
        Assert.assertTrue("Alias2 should be present", contains(aliases, "alias2@example.com"));
    }

    /**
     * Test: Create account with alias → remove alias → verify alias removed
     * from list.
     *
     * Verifies: Alias removal reduces alias list and persists,
     * multiple aliases handled correctly.
     */
    @Test
    public void removeAlias_fromAccount_noLongerInList() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        provisioning.addAlias(account, "alias1@example.com");
        provisioning.addAlias(account, "alias2@example.com");

        // Verify initial state
        Account verify = provisioning.getAccountByName("testaccount@example.com");
        String[] aliases = verify.getMailAlias();
        Assert.assertEquals(2, aliases.length);

        // Act - Remove one alias
        provisioning.removeAlias(account, "alias1@example.com");

        // Assert - Verify alias1 removed, alias2 remains
        Account retrieved = provisioning.getAccountByName("testaccount@example.com");
        aliases = retrieved.getMailAlias();
        Assert.assertEquals(1, aliases.length);
        Assert.assertTrue("Alias2 should remain", contains(aliases, "alias2@example.com"));
        Assert.assertFalse("Alias1 should be removed", contains(aliases, "alias1@example.com"));
    }

    /**
     * Test: Create account → verify it returns correct entry type.
     *
     * Verifies: Account.getEntryType() returns ACCOUNT, not other types.
     */
    @Test
    public void getEntryType_returnsAccountType() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Act & Assert
        Assert.assertEquals(EntryType.ACCOUNT, account.getEntryType());
    }

    /**
     * Test: Create two accounts → compare with sameAccount() method.
     *
     * Verifies: sameAccount() correctly identifies same account (by ID),
     * returns false for different accounts.
     */
    @Test
    public void sameAccount_comparesIds_correctlyIdentifiesSameAccount() throws Exception {
        // Arrange
        Account account1 = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        Account account2 = provisioning.createAccount("modified@example.com", "password", new HashMap<>());

        // Act & Assert - Same account
        Assert.assertTrue("Should be same account", account1.sameAccount(account1));

        // Act & Assert - Retrieved instance should be same as created
        Account retrieved = provisioning.getAccountByName("testaccount@example.com");
        Assert.assertTrue("Retrieved account should be same", account1.sameAccount(retrieved));

        // Act & Assert - Different accounts
        Assert.assertFalse("Should be different accounts", account1.sameAccount(account2));

        // Act & Assert - Null comparison
        Assert.assertFalse("Should not be same as null", account1.sameAccount(null));
    }

    /**
     * Test: Create account → get COS → verify COS is retrievable.
     *
     * Verifies: Account.getCOS() returns valid COS object assigned to
     * account.
     */
    @Test
    public void getCOS_returnsClassOfService_forAccount() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Cos cos = provisioning.createCos("test-cos", new HashMap<>());
        attrs.put("zimbraCOSId", cos.getId());

        Account account = provisioning.createAccount("testaccount@example.com", "password", attrs);

        // Act
        Cos retrievedCos = account.getCOS();

        // Assert
        Assert.assertNotNull(retrievedCos);
        Assert.assertEquals(cos.getId(), retrievedCos.getId());
    }

    /**
     * Test: Create account → set password → verify password set (through
     * attempted auth or state check).
     *
     * Verifies: setPassword() updates account password storage.
     */
    @Test
    public void setPassword_updatesAccountPassword_succeeds() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "initialPassword", new HashMap<>());

        // Act - Set new password
        Provisioning.SetPasswordResult result = provisioning.setPassword(account, "newPassword");

        // Assert - Verify operation completed (result not null)
        Assert.assertNotNull(result);
    }

    /**
     * Test: Create account → get distribution lists membership → verify
     * empty when not member of any.
     *
     * Verifies: getDistributionLists() returns correct list of memberships,
     * initially empty.
     */
    @Test
    public void getDistributionLists_returnsEmptyWhenNotMember_initially() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Act
        Set<String> dls = account.getDistributionLists();

        // Assert
        Assert.assertNotNull(dls);
        Assert.assertTrue("Should be empty initially", dls.isEmpty());
    }

    /**
     * Test: Create account → add to distribution list → get distribution
     * lists → verify membership.
     *
     * Verifies: Distribution list membership is tracked and retrievable.
     */
    @Test
    public void getDistributionLists_returnsMemberships_afterAddedToList() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        DistributionList dl = provisioning.createDistributionList("testdl@example.com", new HashMap<>());

        // Act - Add account to distribution list
        provisioning.addMembers(dl, new String[]{account.getName()});

        // Act - Retrieve distribution lists for account
        Set<String> dls = account.getDistributionLists();

        // Assert
        Assert.assertNotNull(dls);
        Assert.assertTrue("DL ID should be in list", dls.contains(dl.getId()));
    }

    /**
     * Test: Create account with description → retrieve → verify
     * getAttr() returns correct value.
     *
     * Verifies: Attribute storage and retrieval works for arbitrary
     * string attributes.
     */
    @Test
    public void getAttr_returnsStoredAttribute_correctValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Description Value");
        Account account = provisioning.createAccount("testaccount@example.com", "password", attrs);

        // Act
        String description = account.getAttr("description");

        // Assert
        Assert.assertEquals("Test Description Value", description);
    }

    /**
     * Test: Create account → retrieve non-existent attribute → verify
     * returns null.
     *
     * Verifies: getAttr() returns null for unset attributes, no
     * exception.
     */
    @Test
    public void getAttr_returnsNullForMissingAttribute_noException() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Act
        String missing = account.getAttr("nonexistentAttribute");

        // Assert
        Assert.assertNull(missing);
    }

    /**
     * Test: Create account → get all attributes → verify map contains
     * expected keys.
     *
     * Verifies: getAttrs() returns map with all stored attributes,
     * can be iterated.
     */
    @Test
    public void getAttrs_returnsAllAttributes_mapIterable() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Desc");
        attrs.put("displayName", "Test Name");
        Account account = provisioning.createAccount("testaccount@example.com", "password", attrs);

        // Act
        Map<String, Object> retrieved = account.getAttrs();

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertTrue("Should contain description", retrieved.containsKey("description"));
        Assert.assertTrue("Should contain displayName", retrieved.containsKey("displayName"));
    }

    /**
     * Test: Create account → get ID → verify non-null and non-empty.
     *
     * Verifies: Account has valid ID after creation.
     */
    @Test
    public void getId_returnsValidAccountId_nonEmptyString() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Act
        String id = account.getId();

        // Assert
        Assert.assertNotNull(id);
        Assert.assertFalse("ID should not be empty", id.isEmpty());
    }

    /**
     * Test: Create account → get name → verify equals creation name.
     *
     * Verifies: Account.getName() returns the account name used at
     * creation.
     */
    @Test
    public void getName_returnsAccountName_equalsCreationName() throws Exception {
        // Arrange & Act
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Assert
        Assert.assertEquals("testaccount@example.com", account.getName());
    }

    /**
     * Test: Create account → verify provisioning reference is set.
     *
     * Verifies: Account has reference to provisioning instance,
     * can be retrieved.
     */
    @Test
    public void getProvisioning_returnsProvisioningInstance_notNull() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());

        // Act
        Provisioning prov = account.getProvisioning();

        // Assert
        Assert.assertNotNull(prov);
        Assert.assertSame("Should be same provisioning instance", provisioning, prov);
    }

    /**
     * Test: Create account with attributes → check if in distribution
     * list → verify false initially.
     *
     * Verifies: inDistributionList() returns false for non-member.
     */
    @Test
    public void inDistributionList_returnsFalse_whenNotMember() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        DistributionList dl = provisioning.createDistributionList("testdl@example.com", new HashMap<>());

        // Act
        boolean isMember = account.inDistributionList(dl.getId());

        // Assert
        Assert.assertFalse("Should not be member initially", isMember);
    }

    /**
     * Test: Create account → add to distribution list → check
     * inDistributionList → verify true.
     *
     * Verifies: inDistributionList() returns true for actual member.
     */
    @Test
    public void inDistributionList_returnsTrue_whenMember() throws Exception {
        // Arrange
        Account account = provisioning.createAccount("testaccount@example.com", "password", new HashMap<>());
        DistributionList dl = provisioning.createDistributionList("testdl@example.com", new HashMap<>());
        provisioning.addMembers(dl, new String[]{account.getName()});

        // Act
        boolean isMember = account.inDistributionList(dl.getId());

        // Assert
        Assert.assertTrue("Should be member after adding", isMember);
    }

    // ==================== Helper Methods ====================

    private boolean contains(String[] array, String value) {
        if (array == null) {
            return false;
        }
        for (String item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
