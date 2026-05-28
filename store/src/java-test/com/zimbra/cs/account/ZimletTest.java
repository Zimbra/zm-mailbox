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
 * Unit tests for {@link Zimlet}.
 *
 * Tests verify Zimlet creation, attribute management, and lifecycle.
 */
public class ZimletTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        try {
            Zimlet zimlet = provisioning.getZimlet("testzimlet");
            if (zimlet != null) {
                provisioning.deleteZimlet(zimlet.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create Zimlet with attributes → retrieve → verify persist.
     * Verifies: Zimlet creation and attribute persistence.
     */
    @Test
    public void createZimlet_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test Zimlet");

        // Act
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Assert
        Assert.assertNotNull(zimlet);
        Assert.assertNotNull(zimlet.getId());
        Assert.assertEquals("testzimlet", zimlet.getName());
    }

    /**
     * Test: Create Zimlet → call getEntryType() → verify ZIMLET.
     * Verifies: getEntryType() returns correct type.
     */
    @Test
    public void getEntryType_returnsZimletType() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        Entry.EntryType type = zimlet.getEntryType();

        // Assert
        Assert.assertEquals(Entry.EntryType.ZIMLET, type);
    }

    /**
     * Test: Create Zimlet → retrieve by name → verify same object.
     * Verifies: Retrieval by name works.
     */
    @Test
    public void getZimlet_byName_returnsSameObject() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet created = provisioning.createZimlet("testzimlet", attrs);

