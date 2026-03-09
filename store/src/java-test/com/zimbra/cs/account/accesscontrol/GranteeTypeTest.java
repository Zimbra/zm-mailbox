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

import com.zimbra.common.service.ServiceException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GranteeType}.
 */
public class GranteeTypeTest {

    // ---------------------------------------------------------------
    // fromCode
    // ---------------------------------------------------------------

    @Test
    public void testFromCode_usr() throws ServiceException {
        assertEquals(GranteeType.GT_USER, GranteeType.fromCode("usr"));
    }

    @Test
    public void testFromCode_grp() throws ServiceException {
        assertEquals(GranteeType.GT_GROUP, GranteeType.fromCode("grp"));
    }

    @Test
    public void testFromCode_egp() throws ServiceException {
        assertEquals(GranteeType.GT_EXT_GROUP, GranteeType.fromCode("egp"));
    }

    @Test
    public void testFromCode_all() throws ServiceException {
        assertEquals(GranteeType.GT_AUTHUSER, GranteeType.fromCode("all"));
    }

    @Test
    public void testFromCode_dom() throws ServiceException {
        assertEquals(GranteeType.GT_DOMAIN, GranteeType.fromCode("dom"));
    }

    @Test
    public void testFromCode_edom() throws ServiceException {
        assertEquals(GranteeType.GT_EXT_DOMAIN, GranteeType.fromCode("edom"));
    }

    @Test
    public void testFromCode_gst() throws ServiceException {
        assertEquals(GranteeType.GT_GUEST, GranteeType.fromCode("gst"));
    }

    @Test
    public void testFromCode_key() throws ServiceException {
        assertEquals(GranteeType.GT_KEY, GranteeType.fromCode("key"));
    }

    @Test
    public void testFromCode_pub() throws ServiceException {
        assertEquals(GranteeType.GT_PUBLIC, GranteeType.fromCode("pub"));
    }

    @Test
    public void testFromCode_email() throws ServiceException {
        assertEquals(GranteeType.GT_EMAIL, GranteeType.fromCode("email"));
    }

    @Test(expected = ServiceException.class)
    public void testFromCode_invalid() throws ServiceException {
        GranteeType.fromCode("invalid");
    }

    @Test(expected = ServiceException.class)
    public void testFromCode_empty() throws ServiceException {
        GranteeType.fromCode("");
    }

    @Test(expected = ServiceException.class)
    public void testFromCode_null() throws ServiceException {
        GranteeType.fromCode(null);
    }

    // ---------------------------------------------------------------
    // fromJaxb
    // ---------------------------------------------------------------

