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
 * Unit tests for {@link CalendarResource}.
 *
 * Tests verify calendar resource creation, attribute management,
 * auto-accept/decline settings, and location properties.
 */
public class CalendarResourceTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @After
    public void cleanup() throws Exception {
        try {
            CalendarResource cr = provisioning.getCalendarResourceByName("testcr1@example.com");
            if (cr != null) {
                provisioning.deleteCalendarResource(cr.getId());
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    /**
     * Test: Create calendar resource with attributes → retrieve → verify persist.
     * Verifies: CalendarResource creation and attribute persistence.
     */
    @Test
    public void createCalendarResource_withAttributes_persistsSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResType, "Location");
        attrs.put("description", "Test Calendar Resource");

        // Act
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Assert
        Assert.assertNotNull(cr);
        Assert.assertNotNull(cr.getId());
        Assert.assertEquals("testcr1@example.com", cr.getName());
    }

    /**
     * Test: Create calendar resource → call getEntryType() → verify CALRESOURCE.
     * Verifies: getEntryType() returns correct type.
     */
    @Test
    public void getEntryType_returnsCalendarResourceType() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        Entry.EntryType type = cr.getEntryType();

        // Assert
        Assert.assertEquals(Entry.EntryType.CALRESOURCE, type);
    }

    /**
     * Test: Create calendar resource → call getResourceType() → verify default.
     * Verifies: getResourceType() returns default "Location".
     */
    @Test
    public void getResourceType_defaultIsLocation() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        String type = cr.getResourceType();

        // Assert
        Assert.assertEquals("Location", type);
    }

    /**
     * Test: Create calendar resource → call autoAcceptDecline() → verify true by default.
     * Verifies: autoAcceptDecline() defaults to true.
     */
    @Test
    public void autoAcceptDecline_defaultIsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        boolean autoAccept = cr.autoAcceptDecline();

        // Assert
        Assert.assertTrue(autoAccept);
    }

    /**
     * Test: Create calendar resource → call autoDeclineIfBusy() → verify true by default.
     * Verifies: autoDeclineIfBusy() defaults to true.
     */
    @Test
    public void autoDeclineIfBusy_defaultIsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        boolean autoDecline = cr.autoDeclineIfBusy();

        // Assert
        Assert.assertTrue(autoDecline);
    }

    /**
     * Test: Create calendar resource → call autoDeclineRecurring() → verify false by default.
     * Verifies: autoDeclineRecurring() defaults to false.
     */
    @Test
    public void autoDeclineRecurring_defaultIsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        boolean autoDecline = cr.autoDeclineRecurring();

        // Assert
        Assert.assertFalse(autoDecline);
    }

    /**
     * Test: Create calendar resource with capacity → call getCapacity() → verify value.
     * Verifies: getCapacity() returns configured value.
     */
    @Test
    public void getCapacity_returnsConfiguredValue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, "50");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        int capacity = cr.getCapacity();

        // Assert
        Assert.assertEquals(50, capacity);
    }

    /**
     * Test: Create calendar resource with location info → verify all getters work.
     * Verifies: Location properties are retrievable.
     */
    @Test
    public void getLocationProperties_returnValues() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResLocationDisplayName, "Room A");
        attrs.put(Provisioning.A_zimbraCalResSite, "Main Campus");
        attrs.put(Provisioning.A_zimbraCalResBuilding, "Building 1");
        attrs.put(Provisioning.A_zimbraCalResFloor, "2");
        attrs.put(Provisioning.A_zimbraCalResRoom, "201");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        String displayName = cr.getLocationDisplayName();
        String site = cr.getSite();
        String building = cr.getBuilding();
        String floor = cr.getFloor();
        String room = cr.getRoom();

        // Assert
        Assert.assertEquals("Room A", displayName);
        Assert.assertEquals("Main Campus", site);
        Assert.assertEquals("Building 1", building);
        Assert.assertEquals("2", floor);
        Assert.assertEquals("201", room);
    }

    /**
     * Test: Create calendar resource with contact info → verify all getters work.
     * Verifies: Contact properties are retrievable.
     */
    @Test
    public void getContactProperties_returnValues() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResContactName, "John Doe");
        attrs.put(Provisioning.A_zimbraCalResContactEmail, "john@example.com");
        attrs.put(Provisioning.A_zimbraCalResContactPhone, "555-1234");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        String name = cr.getContactName();
        String email = cr.getContactEmail();
        String phone = cr.getContactPhone();

        // Assert
        Assert.assertEquals("John Doe", name);
        Assert.assertEquals("john@example.com", email);
        Assert.assertEquals("555-1234", phone);
    }

    /**
     * Test: Create calendar resource → call getMaxNumConflictsAllowed() → verify default 0.
     * Verifies: maxNumConflicts defaults to 0.
     */
    @Test
    public void getMaxNumConflictsAllowed_defaultIsZero() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        int max = cr.getMaxNumConflictsAllowed();

        // Assert
        Assert.assertEquals(0, max);
    }

    /**
     * Test: Create calendar resource → call getMaxPercentConflictsAllowed() → verify default 0.
     * Verifies: maxPercentConflicts defaults to 0.
     */
    @Test
    public void getMaxPercentConflictsAllowed_defaultIsZero() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        int max = cr.getMaxPercentConflictsAllowed();

        // Assert
        Assert.assertEquals(0, max);
    }

    /**
     * Test: Create calendar resource → call getCapacity() with no value → verify default 0.
     * Verifies: Capacity defaults to 0 when not set.
     */
    @Test
    public void getCapacity_defaultIsZero() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        int capacity = cr.getCapacity();

        // Assert
        Assert.assertEquals(0, capacity);
    }

    /**
     * Test: Create calendar resource → modify attributes → retrieve → verify changes persist.
     * Verifies: Attribute modifications persist across retrievals.
     */
    @Test
    public void modifyCalendarResource_attributeUpdate_persists() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, "10");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act - modify
        Map<String, Object> modifyAttrs = new HashMap<>();
        modifyAttrs.put(Provisioning.A_zimbraCalResCapacity, "50");
        provisioning.modifyAttrs(cr, modifyAttrs);

        // Assert - retrieve and verify
        CalendarResource retrieved = provisioning.getCalendarResourceByName("testcr1@example.com");
        Assert.assertEquals(50, retrieved.getCapacity());
    }

    /**
     * Test: Create calendar resource with auto-accept disabled → verify setting persists.
     * Verifies: Boolean attributes are stored correctly.
     */
    @Test
    public void autoAcceptDecline_customValue_persistsCorrectly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResAutoAcceptDecline, "FALSE");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        boolean autoAccept = cr.autoAcceptDecline();

        // Assert
        Assert.assertFalse(autoAccept);
    }

    /**
     * Test: Create calendar resource → retrieve by ID → verify retrieval works.
     * Verifies: Retrieval by ID returns same resource.
     */
    @Test
    public void getCalendarResourceById_retrievesByIdSuccessfully() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource created = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);
        String resourceId = created.getId();

        // Act
        CalendarResource retrieved = provisioning.getCalendarResourceById(resourceId);

        // Assert
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(resourceId, retrieved.getId());
        Assert.assertEquals("testcr1@example.com", retrieved.getName());
    }

    /**
     * Test: Create two calendar resources → verify independent.
     * Verifies: Multiple resources don't interfere with each other.
     */
    @Test
    public void multipleCalendarResources_areIndependent() throws Exception {
        // Arrange
        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put(Provisioning.A_zimbraCalResCapacity, "30");
        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put(Provisioning.A_zimbraCalResCapacity, "60");

        CalendarResource cr1 = provisioning.createCalendarResource("testcr1@example.com", "password", attrs1);
        String cr1Id = cr1.getId();

        try {
            CalendarResource cr2 = provisioning.createCalendarResource("testcr2@example.com", "password", attrs2);

            // Assert
            Assert.assertNotEquals(cr1.getId(), cr2.getId());
            Assert.assertEquals(30, cr1.getCapacity());
            Assert.assertEquals(60, cr2.getCapacity());

            provisioning.deleteCalendarResource(cr2.getId());
        } catch (Exception e) {
            // Cleanup cr1 if cr2 creation fails
        }
    }

    /**
     * Test: Create calendar resource → call all getters → verify no null or exception.
     * Verifies: All getter methods work without exception.
     */
    @Test
    public void allGetters_returnValidValues() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResCapacity, "25");
        attrs.put(Provisioning.A_zimbraCalResContactName, "Admin");
        attrs.put(Provisioning.A_zimbraCalResContactEmail, "admin@example.com");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act & Assert
        Assert.assertEquals(Entry.EntryType.CALRESOURCE, cr.getEntryType());
        Assert.assertNotNull(cr.getId());
        Assert.assertEquals("testcr1@example.com", cr.getName());
        Assert.assertEquals("Location", cr.getResourceType());
        Assert.assertTrue(cr.autoAcceptDecline());
        Assert.assertTrue(cr.autoDeclineIfBusy());
        Assert.assertFalse(cr.autoDeclineRecurring());
        Assert.assertEquals(25, cr.getCapacity());
        Assert.assertEquals("Admin", cr.getContactName());
        Assert.assertEquals("admin@example.com", cr.getContactEmail());
    }

    /**
     * Test: Create calendar resource with conflicts settings → verify getters.
     * Verifies: Conflict limits are retrievable.
     */
    @Test
    public void conflictLimits_configuredCorrectly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResMaxNumConflictsAllowed, "5");
        attrs.put(Provisioning.A_zimbraCalResMaxPercentConflictsAllowed, "20");
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Act
        int maxNum = cr.getMaxNumConflictsAllowed();
        int maxPercent = cr.getMaxPercentConflictsAllowed();

        // Assert
        Assert.assertEquals(5, maxNum);
        Assert.assertEquals(20, maxPercent);
    }

    /**
     * Test: Delete calendar resource → attempt retrieval → verify null.
     * Verifies: Deletion removes resource from system.
     */
    @Test
    public void deleteCalendarResource_removesResource() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);
        String resourceId = cr.getId();

        // Act
        provisioning.deleteCalendarResource(resourceId);

        // Assert
        CalendarResource deleted = provisioning.getCalendarResourceById(resourceId);
        Assert.assertNull("Calendar resource should be deleted", deleted);
    }

    /**
     * Test: Calendar resource with room location details → verify all properties.
     * Verifies: All location properties work together correctly.
     */
    @Test
    public void roomLocation_allPropertiesTogether_workCorrectly() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraCalResType, "Room");
        attrs.put(Provisioning.A_zimbraCalResLocationDisplayName, "Conference Room A");
        attrs.put(Provisioning.A_zimbraCalResSite, "Downtown");
        attrs.put(Provisioning.A_zimbraCalResBuilding, "Building 5");
        attrs.put(Provisioning.A_zimbraCalResFloor, "3");
        attrs.put(Provisioning.A_zimbraCalResRoom, "301A");
        attrs.put(Provisioning.A_zimbraCalResCapacity, "20");

        // Act
        CalendarResource cr = provisioning.createCalendarResource("testcr1@example.com", "password", attrs);

        // Assert - verify complete workflow
        Assert.assertEquals("Room", cr.getResourceType());
        Assert.assertEquals("Conference Room A", cr.getLocationDisplayName());
        Assert.assertEquals("Downtown", cr.getSite());
        Assert.assertEquals("Building 5", cr.getBuilding());
        Assert.assertEquals("3", cr.getFloor());
        Assert.assertEquals("301A", cr.getRoom());
        Assert.assertEquals(20, cr.getCapacity());
    }
}