        // Act
        Zimlet retrieved = provisioning.getZimlet("testzimlet");

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(created.getId(), retrieved.getId());
        Assert.assertEquals(created.getName(), retrieved.getName());
    }

    /**
     * Test: Create Zimlet → call getId() → verify valid ID.
     * Verifies: ID is set and non-empty.
     */
    @Test
    public void getId_returnsValidId() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        String id = zimlet.getId();

        // Assert
        Assert.assertNotNull(id);
        Assert.assertTrue(id.length() > 0);
    }

    /**
     * Test: Create Zimlet → call getName() → verify name.
     * Verifies: getName() returns correct name.
     */
    @Test
    public void getName_returnsCorrectName() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        String name = zimlet.getName();

        // Assert
        Assert.assertEquals("testzimlet", name);
    }

    /**
     * Test: Create two Zimlets → verify independent.
     * Verifies: Multiple Zimlets are independent.
     */
    @Test
    public void multipleZimlets_areIndependent() throws Exception {
        // Arrange
        Map<String, Object> attrs1 = new HashMap<>();
        Map<String, Object> attrs2 = new HashMap<>();
        Zimlet z1 = provisioning.createZimlet("testzimlet1", attrs1);
        Zimlet z2 = provisioning.createZimlet("testzimlet2", attrs2);

        // Assert
        Assert.assertNotEquals(z1.getId(), z2.getId());
        Assert.assertNotEquals(z1.getName(), z2.getName());
    }

    /**
     * Test: Create Zimlet with description → verify attribute.
     * Verifies: Description attribute is stored and retrieved.
     */
    @Test
    public void zimletAttribute_description_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        String description = "My Test Zimlet Description";
        attrs.put("description", description);

        // Act
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Assert
        String retrieved = zimlet.getAttr("description");
        Assert.assertEquals(description, retrieved);
    }

    /**
     * Test: Delete Zimlet → verify deleted.
     * Verifies: deleteZimlet() removes Zimlet.
     */
    @Test
    public void deleteZimlet_removesZimlet() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        provisioning.deleteZimlet(zimlet.getId());

        // Assert
        Zimlet deleted = provisioning.getZimlet("testzimlet");
        Assert.assertNull("Zimlet should be deleted", deleted);
    }

    /**
     * Test: Create Zimlet → retrieve multiple times → verify stable.
     * Verifies: Retrieval is stable and consistent.
     */
    @Test
    public void getZimlet_multipleRetrievals_isStable() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet created = provisioning.createZimlet("testzimlet", attrs);

        // Act
        Zimlet retrieved1 = provisioning.getZimlet("testzimlet");
        Zimlet retrieved2 = provisioning.getZimlet("testzimlet");

        // Assert
        Assert.assertEquals(retrieved1.getId(), retrieved2.getId());
        Assert.assertEquals(retrieved1.getName(), retrieved2.getName());
    }

    /**
     * Test: Create Zimlet → call toString() → verify contains data.
     * Verifies: toString() produces valid output.
     */
    @Test
    public void toString_returnsValidString() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        String str = zimlet.toString();

        // Assert
        Assert.assertNotNull(str);
        Assert.assertTrue(str.length() > 0);
    }

    /**
     * Test: Create Zimlet with enabled flag → call isEnabled() → verify true.
     * Verifies: isEnabled() returns configured value.
     */
    @Test
    public void isEnabled_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraZimletEnabled, "TRUE");
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        boolean enabled = zimlet.isEnabled();

        // Assert
        Assert.assertTrue(enabled);
    }

    /**
     * Test: Create Zimlet without enabled flag → call isEnabled() → verify false.
     * Verifies: isEnabled() defaults to false.
     */
    @Test
    public void isEnabled_defaultIsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        boolean enabled = zimlet.isEnabled();

        // Assert
        Assert.assertFalse(enabled);
    }

    /**
     * Test: Create Zimlet with properties → retrieve and verify.
     * Verifies: Multiple Zimlet properties persist correctly.
     */
    @Test
    public void zimletProperties_allPersistCorrectly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraZimletEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraZimletPriority, "10");
        attrs.put(Provisioning.A_zimbraZimletDescription, "Test Description");
        attrs.put(Provisioning.A_zimbraZimletHandlerClass, "com.example.Handler");

        // Act
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Assert
        Assert.assertTrue(zimlet.isEnabled());
        Assert.assertEquals("10", zimlet.getPriority());
        Assert.assertEquals("Test Description", zimlet.getDescription());
        Assert.assertEquals("com.example.Handler", zimlet.getHandlerClassName());
    }

    /**
     * Test: Get Zimlet by ID → retrieve successfully.
     * Verifies: Retrieval by ID works.
     */
    @Test
    public void getZimletById_retrievesByIdSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet created = provisioning.createZimlet("testzimlet", attrs);
        String zimletId = created.getId();

        // Act
        Zimlet retrieved = provisioning.getZimlet(zimletId);

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(zimletId, retrieved.getId());
        Assert.assertEquals("testzimlet", retrieved.getName());
    }

    /**
     * Test: Create Zimlet with indexing enabled → verify getter.
     * Verifies: Indexing flag is stored and retrieved.
     */
    @Test
    public void isIndexingEnabled_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraZimletIndexingEnabled, "TRUE");
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        boolean indexing = zimlet.isIndexingEnabled();

        // Assert
        Assert.assertTrue(indexing);
    }

    /**
     * Test: Create Zimlet as extension → verify isExtension().
     * Verifies: Extension flag is stored and retrieved.
     */
    @Test
    public void isExtension_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraZimletIsExtension, "TRUE");
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        boolean extension = zimlet.isExtension();

        // Assert
        Assert.assertTrue(extension);
    }

    /**
     * Test: Modify Zimlet attributes → retrieve → verify changes.
     * Verifies: Attribute modifications persist.
     */
    @Test
    public void modifyZimlet_attributeUpdate_persists() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act - modify
        Map<String, Object> modifyAttrs = new HashMap<>();
        modifyAttrs.put(Provisioning.A_zimbraZimletDescription, "Updated Description");
        provisioning.modifyAttrs(zimlet, modifyAttrs);

        // Assert - retrieve and verify
        Zimlet retrieved = provisioning.getZimlet("testzimlet");
        Assert.assertEquals("Updated Description", retrieved.getDescription());
    }

    /**
     * Test: Zimlet with handler config → verify getters work.
     * Verifies: Handler configuration properties are accessible.
     */
    @Test
    public void handlerConfig_retrievedCorrectly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraZimletHandlerConfig, "<config>test</config>");
        attrs.put(Provisioning.A_zimbraZimletServerIndexRegex, ".*regex.*");
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        String config = zimlet.getHandlerConfig();
        String regex = zimlet.getServerIndexRegex();

        // Assert
        Assert.assertEquals("<config>test</config>", config);
        Assert.assertEquals(".*regex.*", regex);
    }

    /**
     * Test: Zimlet type via getType() → verify readable.
     * Verifies: Type attribute (cn) is readable.
     */
    @Test
    public void getType_returnsTypeAttribute() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_cn, "zimlet-type-name");
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);

        // Act
        String type = zimlet.getType();

        // Assert
        Assert.assertEquals("zimlet-type-name", type);
    }

    /**
     * Test: Zimlet lifecycle → create → modify → delete.
     * Verifies: Complete state transitions.
     */
    @Test
    public void zimletLifecycle_createModifyDelete_stateTransitions() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraZimletDescription, "Initial");

        // Act - Create
        Zimlet zimlet = provisioning.createZimlet("testzimlet", attrs);
        String zimletId = zimlet.getId();
        Assert.assertEquals("Initial", zimlet.getDescription());

        // Act - Modify
        Map<String, Object> modifyAttrs = new HashMap<>();
        modifyAttrs.put(Provisioning.A_zimbraZimletDescription, "Modified");
        provisioning.modifyAttrs(zimlet, modifyAttrs);

        Zimlet modified = provisioning.getZimlet("testzimlet");
        Assert.assertEquals("Modified", modified.getDescription());

        // Act - Delete
        provisioning.deleteZimlet(zimletId);
        Zimlet deleted = provisioning.getZimlet("testzimlet");

        // Assert
        Assert.assertNull("Zimlet should be deleted", deleted);
    }
}
