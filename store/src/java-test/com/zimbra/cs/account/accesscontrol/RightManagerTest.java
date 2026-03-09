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
 * Unit tests for {@link RightManager}.
 *
 * {@code RightManager.getInstance()} reads rights XML files from the file system
 * (configured via {@code LC.zimbra_rights_directory}) and is therefore not
 * exercisable in a pure unit-test context without a Zimbra installation.
 *
 * This class verifies:
 * <ul>
 *   <li>Class-level structural properties of {@code RightManager}.</li>
 *   <li>The presence and access-modifier contracts of the public API methods.</li>
 *   <li>The private {@code CoreRightDefFiles} inner class structural contract.</li>
 * </ul>
 */
public class RightManagerTest {

    // ---------------------------------------------------------------
    // Class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testRightManager_isPublicClass() {
        assertTrue(Modifier.isPublic(RightManager.class.getModifiers()));
    }

    @Test
    public void testRightManager_isConcreteClass() {
        assertFalse(Modifier.isAbstract(RightManager.class.getModifiers()));
        assertFalse(RightManager.class.isInterface());
    }

    @Test
    public void testRightManager_isNotEnum() {
        assertFalse(RightManager.class.isEnum());
    }

    // ---------------------------------------------------------------
    // getInstance() – static factory method signatures
    // ---------------------------------------------------------------

    @Test
    public void testGetInstance_noArgOverload_isPublicAndStatic() throws Exception {
        Method m = RightManager.class.getMethod("getInstance");
        assertNotNull(m);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertTrue(Modifier.isSynchronized(m.getModifiers()));
    }

    @Test
    public void testGetInstance_booleanOverload_isPublicAndStatic() throws Exception {
        Method m = RightManager.class.getMethod("getInstance", boolean.class);
        assertNotNull(m);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetInstance_returnType_isRightManager() throws Exception {
        Method m = RightManager.class.getMethod("getInstance");
        assertEquals(RightManager.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // CoreRightDefFiles inner class – structural tests
    // ---------------------------------------------------------------

    @Test
    public void testCoreRightDefFiles_innerClassExists() {
        Class<?>[] inners = RightManager.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("CoreRightDefFiles".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("CoreRightDefFiles inner class must exist", found);
    }

    @Test
    public void testCoreRightDefFiles_isPrivate() throws Exception {
        Class<?> coreFiles = null;
        for (Class<?> c : RightManager.class.getDeclaredClasses()) {
            if ("CoreRightDefFiles".equals(c.getSimpleName())) {
                coreFiles = c;
                break;
            }
        }
        assertNotNull(coreFiles);
        assertTrue(Modifier.isPrivate(coreFiles.getModifiers()));
    }

    @Test
    public void testCoreRightDefFiles_isConcrete() throws Exception {
        Class<?> coreFiles = null;
        for (Class<?> c : RightManager.class.getDeclaredClasses()) {
            if ("CoreRightDefFiles".equals(c.getSimpleName())) {
                coreFiles = c;
                break;
            }
        }
        assertNotNull(coreFiles);
        assertFalse(Modifier.isAbstract(coreFiles.getModifiers()));
    }

    @Test
    public void testCoreRightDefFiles_isCoreRightFile_methodExists() throws Exception {
        Class<?> coreFiles = null;
        for (Class<?> c : RightManager.class.getDeclaredClasses()) {
            if ("CoreRightDefFiles".equals(c.getSimpleName())) {
                coreFiles = c;
                break;
            }
        }
        assertNotNull(coreFiles);
//        coreFiles.setAccessible(true);
        Method m = coreFiles.getDeclaredMethod("isCoreRightFile", java.io.File.class);
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(boolean.class, m.getReturnType());
    }

    @Test
    public void testCoreRightDefFiles_listCoreDefFiles_methodExists() throws Exception {
        Class<?> coreFiles = null;
        for (Class<?> c : RightManager.class.getDeclaredClasses()) {
            if ("CoreRightDefFiles".equals(c.getSimpleName())) {
                coreFiles = c;
                break;
            }
        }
        assertNotNull(coreFiles);
//        coreFiles.setAccessible(true);
        Method m = coreFiles.getDeclaredMethod("listCoreDefFiles");
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(String.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // Internal maps – structural confirmation via declared fields
    // ---------------------------------------------------------------

    @Test
    public void testRightManager_hasUserRightsField() throws Exception {
        java.lang.reflect.Field f = RightManager.class.getDeclaredField("sUserRights");
        assertNotNull(f);
        assertFalse(Modifier.isStatic(f.getModifiers())); // instance field
    }

    @Test
    public void testRightManager_hasAdminRightsField() throws Exception {
        java.lang.reflect.Field f = RightManager.class.getDeclaredField("sAdminRights");
        assertNotNull(f);
        assertFalse(Modifier.isStatic(f.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Additional method signature tests
    // ---------------------------------------------------------------

    @Test
    public void testGetAllUserRights_methodExists() throws Exception {
        Method m = RightManager.class.getMethod("getAllUserRights");
        assertNotNull(m);
        assertFalse(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetAllAdminRights_methodExists() throws Exception {
        Method m = RightManager.class.getMethod("getAllAdminRights");
        assertNotNull(m);
        assertFalse(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetInstance_noArgOverload_returnType() throws Exception {
        Method m = RightManager.class.getMethod("getInstance");
        assertEquals(RightManager.class, m.getReturnType());
    }

    @Test
    public void testGetInstance_booleanOverload_returnType() throws Exception {
        Method m = RightManager.class.getMethod("getInstance", boolean.class);
        assertEquals(RightManager.class, m.getReturnType());
    }
}
