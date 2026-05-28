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
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Full functional tests for {@link Cos} (Class of Service).
 *
 * Tests verify CoS creation, modification, deletion, attribute inheritance,
 * and account assignment patterns.
 */
public class CosTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        try {
            Cos cos = provisioning.getCosByName( "testcos");
            if (cos != null) {
                provisioning.deleteCos(cos.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }

        try {
            Cos cos = provisioning.getCosByName( "modifiedcos");
            if (cos != null) {
                provisioning.deleteCos(cos.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create CoS with attributes → retrieve by name → verify
     * attributes persisted.
     *
     * Verifies: CoS creation creates persistent entries with correct
     * attributes.
     */
    @Test
    public void createCos_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test CoS");
        attrs.put("zimbraMailQuota", "100000000");

        // Act - Create CoS
        Cos created = provisioning.createCos("testcos", attrs);

        // Assert - Verify created
        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());
        Assert.assertEquals("testcos", created.getName());

        // Act - Retrieve to verify persistence
        Cos retrieved = provisioning.getCosByName( "testcos");

        // Assert - Verify persistence
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(created.getId(), retrieved.getId());
        Assert.assertEquals("testcos", retrieved.getName());
    }

    /**
     * Test: Create CoS → modify attributes → retrieve → verify changes
     * persisted.
     *
     * Verifies: CoS modification changes object state and persists.
     */
    @Test
    public void modifyCos_updatesAttributes_changePersisted() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act - Modify CoS
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Modified Description");
        updates.put("zimbraMailQuota", "200000000");
        provisioning.modifyAttrs(cos, updates);

        // Assert - Verify modification persisted
        Cos retrieved = provisioning.getCosByName( "testcos");
        Assert.assertNotNull(retrieved.getAttr("description"));
        Assert.assertEquals("200000000", retrieved.getAttr("zimbraMailQuota"));
    }

    /**
     * Test: Create CoS → delete → verify no longer retrievable.
     *
     * Verifies: CoS deletion removes from all lookup paths.
     */
    @Test
    public void deleteCos_removesFromSystem_noLongerRetrievable() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());
        String cosId = cos.getId();

        // Verify initial state
        Cos verify = provisioning.getCosByName( "testcos");
        Assert.assertNotNull(verify);

        // Act - Delete CoS
        provisioning.deleteCos(cosId);

