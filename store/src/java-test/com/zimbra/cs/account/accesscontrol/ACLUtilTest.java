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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link ACLUtil}: the grant -> read -> revoke workflow against a real
 * Account target in the in-memory MockProvisioning harness, with persistence verified by
 * reloading the ACL from the entry's attributes, plus the empty-ACL accessor behaviour.
 */
public class ACLUtilTest {

    private static Right userRight;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        RightManager rm = RightManager.getInstance();
        // Pick any real, registered user right to grant (round-trips through serialize).
        userRight = rm.getAllUserRights().values().iterator().next();
        assertNotNull("expected a registered user right", userRight);
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(name, "pw", attrs);
    }

    private ZimbraACE userAce(String granteeId) throws Exception {
        return new ZimbraACE(granteeId, GranteeType.GT_USER, userRight, null, null);
    }

    @Test
    public void getACLNoGrantsReturnsNull() throws Exception {
        // Arrange — a target with no ACEs
        Account target = createAccount("aclempty@example.com");

        // Act
        ZimbraACL acl = ACLUtil.getACL(target);

        // Assert
        assertNull("no grants means no ACL", acl);

        prov.deleteAccount(target.getId());
    }

    @Test
    public void getAllACEsNoGrantsReturnsNull() throws Exception {
        // Arrange
        Account target = createAccount("aceempty@example.com");

        // Act
        List<ZimbraACE> aces = ACLUtil.getAllACEs(target);

        // Assert
        assertNull(aces);

        prov.deleteAccount(target.getId());
    }

    @Test
    public void grantRightNewAclPersistsAceRetrievableByReload() throws Exception {
        // Arrange
        Account target = createAccount("aclgrant@example.com");
        Account grantee = createAccount("aclgrantee@example.com");
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(userAce(grantee.getId()));

        // Act
        List<ZimbraACE> granted = ACLUtil.grantRight(prov, target, aces);

        // Assert — returned grant
        assertEquals(1, granted.size());

        // Assert — persisted: reload from the entry attribute (cache erased by serialize)
        List<ZimbraACE> reloaded = ACLUtil.getAllACEs(target);
        assertNotNull("ACE must persist on the target entry", reloaded);
        assertEquals(1, reloaded.size());
        assertEquals(grantee.getId(), reloaded.get(0).getGrantee());
        assertEquals(userRight.getName(), reloaded.get(0).getRight().getName());

        prov.deleteAccount(target.getId());
        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void grantRightSecondAceAppendsToExistingAcl() throws Exception {
        // Arrange — grant one, then grant a second to grow the existing ACL
        Account target = createAccount("aclgrow@example.com");
        Account g1 = createAccount("aclg1@example.com");
        Account g2 = createAccount("aclg2@example.com");

        Set<ZimbraACE> first = new HashSet<ZimbraACE>();
        first.add(userAce(g1.getId()));
        ACLUtil.grantRight(prov, target, first);

        Set<ZimbraACE> second = new HashSet<ZimbraACE>();
        second.add(userAce(g2.getId()));

        // Act
        ACLUtil.grantRight(prov, target, second);

        // Assert — both ACEs now present
        List<ZimbraACE> reloaded = ACLUtil.getAllACEs(target);
        assertNotNull(reloaded);
        assertEquals(2, reloaded.size());

        prov.deleteAccount(target.getId());
        prov.deleteAccount(g1.getId());
        prov.deleteAccount(g2.getId());
    }

    @Test
    public void revokeRightExistingAceRemovesItFromPersistedAcl() throws Exception {
        // Arrange — grant first
        Account target = createAccount("aclrevoke@example.com");
        Account grantee = createAccount("aclrevgrantee@example.com");
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(userAce(grantee.getId()));
        ACLUtil.grantRight(prov, target, aces);
        assertEquals(1, ACLUtil.getAllACEs(target).size());

        // Act — revoke the same ACE
        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(userAce(grantee.getId()));
        List<ZimbraACE> revoked = ACLUtil.revokeRight(prov, target, toRevoke);

        // Assert — one revoked, and the ACL is now empty
        assertEquals(1, revoked.size());
        List<ZimbraACE> remaining = ACLUtil.getAllACEs(target);
        assertTrue("ACL should be empty after revoke",
                remaining == null || remaining.isEmpty());

        prov.deleteAccount(target.getId());
        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void revokeRightNoExistingAclReturnsEmptyList() throws Exception {
        // Arrange — target with no grants
        Account target = createAccount("aclrevnone@example.com");
        Account grantee = createAccount("aclrevnonegrantee@example.com");
        Set<ZimbraACE> toRevoke = new HashSet<ZimbraACE>();
        toRevoke.add(userAce(grantee.getId()));

        // Act
        List<ZimbraACE> revoked = ACLUtil.revokeRight(prov, target, toRevoke);

        // Assert — nothing to revoke, empty (non-null) list
        assertNotNull(revoked);
        assertTrue(revoked.isEmpty());

        prov.deleteAccount(target.getId());
        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void getACEsFilterByRightReturnsMatchingAces() throws Exception {
        // Arrange
        Account target = createAccount("aclfilter@example.com");
        Account grantee = createAccount("aclfiltergrantee@example.com");
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(userAce(grantee.getId()));
        ACLUtil.grantRight(prov, target, aces);

        Set<Right> filter = new HashSet<Right>();
        filter.add(userRight);

        // Act
        List<ZimbraACE> matching = ACLUtil.getACEs(target, filter);

        // Assert
        assertNotNull(matching);
        assertEquals(1, matching.size());
        assertEquals(userRight.getName(), matching.get(0).getRight().getName());

        prov.deleteAccount(target.getId());
        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void getACLAfterGrantCachesAclOnEntry() throws Exception {
        // Arrange — grant, then read non-forced (cache) ACL
        Account target = createAccount("aclcache@example.com");
        Account grantee = createAccount("aclcachegrantee@example.com");
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(userAce(grantee.getId()));
        ACLUtil.grantRight(prov, target, aces);

        // Act — first read loads & caches, second read should return same cached instance
        ZimbraACL first = ACLUtil.getACL(target);
        ZimbraACL second = ACLUtil.getACL(target);

        // Assert
        assertNotNull(first);
        assertEquals(1, first.getAllACEs().size());
        assertTrue("second read should hit the entry ACL cache", first == second);

        prov.deleteAccount(target.getId());
        prov.deleteAccount(grantee.getId());
    }
}
