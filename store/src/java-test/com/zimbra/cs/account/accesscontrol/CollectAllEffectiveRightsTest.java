/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CollectAllEffectiveRights}.
 *
 * The top-level class methods all require a live LDAP provisioning environment.
 * This test class focuses on the public static inner class
 * {@link CollectAllEffectiveRights.GroupShape}, which is fully in-memory.
 */
public class CollectAllEffectiveRightsTest {

    // ---------------------------------------------------------------
    // GroupShape – construction
    // ---------------------------------------------------------------

    @Test
    public void testGroupShape_newInstance_groupsEmpty() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        assertTrue(shape.getGroups().isEmpty());
    }

    @Test
    public void testGroupShape_newInstance_membersEmpty() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        assertTrue(shape.getMembers().isEmpty());
    }

    // ---------------------------------------------------------------
    // GroupShape – getGroups / getMembers (mutable fields)
    // ---------------------------------------------------------------

    @Test
    public void testGroupShape_addGroupViaField_visibleViaGetGroups() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mGroups.add("group-a");
        assertTrue(shape.getGroups().contains("group-a"));
    }

    @Test
    public void testGroupShape_addMemberViaField_visibleViaGetMembers() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mMembers.add("member-1");
        assertTrue(shape.getMembers().contains("member-1"));
    }

    @Test
    public void testGroupShape_multipleGroups_allVisible() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mGroups.add("grp-1");
        shape.mGroups.add("grp-2");
        shape.mGroups.add("grp-3");
        Set<String> groups = shape.getGroups();
        assertEquals(3, groups.size());
        assertTrue(groups.contains("grp-1"));
        assertTrue(groups.contains("grp-2"));
        assertTrue(groups.contains("grp-3"));
    }

    @Test
    public void testGroupShape_multipleMembers_allVisible() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mMembers.add("mem-a");
        shape.mMembers.add("mem-b");
        Set<String> members = shape.getMembers();
        assertEquals(2, members.size());
        assertTrue(members.contains("mem-a"));
        assertTrue(members.contains("mem-b"));
    }

    // ---------------------------------------------------------------
    // GroupShape – toString
    // ---------------------------------------------------------------

    @Test
    public void testGroupShape_toString_emptyShape_returnsEmptyString() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        assertEquals("", shape.toString());
    }

    @Test
    public void testGroupShape_toString_withGroup_containsGroupLabel() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mGroups.add("group-xyz");
        String str = shape.toString();
        assertTrue(str.contains("group-xyz"));
        assertTrue(str.contains("group "));
    }

    @Test
    public void testGroupShape_toString_withMember_containsMemberLabel() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mMembers.add("member-xyz");
        String str = shape.toString();
        assertTrue(str.contains("member-xyz"));
        assertTrue(str.contains("member "));
    }

    @Test
    public void testGroupShape_toString_withGroupAndMember_containsBoth() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mGroups.add("grp-1");
        shape.mMembers.add("mem-1");
        String str = shape.toString();
        assertTrue(str.contains("grp-1"));
        assertTrue(str.contains("mem-1"));
    }

    // ---------------------------------------------------------------
    // GroupShape – set semantics (no duplicates)
    // ---------------------------------------------------------------

    @Test
    public void testGroupShape_addDuplicateGroup_countRemainsOne() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mGroups.add("dup");
        shape.mGroups.add("dup");
        assertEquals(1, shape.getGroups().size());
    }

    @Test
    public void testGroupShape_addDuplicateMember_countRemainsOne() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        shape.mMembers.add("dup");
        shape.mMembers.add("dup");
        assertEquals(1, shape.getMembers().size());
    }

    // ---------------------------------------------------------------
    // GroupShape – getGroups / getMembers return the live backing set
    // ---------------------------------------------------------------

    @Test
    public void testGroupShape_getGroups_returnsSameReference() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        assertSame(shape.mGroups, shape.getGroups());
    }

    @Test
    public void testGroupShape_getMembers_returnsSameReference() {
        CollectAllEffectiveRights.GroupShape shape = new CollectAllEffectiveRights.GroupShape();
        assertSame(shape.mMembers, shape.getMembers());
    }
}
