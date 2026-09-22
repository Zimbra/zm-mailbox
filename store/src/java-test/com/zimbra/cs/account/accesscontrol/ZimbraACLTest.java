/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link ZimbraACL}: exercises the grant/revoke/serialize/clone
 * workflows over a real ACL built from real {@link ZimbraACE} objects and real
 * {@link UserRight} domain objects (no mocks).
 */
public class ZimbraACLTest {

    private static final String GRANTEE_A = "44d2b6b8-8001-4305-a9c0-419d04a44a9a";

    private static final String GRANTEE_B = "3b110e75-4003-4634-a3ec-fea456ad7d84";

    private UserRight rightInvite() {
        return new UserRight("invite");
    }

    private UserRight rightViewFreeBusy() {
        return new UserRight("viewFreeBusy");
    }

    private ZimbraACE ace(String granteeId, Right right, RightModifier mod) throws Exception {
        return new ZimbraACE(granteeId, GranteeType.GT_USER, right, mod, null);
    }

    @Test
    public void grantAccessSingleAllowGrantAppearsInAllACEs() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        ZimbraACE allow = ace(GRANTEE_A, rightInvite(), null);
        grants.add(allow);

        // Act
        ZimbraACL acl = new ZimbraACL(grants);

        // Assert
        List<ZimbraACE> all = acl.getAllACEs();
        assertEquals(1, all.size());
        assertEquals(GRANTEE_A, all.get(0).getGrantee());
        assertFalse("plain grant must not be a deny", all.get(0).deny());
        assertTrue("plain allow with no modifier is execute-only", all.get(0).canExecuteOnly());
    }

    @Test
    public void addACEDenyGrantInsertedAtFrontAndTrackedInDenied() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, rightInvite(), null));            // allow
        ZimbraACL acl = new ZimbraACL(grants);

        Set<ZimbraACE> denyGrant = new HashSet<ZimbraACE>();
        denyGrant.add(ace(GRANTEE_B, rightViewFreeBusy(), RightModifier.RM_DENY));

        // Act
        acl.grantAccess(denyGrant);

        // Assert - deny is at the front of the ordered list and in the denied set
        List<ZimbraACE> all = acl.getAllACEs();
        assertEquals(2, all.size());
        assertTrue("deny grant must be first", all.get(0).deny());
        assertEquals(GRANTEE_B, all.get(0).getGrantee());
        assertEquals(1, acl.getDeniedACEs().size());
    }

    @Test
    public void addACEDelegableGrantTrackedInDelegableSet() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, rightInvite(), RightModifier.RM_CAN_DELEGATE));

        // Act
        ZimbraACL acl = new ZimbraACL(grants);

        // Assert
        assertEquals(1, acl.getAllowedDelegableACEs().size());
        assertTrue(acl.getAllowedDelegableACEs().iterator().next().canDelegate());
        assertEquals(0, acl.getAllowedNotDelegableACEs().size());
        assertEquals(0, acl.getSubDomainACEs().size());
    }

    @Test
    public void addACESubDomainGrantTrackedInSubDomainSet() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, rightInvite(), RightModifier.RM_SUBDOMAIN));

        // Act
        ZimbraACL acl = new ZimbraACL(grants);

        // Assert
        assertEquals(1, acl.getSubDomainACEs().size());
        assertTrue(acl.getSubDomainACEs().iterator().next().subDomain());
        assertEquals(0, acl.getAllowedNotDelegableACEs().size());
    }

    @Test
    public void addACEPlainAllowTrackedInNotDelegableSet() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, rightInvite(), null));

        // Act
        ZimbraACL acl = new ZimbraACL(grants);

        // Assert
        assertEquals(1, acl.getAllowedNotDelegableACEs().size());
        assertEquals(0, acl.getAllowedDelegableACEs().size());
        assertEquals(0, acl.getDeniedACEs().size());
    }

    @Test
    public void grantDuplicateSameGranteeSameRightSameModifierNotAddedAgain() throws Exception {
        // Arrange
        UserRight invite = rightInvite();
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, invite, null));
        ZimbraACL acl = new ZimbraACL(grants);

        // Act - grant the same grantee/right/modifier again
        Set<ZimbraACE> again = new HashSet<ZimbraACE>();
        again.add(ace(GRANTEE_A, invite, null));
        List<ZimbraACE> granted = acl.grantAccess(again);

        // Assert - nothing changed, no duplicate
        assertTrue("re-granting identical grant reports no change", granted.isEmpty());
        assertEquals(1, acl.getAllACEs().size());
    }

    @Test
    public void grantSameGranteeSameRightDifferentModifierUpdatesModifierInPlace() throws Exception {
        // Arrange
        UserRight invite = rightInvite();
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, invite, null));            // execute-only allow
        ZimbraACL acl = new ZimbraACL(grants);

        // Act - re-grant same grantee/right but as deny
        Set<ZimbraACE> change = new HashSet<ZimbraACE>();
        change.add(ace(GRANTEE_A, invite, RightModifier.RM_DENY));
        List<ZimbraACE> granted = acl.grantAccess(change);

        // Assert - existing ACE flipped to deny, still a single ACE
        assertEquals("modifier change is reported as a grant", 1, granted.size());
        assertEquals(1, acl.getAllACEs().size());
        assertTrue("existing ACE must now be a deny", acl.getAllACEs().get(0).deny());
    }

    @Test
    public void revokeAccessMatchingGrantRemovesIt() throws Exception {
        // Arrange
        UserRight invite = rightInvite();
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, invite, null));
        ZimbraACL acl = new ZimbraACL(grants);

        // Act
        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(ace(GRANTEE_A, invite, null));
        List<ZimbraACE> revoked = acl.revokeAccess(toRevoke);

        // Assert
        assertEquals(1, revoked.size());
        assertTrue("ACL is empty after revoke", acl.getAllACEs().isEmpty());
        assertEquals(0, acl.getAllowedNotDelegableACEs().size());
    }

    @Test
    public void revokeAccessModifierMismatchDoesNotRemove() throws Exception {
        // Arrange - granted as a plain allow
        UserRight invite = rightInvite();
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, invite, null));
        ZimbraACL acl = new ZimbraACL(grants);

        // Act - try to revoke a DENY of the same right (modifier mismatch)
        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(ace(GRANTEE_A, invite, RightModifier.RM_DENY));
        List<ZimbraACE> revoked = acl.revokeAccess(toRevoke);

        // Assert - nothing revoked, grant still present
        assertTrue("modifier mismatch must not revoke", revoked.isEmpty());
        assertEquals(1, acl.getAllACEs().size());
    }

    @Test
    public void getACEsFilterByRightReturnsOnlyMatching() throws Exception {
        // Arrange
        UserRight invite = rightInvite();
        UserRight viewFb = rightViewFreeBusy();
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, invite, null));
        grants.add(ace(GRANTEE_B, viewFb, null));
        ZimbraACL acl = new ZimbraACL(grants);

        Set<Right> wanted = new HashSet<Right>();
        wanted.add(invite);

        // Act
        List<ZimbraACE> matched = acl.getACEs(wanted);

        // Assert
        assertEquals(1, matched.size());
        assertEquals(GRANTEE_A, matched.get(0).getGrantee());
    }

    @Test
    public void serializeRoundTripsEveryAce() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, rightInvite(), null));
        grants.add(ace(GRANTEE_B, rightViewFreeBusy(), RightModifier.RM_DENY));
        ZimbraACL acl = new ZimbraACL(grants);

        // Act
        List<String> serialized = acl.serialize();

        // Assert - one serialized string per ACE, deny token present for the deny grant
        assertEquals(2, serialized.size());
        boolean sawDenyToken = false;
        for (String s : serialized) {
            if (s.contains("-viewFreeBusy")) {
                sawDenyToken = true;
            }
        }
        assertTrue("deny grant must serialize with leading '-'", sawDenyToken);
    }

    @Test
    public void cloneProducesIndependentDeepCopy() throws Exception {
        // Arrange
        Set<ZimbraACE> grants = new HashSet<ZimbraACE>();
        grants.add(ace(GRANTEE_A, rightInvite(), null));
        ZimbraACL original = new ZimbraACL(grants);

        // Act
        ZimbraACL copy = original.clone();

        // Assert - same content, different object graph
        assertNotSame("clone must be a distinct object", original, copy);
        assertEquals(original.getAllACEs().size(), copy.getAllACEs().size());
        assertNotSame("ACE list must be deep-copied",
                original.getAllACEs().get(0), copy.getAllACEs().get(0));
        assertEquals(original.getAllACEs().get(0).getGrantee(),
                copy.getAllACEs().get(0).getGrantee());
    }
}
