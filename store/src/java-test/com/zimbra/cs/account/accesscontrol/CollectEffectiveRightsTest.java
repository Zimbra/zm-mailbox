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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CollectEffectiveRights}.
 *
 * The static entry-points {@code getEffectiveRights(...)} require a fully
 * wired {@link com.zimbra.cs.account.Provisioning} and {@link RightBearer};
 * neither is available in a pure unit-test context.
 *
 * This class therefore verifies the structural contract of
 * {@code CollectEffectiveRights}: the expected static methods are present
 * with the correct signatures and access modifiers.
 */
public class CollectEffectiveRightsTest {

    // ---------------------------------------------------------------
    // Class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testCollectEffectiveRights_isConcreteClass() {
        assertFalse(Modifier.isAbstract(CollectEffectiveRights.class.getModifiers()));
        assertFalse(CollectEffectiveRights.class.isInterface());
    }

    @Test
    public void testCollectEffectiveRights_isPublicClass() {
        assertTrue(Modifier.isPublic(CollectEffectiveRights.class.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Static method signatures
    // ---------------------------------------------------------------

    @Test
    public void testGetEffectiveRights_fiveArgVariant_exists() throws Exception {
        // getEffectiveRights(RightBearer, Entry, TargetType, boolean, boolean, EffectiveRights)
        Method m = CollectEffectiveRights.class.getDeclaredMethod(
                "getEffectiveRights",
                RightBearer.class,
                com.zimbra.cs.account.Entry.class,
                TargetType.class,
                boolean.class,
                boolean.class,
                RightCommand.EffectiveRights.class);
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetEffectiveRights_fourArgVariant_exists() throws Exception {
        // getEffectiveRights(RightBearer, Entry, boolean, boolean, EffectiveRights)
        Method m = CollectEffectiveRights.class.getDeclaredMethod(
                "getEffectiveRights",
                RightBearer.class,
                com.zimbra.cs.account.Entry.class,
                boolean.class,
                boolean.class,
                RightCommand.EffectiveRights.class);
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetEffectiveRights_fiveArgVariant_returnTypeIsVoid() throws Exception {
        Method m = CollectEffectiveRights.class.getDeclaredMethod(
                "getEffectiveRights",
                RightBearer.class,
                com.zimbra.cs.account.Entry.class,
                TargetType.class,
                boolean.class,
                boolean.class,
                RightCommand.EffectiveRights.class);
        assertEquals(void.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // Additional structural verifications
    // ---------------------------------------------------------------

    @Test
    public void testGetEffectiveRights_fourArgVariant_returnTypeIsVoid() throws Exception {
        Method m = CollectEffectiveRights.class.getDeclaredMethod(
                "getEffectiveRights",
                RightBearer.class,
                com.zimbra.cs.account.Entry.class,
                boolean.class,
                boolean.class,
                RightCommand.EffectiveRights.class);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void testCollectEffectiveRights_isNotEnum() {
        assertFalse(CollectEffectiveRights.class.isEnum());
    }

    @Test
    public void testCollectEffectiveRights_isNotInterface() {
        assertFalse(CollectEffectiveRights.class.isInterface());
    }
}
