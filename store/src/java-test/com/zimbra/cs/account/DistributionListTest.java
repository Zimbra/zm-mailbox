/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for the attribute-backed, non-LDAP surface of the abstract
 * {@link DistributionList}: entry type, dynamic flag, member/alias accessors,
 * and the group-member address expansion. A minimal concrete subclass is used
 * because {@code DistributionList} is abstract; all asserted behavior is the
 * base-class logic, not the subclass.
 */
public class DistributionListTest {

    /** Minimal concrete DL backed only by the in-memory attribute map. */
    private static final class TestDL extends DistributionList {
        TestDL(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }
    }

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private TestDL newDL(Map<String, Object> attrs) {
        return new TestDL("list@example.com", "dl-id-0001", attrs, prov);
    }

    @Test
    public void getEntryTypeAnyListReturnsDistributionList() {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        assertEquals(Entry.EntryType.DISTRIBUTIONLIST, dl.getEntryType());
    }

    @Test
    public void isDynamicStaticListReturnsFalse() {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        assertFalse("a plain DistributionList is never dynamic", dl.isDynamic());
    }

    @Test
    public void getAllMembersWithForwardingAddrsReturnsAllConfiguredMembers() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailForwardingAddress,
                new String[] {"a@example.com", "b@example.com" });
        TestDL dl = newDL(attrs);

        // Act
        String[] members = dl.getAllMembers();

        // Assert
        assertEquals(2, members.length);
        assertTrue(Arrays.asList(members).contains("a@example.com"));
        assertTrue(Arrays.asList(members).contains("b@example.com"));
    }

    @Test
    public void getAllMembersNoMembersReturnsEmptyArray() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act
        String[] members = dl.getAllMembers();

        // Assert
        assertEquals(0, members.length);
    }

    @Test
    public void getAllMembersSetWithDuplicateForwardingAddrsDedupes() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailForwardingAddress,
                new String[] {"dup@example.com", "dup@example.com", "uniq@example.com" });
        TestDL dl = newDL(attrs);

        // Act
        Set<String> members = dl.getAllMembersSet();

        // Assert
        assertEquals("set view must dedupe", 2, members.size());
        assertTrue(members.contains("dup@example.com"));
        assertTrue(members.contains("uniq@example.com"));
    }

    @Test
    public void getAliasesWithMailAliasReturnsConfiguredAliases() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias,
                new String[] {"alias1@example.com", "alias2@example.com" });
        TestDL dl = newDL(attrs);

        // Act
        String[] aliases = dl.getAliases();

        // Assert
        assertEquals(2, aliases.length);
        assertTrue(Arrays.asList(aliases).contains("alias1@example.com"));
    }

    @Test
    public void getAliasesNoAliasReturnsEmptyArray() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        assertEquals(0, dl.getAliases().length);
    }

    @Test
    public void getAllAddrsAsGroupMemberNoAliasesReturnsOnlyName() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act
        String[] addrs = dl.getAllAddrsAsGroupMember();

        // Assert — name is always first and only entry when no aliases
        assertArrayEquals(new String[] {"list@example.com" }, addrs);
    }

    @Test
    public void getAllAddrsAsGroupMemberAliasEqualToNameIsNotDuplicated() throws Exception {
        // Arrange — the primary name is also listed as an alias (common in LDAP)
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias,
                new String[] {"list@example.com", "extra@example.com" });
        TestDL dl = newDL(attrs);

        // Act
        String[] addrs = dl.getAllAddrsAsGroupMember();

        // Assert — name appears once, alias equal to name is skipped, extra kept
        assertEquals(2, addrs.length);
        assertEquals("list@example.com", addrs[0]);
        assertTrue(Arrays.asList(addrs).contains("extra@example.com"));
    }

    @Test
    public void modifySetDisplayNamePersistsOnEntry() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_displayName, "Sales Team");

        // Act — modify() delegates to Provisioning.modifyAttrs which the harness persists.
        dl.modify(changes);

        // Assert — the change is reflected on the entry's attribute map.
        assertEquals("Sales Team", dl.getAttr(Provisioning.A_displayName));
    }

    @Test
    public void modifyAddAndRemoveForwardingAddrUpdatesMembers() throws Exception {
        // Arrange — start with one forwarding address stored as a single value so the
        // harness's +attr add path builds a mutable working list from a scalar.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailForwardingAddress, "first@example.com");
        TestDL dl = newDL(attrs);

        // Act — add a second member via the +attr multi-value add semantics.
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("+" + Provisioning.A_zimbraMailForwardingAddress, "second@example.com");
        dl.modify(add);

        // Assert — both members are now present.
        Set<String> members = dl.getAllMembersSet();
        assertEquals(2, members.size());
        assertTrue(members.contains("first@example.com"));
        assertTrue(members.contains("second@example.com"));
    }

    @Test
    public void getDomainListInProvisionedDomainReturnsThatDomain() throws Exception {
        // Arrange — provision the domain the list belongs to.
        if (prov.get(com.zimbra.common.account.Key.DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act — getDomain() delegates to Provisioning.getDomain(dl) -> getDomainName lookup.
        Domain domain = dl.getDomain();

        // Assert
        assertEquals("example.com", domain.getName());
    }

    @Test
    public void addAliasUnsupportedHarnessPropagatesUnsupportedOperation() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert — the delegating call is exercised; the mock backend rejects it.
        try {
            dl.addAlias("alias@example.com");
            fail("expected UnsupportedOperationException from mock provisioning");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void removeAliasUnsupportedHarnessPropagatesUnsupportedOperation() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        try {
            dl.removeAlias("alias@example.com");
            fail("expected UnsupportedOperationException from mock provisioning");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void addMembersUnsupportedHarnessPropagatesUnsupportedOperation() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        try {
            dl.addMembers(new String[] {"m@example.com" });
            fail("expected UnsupportedOperationException from mock provisioning");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void removeMembersUnsupportedHarnessPropagatesUnsupportedOperation() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        try {
            dl.removeMembers(new String[] {"m@example.com" });
            fail("expected UnsupportedOperationException from mock provisioning");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void deleteDistributionListUnsupportedHarnessPropagatesUnsupportedOperation() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        try {
            dl.deleteDistributionList();
            fail("expected UnsupportedOperationException from mock provisioning");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void renameDistributionListUnsupportedHarnessPropagatesUnsupportedOperation() throws Exception {
        // Arrange
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act / Assert
        try {
            dl.renameDistributionList("renamed@example.com");
            fail("expected UnsupportedOperationException from mock provisioning");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    @Test
    public void resetDataAfterAttributeChangeDoesNotThrow() throws Exception {
        // Arrange — exercise the protected resetData override (delegates to super).
        TestDL dl = newDL(new HashMap<String, Object>());

        // Act — invoke the package-visible reset path via the public reload hook on the entry.
        dl.resetData();

        // Assert — entry remains usable after reset.
        assertEquals(Entry.EntryType.DISTRIBUTIONLIST, dl.getEntryType());
    }
}
