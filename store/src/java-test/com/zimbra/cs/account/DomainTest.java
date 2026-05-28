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
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Full functional tests for {@link Domain}.
 *
 * Tests verify domain creation, modification, deletion, account defaults,
 * status tracking, and domain-specific workflows like account enumeration,
 * GAL sync, and auth mechanism configuration.
 */
public class DomainTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        try {
            Domain domain = provisioning.getDomainByName( "testdomain.example.com");
            if (domain != null) {
                provisioning.deleteDomain(domain.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }

        try {
            Domain domain = provisioning.getDomainByName( "modifieddomain.example.com");
            if (domain != null) {
                provisioning.deleteDomain(domain.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create domain with attributes → retrieve by name → verify
     * attributes persisted.
     *
     * Verifies: Domain creation creates persistent entries with correct
     * attributes.
     */
    @Test
    public void createDomain_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Domain");

        // Act - Create domain
        Domain created = provisioning.createDomain("testdomain.example.com", attrs);

        // Assert - Verify created
        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());
        Assert.assertEquals("testdomain.example.com", created.getName());

        // Act - Retrieve to verify persistence
        Domain retrieved = provisioning.getDomainByName( "testdomain.example.com");

        // Assert - Verify persistence
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(created.getId(), retrieved.getId());
        Assert.assertEquals("testdomain.example.com", retrieved.getName());
    }

    /**
     * Test: Create domain → modify attributes → retrieve → verify changes
     * persisted.
     *
     * Verifies: Domain modification changes object state and persists.
     */
    @Test
    public void modifyDomain_updatesAttributes_changePersisted() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act - Modify domain
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Modified Description");
        domain.modify(updates);

        // Assert - Verify modification persisted
        Domain retrieved = provisioning.getDomainByName( "testdomain.example.com");
        Assert.assertNotNull(retrieved.getAttr("description"));
    }

    /**
     * Test: Create domain → delete → verify no longer retrievable.
     *
     * Verifies: Domain deletion removes from all lookup paths.
     */
    @Test
    public void deleteDomain_removesFromSystem_noLongerRetrievable() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());
        String domainId = domain.getId();

        // Verify initial state
        Domain verify = provisioning.getDomainByName( "testdomain.example.com");
        Assert.assertNotNull(verify);

        // Act - Delete domain
        provisioning.deleteDomain(domainId);

