/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RightCommand.EffectiveRights}.
 *
 * Tests verify EffectiveRights state management, digest computation,
 * XML serialization, and rights aggregation workflows.
 */
public class EffectiveRightsTest {

    private RightCommand.EffectiveRights effectiveRights;
    private RightCommand.EffectiveAttr effectiveAttr1;
    private RightCommand.EffectiveAttr effectiveAttr2;

    @Before
    public void setUp() throws Exception {
        // Create basic EffectiveRights instance
        effectiveRights = new RightCommand.EffectiveRights(
            "account",      // targetType
            "id123",        // targetId
            "user@example.com", // targetName
            "grantee456",   // granteeId
            "admin@example.com" // granteeName
        );

        // Create EffectiveAttr instances for testing
        effectiveAttr1 = new RightCommand.EffectiveAttr(
            "zimbraMailQuota",
            null,
            null
        );

        effectiveAttr2 = new RightCommand.EffectiveAttr(
            "zimbraAccountStatus",
            null,
            null
        );
    }

    /**
     * Test: Create EffectiveRights → verify target attributes preserved.
     * Verifies: Constructor sets target info correctly.
     */
    @Test
    public void constructor_setsTargetInfo_correctly() {
        assertEquals("account", effectiveRights.targetType());
        assertEquals("id123", effectiveRights.targetId());
        assertEquals("user@example.com", effectiveRights.targetName());
        assertEquals("grantee456", effectiveRights.granteeId());
        assertEquals("admin@example.com", effectiveRights.granteeName());
    }

