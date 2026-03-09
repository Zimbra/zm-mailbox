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

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RightCommand} and its public/package-private inner classes.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@link RightCommand.Grants} – no-arg constructor and {@code getACEs()} behaviour.</li>
 *   <li>{@link RightCommand.EffectiveRights} – package-private constructor, initial field
 *       state, and {@code mPresetRights} list.</li>
 *   <li>{@link RightCommand.EffectiveAttr} – package-private constructor, accessor methods,
 *       and {@code getDefault()} when default is {@code null}.</li>
 * </ul>
 *
 * Methods that require LDAP, SOAP elements, or {@link com.zimbra.cs.account.AttributeManager}
 * (e.g. {@code fromJaxb_EffectiveRights}, {@code toXML}) are not exercised here.
 */
public class RightCommandTest {

    // ---------------------------------------------------------------
    // RightCommand class-level tests
    // ---------------------------------------------------------------

    @Test
    public void testRightCommand_isPublicClass() {
        assertTrue(java.lang.reflect.Modifier.isPublic(RightCommand.class.getModifiers()));
    }

    @Test
    public void testRightCommand_isConcreteClass() {
        assertFalse(java.lang.reflect.Modifier.isAbstract(RightCommand.class.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Grants – construction
    // ---------------------------------------------------------------

    @Test
    public void testGrants_newInstance_acesEmpty() {
        RightCommand.Grants grants = new RightCommand.Grants();
        assertTrue(grants.getACEs().isEmpty());
    }

    @Test
    public void testGrants_newInstance_getACEs_returnsNonNull() {
        RightCommand.Grants grants = new RightCommand.Grants();
        assertNotNull(grants.getACEs());
    }

    @Test
    public void testGrants_getACEs_returnsSet() {
        RightCommand.Grants grants = new RightCommand.Grants();
        assertNotNull(grants.getACEs());
        assertTrue(grants.getACEs() instanceof Set);
    }

    // ---------------------------------------------------------------
    // EffectiveRights – construction and initial state
    // ---------------------------------------------------------------

    @Test
    public void testEffectiveRights_constructor_storesTargetType() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "account", "id-1", "user@example.com", "grantee-id", "admin");
        assertEquals("account", er.mTargetType);
    }

    @Test
    public void testEffectiveRights_constructor_storesTargetName() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "domain", "id-2", "example.com", "grantee-id", "admin");
        assertEquals("example.com", er.mTargetName);
    }

    @Test
    public void testEffectiveRights_constructor_storesGranteeId() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "cos", "id-3", "defaultCOS", "g-id-99", "grantee-name");
        assertEquals("g-id-99", er.mGranteeId);
    }

    @Test
    public void testEffectiveRights_constructor_storesGranteeName() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "server", "srv-id", "mail.example.com", "grantee-id", "admin-name");
        assertEquals("admin-name", er.mGranteeName);
    }

    @Test
    public void testEffectiveRights_constructor_nullTargetId_storedAsEmpty() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "global", null, "global", "grantee-id", "admin");
        // null targetId is normalised to "" in the constructor
        assertEquals("", er.mTargetId);
    }

    @Test
    public void testEffectiveRights_constructor_presetRightsInitiallyEmpty() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "account", "id-1", "user@example.com", "grantee-id", "admin");
        assertTrue(er.mPresetRights.isEmpty());
    }

    @Test
    public void testEffectiveRights_constructor_canSetAllAttrs_initiallyFalse() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "account", "id-1", "user@example.com", "grantee-id", "admin");
        assertFalse(er.mCanSetAllAttrs);
    }

    @Test
    public void testEffectiveRights_constructor_canGetAllAttrs_initiallyFalse() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "account", "id-1", "user@example.com", "grantee-id", "admin");
        assertFalse(er.mCanGetAllAttrs);
    }

    @Test
    public void testEffectiveRights_constructor_canSetAttrs_initiallyEmpty() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "account", "id-1", "user@example.com", "grantee-id", "admin");
        assertTrue(er.mCanSetAttrs.isEmpty());
    }

    @Test
    public void testEffectiveRights_constructor_canGetAttrs_initiallyEmpty() {
        RightCommand.EffectiveRights er = new RightCommand.EffectiveRights(
                "account", "id-1", "user@example.com", "grantee-id", "admin");
        assertTrue(er.mCanGetAttrs.isEmpty());
    }

    // ---------------------------------------------------------------
    // EffectiveAttr – construction and accessors
    // ---------------------------------------------------------------

    @Test
    public void testEffectiveAttr_getAttrName_returnsStoredName() {
        RightCommand.EffectiveAttr ea =
                new RightCommand.EffectiveAttr("zimbraMailQuota", null, null);
        assertEquals("zimbraMailQuota", ea.getAttrName());
    }

    @Test
    public void testEffectiveAttr_getDefault_withNull_returnsEmptySet() {
        RightCommand.EffectiveAttr ea =
                new RightCommand.EffectiveAttr("zimbraMailQuota", null, null);
        Set<String> defaults = ea.getDefault();
        assertNotNull(defaults);
        assertTrue(defaults.isEmpty());
    }

    @Test
    public void testEffectiveAttr_getDefault_withValue_returnsProvidedSet() {
        Set<String> provided = new HashSet<String>();
        provided.add("10mb");
        RightCommand.EffectiveAttr ea =
                new RightCommand.EffectiveAttr("zimbraMailQuota", provided, null);
        assertTrue(ea.getDefault().contains("10mb"));
    }

    @Test
    public void testEffectiveAttr_getDefault_withMultipleValues_allReturned() {
        Set<String> provided = new HashSet<String>();
        provided.add("val1");
        provided.add("val2");
        RightCommand.EffectiveAttr ea =
                new RightCommand.EffectiveAttr("attr", provided, null);
        assertEquals(2, ea.getDefault().size());
    }

    @Test
    public void testEffectiveAttr_getConstraint_withNull_returnsNull() {
        RightCommand.EffectiveAttr ea =
                new RightCommand.EffectiveAttr("attr", null, null);
        assertNull(ea.getConstraint());
    }

    // ---------------------------------------------------------------
    // ACE – structural tests (constructors are private; check via class metadata)
    // ---------------------------------------------------------------

    @Test
    public void testACE_isPublicStaticInnerClass() {
        assertTrue(java.lang.reflect.Modifier.isPublic(RightCommand.ACE.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(RightCommand.ACE.class.getModifiers()));
    }

    @Test
    public void testACE_accessorMethods_exist() throws Exception {
        assertNotNull(RightCommand.ACE.class.getMethod("targetType"));
        assertNotNull(RightCommand.ACE.class.getMethod("targetId"));
        assertNotNull(RightCommand.ACE.class.getMethod("targetName"));
        assertNotNull(RightCommand.ACE.class.getMethod("granteeType"));
        assertNotNull(RightCommand.ACE.class.getMethod("granteeId"));
        assertNotNull(RightCommand.ACE.class.getMethod("granteeName"));
        assertNotNull(RightCommand.ACE.class.getMethod("right"));
        assertNotNull(RightCommand.ACE.class.getMethod("rightModifier"));
    }

    // ---------------------------------------------------------------
    // Grants – structural tests
    // ---------------------------------------------------------------

    @Test
    public void testGrants_isPublicStaticInnerClass() {
        assertTrue(java.lang.reflect.Modifier.isPublic(RightCommand.Grants.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(RightCommand.Grants.class.getModifiers()));
    }

    @Test
    public void testGrants_getACEs_isPublicMethod() throws Exception {
        java.lang.reflect.Method m = RightCommand.Grants.class.getMethod("getACEs");
        assertTrue(java.lang.reflect.Modifier.isPublic(m.getModifiers()));
    }

    // ---------------------------------------------------------------
    // EffectiveRights – structural tests
    // ---------------------------------------------------------------

    @Test
    public void testEffectiveRights_isPublicStaticInnerClass() {
        assertTrue(java.lang.reflect.Modifier.isPublic(
                RightCommand.EffectiveRights.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(
                RightCommand.EffectiveRights.class.getModifiers()));
    }
}
