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
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ParticallyDenied}.
 *
 * All methods in {@code ParticallyDenied} require a live LDAP provisioning
 * environment ({@link com.zimbra.cs.account.Provisioning}, active grants,
 * group membership queries, etc.) so they cannot be exercised in a pure
 * unit-test context.
 *
 * This class therefore verifies the structural contract of
 * {@code ParticallyDenied}: access modifiers, method existence, and
 * class-level properties.
 */
public class ParticallyDeniedTest {

    // ---------------------------------------------------------------
    // Class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testParticallyDenied_isPublicClass() {
        assertTrue(Modifier.isPublic(ParticallyDenied.class.getModifiers()));
    }

    @Test
    public void testParticallyDenied_isConcreteClass() {
        assertFalse(Modifier.isAbstract(ParticallyDenied.class.getModifiers()));
        assertFalse(ParticallyDenied.class.isInterface());
    }

    @Test
    public void testParticallyDenied_isNotEnum() {
        assertFalse(ParticallyDenied.class.isEnum());
    }

    // ---------------------------------------------------------------
    // checkPartiallyDenied method – structural contract
    // ---------------------------------------------------------------

    @Test
    public void testCheckPartiallyDenied_methodExists() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "checkPartiallyDenied",
                com.zimbra.cs.account.Account.class,
                TargetType.class,
                com.zimbra.cs.account.Entry.class,
                Right.class);
        assertNotNull(m);
    }

    @Test
    public void testCheckPartiallyDenied_isStatic() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "checkPartiallyDenied",
                com.zimbra.cs.account.Account.class,
                TargetType.class,
                com.zimbra.cs.account.Entry.class,
                Right.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testCheckPartiallyDenied_returnTypeIsVoid() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "checkPartiallyDenied",
                com.zimbra.cs.account.Account.class,
                TargetType.class,
                com.zimbra.cs.account.Entry.class,
                Right.class);
        assertEquals(void.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // getAllGrantableTargetTypes – private static helper
    // ---------------------------------------------------------------

    @Test
    public void testGetAllGrantableTargetTypes_methodExists() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes",
                Right.class,
                java.util.Set.class);
        assertNotNull(m);
    }

    @Test
    public void testGetAllGrantableTargetTypes_isPrivateAndStatic() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes",
                Right.class,
                java.util.Set.class);
        assertTrue(Modifier.isPrivate(m.getModifiers()));
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetAllGrantableTargetTypes_returnTypeIsVoid() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes",
                Right.class,
                java.util.Set.class);
        assertEquals(void.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // checkPartiallyDenied parameter types
    // ---------------------------------------------------------------

    @Test
    public void testCheckPartiallyDenied_parameterTypes() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "checkPartiallyDenied",
                com.zimbra.cs.account.Account.class,
                TargetType.class,
                com.zimbra.cs.account.Entry.class,
                Right.class);
        Class<?>[] params = m.getParameterTypes();
        assertEquals(4, params.length);
        assertEquals(com.zimbra.cs.account.Account.class, params[0]);
        assertEquals(TargetType.class, params[1]);
        assertEquals(com.zimbra.cs.account.Entry.class, params[2]);
        assertEquals(Right.class, params[3]);
    }

    @Test
    public void testGetAllGrantableTargetTypes_parameterTypes() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes",
                Right.class,
                java.util.Set.class);
        Class<?>[] params = m.getParameterTypes();
        assertEquals(2, params.length);
        assertEquals(Right.class, params[0]);
        assertEquals(java.util.Set.class, params[1]);
    }

    // ---------------------------------------------------------------
    // getAllGrantableTargetTypes — functional tests via reflection
    // ---------------------------------------------------------------

    /**
     * A preset right with known grantable target types → result contains those types.
     */
    @Test
    public void testGetAllGrantableTargetTypes_presetRight_addsGrantableTypes() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes", Right.class, Set.class);
        m.setAccessible(true);

        Set<TargetType> grantable = new HashSet<>(Arrays.asList(TargetType.account, TargetType.dl));
        Right mockPreset = Mockito.mock(Right.class);
        Mockito.when(mockPreset.isPresetRight()).thenReturn(true);
        Mockito.when(mockPreset.isAttrRight()).thenReturn(false);
        Mockito.when(mockPreset.isComboRight()).thenReturn(false);
        Mockito.when(mockPreset.getGrantableTargetTypes()).thenReturn(grantable);

        Set<TargetType> result = new HashSet<>();
        m.invoke(null, mockPreset, result);

        assertEquals(2, result.size());
        assertTrue(result.contains(TargetType.account));
        assertTrue(result.contains(TargetType.dl));
    }

    /**
     * An attr right → result contains its grantable types.
     */
    @Test
    public void testGetAllGrantableTargetTypes_attrRight_addsGrantableTypes() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes", Right.class, Set.class);
        m.setAccessible(true);

        Set<TargetType> grantable = new HashSet<>(Arrays.asList(TargetType.domain));
        Right mockAttr = Mockito.mock(Right.class);
        Mockito.when(mockAttr.isPresetRight()).thenReturn(false);
        Mockito.when(mockAttr.isAttrRight()).thenReturn(true);
        Mockito.when(mockAttr.isComboRight()).thenReturn(false);
        Mockito.when(mockAttr.getGrantableTargetTypes()).thenReturn(grantable);

        Set<TargetType> result = new HashSet<>();
        m.invoke(null, mockAttr, result);

        assertEquals(1, result.size());
        assertTrue(result.contains(TargetType.domain));
    }

    /**
     * An empty ComboRight (no sub-rights) → result stays empty.
     */
    @Test
    public void testGetAllGrantableTargetTypes_comboRightNoSubRights_emptyResult() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes", Right.class, Set.class);
        m.setAccessible(true);

        ComboRight combo = new ComboRight("emptyCombo");
        Set<TargetType> result = new HashSet<>();
        m.invoke(null, combo, result);

        assertTrue("Empty combo right should produce empty result", result.isEmpty());
    }

    /**
     * A right that is neither preset, attr, nor combo → result stays empty.
     */
    @Test
    public void testGetAllGrantableTargetTypes_unknownRightType_emptyResult() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes", Right.class, Set.class);
        m.setAccessible(true);

        Right mockUnknown = Mockito.mock(Right.class);
        Mockito.when(mockUnknown.isPresetRight()).thenReturn(false);
        Mockito.when(mockUnknown.isAttrRight()).thenReturn(false);
        Mockito.when(mockUnknown.isComboRight()).thenReturn(false);

        Set<TargetType> result = new HashSet<>();
        m.invoke(null, mockUnknown, result);

        assertTrue("Unknown right type should produce empty result", result.isEmpty());
    }

    /**
     * A preset right that returns an empty grantable set → result stays empty.
     */
    @Test
    public void testGetAllGrantableTargetTypes_presetRightEmptyGrantable_emptyResult() throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes", Right.class, Set.class);
        m.setAccessible(true);

        Right mockPreset = Mockito.mock(Right.class);
        Mockito.when(mockPreset.isPresetRight()).thenReturn(true);
        Mockito.when(mockPreset.isAttrRight()).thenReturn(false);
        Mockito.when(mockPreset.isComboRight()).thenReturn(false);
        Mockito.when(mockPreset.getGrantableTargetTypes()).thenReturn(new HashSet<TargetType>());

        Set<TargetType> result = new HashSet<>();
        m.invoke(null, mockPreset, result);

        assertTrue("Preset right with no grantable types should produce empty result", result.isEmpty());
    }
}