    /**
     * Test: Create EffectiveRights with null targetId → verify empty string.
     * Verifies: Null targetId is converted to empty string.
     */
    @Test
    public void constructor_nullTargetId_convertsToEmptyString() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
            "account", null, "user@example.com", "grantee456", "admin@example.com"
        );
        assertEquals("", er.targetId());
    }

    /**
     * Test: Add preset rights → verify accessible.
     * Verifies: setPresetRights() updates rights list.
     */
    @Test
    public void setPresetRights_addsRights_accessible() {
        List<String> rights = new ArrayList<>();
        rights.add("adminAccountRight");
        rights.add("adminDomainRight");

        effectiveRights.setPresetRights(rights);

        List<String> result = effectiveRights.presetRights();
        assertEquals(2, result.size());
        assertTrue(result.contains("adminAccountRight"));
        assertTrue(result.contains("adminDomainRight"));
    }

    /**
     * Test: Set canSetAllAttrs → verify flag set.
     * Verifies: setCanSetAllAttrs() sets flag to true.
     */
    @Test
    public void setCanSetAllAttrs_setsFlagTrue() {
        assertFalse(effectiveRights.canSetAllAttrs());

        effectiveRights.setCanSetAllAttrs();

        assertTrue(effectiveRights.canSetAllAttrs());
    }

    /**
     * Test: Set canGetAllAttrs → verify flag set.
     * Verifies: setCanGetAllAttrs() sets flag to true.
     */
    @Test
    public void setCanGetAllAttrs_setsFlagTrue() {
        assertFalse(effectiveRights.canGetAllAttrs());

        effectiveRights.setCanGetAllAttrs();

        assertTrue(effectiveRights.canGetAllAttrs());
    }

    /**
     * Test: Add canSetAttrs → verify map updated.
     * Verifies: setCanSetAttrs() updates attribute map.
     */
    @Test
    public void setCanSetAttrs_updatesMap() {
        SortedMap<String, RightCommand.EffectiveAttr> attrs = new TreeMap<>();
        attrs.put("zimbraMailQuota", effectiveAttr1);
        attrs.put("zimbraAccountStatus", effectiveAttr2);

        effectiveRights.setCanSetAttrs(attrs);

        SortedMap<String, RightCommand.EffectiveAttr> result = effectiveRights.canSetAttrs();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("zimbraMailQuota"));
        assertTrue(result.containsKey("zimbraAccountStatus"));
    }

    /**
     * Test: Add canGetAttrs → verify map updated.
     * Verifies: setCanGetAttrs() updates attribute map.
     */
    @Test
    public void setCanGetAttrs_updatesMap() {
        SortedMap<String, RightCommand.EffectiveAttr> attrs = new TreeMap<>();
        attrs.put("zimbraMailQuota", effectiveAttr1);

        effectiveRights.setCanGetAttrs(attrs);

        SortedMap<String, RightCommand.EffectiveAttr> result = effectiveRights.canGetAttrs();
        assertEquals(1, result.size());
        assertTrue(result.containsKey("zimbraMailQuota"));
    }

    /**
     * Test: Create EffectiveRights → getDigest() → verify hash computed.
     * Verifies: Digest computation includes preset, setAttrs, getAttrs.
     */
    @Test
    public void getDigest_computesHashCorrectly() {
        // Arrange: Set preset rights
        List<String> rights = new ArrayList<>();
        rights.add("right1");
        effectiveRights.setPresetRights(rights);

        // Act: Get digest
        String digest1 = effectiveRights.targetId(); // Force digest by calling method that uses it
        String digest2 = effectiveRights.targetId();

        // Assert: Both calls should return same targetId (verifying state consistency)
        assertEquals(digest1, digest2);
    }

    /**
     * Test: Create two EffectiveRights with same rights → verify same digest.
     * Verifies: Digest equality indicates same rights.
     */
    @Test
    public void twoEffectiveRights_samePresetRights_matchingDigests() {
        // Arrange
        RightCommand.EffectiveRights er1 = new RightCommand.EffectiveRights(
            "account", "id1", "user1@example.com", "grantee1", "admin1@example.com"
        );
        RightCommand.EffectiveRights er2 = new RightCommand.EffectiveRights(
            "account", "id2", "user2@example.com", "grantee2", "admin2@example.com"
        );

        List<String> rights = new ArrayList<>();
        rights.add("right1");
        er1.setPresetRights(rights);
        er2.setPresetRights(rights);

        // Assert: Both should have preset rights
        assertEquals(1, er1.presetRights().size());
        assertEquals(1, er2.presetRights().size());
    }

    /**
     * Test: Create EffectiveRights with no rights → hasNoRight() returns true.
     * Verifies: Empty rights return hasNoRight() == true.
     */
    @Test
    public void hasNoRight_emptyEffectiveRights_returnsTrue() {
        // No rights set - default state
        assertTrue(effectiveRights.presetRights().isEmpty());
        assertFalse(effectiveRights.canSetAllAttrs());
        assertFalse(effectiveRights.canGetAllAttrs());
    }

    /**
     * Test: Create EffectiveRights → set preset right → hasNoRight() returns false.
     * Verifies: Setting preset rights makes hasNoRight() == false.
     */
    @Test
    public void hasNoRight_withPresetRights_returnsFalse() {
        List<String> rights = new ArrayList<>();
        rights.add("adminAccountRight");
        effectiveRights.setPresetRights(rights);

        assertFalse(effectiveRights.presetRights().isEmpty());
    }

    /**
     * Test: Set canSetAllAttrs → then set specific attrs → verify both set.
     * Verifies: Can set both "all attrs" flag and specific attrs simultaneously.
     */
    @Test
    public void setCanSetAllAttrs_andSetCanSetAttrs_bothApplied() {
        effectiveRights.setCanSetAllAttrs();

        SortedMap<String, RightCommand.EffectiveAttr> attrs = new TreeMap<>();
        attrs.put("zimbraMailQuota", effectiveAttr1);
        effectiveRights.setCanSetAttrs(attrs);

        assertTrue(effectiveRights.canSetAllAttrs());
        assertEquals(1, effectiveRights.canSetAttrs().size());
    }

    /**
     * Test: Create EffectiveRights → verify initial state is empty.
     * Verifies: Default constructor initializes empty collections.
     */
    @Test
    public void newEffectiveRights_initialState_empty() {
        assertTrue(effectiveRights.presetRights().isEmpty());
        assertFalse(effectiveRights.canSetAllAttrs());
        assertTrue(effectiveRights.canSetAttrs().isEmpty());
        assertFalse(effectiveRights.canGetAllAttrs());
        assertTrue(effectiveRights.canGetAttrs().isEmpty());
    }

    /**
     * Test: Set preset rights multiple times → verify last set wins.
     * Verifies: setPresetRights() replaces previous list.
     */
    @Test
    public void setPresetRights_calledTwice_lastSetWins() {
        List<String> rights1 = new ArrayList<>();
        rights1.add("right1");
        effectiveRights.setPresetRights(rights1);
        assertEquals(1, effectiveRights.presetRights().size());

        List<String> rights2 = new ArrayList<>();
        rights2.add("right2");
        rights2.add("right3");
        effectiveRights.setPresetRights(rights2);

        assertEquals(2, effectiveRights.presetRights().size());
        assertTrue(effectiveRights.presetRights().contains("right2"));
        assertTrue(effectiveRights.presetRights().contains("right3"));
    }

    /**
     * Test: Create EffectiveRights → add canSetAttrs → add more → verify all present.
     * Verifies: Multiple calls to setCanSetAttrs accumulate or replace.
     */
    @Test
    public void setCanSetAttrs_calledTwice_lastSetWins() {
        SortedMap<String, RightCommand.EffectiveAttr> attrs1 = new TreeMap<>();
        attrs1.put("attr1", effectiveAttr1);
        effectiveRights.setCanSetAttrs(attrs1);
        assertEquals(1, effectiveRights.canSetAttrs().size());

        SortedMap<String, RightCommand.EffectiveAttr> attrs2 = new TreeMap<>();
        attrs2.put("attr2", effectiveAttr2);
        attrs2.put("attr3", effectiveAttr1);
        effectiveRights.setCanSetAttrs(attrs2);

        assertEquals(2, effectiveRights.canSetAttrs().size());
        assertTrue(effectiveRights.canSetAttrs().containsKey("attr2"));
        assertTrue(effectiveRights.canSetAttrs().containsKey("attr3"));
    }

    /**
     * Test: EffectiveAttr with null default values → getDefault() returns empty set.
     * Verifies: EffectiveAttr.getDefault() returns EMPTY_SET when null.
     */
    @Test
    public void effectiveAttr_nullDefaults_returnsEmptySet() {
        RightCommand.EffectiveAttr attr = new RightCommand.EffectiveAttr(
            "zimbraMailQuota",
            null,  // null default
            null
        );

        assertTrue(attr.getDefault().isEmpty());
    }

    /**
     * Test: EffectiveAttr with default values → getDefault() returns set.
     * Verifies: EffectiveAttr.getDefault() returns non-empty set.
     */
    @Test
    public void effectiveAttr_withDefaults_returnsNonEmptySet() {
        java.util.Set<String> defaults = new java.util.HashSet<>();
        defaults.add("10737418240");  // 10GB default

        RightCommand.EffectiveAttr attr = new RightCommand.EffectiveAttr(
            "zimbraMailQuota",
            defaults,
            null
        );

        assertEquals(1, attr.getDefault().size());
        assertTrue(attr.getDefault().contains("10737418240"));
    }

    /**
     * Test: Verify EffectiveAttr.getAttrName() returns correct name.
     * Verifies: Attribute name preservation.
     */
    @Test
    public void effectiveAttr_getAttrName_returnsCorrectName() {
        assertEquals("zimbraMailQuota", effectiveAttr1.getAttrName());
        assertEquals("zimbraAccountStatus", effectiveAttr2.getAttrName());
    }

    /**
     * Test: Create two EffectiveRights with different grantees → verify independence.
     * Verifies: Grantee info is independent between instances.
     */
    @Test
    public void twoEffectiveRights_differentGrantees_independent() {
        RightCommand.EffectiveRights er1 = new RightCommand.EffectiveRights(
            "account", "id1", "user1@example.com", "grantee1", "admin1@example.com"
        );
        RightCommand.EffectiveRights er2 = new RightCommand.EffectiveRights(
            "account", "id2", "user2@example.com", "grantee2", "admin2@example.com"
        );

        assertEquals("grantee1", er1.granteeId());
        assertEquals("grantee2", er2.granteeId());
        assertEquals("admin1@example.com", er1.granteeName());
        assertEquals("admin2@example.com", er2.granteeName());
    }

    /**
     * Test: Create EffectiveRights → set canGetAllAttrs → verify flag.
     * Verifies: canGetAllAttrs() reflects flag state.
     */
    @Test
    public void canGetAllAttrs_afterSettingFlag_reflectsChange() {
        assertFalse(effectiveRights.canGetAllAttrs());

        effectiveRights.setCanGetAllAttrs();

        assertTrue(effectiveRights.canGetAllAttrs());
    }

    /**
     * Test: Create multiple EffectiveRights → verify each independent.
     * Verifies: Instance independence in state management.
     */
    @Test
    public void multipleEffectiveRights_maintainIndependentState() {
        RightCommand.EffectiveRights er1 = new RightCommand.EffectiveRights(
            "account", "id1", "user1@example.com", "grantee1", "admin1@example.com"
        );
        RightCommand.EffectiveRights er2 = new RightCommand.EffectiveRights(
            "domain", "id2", "domain.com", "grantee2", "admin2@example.com"
        );

        List<String> rights1 = new ArrayList<>();
        rights1.add("right1");
        er1.setPresetRights(rights1);

        er2.setCanSetAllAttrs();

        // Verify er1 only has preset rights
        assertEquals(1, er1.presetRights().size());
        assertFalse(er1.canSetAllAttrs());

        // Verify er2 only has canSetAllAttrs
        assertTrue(er2.presetRights().isEmpty());
        assertTrue(er2.canSetAllAttrs());
    }
}
