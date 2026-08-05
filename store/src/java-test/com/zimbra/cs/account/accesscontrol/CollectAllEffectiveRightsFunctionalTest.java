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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.CollectAllEffectiveRights.AllGroupMembers;
import com.zimbra.cs.account.accesscontrol.CollectAllEffectiveRights.GroupShape;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for the pure "shaping" data model inside {@link CollectAllEffectiveRights} -
 * {@link GroupShape} and {@link AllGroupMembers}. The {@code shapeMembers} algorithm partitions
 * group members into disjoint "shapes" where every member of a shape belongs to exactly the same
 * set of groups; this is the heart of how all-effective-rights aggregates group-inherited rights,
 * and it is pure logic requiring no LDAP. We drive it through real multi-group workflows and assert
 * the resulting shapes' membership and group labels.
 *
 * <p>The {@code collect()} workflow and the LDAP search helpers ({@code getAllGroups},
 * {@code getAllCalendarResources}, {@code searchSubDomains}) require {@code LdapProv} and a live
 * directory and so are exercised indirectly only via these pure building blocks (see "skipped").
 */
public class CollectAllEffectiveRightsFunctionalTest {

    /* Builds an AllGroupMembers and seeds its account members (getMembers returns the live set). */
    private static AllGroupMembers groupWithAccounts(String groupName, String... accounts)
            throws ServiceException {
        AllGroupMembers g = new AllGroupMembers(groupName);
        for (String a : accounts) {
            g.getMembers(TargetType.account).add(a);
        }
        return g;
    }

    @Test
    public void allGroupMembersGetMembersByTypeReturnsTypeSpecificSets() throws Exception {
        // Arrange
        AllGroupMembers g = new AllGroupMembers("grp@zimbra.com");
        g.getMembers(TargetType.account).add("a@zimbra.com");
        g.getMembers(TargetType.calresource).add("room@zimbra.com");
        g.getMembers(TargetType.dl).add("dl@zimbra.com");

        // Act / Assert — each target type yields its own member set
        assertEquals("grp@zimbra.com", g.getGroupName());
        assertTrue(g.getMembers(TargetType.account).contains("a@zimbra.com"));
        assertTrue(g.getMembers(TargetType.calresource).contains("room@zimbra.com"));
        assertTrue(g.getMembers(TargetType.dl).contains("dl@zimbra.com"));
        assertFalse("accounts and crs are separate buckets",
                g.getMembers(TargetType.account).contains("room@zimbra.com"));
    }