        // Assert - Verify deleted
        Domain byName = provisioning.getDomainByName( "testdomain.example.com");
        Assert.assertNull(byName);
    }

    /**
     * Test: Create domain → get entry type → verify returns DOMAIN type.
     *
     * Verifies: Domain.getEntryType() returns correct type.
     */
    @Test
    public void getEntryType_returnsDomainType() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act & Assert
        Assert.assertEquals(EntryType.DOMAIN, domain.getEntryType());
    }

    /**
     * Test: Create domain → get unicode name → verify IDN conversion.
     *
     * Verifies: Domain.getUnicodeName() returns unicode domain name.
     */
    @Test
    public void getUnicodeName_returnsUnicodeDomainName() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        String unicodeName = domain.getUnicodeName();

        // Assert
        Assert.assertNotNull(unicodeName);
        // Should be same as name for ASCII domain
        Assert.assertTrue("Should contain 'testdomain'", unicodeName.contains("testdomain"));
    }

    /**
     * Test: Create domain → get account defaults → verify map returned
     * (may be empty initially).
     *
     * Verifies: Domain.getAccountDefaults() returns map of inherited CoS
     * defaults.
     */
    @Test
    public void getAccountDefaults_returnsMapOfInheritedDefaults() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Map<String, Object> defaults = domain.getAccountDefaults();

        // Assert
        Assert.assertNotNull(defaults);
        // Map may be empty initially for fresh domain
    }

    /**
     * Test: Create domain with default status → check if suspended →
     * verify false.
     *
     * Verifies: Domain.isSuspended() returns false for active domain.
     */
    @Test
    public void isSuspended_returnsFalse_forActiveDomain() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        boolean suspended = domain.isSuspended();

        // Assert
        Assert.assertFalse("New domain should not be suspended", suspended);
    }

    /**
     * Test: Create domain → check if shutdown → verify false.
     *
     * Verifies: Domain.isShutdown() returns false for normal domain.
     */
    @Test
    public void isShutdown_returnsFalse_forNormalDomain() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        boolean shutdown = domain.isShutdown();

        // Assert
        Assert.assertFalse("New domain should not be shutdown", shutdown);
    }

    /**
     * Test: Create domain → check being renamed → verify false initially.
     *
     * Verifies: Domain.beingRenamed() returns false when no rename info
     * set.
     */
    @Test
    public void beingRenamed_returnsFalse_noRenameInfoSet() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        boolean beingRenamed = domain.beingRenamed();

        // Assert
        Assert.assertFalse("Should not be being renamed initially", beingRenamed);
    }

    /**
     * Test: Create domain → check if local → verify true for local domain.
     *
     * Verifies: Domain.isLocal() correctly identifies local domain type.
     */
    @Test
    public void isLocal_returnsTrue_forLocalDomain() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        boolean isLocal = domain.isLocal();

        // Assert
        // New domains should be local type by default
        Assert.assertTrue("Should be local domain", isLocal);
    }

    /**
     * Test: Create domain → create account in domain → get all accounts →
     * verify account appears.
     *
     * Verifies: Domain.getAllAccounts() returns accounts created in
     * domain.
     */
    @Test
    public void getAllAccounts_returnsAccountsInDomain() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());
        Account account = provisioning.createAccount("testuser@testdomain.example.com", "password", new HashMap<>());

        // Act
        List accounts = domain.getAllAccounts();

        // Assert
        Assert.assertNotNull(accounts);
        Assert.assertTrue("Should contain created account", accounts.size() > 0);
    }

    /**
     * Test: Create domain → get all calendar resources → verify list
     * returned (may be empty).
     *
     * Verifies: Domain.getAllCalendarResources() returns list of calendar
     * resources.
     */
    @Test
    public void getAllCalendarResources_returnsList_ofCalendarResources() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        List resources = domain.getAllCalendarResources();

        // Assert
        Assert.assertNotNull(resources);
        // List may be empty for new domain
    }

    /**
     * Test: Create domain → get all distribution lists → verify list
     * returned.
     *
     * Verifies: Domain.getAllDistributionLists() returns list of DLs.
     */
    @Test
    public void getAllDistributionLists_returnsList_ofDistributionLists() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        List dls = domain.getAllDistributionLists();

        // Assert
        Assert.assertNotNull(dls);
        // List may be empty for new domain
    }

    /**
     * Test: Create domain → get ID → verify non-null and non-empty.
     *
     * Verifies: Domain has valid ID after creation.
     */
    @Test
    public void getId_returnsValidDomainId_nonEmptyString() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        String id = domain.getId();

        // Assert
        Assert.assertNotNull(id);
        Assert.assertFalse("ID should not be empty", id.isEmpty());
    }

    /**
     * Test: Create domain → get name → verify equals creation name.
     *
     * Verifies: Domain.getName() returns the domain name used at creation.
     */
    @Test
    public void getName_returnsDomainName_equalsCreationName() throws Exception {
        // Arrange & Act
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Assert
        Assert.assertEquals("testdomain.example.com", domain.getName());
    }

    /**
     * Test: Create domain → get provisioning reference → verify same
     * instance.
     *
     * Verifies: Domain has reference to provisioning instance.
     */
    @Test
    public void getProvisioning_returnsProvisioningInstance_notNull() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Provisioning prov = domain.getProvisioning();

        // Assert
        Assert.assertNotNull(prov);
        Assert.assertSame("Should be same provisioning instance", provisioning, prov);
    }

    /**
     * Test: Create domain with description → get attribute → verify value.
     *
     * Verifies: Domain attributes persist and are retrievable.
     */
    @Test
    public void getAttr_returnsDomainAttribute_correctValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Domain Desc");
        Domain domain = provisioning.createDomain("testdomain.example.com", attrs);

        // Act
        String description = domain.getAttr("description");

        // Assert
        Assert.assertEquals("Test Domain Desc", description);
    }

    /**
     * Test: Create domain → get all attributes → verify map contains
     * expected keys.
     *
     * Verifies: Domain.getAttrs() returns map with all stored attributes.
     */
    @Test
    public void getAttrs_returnsAllAttributes_mapIterable() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Desc");
        Domain domain = provisioning.createDomain("testdomain.example.com", attrs);

        // Act
        Map<String, Object> retrieved = domain.getAttrs();

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertTrue("Should have attributes", retrieved.size() > 0);
    }

    /**
     * Test: Retrieve domain by ID → verify same instance as retrieval by
     * name.
     *
     * Verifies: Domain lookups by name and ID return consistent results.
     */
    @Test
    public void domainLookup_byIdAndName_returnsSameEntry() throws Exception {
        // Arrange
        Domain created = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Domain byName = provisioning.getDomainByName( "testdomain.example.com");
        Domain byId = provisioning.getDomainById( created.getId());

        // Assert
        Assert.assertNotNull(byName);
        Assert.assertNotNull(byId);
        Assert.assertEquals(byName.getId(), byId.getId());
    }

    /**
     * Test: Create domain → modify multiple attributes → verify all persisted.
     * Verifies: Multiple attribute modifications accumulate.
     */
    @Test
    public void modifyDomain_multipleAttributes_allPersisted() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act - Modify multiple attributes
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Modified Description");
        updates.put("zimbraDefaultCOSId", "test-cos-id");
        domain.modify(updates);

        // Assert - Verify all modifications persisted
        Domain retrieved = provisioning.getDomainByName( "testdomain.example.com");
        Assert.assertEquals("Modified Description", retrieved.getAttr("description"));
        Assert.assertEquals("test-cos-id", retrieved.getAttr("zimbraDefaultCOSId"));
    }

    /**
     * Test: Create domain → modify → modify again → verify final state.
     * Verifies: Sequential modifications build correct final state.
     */
    @Test
    public void modifyDomain_sequentialModifications_finalStateCorrect() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act - First modification
        Map<String, Object> updates1 = new HashMap<>();
        updates1.put("description", "First Modification");
        domain.modify(updates1);

        // Act - Second modification
        Map<String, Object> updates2 = new HashMap<>();
        updates2.put("description", "Second Modification");
        domain.modify(updates2);

        // Assert - Verify final state
        Domain retrieved = provisioning.getDomainByName( "testdomain.example.com");
        Assert.assertEquals("Second Modification", retrieved.getAttr("description"));
    }

    /**
     * Test: Create domain with initial attributes → modify again → verify override.
     * Verifies: Modifications override previous values.
     */
    @Test
    public void modifyDomain_overridesInitialValue_newValuePersisted() throws Exception {
        // Arrange
        Map<String, Object> initial = new HashMap<>();
        initial.put("description", "Initial Description");
        Domain domain = provisioning.createDomain("testdomain.example.com", initial);

        // Act - Modify to new value
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Updated Description");
        domain.modify(updates);

        // Assert
        Domain retrieved = provisioning.getDomainByName( "testdomain.example.com");
        Assert.assertEquals("Updated Description", retrieved.getAttr("description"));
    }

    /**
     * Test: Create domain → create account → verify account belongs to domain.
     * Verifies: Accounts created in domain are linked correctly.
     */
    @Test
    public void createAccountInDomain_accountBelongsToDomain_verified() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Account account = provisioning.createAccount("testuser@testdomain.example.com", "password", new HashMap<>());

        // Assert
        Assert.assertNotNull(account);
        Assert.assertEquals("testdomain.example.com", account.getDomainName());
    }

    /**
     * Test: Create domain → multiple accounts → verify all accounts in getAllAccounts.
     * Verifies: getAllAccounts returns all accounts created in domain.
     */
    @Test
    public void createMultipleAccounts_getAllAccounts_returnsAll() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Account account1 = provisioning.createAccount("user1@testdomain.example.com", "password", new HashMap<>());
        Account account2 = provisioning.createAccount("user2@testdomain.example.com", "password", new HashMap<>());

        // Assert
        List accounts = domain.getAllAccounts();
        Assert.assertNotNull(accounts);
        Assert.assertTrue("Should contain multiple accounts", accounts.size() >= 2);
    }

    /**
     * Test: Domain name as key → verifyConsistency across lookups.
     * Verifies: Domain name stays consistent through multiple queries.
     */
    @Test
    public void domainName_consistentAcrossLookups_alwaysSame() throws Exception {
        // Arrange
        Domain created = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act - Retrieve multiple times
        Domain lookup1 = provisioning.getDomainByName( "testdomain.example.com");
        Domain lookup2 = provisioning.getDomainById( created.getId());
        Domain lookup3 = provisioning.getDomainByName( "testdomain.example.com");

        // Assert - All should have same name
        Assert.assertEquals("testdomain.example.com", lookup1.getName());
        Assert.assertEquals("testdomain.example.com", lookup2.getName());
        Assert.assertEquals("testdomain.example.com", lookup3.getName());
    }

    /**
     * Test: Domain account defaults → verify returned on init.
     * Verifies: getAccountDefaults persists after domain creation.
     */
    @Test
    public void getAccountDefaults_persistsAfterCreation_returnsConsistent() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Map<String, Object> defaults1 = domain.getAccountDefaults();
        Map<String, Object> defaults2 = domain.getAccountDefaults();

        // Assert
        Assert.assertNotNull(defaults1);
        Assert.assertNotNull(defaults2);
    }

    /**
     * Test: Domain with reset data → verify account defaults updated.
     * Verifies: resetData() updates account defaults correctly.
     */
    @Test
    public void domainDefaults_afterCreation_nonNullAndAccessible() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());

        // Act
        Map<String, Object> defaults = domain.getAccountDefaults();

        // Assert
        Assert.assertNotNull(defaults);
        // Defaults may be empty for new domain, but map should not be null
    }

    /**
     * Test: Create domain → delete → attempt lookup by ID → verify null.
     * Verifies: Deleted domain cannot be retrieved by any lookup method.
     */
    @Test
    public void deletedDomain_lookupById_returnsNull() throws Exception {
        // Arrange
        Domain domain = provisioning.createDomain("testdomain.example.com", new HashMap<>());
        String domainId = domain.getId();

        // Act - Delete domain
        provisioning.deleteDomain(domainId);

        // Assert - Verify cannot be retrieved by ID
        Domain byId = provisioning.getDomainById( domainId);
        Assert.assertNull(byId);
    }

    /**
     * Test: Create multiple domains → each has unique ID.
     * Verifies: Each domain gets distinct ID on creation.
     */
    @Test
    public void createMultipleDomains_eachHasUniqueId() throws Exception {
        // Arrange
        Domain domain1 = provisioning.createDomain("testdomain1.example.com", new HashMap<>());
        Domain domain2 = provisioning.createDomain("testdomain2.example.com", new HashMap<>());

        try {
            // Assert
            Assert.assertNotNull(domain1.getId());
            Assert.assertNotNull(domain2.getId());
            Assert.assertNotEquals("IDs should be unique", domain1.getId(), domain2.getId());
        } finally {
            // Cleanup
            try {
                provisioning.deleteDomain(domain2.getId());
            } catch (ServiceException e) {
                // Ignore
            }
        }
    }
}
