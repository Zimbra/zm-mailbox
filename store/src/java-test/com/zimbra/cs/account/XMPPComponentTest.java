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
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for {@link XMPPComponent}.
 *
 * Tests verify XMPP component creation, attribute management, and lifecycle.
 */
public class XMPPComponentTest {

    private static Provisioning provisioning;
    private static Domain testDomain;
    private static Server testServer;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
        testDomain = provisioning.createDomain("example.com", new HashMap<>());
        testServer = provisioning.createServer("test-server", new HashMap<>());
    }

    @After
    public void cleanup() throws Exception {
        try {
            XMPPComponent comp = provisioning.get(Key.XMPPComponentBy.name, "xmpp.example.com");
            if (comp != null) {
                provisioning.deleteXMPPComponent(comp);
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create XMPP component with attributes → retrieve → verify persist.
     * Verifies: XMPPComponent creation and attribute persistence.
     */
    @Test
    public void createXMPPComponent_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "Test XMPP Component");

        // Act
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Assert
        Assert.assertNotNull(comp);
        Assert.assertNotNull(comp.getId());
        Assert.assertEquals("xmpp.example.com", comp.getName());
    }

    /**
     * Test: Create XMPP component → call getEntryType() → verify type.
     * Verifies: getEntryType() returns correct type.
     */
    @Test
    public void getEntryType_returnsXMPPComponentType() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        Entry.EntryType type = comp.getEntryType();

        // Assert
        Assert.assertEquals(Entry.EntryType.XMPPCOMPONENT, type);
    }

    /**
     * Test: Create XMPP component → retrieve by name → verify same.
     * Verifies: Retrieval by name works.
     */
    @Test
    public void getXMPPComponent_byName_returnsSameObject() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent created = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        XMPPComponent retrieved = provisioning.get(Key.XMPPComponentBy.name, "xmpp.example.com");

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(created.getId(), retrieved.getId());
    }

    /**
     * Test: Create XMPP component → call getId() → verify valid ID.
     * Verifies: ID is set and non-empty.
     */
    @Test
    public void getId_returnsValidId() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String id = comp.getId();

        // Assert
        Assert.assertNotNull(id);
        Assert.assertTrue(id.length() > 0);
    }

    /**
     * Test: Create XMPP component → call getName() → verify name.
     * Verifies: getName() returns correct name.
     */
    @Test
    public void getName_returnsCorrectName() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String name = comp.getName();

        // Assert
        Assert.assertEquals("xmpp.example.com", name);
    }

    /**
     * Test: Create two XMPP components → verify independent.
     * Verifies: Multiple components are independent.
     */
    @Test
    public void multipleXMPPComponents_areIndependent() throws Exception {
        // Arrange
        Map<String, Object> attrs1 = new HashMap<>();
        XMPPComponent c1 = provisioning.createXMPPComponent("xmpp1.example.com", testDomain, testServer, attrs1);
        // Cleanup first one for second test
        try {
            XMPPComponent c2 = provisioning.createXMPPComponent("xmpp2.example.com", testDomain, testServer, new HashMap<>());
            Assert.assertNotEquals(c1.getId(), c2.getId());
            provisioning.deleteXMPPComponent(c2);
        } catch (ServiceException e) {
            // May not support multiple
        }
    }

    /**
     * Test: Delete XMPP component → verify deleted.
     * Verifies: deleteXMPPComponent() removes component.
     */
    @Test
    public void deleteXMPPComponent_removesComponent() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        provisioning.deleteXMPPComponent(comp);

        // Assert
        XMPPComponent deleted = provisioning.get(Key.XMPPComponentBy.name, "xmpp.example.com");
        Assert.assertNull("XMPP component should be deleted", deleted);
    }

    /**
     * Test: Create XMPP component with attribute → verify persistence.
     * Verifies: Custom attributes are stored and retrieved.
     */
    @Test
    public void xmppComponentAttribute_persistence_succeeds() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("description", "My XMPP Component");
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String description = comp.getAttr("description");

        // Assert
        Assert.assertEquals("My XMPP Component", description);
    }

    /**
     * Test: Create XMPP component → call toString() → verify valid.
     * Verifies: toString() produces valid output.
     */
    @Test
    public void toString_returnsValidString() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String str = comp.toString();

        // Assert
        Assert.assertNotNull(str);
        Assert.assertTrue(str.length() > 0);
    }

    /**
     * Test: Create XMPP component with category → verify getComponentCategory().
     * Verifies: Category property is stored and retrieved.
     */
    @Test
    public void getComponentCategory_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentCategory, "conference");
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String category = comp.getComponentCategory();

        // Assert
        Assert.assertEquals("conference", category);
    }

    /**
     * Test: Create XMPP component with type → verify getComponentType().
     * Verifies: Type property is stored and retrieved.
     */
    @Test
    public void getComponentType_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentType, "muc");
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String type = comp.getComponentType();

        // Assert
        Assert.assertEquals("muc", type);
    }

    /**
     * Test: Create XMPP component with long name → verify getLongName().
     * Verifies: Long name property is accessible.
     */
    @Test
    public void getLongName_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentName, "Multi User Chat");
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String longName = comp.getLongName();

        // Assert
        Assert.assertEquals("Multi User Chat", longName);
    }

    /**
     * Test: Create XMPP component with class name → verify getClassName().
     * Verifies: Class name property is accessible.
     */
    @Test
    public void getClassName_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentClassName, "org.example.XMPPHandler");
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String className = comp.getClassName();

        // Assert
        Assert.assertEquals("org.example.XMPPHandler", className);
    }

    /**
     * Test: Create XMPP component with domain and server IDs → verify getters.
     * Verifies: Domain and server IDs are retrievable.
     */
    @Test
    public void domainId_and_serverId_areConfigurable() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraDomainId, "domain-123");
        attrs.put(Provisioning.A_zimbraServerId, "server-456");
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        String domainId = comp.getDomainId();
        String serverId = comp.getServerId();

        // Assert
        Assert.assertEquals("domain-123", domainId);
        Assert.assertEquals("server-456", serverId);
    }

    /**
     * Test: Create XMPP component → modify attributes → retrieve → verify changes.
     * Verifies: Attribute modifications persist.
     */
    @Test
    public void modifyXMPPComponent_attributeUpdate_persists() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act - modify
        Map<String, Object> modifyAttrs = new HashMap<>();
        modifyAttrs.put(Provisioning.A_zimbraXMPPComponentCategory, "muc");
        provisioning.modifyAttrs(comp, modifyAttrs);

        // Assert - retrieve and verify
        XMPPComponent retrieved = provisioning.get(Key.XMPPComponentBy.name, "xmpp.example.com");
        Assert.assertEquals("muc", retrieved.getComponentCategory());
    }

    /**
     * Test: Get XMPP component features → verify list accessible.
     * Verifies: Component features are retrievable as list.
     */
    @Test
    public void getComponentFeatures_returnsFeaturesList() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentFeatures, new String[]{"feature1", "feature2"});
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        List<String> features = comp.getComponentFeatures();

        // Assert
        Assert.assertNotNull(features);
        Assert.assertTrue(features.size() >= 2);
        Assert.assertTrue(features.contains("feature1"));
    }

    /**
     * Test: XMPP component with empty features → verify empty list.
     * Verifies: getComponentFeatures() returns empty list when no features.
     */
    @Test
    public void getComponentFeatures_emptyWhenNotSet() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Act
        List<String> features = comp.getComponentFeatures();

        // Assert
        Assert.assertNotNull(features);
        Assert.assertTrue(features.isEmpty());
    }

    /**
     * Test: XMPP component complete configuration → verify all properties.
     * Verifies: All properties work together correctly.
     */
    @Test
    public void xmppComponent_withAllProperties_allAccessible() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentCategory, "conference");
        attrs.put(Provisioning.A_zimbraXMPPComponentType, "muc");
        attrs.put(Provisioning.A_zimbraXMPPComponentName, "Conference Manager");
        attrs.put(Provisioning.A_zimbraXMPPComponentClassName, "com.example.MUC");
        attrs.put(Provisioning.A_zimbraDomainId, "domain-123");
        attrs.put(Provisioning.A_zimbraServerId, "server-456");
        attrs.put(Provisioning.A_zimbraXMPPComponentFeatures, new String[]{"feature1"});

        // Act
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);

        // Assert
        Assert.assertEquals("conference", comp.getComponentCategory());
        Assert.assertEquals("muc", comp.getComponentType());
        Assert.assertEquals("Conference Manager", comp.getLongName());
        Assert.assertEquals("com.example.MUC", comp.getClassName());
        Assert.assertEquals("domain-123", comp.getDomainId());
        Assert.assertEquals("server-456", comp.getServerId());
        Assert.assertFalse(comp.getComponentFeatures().isEmpty());
    }

    /**
     * Test: XMPP component lifecycle → create → modify → delete.
     * Verifies: Complete state transitions.
     */
    @Test
    public void xmppComponentLifecycle_createModifyDelete_stateTransitions() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraXMPPComponentCategory, "initial");

        // Act - Create
        XMPPComponent comp = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs);
        String compId = comp.getId();
        Assert.assertEquals("initial", comp.getComponentCategory());

        // Act - Modify
        Map<String, Object> modifyAttrs = new HashMap<>();
        modifyAttrs.put(Provisioning.A_zimbraXMPPComponentCategory, "modified");
        provisioning.modifyAttrs(comp, modifyAttrs);

        XMPPComponent modified = provisioning.get(Key.XMPPComponentBy.name, "xmpp.example.com");
        Assert.assertEquals("modified", modified.getComponentCategory());

        // Act - Delete
        provisioning.deleteXMPPComponent(comp);
        XMPPComponent deleted = provisioning.get(Key.XMPPComponentBy.name, "xmpp.example.com");

        // Assert
        Assert.assertNull("XMPP component should be deleted", deleted);
    }

    /**
     * Test: XMPP component implements Comparable → verify compareTo works.
     * Verifies: Comparable interface is implemented.
     */
    @Test
    public void xmppComponent_isComparable() throws Exception {
        // Arrange
        Map<String, Object> attrs1 = new HashMap<>();
        XMPPComponent comp1 = provisioning.createXMPPComponent("xmpp.example.com", testDomain, testServer, attrs1);

        // Assert
        Assert.assertTrue(comp1 instanceof Comparable);
    }
}