    @Test
    public void allGroupMembersGetMembersUnsupportedTypeThrowsFailure() throws Exception {
        // Arrange
        AllGroupMembers g = new AllGroupMembers("grp@zimbra.com");

        // Act / Assert — only account/calresource/dl are valid; domain is not
        try {
            g.getMembers(TargetType.domain);
            fail("expected FAILURE for unsupported target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void shapeMembersSingleGroupCreatesOneShapeWithAllMembers() throws Exception {
        // Arrange — one group A with members m1, m2
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        AllGroupMembers groupA = groupWithAccounts("A", "m1", "m2");

        // Act
        GroupShape.shapeMembers(TargetType.account, shapes, groupA);

        // Assert — single shape labelled A containing both members
        assertEquals("one shape", 1, shapes.size());
        GroupShape shape = shapes.iterator().next();
        assertTrue("shape belongs to group A", shape.getGroups().contains("A"));
        assertEquals("both members in the A shape", 2, shape.getMembers().size());
        assertTrue(shape.getMembers().contains("m1"));
        assertTrue(shape.getMembers().contains("m2"));
    }

    @Test
    public void shapeMembersOverlappingGroupsPartitionsIntoThreeShapes() throws Exception {
        // Arrange — A={m1,m2,m3}, B={m2,m3,m4}: overlap on m2,m3
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        AllGroupMembers groupA = groupWithAccounts("A", "m1", "m2", "m3");
        AllGroupMembers groupB = groupWithAccounts("B", "m2", "m3", "m4");

        // Act — shape A then B
        GroupShape.shapeMembers(TargetType.account, shapes, groupA);
        GroupShape.shapeMembers(TargetType.account, shapes, groupB);

        // Assert — three disjoint shapes: {m1}=A, {m2,m3}=AB, {m4}=B
        assertEquals("A, AB, B shapes", 3, shapes.size());

        GroupShape shapeOnlyA = findShapeContaining(shapes, "m1");
        assertEquals("m1 is in A only", setOf("A"), shapeOnlyA.getGroups());
        assertEquals(setOf("m1"), shapeOnlyA.getMembers());

        GroupShape shapeAB = findShapeContaining(shapes, "m2");
        assertEquals("m2,m3 are in A and B", setOf("A", "B"), shapeAB.getGroups());
        assertTrue(shapeAB.getMembers().contains("m2"));
        assertTrue(shapeAB.getMembers().contains("m3"));

        GroupShape shapeOnlyB = findShapeContaining(shapes, "m4");
        assertEquals("m4 is in B only", setOf("B"), shapeOnlyB.getGroups());
    }

    @Test
    public void shapeMembersDisjointGroupsKeepsSeparateShapes() throws Exception {
        // Arrange — A={m1}, C={m5} share nothing
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "m1"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("C", "m5"));

        // Assert — two independent single-member shapes
        assertEquals(2, shapes.size());
        assertEquals(setOf("A"), findShapeContaining(shapes, "m1").getGroups());
        assertEquals(setOf("C"), findShapeContaining(shapes, "m5").getGroups());
    }

    @Test
    public void shapeMembersEmptyGroupAddsNoShape() throws Exception {
        // Arrange — a group with no members of the requested type
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        AllGroupMembers empty = new AllGroupMembers("E");

        // Act
        GroupShape.shapeMembers(TargetType.account, shapes, empty);

        // Assert — nothing to shape => no shapes created
        assertTrue("empty group contributes no shape", shapes.isEmpty());
    }

    @Test
    public void shapeMembersIdenticalGroupsCollapseMembersIntoSharedShape() throws Exception {
        // Arrange — A and B contain exactly the same members
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "m1", "m2"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("B", "m1", "m2"));

        // Assert — shaping B drains all members out of the original A-only shape into a new AB
        // shape, but the algorithm does NOT prune the now-empty A-only shape, so two shapes remain:
        // the AB shape holding both members, and a leftover empty A-only shape.
        assertEquals("emptied A shape is retained alongside the AB shape", 2, shapes.size());

        GroupShape shapeAB = findShapeContaining(shapes, "m1");
        assertEquals("the populated shape belongs to both A and B", setOf("A", "B"), shapeAB.getGroups());
        assertEquals("both members collapse into the AB shape", setOf("m1", "m2"), shapeAB.getMembers());

        // exactly one shape carries members; the other is the drained A-only remainder
        int populated = 0;
        GroupShape leftover = null;
        for (GroupShape shape : shapes) {
            if (shape.getMembers().isEmpty()) {
                leftover = shape;
            } else {
                populated++;
            }
        }
        assertEquals("only one shape carries members", 1, populated);
        assertEquals("the leftover empty shape is the original A-only shape", setOf("A"), leftover.getGroups());
        assertTrue("the leftover shape has no members", leftover.getMembers().isEmpty());
    }