    @Test
    public void testFromJaxb_usr() {
        assertEquals(GranteeType.GT_USER, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.usr));
    }

    @Test
    public void testFromJaxb_grp() {
        assertEquals(GranteeType.GT_GROUP, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.grp));
    }

    @Test
    public void testFromJaxb_egp() {
        assertEquals(GranteeType.GT_EXT_GROUP, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.egp));
    }

    @Test
    public void testFromJaxb_all() {
        assertEquals(GranteeType.GT_AUTHUSER, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.all));
    }

    @Test
    public void testFromJaxb_dom() {
        assertEquals(GranteeType.GT_DOMAIN, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.dom));
    }

    @Test
    public void testFromJaxb_edom() {
        assertEquals(GranteeType.GT_EXT_DOMAIN, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.edom));
    }

    @Test
    public void testFromJaxb_gst() {
        assertEquals(GranteeType.GT_GUEST, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.gst));
    }

    @Test
    public void testFromJaxb_key() {
        assertEquals(GranteeType.GT_KEY, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.key));
    }

    @Test
    public void testFromJaxb_pub() {
        assertEquals(GranteeType.GT_PUBLIC, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.pub));
    }

    @Test
    public void testFromJaxb_email() {
        assertEquals(GranteeType.GT_EMAIL, GranteeType.fromJaxb(com.zimbra.soap.type.GranteeType.email));
    }

    // ---------------------------------------------------------------
    // toJaxb
    // ---------------------------------------------------------------

    @Test
    public void testToJaxb_roundTrip() {
        for (GranteeType gt : GranteeType.values()) {
            assertEquals(gt, GranteeType.fromJaxb(gt.toJaxb()));
        }
    }

    // ---------------------------------------------------------------
    // getCode
    // ---------------------------------------------------------------

    @Test
    public void testGetCode_allTypes() throws ServiceException {
        for (GranteeType gt : GranteeType.values()) {
            // round-trip: fromCode(getCode()) should return the same enum
            assertEquals(gt, GranteeType.fromCode(gt.getCode()));
        }
    }

    @Test
    public void testGetCode_specificValues() {
        assertEquals("usr",   GranteeType.GT_USER.getCode());
        assertEquals("grp",   GranteeType.GT_GROUP.getCode());
        assertEquals("egp",   GranteeType.GT_EXT_GROUP.getCode());
        assertEquals("all",   GranteeType.GT_AUTHUSER.getCode());
        assertEquals("dom",   GranteeType.GT_DOMAIN.getCode());
        assertEquals("edom",  GranteeType.GT_EXT_DOMAIN.getCode());
        assertEquals("gst",   GranteeType.GT_GUEST.getCode());
        assertEquals("key",   GranteeType.GT_KEY.getCode());
        assertEquals("pub",   GranteeType.GT_PUBLIC.getCode());
        assertEquals("email", GranteeType.GT_EMAIL.getCode());
    }

    // ---------------------------------------------------------------
    // allowedForAdminRights
    // ---------------------------------------------------------------

    @Test
    public void testAllowedForAdminRights_trueTypes() {
        assertTrue(GranteeType.GT_USER.allowedForAdminRights());
        assertTrue(GranteeType.GT_GROUP.allowedForAdminRights());
        assertTrue(GranteeType.GT_EXT_GROUP.allowedForAdminRights());
        assertTrue(GranteeType.GT_DOMAIN.allowedForAdminRights());
    }

    @Test
    public void testAllowedForAdminRights_falseTypes() {
        assertFalse(GranteeType.GT_AUTHUSER.allowedForAdminRights());
        assertFalse(GranteeType.GT_EXT_DOMAIN.allowedForAdminRights());
        assertFalse(GranteeType.GT_GUEST.allowedForAdminRights());
        assertFalse(GranteeType.GT_KEY.allowedForAdminRights());
        assertFalse(GranteeType.GT_PUBLIC.allowedForAdminRights());
        assertFalse(GranteeType.GT_EMAIL.allowedForAdminRights());
    }

    // ---------------------------------------------------------------
    // allowSecret
    // ---------------------------------------------------------------

    @Test
    public void testAllowSecret_trueTypes() {
        assertTrue(GranteeType.GT_GUEST.allowSecret());
        assertTrue(GranteeType.GT_KEY.allowSecret());
    }

    @Test
    public void testAllowSecret_falseTypes() {
        assertFalse(GranteeType.GT_USER.allowSecret());
        assertFalse(GranteeType.GT_GROUP.allowSecret());
        assertFalse(GranteeType.GT_EXT_GROUP.allowSecret());
        assertFalse(GranteeType.GT_AUTHUSER.allowSecret());
        assertFalse(GranteeType.GT_DOMAIN.allowSecret());
        assertFalse(GranteeType.GT_EXT_DOMAIN.allowSecret());
        assertFalse(GranteeType.GT_PUBLIC.allowSecret());
        assertFalse(GranteeType.GT_EMAIL.allowSecret());
    }

    // ---------------------------------------------------------------
    // isZimbraEntry
    // ---------------------------------------------------------------

    @Test
    public void testIsZimbraEntry_trueTypes() {
        assertTrue(GranteeType.GT_USER.isZimbraEntry());
        assertTrue(GranteeType.GT_GROUP.isZimbraEntry());
        assertTrue(GranteeType.GT_DOMAIN.isZimbraEntry());
    }

    @Test
    public void testIsZimbraEntry_falseTypes() {
        assertFalse(GranteeType.GT_EXT_GROUP.isZimbraEntry());
        assertFalse(GranteeType.GT_AUTHUSER.isZimbraEntry());
        assertFalse(GranteeType.GT_EXT_DOMAIN.isZimbraEntry());
        assertFalse(GranteeType.GT_GUEST.isZimbraEntry());
        assertFalse(GranteeType.GT_KEY.isZimbraEntry());
        assertFalse(GranteeType.GT_PUBLIC.isZimbraEntry());
        assertFalse(GranteeType.GT_EMAIL.isZimbraEntry());
    }

    // ---------------------------------------------------------------
    // needsGranteeIdentity
    // ---------------------------------------------------------------

    @Test
    public void testNeedsGranteeIdentity_falseForAuthUserAndPublic() {
        assertFalse(GranteeType.GT_AUTHUSER.needsGranteeIdentity());
        assertFalse(GranteeType.GT_PUBLIC.needsGranteeIdentity());
    }

    @Test
    public void testNeedsGranteeIdentity_trueForOthers() {
        assertTrue(GranteeType.GT_USER.needsGranteeIdentity());
        assertTrue(GranteeType.GT_GROUP.needsGranteeIdentity());
        assertTrue(GranteeType.GT_EXT_GROUP.needsGranteeIdentity());
        assertTrue(GranteeType.GT_DOMAIN.needsGranteeIdentity());
        assertTrue(GranteeType.GT_EXT_DOMAIN.needsGranteeIdentity());
        assertTrue(GranteeType.GT_GUEST.needsGranteeIdentity());
        assertTrue(GranteeType.GT_KEY.needsGranteeIdentity());
        assertTrue(GranteeType.GT_EMAIL.needsGranteeIdentity());
    }

    // ---------------------------------------------------------------
    // hasFlags
    // ---------------------------------------------------------------

    @Test
    public void testHasFlags_adminFlag() {
        assertTrue(GranteeType.GT_USER.hasFlags(GranteeFlag.F_ADMIN));
        assertTrue(GranteeType.GT_GROUP.hasFlags(GranteeFlag.F_ADMIN));
        assertFalse(GranteeType.GT_AUTHUSER.hasFlags(GranteeFlag.F_ADMIN));
        assertFalse(GranteeType.GT_PUBLIC.hasFlags(GranteeFlag.F_ADMIN));
    }

    @Test
    public void testHasFlags_individualFlag() {
        assertTrue(GranteeType.GT_USER.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertTrue(GranteeType.GT_EXT_DOMAIN.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertTrue(GranteeType.GT_GUEST.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertTrue(GranteeType.GT_KEY.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertFalse(GranteeType.GT_GROUP.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertFalse(GranteeType.GT_AUTHUSER.hasFlags(GranteeFlag.F_INDIVIDUAL));
        assertFalse(GranteeType.GT_PUBLIC.hasFlags(GranteeFlag.F_INDIVIDUAL));
    }

    @Test
    public void testHasFlags_groupFlag() {
        assertTrue(GranteeType.GT_GROUP.hasFlags(GranteeFlag.F_GROUP));
        assertTrue(GranteeType.GT_EXT_GROUP.hasFlags(GranteeFlag.F_GROUP));
        assertFalse(GranteeType.GT_USER.hasFlags(GranteeFlag.F_GROUP));
        assertFalse(GranteeType.GT_DOMAIN.hasFlags(GranteeFlag.F_GROUP));
    }

    @Test
    public void testHasFlags_domainFlag() {
        assertTrue(GranteeType.GT_DOMAIN.hasFlags(GranteeFlag.F_DOMAIN));
        assertFalse(GranteeType.GT_USER.hasFlags(GranteeFlag.F_DOMAIN));
    }

    @Test
    public void testHasFlags_publicFlag() {
        assertTrue(GranteeType.GT_PUBLIC.hasFlags(GranteeFlag.F_PUBLIC));
        assertFalse(GranteeType.GT_AUTHUSER.hasFlags(GranteeFlag.F_PUBLIC));
        assertFalse(GranteeType.GT_USER.hasFlags(GranteeFlag.F_PUBLIC));
    }

    @Test
    public void testHasFlags_authUserFlag() {
        assertTrue(GranteeType.GT_AUTHUSER.hasFlags(GranteeFlag.F_AUTHUSER));
        assertFalse(GranteeType.GT_PUBLIC.hasFlags(GranteeFlag.F_AUTHUSER));
        assertFalse(GranteeType.GT_USER.hasFlags(GranteeFlag.F_AUTHUSER));
    }

    @Test
    public void testHasFlags_hasSecretFlag() {
        assertTrue(GranteeType.GT_GUEST.hasFlags(GranteeFlag.F_HAS_SECRET));
        assertTrue(GranteeType.GT_KEY.hasFlags(GranteeFlag.F_HAS_SECRET));
        assertFalse(GranteeType.GT_USER.hasFlags(GranteeFlag.F_HAS_SECRET));
        assertFalse(GranteeType.GT_GROUP.hasFlags(GranteeFlag.F_HAS_SECRET));
    }

    @Test
    public void testHasFlags_isZimbraEntryFlag() {
        assertTrue(GranteeType.GT_USER.hasFlags(GranteeFlag.F_IS_ZIMBRA_ENTRY));
        assertTrue(GranteeType.GT_GROUP.hasFlags(GranteeFlag.F_IS_ZIMBRA_ENTRY));
        assertTrue(GranteeType.GT_DOMAIN.hasFlags(GranteeFlag.F_IS_ZIMBRA_ENTRY));
        assertFalse(GranteeType.GT_EXT_GROUP.hasFlags(GranteeFlag.F_IS_ZIMBRA_ENTRY));
        assertFalse(GranteeType.GT_GUEST.hasFlags(GranteeFlag.F_IS_ZIMBRA_ENTRY));
    }

    @Test
    public void testHasFlags_combinedFlags() {
        // GT_USER has F_ADMIN | F_INDIVIDUAL | F_IS_ZIMBRA_ENTRY
        short combined = (short)(GranteeFlag.F_ADMIN | GranteeFlag.F_INDIVIDUAL);
        assertTrue(GranteeType.GT_USER.hasFlags(combined));

        // GT_AUTHUSER does NOT have F_ADMIN
        assertFalse(GranteeType.GT_AUTHUSER.hasFlags(combined));
    }
}
