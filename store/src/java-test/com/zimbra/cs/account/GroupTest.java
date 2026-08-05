/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import com.zimbra.common.account.ZAttrProvisioning.DistributionListSubscriptionPolicy;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListUnsubscriptionPolicy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for the concrete logic in the abstract {@link Group} class.
 * A minimal concrete {@code TestGroup} subclass is defined so the real
 * (non-abstract) behavior — alias matching, address sets, GAL hiding,
 * subscription-policy defaulting, HAB flag state, and server resolution — can be
 * exercised against the in-memory MockProvisioning harness with deep assertions.
 */
public class GroupTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        // Initialize the right registry so User.R_ownDistList (the GROUP_OWNER_RIGHT) is non-null
        // and ACEs granting it can be parsed/serialized for the GroupOwner workflow tests.
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllUserRights();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    /** Minimal concrete Group implementing the abstract surface for testing concrete methods. */
    private static class TestGroup extends Group {
        private final DistributionListSubscriptionPolicy subPolicy;

        private final DistributionListUnsubscriptionPolicy unsubPolicy;

        TestGroup(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            this(name, id, attrs, prov, null, null);
        }

        TestGroup(String name, String id, Map<String, Object> attrs, Provisioning prov,
                DistributionListSubscriptionPolicy subPolicy,
                DistributionListUnsubscriptionPolicy unsubPolicy) {
            super(name, id, attrs, prov);
            this.subPolicy = subPolicy;
            this.unsubPolicy = unsubPolicy;
        }

        @Override
        public boolean isDynamic() {
            return false;
        }

        @Override
        public String[] getAliases() throws ServiceException {
            return new String[0];
        }

        @Override
        public Domain getDomain() throws ServiceException {
            return null;
        }

        @Override
        public String[] getAllMembers() throws ServiceException {
            return new String[0];
        }

        @Override
        public Set<String> getAllMembersSet() throws ServiceException {
            return null;
        }

        @Override
        public String getDisplayName() {
            return getAttr(Provisioning.A_displayName);
        }

        @Override
        public String getMail() {
            return getName();
        }

        @Override
        public boolean isPrefReplyToEnabled() {
            return false;
        }

        @Override
        public String getPrefReplyToAddress() {
            return null;
        }

        @Override
        public String getPrefReplyToDisplay() {
            return null;
        }

        @Override
        public DistributionListSubscriptionPolicy getDistributionListSubscriptionPolicy() {
            return subPolicy;
        }

        @Override
        public DistributionListUnsubscriptionPolicy getDistributionListUnsubscriptionPolicy() {
            return unsubPolicy;
        }
    }

    private TestGroup newGroup(String name, Map<String, Object> attrs) {
        return new TestGroup(name, "group-id-" + name, attrs, prov);
    }

    @Test
    public void getSubscriptionPolicyNullPolicyReturnsRejectDefault() {
        // Arrange — no explicit policy supplied
        TestGroup g = newGroup("dl@example.com", new HashMap<String, Object>());

        // Act + Assert — defaulting branch kicks in
        assertEquals(Group.DEFAULT_SUBSCRIPTION_POLICY, g.getSubscriptionPolicy());
        assertEquals(DistributionListSubscriptionPolicy.REJECT, g.getSubscriptionPolicy());
    }

    @Test
    public void getSubscriptionPolicyExplicitPolicyReturnsThatPolicy() {
        // Arrange — explicit ACCEPT policy
        TestGroup g = new TestGroup("dl@example.com", "id", new HashMap<String, Object>(), prov,
                DistributionListSubscriptionPolicy.ACCEPT, null);

        // Act + Assert — non-null policy is returned as-is, not defaulted
        assertEquals(DistributionListSubscriptionPolicy.ACCEPT, g.getSubscriptionPolicy());
    }

    @Test
    public void getUnsubscriptionPolicyNullPolicyReturnsRejectDefault() {
        // Arrange
        TestGroup g = newGroup("dl@example.com", new HashMap<String, Object>());

        // Act + Assert
        assertEquals(Group.DEFAULT_UNSUBSCRIPTION_POLICY, g.getUnsubscriptionPolicy());
        assertEquals(DistributionListUnsubscriptionPolicy.REJECT, g.getUnsubscriptionPolicy());
    }

    @Test
    public void getUnsubscriptionPolicyExplicitPolicyReturnsThatPolicy() {
        // Arrange
        TestGroup g = new TestGroup("dl@example.com", "id", new HashMap<String, Object>(), prov,
                null, DistributionListUnsubscriptionPolicy.ACCEPT);

        // Act + Assert
        assertEquals(DistributionListUnsubscriptionPolicy.ACCEPT, g.getUnsubscriptionPolicy());
    }

    @Test
    public void hideInGalAttributeTrueReturnsTrue() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, "TRUE");
        TestGroup g = newGroup("hidden@example.com", attrs);

        // Act + Assert
        assertTrue(g.hideInGal());
    }

    @Test
    public void hideInGalAttributeAbsentReturnsFalse() {
        // Arrange — no hideInGal attribute set
        TestGroup g = newGroup("visible@example.com", new HashMap<String, Object>());

        // Act + Assert
        assertFalse(g.hideInGal());
    }

    @Test
    public void isAddrOfEntryPrimaryNameMatchesCaseInsensitively() {
        // Arrange
        TestGroup g = newGroup("group@example.com", new HashMap<String, Object>());

        // Act + Assert — the address is lowercased before comparison
        assertTrue("upper-cased primary name must match", g.isAddrOfEntry("GROUP@EXAMPLE.COM"));
    }

    @Test
    public void isAddrOfEntryAliasAddressMatchesViaAliasSet() {
        // Arrange — group carries an alias
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "alias@example.com");
        TestGroup g = newGroup("group@example.com", attrs);

        // Act + Assert — alias is recognized as an address of the group
        assertTrue(g.isAddrOfEntry("alias@example.com"));
    }

    @Test
    public void isAddrOfEntryUnrelatedAddressReturnsFalse() {
        // Arrange
        TestGroup g = newGroup("group@example.com", new HashMap<String, Object>());

        // Act + Assert
        assertFalse(g.isAddrOfEntry("stranger@example.com"));
    }

    @Test
    public void getAllAddrsSetIncludesNameAndAliasesAndIsUnmodifiable() {
        // Arrange — primary name plus one alias
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "alias@example.com");
        TestGroup g = newGroup("group@example.com", attrs);

        // Act
        Set<String> addrs = g.getAllAddrsSet();

        // Assert — both the name and alias are present
        assertTrue("primary name present", addrs.contains("group@example.com"));
        assertTrue("alias present", addrs.contains("alias@example.com"));
        assertEquals(2, addrs.size());

        // Assert — the returned set is unmodifiable
        try {
            addrs.add("x@example.com");
            fail("getAllAddrsSet must return an unmodifiable set");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void setHABGroupThenIsHABGroupReflectsState() {
        // Arrange — default state is false
        TestGroup g = newGroup("group@example.com", new HashMap<String, Object>());
        assertFalse("HAB flag defaults to false", g.isHABGroup());

        // Act — flip the flag on
        g.setHABGroup(true);

        // Assert — state transition observed
        assertTrue(g.isHABGroup());

        // Act — flip back off
        g.setHABGroup(false);
        assertFalse(g.isHABGroup());
    }

    @Test
    public void getServerNoMailHostReturnsNull() throws Exception {
        // Arrange — no zimbraMailHost attribute
        TestGroup g = newGroup("group@example.com", new HashMap<String, Object>());

        // Act + Assert — null host short-circuits to null server
        assertNull(g.getServer());
    }

    @Test
    public void getServerWithMailHostResolvesServerFromProvisioning() throws Exception {
        // Arrange — register a server and point the group at it
        prov.createServer("mail1.example.com", new HashMap<String, Object>());
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailHost, "mail1.example.com");
        TestGroup g = newGroup("group@example.com", attrs);

        // Act
        Server resolved = g.getServer();

        // Assert — the real server object is resolved through the harness
        assertNotNull("server must resolve via provisioning", resolved);
        assertEquals("mail1.example.com", resolved.getName());
    }

    @Test
    public void getHABMembersBaseImplementationThrowsUnsupported() throws Exception {
        // Arrange
        TestGroup g = newGroup("group@example.com", new HashMap<String, Object>());

        // Act + Assert — the base Group does not support HAB member listing
        try {
            g.getHABMembers();
            fail("expected UNSUPPORTED ServiceException");
        } catch (ServiceException e) {
            assertEquals(ServiceException.UNSUPPORTED, e.getCode());
        }
    }

    @Test
    public void isMemberOfMockProvisioningInACLGroupUnsupportedPropagates() throws Exception {
        // Arrange — MockProvisioning.inACLGroup is unsupported, so isMemberOf must surface it
        TestGroup g = newGroup("group@example.com", new HashMap<String, Object>());
        Account acct = prov.createAccount("member@example.com", "secret",
                new HashMap<String, Object>());

        // Act + Assert
        try {
            g.isMemberOf(acct);
            fail("expected UNSUPPORTED ServiceException from inACLGroup");
        } catch (ServiceException e) {
            assertEquals(ServiceException.UNSUPPORTED, e.getCode());
        }
    }

    @Test
    public void groupOwnerOwnerRightConstantIsOwnDistListUserRight() {
        // Act + Assert — the static owner-right wiring is the ownDistList user right reference.
        assertSame(com.zimbra.cs.account.accesscontrol.Rights.User.R_ownDistList,
                Group.GroupOwner.GROUP_OWNER_RIGHT);
    }

    /** Minimal concrete DistributionList so a real group target can carry an ownDistList ACE. */
    private static final class TestDL extends DistributionList {
        TestDL(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }

        @Override
        public Domain getDomain() throws ServiceException {
            return null;
        }
    }

    private void grantOwner(DistributionList dl, String granteeId) throws Exception {
        com.zimbra.cs.account.accesscontrol.ZimbraACE ace =
                new com.zimbra.cs.account.accesscontrol.ZimbraACE(
                        granteeId,
                        com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER,
                        Group.GroupOwner.GROUP_OWNER_RIGHT, null, null);
        java.util.Set<com.zimbra.cs.account.accesscontrol.ZimbraACE> aces =
                new java.util.HashSet<com.zimbra.cs.account.accesscontrol.ZimbraACE>();
        aces.add(ace);
        com.zimbra.cs.account.accesscontrol.ACLUtil.grantRight(prov, dl, aces);
    }

    @Test
    public void getOwnersNoAclReturnsEmptyList() throws Exception {
        // Arrange — a group with no zimbraACE attribute at all
        TestGroup g = newGroup("noowners@example.com", new HashMap<String, Object>());

        // Act
        java.util.List<Group.GroupOwner> owners = Group.GroupOwner.getOwners(g, false);

        // Assert — null ACL path yields an empty (but non-null) owner list
        assertNotNull(owners);
        assertTrue("a group with no grants has no owners", owners.isEmpty());
    }

    @Test
    public void getOwnerEmailsNoAclAddsNothing() throws Exception {
        // Arrange
        TestGroup g = newGroup("noemails@example.com", new HashMap<String, Object>());
        java.util.List<String> result = new java.util.ArrayList<String>();

        // Act
        Group.GroupOwner.getOwnerEmails(g, result);

        // Assert — no ACEs means the collection is left untouched
        assertTrue(result.isEmpty());
    }

    @Test
    public void getOwnersWithGrantedOwnerAceReturnsOwnerWithTypeAndId() throws Exception {
        // Arrange — a real account that will be the appointed owner, and a DL it owns
        Account owner = prov.createAccount("dlowner@example.com", "pw",
                new HashMap<String, Object>());
        TestDL dl = new TestDL("owned@example.com", "dl-owned-1",
                new HashMap<String, Object>(), prov);
        grantOwner(dl, owner.getId());

        // Act — needName=true exercises the GroupOwner display-name population branch
        java.util.List<Group.GroupOwner> owners = Group.GroupOwner.getOwners(dl, true);

        // Assert — exactly one owner, the granted account, with correct grantee type/id/name
        assertEquals(1, owners.size());
        Group.GroupOwner go = owners.get(0);
        assertEquals(com.zimbra.cs.account.accesscontrol.GranteeType.GT_USER, go.getType());
        assertEquals(owner.getId(), go.getId());
        assertEquals("dlowner@example.com", go.getName());
    }

    @Test
    public void getOwnersNeedNameFalseLeavesNameNull() throws Exception {
        // Arrange
        Account owner = prov.createAccount("dlowner2@example.com", "pw",
                new HashMap<String, Object>());
        TestDL dl = new TestDL("owned2@example.com", "dl-owned-2",
                new HashMap<String, Object>(), prov);
        grantOwner(dl, owner.getId());

        // Act — needName=false skips display-name lookup
        java.util.List<Group.GroupOwner> owners = Group.GroupOwner.getOwners(dl, false);

        // Assert — the owner is present but its name was never populated
        assertEquals(1, owners.size());
        assertEquals(owner.getId(), owners.get(0).getId());
        assertNull("name must remain null when needName is false", owners.get(0).getName());
    }

    @Test
    public void getOwnerEmailsWithGrantedOwnerAceCollectsGranteeDisplayName() throws Exception {
        // Arrange
        Account owner = prov.createAccount("emailowner@example.com", "pw",
                new HashMap<String, Object>());
        TestDL dl = new TestDL("emailowned@example.com", "dl-owned-3",
                new HashMap<String, Object>(), prov);
        grantOwner(dl, owner.getId());
        java.util.List<String> result = new java.util.ArrayList<String>();

        // Act
        Group.GroupOwner.getOwnerEmails(dl, result);

        // Assert — the owner's display name (its account name) is collected
        assertEquals(1, result.size());
        assertEquals("emailowner@example.com", result.get(0));
    }

    @Test
    public void hasOwnerPrivilegeGlobalAdminReturnsTrue() throws Exception {
        // Arrange — a global admin account always has owner privilege (admin short-circuit)
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = prov.createAccount("groupadmin@example.com", "pw", attrs);
        TestDL dl = new TestDL("adminowned@example.com", "dl-owned-4",
                new HashMap<String, Object>(), prov);

        // Act + Assert — hasOwnerPrivilege takes admin privilege into account
        assertTrue(Group.GroupOwner.hasOwnerPrivilege(admin, dl));
    }

    @Test
    public void isOwnerNonAdminNoGrantReturnsFalse() throws Exception {
        // Arrange — a regular (non-admin) account that was never appointed owner
        Account regular = prov.createAccount("notowner@example.com", "pw",
                new HashMap<String, Object>());
        TestDL dl = new TestDL("unowned@example.com", "dl-owned-5",
                new HashMap<String, Object>(), prov);

        // Act + Assert — isOwner ignores admin privilege and finds no owner grant
        assertFalse(Group.GroupOwner.isOwner(regular, dl));
    }
}