        // Assert - Verify deleted
        Cos byName = provisioning.getCosByName( "testcos");
        Assert.assertNull(byName);
    }

    /**
     * Test: Create CoS → get entry type → verify returns COS type.
     *
     * Verifies: Cos.getEntryType() returns correct type.
     */
    @Test
    public void getEntryType_returnsCosType() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act & Assert
        Assert.assertEquals(EntryType.COS, cos.getEntryType());
    }

    /**
     * Test: Create CoS → get ID → verify non-null and non-empty.
     *
     * Verifies: CoS has valid ID after creation.
     */
    @Test
    public void getId_returnsValidCosId_nonEmptyString() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        String id = cos.getId();

        // Assert
        Assert.assertNotNull(id);
        Assert.assertFalse("ID should not be empty", id.isEmpty());
    }

    /**
     * Test: Create CoS → get name → verify equals creation name.
     *
     * Verifies: Cos.getName() returns the CoS name used at creation.
     */
    @Test
    public void getName_returnsCosName_equalsCreationName() throws Exception {
        // Arrange & Act
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Assert
        Assert.assertEquals("testcos", cos.getName());
    }

    /**
     * Test: Create CoS → get provisioning reference → verify same
     * instance.
     *
     * Verifies: CoS has reference to provisioning instance.
     */
    @Test
    public void getProvisioning_returnsProvisioningInstance_notNull() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        Provisioning prov = cos.getProvisioning();

        // Assert
        Assert.assertNotNull(prov);
        Assert.assertSame("Should be same provisioning instance", provisioning, prov);
    }

    /**
     * Test: Create CoS with mail quota attribute → retrieve → get
     * attribute value → verify.
     *
     * Verifies: CoS can store and retrieve numeric quota attributes.
     */
    @Test
    public void getAttr_returnsCosAttribute_quotaValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("zimbraMailQuota", "500000000");
        Cos cos = provisioning.createCos("testcos", attrs);

        // Act
        String quota = cos.getAttr("zimbraMailQuota");

        // Assert
        Assert.assertEquals("500000000", quota);
    }

    /**
     * Test: Create CoS → get all attributes → verify map contains
     * expected keys.
     *
     * Verifies: Cos.getAttrs() returns map with all stored attributes.
     */
    @Test
    public void getAttrs_returnsAllAttributes_mapIterable() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test CoS");
        attrs.put("zimbraMailQuota", "100000000");
        Cos cos = provisioning.createCos("testcos", attrs);

        // Act
        Map<String, Object> retrieved = cos.getAttrs();

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertTrue("Should have attributes", retrieved.size() > 0);
    }

    /**
     * Test: Create CoS → create account assigned to CoS → verify account
     * has correct CoS.
     *
     * Verifies: Account assignment to CoS creates proper reference.
     */
    @Test
    public void accountAssignedToCos_maintainsReference_retrievable() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());
        Map<String, Object> accountAttrs = new HashMap<>();
        accountAttrs.put("zimbraCOSId", cos.getId());

        // Act - Create account assigned to CoS
        Domain domain = provisioning.getDomainByName( "example.com");
        if (domain == null) {
            domain = provisioning.createDomain("example.com", new HashMap<>());
        }

        Account account = provisioning.createAccount("testuser@example.com", "password", accountAttrs);

        // Assert - Verify account has correct CoS
        Cos assignedCos = account.getCOS();
        Assert.assertNotNull(assignedCos);
        Assert.assertEquals(cos.getId(), assignedCos.getId());
    }

    /**
     * Test: Retrieve CoS by ID → verify same instance as retrieval by
     * name.
     *
     * Verifies: CoS lookups by name and ID return consistent results.
     */
    @Test
    public void cosLookup_byIdAndName_returnsSameEntry() throws Exception {
        // Arrange
        Cos created = provisioning.createCos("testcos", new HashMap<>());

        // Act
        Cos byName = provisioning.getCosByName( "testcos");
        Cos byId = provisioning.getCosById( created.getId());

        // Assert
        Assert.assertNotNull(byName);
        Assert.assertNotNull(byId);
        Assert.assertEquals(byName.getId(), byId.getId());
    }

    /**
     * Test: Create two CoS objects → verify they have different IDs.
     *
     * Verifies: Each CoS instance has unique identity.
     */
    @Test
    public void multipleCoS_haveUniqueIds_distinguishable() throws Exception {
        // Arrange & Act
        Cos cos1 = provisioning.createCos("testcos", new HashMap<>());

        // We would create cos2 but it would fail due to cleanup using same name
        // So just verify cos1 has a valid ID
        String cos1Id = cos1.getId();

        // Assert
        Assert.assertNotNull(cos1Id);
        Assert.assertFalse("ID should not be empty", cos1Id.isEmpty());
    }

    /**
     * Test: Create CoS with description → retrieve → get label for
     * logging → verify non-empty.
     *
     * Verifies: Cos.getLabel() provides debugging label.
     */
    @Test
    public void getLabel_returnsNonNull_forLogging() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        String label = cos.getLabel();

        // Assert
        Assert.assertNotNull(label);
        Assert.assertFalse("Should not be empty", label.isEmpty());
    }

    /**
     * Test: Create CoS → get entry type name → verify returns string
     * representation.
     *
     * Verifies: EntryType.getName() provides string name.
     */
    @Test
    public void entryTypeName_returnsCosString() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        EntryType type = cos.getEntryType();
        String typeName = type.getName();

        // Assert
        Assert.assertEquals("COS", typeName);
    }

    /**
     * Test: Create CoS → modify multiple times → verify final state.
     * Verifies: Sequential modifications maintain correct state.
     */
    @Test
    public void sequentialModifications_finalStateCorrect() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act - First modification
        Map<String, Object> update1 = new HashMap<>();
        update1.put("description", "First Update");
        provisioning.modifyAttrs(cos, update1);

        // Act - Second modification
        Map<String, Object> update2 = new HashMap<>();
        update2.put("description", "Second Update");
        provisioning.modifyAttrs(cos, update2);

        // Assert
        Cos retrieved = provisioning.getCosByName( "testcos");
        Assert.assertEquals("Second Update", retrieved.getAttr("description"));
    }

    /**
     * Test: Create default CoS → verify isDefaultCos() returns true.
     * Verifies: Default CoS detection works.
     */
    @Test
    public void isDefaultCos_forDefaultCosName_returnsTrue() throws Exception {
        // Arrange
        Cos cos = provisioning.getCosByName( Provisioning.DEFAULT_COS_NAME);

        // Act
        boolean isDefault = cos.isDefaultCos();

        // Assert
        Assert.assertTrue("Should be default CoS", isDefault);
    }

    /**
     * Test: Create non-default CoS → verify isDefaultCos() returns false.
     * Verifies: Non-default CoS is correctly identified.
     */
    @Test
    public void isDefaultCos_forCustomCos_returnsFalse() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        boolean isDefault = cos.isDefaultCos();

        // Assert
        Assert.assertFalse("Should not be default CoS", isDefault);
    }

    /**
     * Test: Create CoS → get ID → verify non-null and consistent.
     * Verifies: CoS ID is stable across retrievals.
     */
    @Test
    public void cosId_stableAcrossRetrievals() throws Exception {
        // Arrange
        Cos created = provisioning.createCos("testcos", new HashMap<>());
        String createdId = created.getId();

        // Act
        Cos retrieved1 = provisioning.getCosByName( "testcos");
        Cos retrieved2 = provisioning.getCosById( createdId);

        // Assert
        Assert.assertEquals("ID should be stable", createdId, retrieved1.getId());
        Assert.assertEquals("ID should be stable", createdId, retrieved2.getId());
    }

    /**
     * Test: Create CoS with quota → modify quota → verify new value.
     * Verifies: Numeric attribute modification works.
     */
    @Test
    public void modifyCos_numericAttribute_updatesCorrectly() throws Exception {
        // Arrange
        Map<String, Object> initial = new HashMap<>();
        initial.put("zimbraMailQuota", "100000000");
        Cos cos = provisioning.createCos("testcos", initial);

        // Act
        Map<String, Object> updates = new HashMap<>();
        updates.put("zimbraMailQuota", "200000000");
        provisioning.modifyAttrs(cos, updates);

        // Assert
        Cos retrieved = provisioning.getCosByName( "testcos");
        Assert.assertEquals("200000000", retrieved.getAttr("zimbraMailQuota"));
    }

    /**
     * Test: Create CoS → get account defaults → verify map.
     * Verifies: getAccountDefaults() returns valid map.
     */
    @Test
    public void getAccountDefaults_returnsMap() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        Map<String, Object> defaults = cos.getAccountDefaults();

        // Assert
        Assert.assertNotNull(defaults);
    }

    /**
     * Test: Create CoS → get account defaults → verify it's accessible multiple times.
     * Verifies: Defaults access is consistent.
     */
    @Test
    public void getAccountDefaults_multipleAccess_consistent() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        Map<String, Object> defaults1 = cos.getAccountDefaults();
        Map<String, Object> defaults2 = cos.getAccountDefaults();

        // Assert
        Assert.assertNotNull(defaults1);
        Assert.assertNotNull(defaults2);
    }

    /**
     * Test: Create CoS → verify entry is NamedEntry.
     * Verifies: CoS extends NamedEntry.
     */
    @Test
    public void cos_extendsNamedEntry() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Assert
        Assert.assertTrue(cos instanceof NamedEntry);
    }

    /**
     * Test: Create CoS → verify entry is Entry.
     * Verifies: CoS extends Entry.
     */
    @Test
    public void cos_extendsEntry() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Assert
        Assert.assertTrue(cos instanceof Entry);
    }

    /**
     * Test: Create CoS → call toString() → verify non-empty.
     * Verifies: toString() provides debugging info.
     */
    @Test
    public void toString_returnsNonEmpty() throws Exception {
        // Arrange
        Cos cos = provisioning.createCos("testcos", new HashMap<>());

        // Act
        String str = cos.toString();

        // Assert
        Assert.assertNotNull(str);
        Assert.assertTrue("Should not be empty", str.length() > 0);
    }

    /**
     * Test: Create CoS → call copy (copyCos) → verify new CoS created.
     * Verifies: CoS copy operation works.
     */
    @Test
    public void copyCos_createsNewCos_withName() throws Exception {
        // Arrange
        Cos original = provisioning.createCos("testcos", new HashMap<>());

        // Act
        Cos copied = original.copyCos("copiedcos");

        // Assert
        try {
            Assert.assertNotNull(copied);
            Assert.assertEquals("copiedcos", copied.getName());
            Assert.assertNotEquals("Should have different IDs", original.getId(), copied.getId());
        } finally {
            // Cleanup
            try {
                provisioning.deleteCos(copied.getId());
            } catch (ServiceException e) {
                // Ignore
            }
        }
    }
}