    @Test
    public void groupShapeToStringListsGroupsAndMembers() throws Exception {
        // Arrange — produce a shape via the algorithm
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "m1"));
        GroupShape shape = shapes.iterator().next();

        // Act
        String s = shape.toString();

        // Assert — human-readable dump mentions the group and the member
        assertTrue("toString mentions group", s.contains("group A"));
        assertTrue("toString mentions member", s.contains("m1"));
    }

    @Test
    public void shapeMembersThreeWayOverlapIsolatesCommonMember() throws Exception {
        // Arrange — A,B,C all contain m_all; each also has a private member
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "m_all", "mA"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("B", "m_all", "mB"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("C", "m_all", "mC"));

        // Assert — the shape holding m_all belongs to all three groups
        GroupShape common = findShapeContaining(shapes, "m_all");
        assertEquals("m_all is in A, B and C", setOf("A", "B", "C"), common.getGroups());
        assertEquals("only the common member is in the ABC shape", setOf("m_all"), common.getMembers());
    }

    @Test
    public void shapeMembersCalendarResourceTypeShapesCrBucketIndependently() throws Exception {
        // Arrange — two groups sharing a calendar resource, with crs in their own bucket
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        AllGroupMembers groupA = new AllGroupMembers("A");
        groupA.getMembers(TargetType.calresource).add("room1");
        groupA.getMembers(TargetType.calresource).add("room2");
        AllGroupMembers groupB = new AllGroupMembers("B");
        groupB.getMembers(TargetType.calresource).add("room2");

        // Act — shape on the calresource target type only
        GroupShape.shapeMembers(TargetType.calresource, shapes, groupA);
        GroupShape.shapeMembers(TargetType.calresource, shapes, groupB);

        // Assert — room2 is shared (AB), room1 is A-only
        GroupShape shared = findShapeContaining(shapes, "room2");
        assertEquals("the shared room belongs to both groups", setOf("A", "B"), shared.getGroups());
        GroupShape onlyA = findShapeContaining(shapes, "room1");
        assertEquals("room1 stays in A only", setOf("A"), onlyA.getGroups());
    }

    @Test
    public void shapeMembersDlTypeShapesDistributionListBucketIndependently() throws Exception {
        // Arrange — nested-dl membership shaped via the dl target type
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        AllGroupMembers parent = new AllGroupMembers("parent");
        parent.getMembers(TargetType.dl).add("childdl@zimbra.com");

        // Act
        GroupShape.shapeMembers(TargetType.dl, shapes, parent);

        // Assert — the nested dl forms a single shape under the parent group
        assertEquals(1, shapes.size());
        GroupShape shape = shapes.iterator().next();
        assertTrue(shape.getMembers().contains("childdl@zimbra.com"));
        assertEquals(setOf("parent"), shape.getGroups());
    }

    @Test
    public void shapeMembersSecondGroupSupersetOfFirstSplitsIntoSharedAndExtra() throws Exception {
        // Arrange — A={m1,m2}, B={m1,m2,m3}: B is a superset of A
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "m1", "m2"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("B", "m1", "m2", "m3"));

        // Assert — {m1,m2} become AB; m3 becomes B-only; the drained A shape is left empty
        GroupShape ab = findShapeContaining(shapes, "m1");
        assertEquals(setOf("A", "B"), ab.getGroups());
        assertTrue(ab.getMembers().contains("m2"));

        GroupShape onlyB = findShapeContaining(shapes, "m3");
        assertEquals("m3 belongs to B only", setOf("B"), onlyB.getGroups());
    }

    @Test
    public void shapeMembersMemberOfTypeNotRequestedIsIgnoredForThatShaping() throws Exception {
        // Arrange — a group whose only members are calendar resources, shaped by account type
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        AllGroupMembers g = new AllGroupMembers("crOnly");
        g.getMembers(TargetType.calresource).add("room@zimbra.com");

        // Act — request account shaping; the cr member must not contribute an account shape
        GroupShape.shapeMembers(TargetType.account, shapes, g);

        // Assert — no account-type members => no shape is produced
        assertTrue("cr-only group contributes nothing to account shaping", shapes.isEmpty());
    }

    @Test
    public void allGroupMembersSeparateBucketsDoNotLeakAcrossTypes() throws Exception {
        // Arrange
        AllGroupMembers g = new AllGroupMembers("g@zimbra.com");
        g.getMembers(TargetType.account).add("a@zimbra.com");

        // Act / Assert — the other buckets remain empty and independent
        assertTrue("account bucket holds the account", g.getMembers(TargetType.account).contains("a@zimbra.com"));
        assertTrue("calresource bucket is empty", g.getMembers(TargetType.calresource).isEmpty());
        assertTrue("dl bucket is empty", g.getMembers(TargetType.dl).isEmpty());
    }

    @Test
    public void groupShapeToStringMultipleGroupsAndMembersListsAll() throws Exception {
        // Arrange — build an AB shape via overlapping groups
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "shared"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("B", "shared"));
        GroupShape ab = findShapeContaining(shapes, "shared");

        // Act
        String s = ab.toString();

        // Assert — both group labels and the member appear in the dump
        assertTrue("dump lists group A", s.contains("group A"));
        assertTrue("dump lists group B", s.contains("group B"));
        assertTrue("dump lists the shared member", s.contains("shared"));
    }

    @Test
    public void shapeMembersFourWayCascadingOverlapBuildsExpectedSharedShapes() throws Exception {
        // Arrange — a chain where each later group shares one member with the prior
        Set<GroupShape> shapes = new HashSet<GroupShape>();
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("A", "m1", "m2"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("B", "m2", "m3"));
        GroupShape.shapeMembers(TargetType.account, shapes, groupWithAccounts("C", "m3", "m4"));

        // Assert — m2 is shared by A,B; m3 is shared by B,C
        assertEquals("m2 in A and B", setOf("A", "B"), findShapeContaining(shapes, "m2").getGroups());
        assertEquals("m3 in B and C", setOf("B", "C"), findShapeContaining(shapes, "m3").getGroups());
        assertEquals("m1 in A only", setOf("A"), findShapeContaining(shapes, "m1").getGroups());
        assertEquals("m4 in C only", setOf("C"), findShapeContaining(shapes, "m4").getGroups());
    }

    // ---- helpers ----

    private static GroupShape findShapeContaining(Set<GroupShape> shapes, String member) {
        for (GroupShape s : shapes) {
            if (s.getMembers().contains(member)) {
                return s;
            }
        }
        throw new AssertionError("no shape contains member " + member);
    }

    private static Set<String> setOf(String... values) {
        Set<String> s = new HashSet<String>();
        for (String v : values) {
            s.add(v);
        }
        return s;
    }
}
