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
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZimbraACL}.
 *
 * Uses the Set-of-ZimbraACE constructor to avoid needing RightManager.
 * Right instances are Mockito mocks; isTheSameRight uses reference equality
 * (this == other) in the base Right class, so same mock instance == same right.
 */
public class ZimbraACLTest {

    private static final String UUID_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String UUID_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    private static Right mockRight(String name) {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getName()).thenReturn(name);
        // isTheSameRight in base Right uses reference equality, so the
        // real method would be this == other. Since we mock, we have to
        // stub so the same mock returns true for itself.
        Mockito.when(r.isTheSameRight(r)).thenReturn(true);
        return r;
    }

    private static ZimbraACE makeACE(String granteeId, GranteeType gt, Right r, RightModifier rm)
            throws ServiceException {
        return new ZimbraACE(granteeId, gt, r, rm, null);
    }

    private static ZimbraACL aclFrom(ZimbraACE... aces) throws ServiceException {
        Set<ZimbraACE> set = new HashSet<ZimbraACE>();
        for (ZimbraACE ace : aces) {
            set.add(ace);
        }
        return new ZimbraACL(set);
    }

    // ---------------------------------------------------------------
    // grantAccess — new ACE added
    // ---------------------------------------------------------------

    @Test
    public void testGrantAccess_newAce_returnsGranted() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = makeACE(UUID_A, GranteeType.GT_USER, r, null);
        ZimbraACL acl = aclFrom(ace);

        List<ZimbraACE> all = acl.getAllACEs();
        assertEquals(1, all.size());
        assertSame(ace, all.get(0));
    }

    @Test
    public void testGrantAccess_multipleAces_allPresent() throws ServiceException {
        Right r1 = mockRight("viewFreeBusy");
        Right r2 = mockRight("invite");
        ZimbraACE ace1 = makeACE(UUID_A, GranteeType.GT_USER, r1, null);
        ZimbraACE ace2 = makeACE(UUID_B, GranteeType.GT_USER, r2, null);

        ZimbraACL acl = aclFrom(ace1, ace2);
        assertEquals(2, acl.getAllACEs().size());
    }

    // ---------------------------------------------------------------
    // grantAccess — duplicate (same grantee + same right + same modifier)
    // ---------------------------------------------------------------

    @Test
    public void testGrantAccess_duplicate_notAddedTwice() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace1 = makeACE(UUID_A, GranteeType.GT_USER, r, null);
        ZimbraACE ace2 = makeACE(UUID_A, GranteeType.GT_USER, r, null);  // same grantee + right + modifier

        Set<ZimbraACE> initial = new HashSet<ZimbraACE>();
        initial.add(ace1);
        ZimbraACL acl = new ZimbraACL(initial);

        // grant the "duplicate"
        Set<ZimbraACE> second = new HashSet<ZimbraACE>();
        second.add(ace2);
        List<ZimbraACE> granted = acl.grantAccess(second);

        // no change because same modifier — grant() returns false → not in "granted"
        assertEquals(0, granted.size());
        assertEquals(1, acl.getAllACEs().size());
    }

    // ---------------------------------------------------------------
    // grantAccess — different modifier updates existing
    // ---------------------------------------------------------------

    @Test
    public void testGrantAccess_differentModifier_updatesExisting() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE aceAllow = makeACE(UUID_A, GranteeType.GT_USER, r, null);         // no modifier
        ZimbraACE aceDeny  = makeACE(UUID_A, GranteeType.GT_USER, r, RightModifier.RM_DENY); // deny

        Set<ZimbraACE> initial = new HashSet<ZimbraACE>();
        initial.add(aceAllow);
        ZimbraACL acl = new ZimbraACL(initial);

        // grant the deny version of the same right to the same grantee
        Set<ZimbraACE> update = new HashSet<ZimbraACE>();
        update.add(aceDeny);
        List<ZimbraACE> changed = acl.grantAccess(update);

        // should report "changed"
        assertEquals(1, changed.size());
        // underlying ACE modifier should now be DENY
        assertEquals(RightModifier.RM_DENY, acl.getAllACEs().get(0).getRightModifier());
        // still only one ACE
        assertEquals(1, acl.getAllACEs().size());
    }

    // ---------------------------------------------------------------
    // ACE ordering — denied go to front
    // ---------------------------------------------------------------

    @Test
    public void testAddOrder_deniedAcesFirstInAllACEs() throws ServiceException {
        Right r1 = mockRight("invite");
        Right r2 = mockRight("viewFreeBusy");

        ZimbraACE allowAce = makeACE(UUID_A, GranteeType.GT_USER, r1, null);
        ZimbraACE denyAce  = makeACE(UUID_B, GranteeType.GT_USER, r2, RightModifier.RM_DENY);

        // grant allow first, then deny
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(allowAce);
        aces.add(denyAce);
        ZimbraACL acl = new ZimbraACL(aces);

        List<ZimbraACE> all = acl.getAllACEs();
        assertEquals(2, all.size());
        // denied must appear before allowed
        assertTrue(all.get(0).deny());
        assertFalse(all.get(1).deny());
    }

    // ---------------------------------------------------------------
    // getDeniedACEs / getAllowedDelegableACEs / getAllowedNotDelegableACEs
    // ---------------------------------------------------------------

    @Test
    public void testGetDeniedACEs_onlyDenied() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE denyAce  = makeACE(UUID_A, GranteeType.GT_USER, r, RightModifier.RM_DENY);
        ZimbraACE allowAce = makeACE(UUID_B, GranteeType.GT_USER, r, null);

        ZimbraACL acl = aclFrom(denyAce, allowAce);

        Set<ZimbraACE> denied = acl.getDeniedACEs();
        assertEquals(1, denied.size());
        assertTrue(denied.contains(denyAce));
        assertFalse(denied.contains(allowAce));
    }

    @Test
    public void testGetAllowedDelegableACEs_onlyDelegable() throws ServiceException {
        Right r = mockRight("modifyAccount");
        ZimbraACE delegable    = makeACE(UUID_A, GranteeType.GT_USER, r, RightModifier.RM_CAN_DELEGATE);
        ZimbraACE notDelegable = makeACE(UUID_B, GranteeType.GT_USER, r, null);

        ZimbraACL acl = aclFrom(delegable, notDelegable);

        Set<ZimbraACE> deleg = acl.getAllowedDelegableACEs();
        assertEquals(1, deleg.size());
        assertTrue(deleg.contains(delegable));
        assertFalse(deleg.contains(notDelegable));
    }

    @Test
    public void testGetAllowedNotDelegableACEs_onlyNonDelegable() throws ServiceException {
        Right r = mockRight("modifyAccount");
        ZimbraACE delegable    = makeACE(UUID_A, GranteeType.GT_USER, r, RightModifier.RM_CAN_DELEGATE);
        ZimbraACE notDelegable = makeACE(UUID_B, GranteeType.GT_USER, r, null);

        ZimbraACL acl = aclFrom(delegable, notDelegable);

        Set<ZimbraACE> notDeleg = acl.getAllowedNotDelegableACEs();
        assertEquals(1, notDeleg.size());
        assertTrue(notDeleg.contains(notDelegable));
        assertFalse(notDeleg.contains(delegable));
    }

    @Test
    public void testGetSubDomainACEs_onlySubDomain() throws ServiceException {
        Right r = mockRight("domainRight");
        ZimbraACE subDomainAce = makeACE(UUID_A, GranteeType.GT_DOMAIN, r, RightModifier.RM_SUBDOMAIN);
        ZimbraACE normalAce    = makeACE(UUID_B, GranteeType.GT_DOMAIN, r, null);

        ZimbraACL acl = aclFrom(subDomainAce, normalAce);

        Set<ZimbraACE> subDomain = acl.getSubDomainACEs();
        assertEquals(1, subDomain.size());
        assertTrue(subDomain.contains(subDomainAce));
    }

    // ---------------------------------------------------------------
    // revokeAccess
    // ---------------------------------------------------------------

    @Test
    public void testRevokeAccess_existingAce_removedAndReturned() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = makeACE(UUID_A, GranteeType.GT_USER, r, null);
        ZimbraACL acl = aclFrom(ace);
        assertEquals(1, acl.getAllACEs().size());

        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(ace);
        List<ZimbraACE> revoked = acl.revokeAccess(toRevoke);

        assertEquals(1, revoked.size());
        assertEquals(0, acl.getAllACEs().size());
        assertTrue(acl.getDeniedACEs().isEmpty());
        assertTrue(acl.getAllowedNotDelegableACEs().isEmpty());
    }

    @Test
    public void testRevokeAccess_nonExistingAce_emptyResult() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE existing    = makeACE(UUID_A, GranteeType.GT_USER, r, null);
        ZimbraACE nonExisting = makeACE(UUID_B, GranteeType.GT_USER, r, null);

        ZimbraACL acl = aclFrom(existing);

        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(nonExisting);
        List<ZimbraACE> revoked = acl.revokeAccess(toRevoke);

        assertEquals(0, revoked.size());
        assertEquals(1, acl.getAllACEs().size()); // original still there
    }

    @Test
    public void testRevokeAccess_denyAce_removedFromDenied() throws ServiceException {
        Right r = mockRight("invite");
        ZimbraACE denyAce = makeACE(UUID_A, GranteeType.GT_USER, r, RightModifier.RM_DENY);
        ZimbraACL acl = aclFrom(denyAce);

        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(denyAce);
        acl.revokeAccess(toRevoke);

        assertTrue(acl.getDeniedACEs().isEmpty());
        assertEquals(0, acl.getAllACEs().size());
    }

    @Test
    public void testRevokeAccess_modifierMismatch_notRevoked() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE allowAce = makeACE(UUID_A, GranteeType.GT_USER, r, null);
        ZimbraACE denyAce  = makeACE(UUID_A, GranteeType.GT_USER, r, RightModifier.RM_DENY);

        // ACL has an allow grant; try to revoke the deny version → no match
        ZimbraACL acl = aclFrom(allowAce);
        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(denyAce);
        List<ZimbraACE> revoked = acl.revokeAccess(toRevoke);

        assertEquals(0, revoked.size());
        assertEquals(1, acl.getAllACEs().size());
    }

    // ---------------------------------------------------------------
    // getACEs (by right set)
    // ---------------------------------------------------------------

    @Test
    public void testGetACEs_returnsMatchingRights() throws ServiceException {
        Right r1 = mockRight("viewFreeBusy");
        Right r2 = mockRight("invite");

        ZimbraACE ace1 = makeACE(UUID_A, GranteeType.GT_USER, r1, null);
        ZimbraACE ace2 = makeACE(UUID_B, GranteeType.GT_USER, r2, null);

        ZimbraACL acl = aclFrom(ace1, ace2);

        Set<Right> searchRights = new HashSet<Right>();
        searchRights.add(r1);

        List<ZimbraACE> found = acl.getACEs(searchRights);
        assertEquals(1, found.size());
        assertSame(r1, found.get(0).getRight());
    }

    @Test
    public void testGetACEs_noMatch_emptyList() throws ServiceException {
        Right r1 = mockRight("viewFreeBusy");
        Right r2 = mockRight("invite");

        ZimbraACE ace1 = makeACE(UUID_A, GranteeType.GT_USER, r1, null);
        ZimbraACL acl = aclFrom(ace1);

        Set<Right> searchRights = new HashSet<Right>();
        searchRights.add(r2);  // r2 not in ACL

        List<ZimbraACE> found = acl.getACEs(searchRights);
        assertTrue(found.isEmpty());
    }

    // ---------------------------------------------------------------
    // serialize
    // ---------------------------------------------------------------

    @Test
    public void testSerialize_allACEsSerialized() throws ServiceException {
        Right r1 = mockRight("viewFreeBusy");
        Right r2 = mockRight("invite");

        ZimbraACE ace1 = makeACE(UUID_A, GranteeType.GT_USER, r1, null);
        ZimbraACE ace2 = makeACE(UUID_B, GranteeType.GT_USER, r2, RightModifier.RM_DENY);

        ZimbraACL acl = aclFrom(ace1, ace2);

        List<String> serialized = acl.serialize();
        assertEquals(2, serialized.size());
        assertTrue(serialized.contains(UUID_A + " usr viewFreeBusy"));
        assertTrue(serialized.contains(UUID_B + " usr -invite"));
    }

    @Test
    public void testSerialize_emptyACL_emptyList() throws ServiceException {
        ZimbraACL acl = new ZimbraACL(new HashSet<ZimbraACE>());
        List<String> serialized = acl.serialize();
        assertTrue(serialized.isEmpty());
    }

    // ---------------------------------------------------------------
    // clone
    // ---------------------------------------------------------------

    @Test
    public void testClone_isDeepCopy() throws ServiceException {
        Right r = mockRight("viewFreeBusy");
        ZimbraACE ace = makeACE(UUID_A, GranteeType.GT_USER, r, null);
        ZimbraACL original = aclFrom(ace);
        ZimbraACL cloned   = original.clone();

        assertEquals(1, cloned.getAllACEs().size());

        // modifying the clone does not affect the original
        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(cloned.getAllACEs().get(0));
        cloned.revokeAccess(toRevoke);

        assertEquals(0, cloned.getAllACEs().size());
        assertEquals(1, original.getAllACEs().size());
    }

    // ---------------------------------------------------------------
    // edge cases — empty ACL
    // ---------------------------------------------------------------

    @Test
    public void testEmptyACL_allCollectionsEmpty() throws ServiceException {
        ZimbraACL acl = new ZimbraACL(new HashSet<ZimbraACE>());

        assertTrue(acl.getAllACEs().isEmpty());
        assertTrue(acl.getDeniedACEs().isEmpty());
        assertTrue(acl.getAllowedDelegableACEs().isEmpty());
        assertTrue(acl.getAllowedNotDelegableACEs().isEmpty());
        assertTrue(acl.getSubDomainACEs().isEmpty());
    }
}
