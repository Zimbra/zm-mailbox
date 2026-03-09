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
import com.zimbra.cs.account.GuestAccount;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZimbraACE} and {@link ZimbraACE.ExternalGroupInfo}.
 */
public class ZimbraACETest {

    private static final String TEST_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    // helper to create a mock Right with a given name
    private static Right mockRight(String name) {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getName()).thenReturn(name);
        return r;
    }

    // ---------------------------------------------------------------
    // Constructor — GT_USER (granteeId stored as-is)
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_GT_USER_granteeStoredAsIs() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);

        assertEquals(TEST_UUID,           ace.getGrantee());
        assertEquals(GranteeType.GT_USER, ace.getGranteeType());
        assertSame(r,                      ace.getRight());
        assertNull(ace.getSecret());
        assertNull(ace.getRightModifier());
    }

    // ---------------------------------------------------------------
    // Constructor — GT_AUTHUSER always stores GUID_AUTHUSER
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_GT_AUTHUSER_setsGuidAuthUser() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE ace = new ZimbraACE("anything", GranteeType.GT_AUTHUSER, r, null, null);

        assertEquals(GuestAccount.GUID_AUTHUSER, ace.getGrantee());
        assertEquals(GranteeType.GT_AUTHUSER,    ace.getGranteeType());
    }

    // ---------------------------------------------------------------
    // Constructor — GT_PUBLIC always stores GUID_PUBLIC
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_GT_PUBLIC_setsGuidPublic() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE ace = new ZimbraACE("anything", GranteeType.GT_PUBLIC, r, null, null);

        assertEquals(GuestAccount.GUID_PUBLIC, ace.getGrantee());
        assertEquals(GranteeType.GT_PUBLIC,    ace.getGranteeType());
    }

    // ---------------------------------------------------------------
    // Constructor — secret is stored for GT_GUEST
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_GT_GUEST_secretStored() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE("guest@example.com", GranteeType.GT_GUEST, r, null, "s3cr3t");

        assertEquals("guest@example.com", ace.getGrantee());
        assertEquals("s3cr3t",             ace.getSecret());
        assertEquals(GranteeType.GT_GUEST, ace.getGranteeType());
    }

    @Test
    public void testConstructor_GT_GUEST_nullSecret() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE("guest@example.com", GranteeType.GT_GUEST, r, null, null);
        assertNull(ace.getSecret());
    }

    // ---------------------------------------------------------------
    // isGrantee
    // ---------------------------------------------------------------

    @Test
    public void testIsGrantee_null_returnsTrueOnlyForGT_PUBLIC() throws ServiceException {
        ZimbraACE pubAce  = new ZimbraACE("id", GranteeType.GT_PUBLIC, mockRight("r"), null, null);
        ZimbraACE userAce = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);

        assertTrue(pubAce.isGrantee(null));
        assertFalse(userAce.isGrantee(null));
    }

    @Test
    public void testIsGrantee_guidPublic_returnsTrueOnlyForGT_PUBLIC() throws ServiceException {
        ZimbraACE pubAce  = new ZimbraACE("id", GranteeType.GT_PUBLIC, mockRight("r"), null, null);
        ZimbraACE userAce = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);

        assertTrue(pubAce.isGrantee(GuestAccount.GUID_PUBLIC));
        assertFalse(userAce.isGrantee(GuestAccount.GUID_PUBLIC));
    }

    @Test
    public void testIsGrantee_guidAuthUser_returnsTrueOnlyForGT_AUTHUSER() throws ServiceException {
        ZimbraACE authAce = new ZimbraACE("id", GranteeType.GT_AUTHUSER, mockRight("r"), null, null);
        ZimbraACE userAce = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);

        assertTrue(authAce.isGrantee(GuestAccount.GUID_AUTHUSER));
        assertFalse(userAce.isGrantee(GuestAccount.GUID_AUTHUSER));
    }

    @Test
    public void testIsGrantee_matchingId_returnsTrue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);
        assertTrue(ace.isGrantee(TEST_UUID));
    }

    @Test
    public void testIsGrantee_nonMatchingId_returnsFalse() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);
        assertFalse(ace.isGrantee("other-uuid"));
    }

    // ---------------------------------------------------------------
    // deny / canDelegate / subDomain / disinheritSubGroups / canExecuteOnly
    // ---------------------------------------------------------------

    @Test
    public void testDeny_RM_DENY_isTrue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), RightModifier.RM_DENY, null);
        assertTrue(ace.deny());
        assertFalse(ace.canDelegate());
        assertFalse(ace.subDomain());
        assertFalse(ace.disinheritSubGroups());
        assertFalse(ace.canExecuteOnly());  // deny() is true
    }

    @Test
    public void testCanDelegate_RM_CAN_DELEGATE_isTrue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), RightModifier.RM_CAN_DELEGATE, null);
        assertTrue(ace.canDelegate());
        assertFalse(ace.deny());
        assertFalse(ace.canExecuteOnly());  // canDelegate() is true
    }

    @Test
    public void testSubDomain_RM_SUBDOMAIN_isTrue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_DOMAIN, mockRight("r"), RightModifier.RM_SUBDOMAIN, null);
        assertTrue(ace.subDomain());
        assertFalse(ace.deny());
        assertFalse(ace.canDelegate());
    }

    @Test
    public void testDisinheritSubGroups_RM_DISINHERIT_isTrue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), RightModifier.RM_DISINHERIT_SUB_GROUPS, null);
        assertTrue(ace.disinheritSubGroups());
        assertFalse(ace.deny());
        assertFalse(ace.canDelegate());
    }

    @Test
    public void testCanExecuteOnly_noModifier_isTrue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);
        assertTrue(ace.canExecuteOnly());
        assertFalse(ace.deny());
        assertFalse(ace.canDelegate());
    }

    // ---------------------------------------------------------------
    // setSecret / setRightModifier / setRight
    // ---------------------------------------------------------------

    @Test
    public void testSetSecret_updatesValue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE("guest@ex.com", GranteeType.GT_GUEST, mockRight("r"), null, "old");
        ace.setSecret("new");
        assertEquals("new", ace.getSecret());
    }

    @Test
    public void testSetRightModifier_updatesValue() throws ServiceException {
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, mockRight("r"), null, null);
        assertNull(ace.getRightModifier());
        ace.setRightModifier(RightModifier.RM_DENY);
        assertEquals(RightModifier.RM_DENY, ace.getRightModifier());
    }

    @Test
    public void testSetRight_updatesValue() throws ServiceException {
        Right r1 = mockRight("viewFreeBusy");
        Right r2 = mockRight("invite");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r1, null, null);
        ace.setRight(r2);
        assertSame(r2, ace.getRight());
    }

    // ---------------------------------------------------------------
    // clone
    // ---------------------------------------------------------------

    @Test
    public void testClone_isDeepCopy() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE original = new ZimbraACE("guest@ex.com", GranteeType.GT_GUEST, r, null, "pass");
        ZimbraACE cloned   = original.clone();

        // same field values
        assertEquals(original.getGrantee(),     cloned.getGrantee());
        assertEquals(original.getGranteeType(), cloned.getGranteeType());
        assertSame(original.getRight(),          cloned.getRight());
        assertEquals(original.getSecret(),       cloned.getSecret());

        // independent: modifying clone does not affect original
        cloned.setSecret("other");
        assertEquals("pass",  original.getSecret());
        assertEquals("other", cloned.getSecret());
    }

    @Test
    public void testClone_nullSecret_handledGracefully() throws ServiceException {
        Right r = mockRight("r");
        ZimbraACE original = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);
        ZimbraACE cloned   = original.clone();
        assertNull(cloned.getSecret());
        assertEquals(original.getGrantee(), cloned.getGrantee());
    }

    // ---------------------------------------------------------------
    // serialize
    // ---------------------------------------------------------------

    @Test
    public void testSerialize_GT_USER_noModifier() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);
        String s = ace.serialize();

        assertEquals(TEST_UUID + " usr viewFreeBusy", s);
    }

    @Test
    public void testSerialize_GT_USER_denyModifier() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, RightModifier.RM_DENY, null);
        String s = ace.serialize();

        assertEquals(TEST_UUID + " usr -invite", s);
    }

    @Test
    public void testSerialize_GT_USER_canDelegateModifier() throws ServiceException {
        Right r = mockRight("modifyAccount");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, RightModifier.RM_CAN_DELEGATE, null);
        String s = ace.serialize();

        assertEquals(TEST_UUID + " usr +modifyAccount", s);
    }

    @Test
    public void testSerialize_GT_AUTHUSER() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE("anything", GranteeType.GT_AUTHUSER, r, null, null);
        String s = ace.serialize();

        assertEquals(GuestAccount.GUID_AUTHUSER + " all viewFreeBusy", s);
    }

    @Test
    public void testSerialize_GT_PUBLIC() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE ace = new ZimbraACE("anything", GranteeType.GT_PUBLIC, r, null, null);
        String s = ace.serialize();

        assertEquals(GuestAccount.GUID_PUBLIC + " pub invite", s);
    }

    @Test
    public void testSerialize_GT_GUEST_withSecret() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE("guest@ex.com", GranteeType.GT_GUEST, r, null, "s3cr3t");
        String s = ace.serialize();

        // format: grantee:secret gst rightName
        assertEquals("guest@ex.com:s3cr3t gst viewFreeBusy", s);
    }

    @Test
    public void testSerialize_GT_GUEST_nullSecret() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE("guest@ex.com", GranteeType.GT_GUEST, r, null, null);
        String s = ace.serialize();

        // encodeSecret returns "" for null
        assertEquals("guest@ex.com: gst viewFreeBusy", s);
    }

    @Test
    public void testSerialize_GT_GROUP() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_GROUP, r, null, null);
        String s = ace.serialize();

        assertEquals(TEST_UUID + " grp viewFreeBusy", s);
    }

    // ---------------------------------------------------------------
    // validate
    // ---------------------------------------------------------------

    @Test
    public void testValidate_GT_USER_noException() throws ServiceException {
        Right r = mockRight("r");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);
        ZimbraACE.validate(ace); // should not throw
    }

    @Test
    public void testValidate_GT_GUEST_validGranteeNoSecret_noException() throws ServiceException {
        Right r = mockRight("r");
        ZimbraACE ace = new ZimbraACE("guestuser", GranteeType.GT_GUEST, r, null, null);
        ZimbraACE.validate(ace); // should not throw
    }

    @Test(expected = ServiceException.class)
    public void testValidate_GT_GUEST_granteeContainsColon_throws() throws ServiceException {
        Right r = mockRight("r");
        ZimbraACE ace = new ZimbraACE("guest:user", GranteeType.GT_GUEST, r, null, null);
        ZimbraACE.validate(ace);
    }

    @Test(expected = ServiceException.class)
    public void testValidate_GT_GUEST_secretContainsColon_throws() throws ServiceException {
        Right r = mockRight("r");
        ZimbraACE ace = new ZimbraACE("guestuser", GranteeType.GT_GUEST, r, null, "bad:secret");
        ZimbraACE.validate(ace);
    }

    @Test(expected = ServiceException.class)
    public void testValidate_GT_KEY_granteeContainsColon_throws() throws ServiceException {
        Right r = mockRight("r");
        ZimbraACE ace = new ZimbraACE("key:user", GranteeType.GT_KEY, r, null, null);
        ZimbraACE.validate(ace);
    }

    // ---------------------------------------------------------------
    // ExternalGroupInfo
    // ---------------------------------------------------------------

    @Test
    public void testExternalGroupInfo_parse_valid() throws ServiceException {
        ZimbraACE.ExternalGroupInfo info = ZimbraACE.ExternalGroupInfo.parse("domainId:extGroup@ext.com");
        assertEquals("domainId",          info.getZimbraDmain());
        assertEquals("extGroup@ext.com",  info.getExternalGroupName());
    }

    @Test(expected = ServiceException.class)
    public void testExternalGroupInfo_parse_tooFewParts_throws() throws ServiceException {
        ZimbraACE.ExternalGroupInfo.parse("nodomain");
    }

    @Test(expected = ServiceException.class)
    public void testExternalGroupInfo_parse_empty_throws() throws ServiceException {
        ZimbraACE.ExternalGroupInfo.parse("");
    }

    @Test
    public void testExternalGroupInfo_encode() {
        String encoded = ZimbraACE.ExternalGroupInfo.encode("domainId", "group@ext.com");
        assertEquals("domainId:group@ext.com", encoded);
    }

    @Test
    public void testExternalGroupInfo_encodeIfMissingDomain_alreadyHasColon_unchanged() {
        String result = ZimbraACE.ExternalGroupInfo.encodeIfExtGroupNameMissingDomain("dom", "domainId:group@ext.com");
        assertEquals("domainId:group@ext.com", result);
    }

    @Test
    public void testExternalGroupInfo_encodeIfMissingDomain_missingColon_addsDomain() {
        String result = ZimbraACE.ExternalGroupInfo.encodeIfExtGroupNameMissingDomain("myDomainId", "group@ext.com");
        assertEquals("myDomainId:group@ext.com", result);
    }

    @Test
    public void testExternalGroupInfo_encodeIfMissingDomain_emptyDomain() {
        String result = ZimbraACE.ExternalGroupInfo.encodeIfExtGroupNameMissingDomain("", "group@ext.com");
        assertEquals(":group@ext.com", result);
    }

    // ---------------------------------------------------------------
    // toString / dump
    // ---------------------------------------------------------------

    @Test
    public void testToString_notNull() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);
        assertNotNull(ace.toString());
        assertFalse(ace.toString().isEmpty());
    }

    @Test
    public void testDump_verbose_notNull() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);
        String dump = ace.dump(true);
        assertNotNull(dump);
        assertTrue(dump.contains("viewFreeBusy"));
    }

    @Test
    public void testDump_notVerbose_containsSerializedForm() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE ace = new ZimbraACE(TEST_UUID, GranteeType.GT_USER, r, null, null);
        String dump = ace.dump(false);
        assertNotNull(dump);
        // non-verbose calls serialize() internally
        assertTrue(dump.contains("invite"));
    }
}
