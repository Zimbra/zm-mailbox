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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Functional tests for {@link TargetIterator}. Drives the real iterator chain over entries from
 * the in-memory {@link com.zimbra.cs.account.MockProvisioning} harness. The harness'
 * {@code getGlobalGrant()} returns null, so chains end cleanly at the global step. Group
 * expansion (which needs LDAP-backed {@code getGroupMembership}) is exercised only in the
 * non-expand path where it is not reached.
 */
public class TargetIteratorFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        // The pseudo-account path resolves the default COS (Provisioning.get(CosBy.name, DEFAULT_COS_NAME))
        // to populate account defaults; the in-memory harness does not pre-register it, so create it here.
        if (prov.get(com.zimbra.common.account.Key.CosBy.name, Provisioning.DEFAULT_COS_NAME) == null) {
            prov.createCos(Provisioning.DEFAULT_COS_NAME, new HashMap<String, Object>());
        }
        if (prov.get(DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
        prov.createAccount("user@example.com", "secret", new HashMap<String, Object>());
    }

    /* Drain the iterator into a list, stopping at the first null. */
    private List<Entry> drain(TargetIterator iter) throws ServiceException {
        List<Entry> entries = new ArrayList<Entry>();
        Entry e;
        while ((e = iter.next()) != null) {
            entries.add(e);
        }
        return entries;
    }

    @Test
    public void getTargetIeteratorAccountReturnsAccountIterator() throws Exception {
        // Arrange
        Account acct = prov.get(AccountBy.name, "user@example.com");

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, acct, false);

        // Assert — the factory chooses the account-specialized iterator.
        assertNotNull(iter);
        assertTrue(iter instanceof TargetIterator.AccountTargetIterator);
    }

    @Test
    public void nextAccountNoExpandWalksDomainThenGlobal() throws Exception {
        // Arrange — non-expand path skips group membership, going account -> domain -> global.
        Account acct = prov.get(AccountBy.name, "user@example.com");
        Domain domain = prov.get(DomainBy.name, "example.com");

        // The factory consumes the perspective target (self) already, so first next() is the domain.
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, acct, false);

        // Act
        Entry domainEntry = iter.next();

        // Assert — the account's domain is yielded next.
        assertNotNull(domainEntry);
        assertTrue(domainEntry instanceof Domain);
        assertEquals(domain.getId(), ((Domain) domainEntry).getId());

        // After the domain, the global step yields null (harness getGlobalGrant() is null).
        assertNull("global grant is null in the harness", iter.next());
    }

    @Test
    public void getTargetIeteratorDomainReturnsDomainIterator() throws Exception {
        // Arrange
        Domain domain = prov.get(DomainBy.name, "example.com");

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, domain, false);

        // Assert
        assertNotNull(iter);
        assertTrue(iter instanceof TargetIterator.DomainTargetIterator);
    }

    @Test
    public void nextSimpleDomainNoSuperDomainEndsAtGlobal() throws Exception {
        // Arrange — "example.com" has no provisioned super-domain ("com" is not created).
        Domain domain = prov.get(DomainBy.name, "example.com");
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, domain, false);

        // Act — factory consumed self; with no super domains the next step is global (null).
        Entry next = iter.next();

        // Assert
        assertNull("no super domain and null global grant", next);
    }

    @Test
    public void nextSubDomainWithSuperDomainYieldsSuperDomain() throws Exception {
        // Arrange — create a parent domain so the sub-domain walk has something to yield.
        if (prov.get(DomainBy.name, "top.example.com") == null) {
            prov.createDomain("top.example.com", new HashMap<String, Object>());
        }
        if (prov.get(DomainBy.name, "child.top.example.com") == null) {
            prov.createDomain("child.top.example.com", new HashMap<String, Object>());
        }
        Domain parent = prov.get(DomainBy.name, "top.example.com");
        Domain child = prov.get(DomainBy.name, "child.top.example.com");

        TargetIterator iter = TargetIterator.getTargetIeterator(prov, child, false);

        // Act — factory consumed the child itself; the next entry is the parent domain.
        Entry superDomain = iter.next();

        // Assert
        assertNotNull(superDomain);
        assertTrue(superDomain instanceof Domain);
        assertEquals(parent.getId(), ((Domain) superDomain).getId());
    }

    @Test
    public void getTargetIeteratorConfigReturnsConfigIterator() throws Exception {
        // Arrange
        Config config = prov.getConfig();

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, config, false);

        // Assert — config uses the base iterator's global-only chain.
        assertNotNull(iter);
        assertTrue(iter instanceof TargetIterator.ConfigTargetIterator);
    }

    @Test
    public void nextConfigEndsAtGlobalImmediately() throws Exception {
        // Arrange — base TargetIterator: self was consumed by factory; next is the global step.
        Config config = prov.getConfig();
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, config, false);

        // Act / Assert — global grant is null, so the chain is exhausted.
        assertNull(iter.next());
    }

    @Test
    public void nextDynamicGroupBaseIteratorTerminatesCleanly() throws Exception {
        // Arrange — the GlobalGrant-style base iterators end without touching LDAP. Verify the
        // server iterator (a pure base-class iterator) drains to empty after the factory consumes
        // the perspective target.
        com.zimbra.cs.account.Server server = prov.getLocalServer();
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, server, false);

        // Act
        List<Entry> remaining = drain(iter);

        // Assert — server iterator only walks the global step, which is null here.
        assertTrue("server iterator yields nothing past self", remaining.isEmpty());
        assertTrue(iter instanceof TargetIterator.ServerTargetIterator);
    }

    @Test
    public void getTargetIeteratorGuestAccountRoutesThroughAccountIterator() throws Exception {
        // Arrange — GuestAccount extends Account, so it must take the account branch.
        Entry guest = new com.zimbra.cs.account.GuestAccount("guest@nowhere.test", null);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, guest, false);

        // Assert
        assertTrue("GuestAccount routes through the account iterator",
                iter instanceof TargetIterator.AccountTargetIterator);
    }

    @Test
    public void getTargetIeteratorCosReturnsCosIterator() throws Exception {
        // Arrange — create a COS and confirm the factory selects the COS base iterator.
        com.zimbra.cs.account.Cos cos = prov.createCos("itercos", new HashMap<String, Object>());

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, cos, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.CosTargetIterator);
        // base iterator: self consumed by factory, global step is null
        assertNull(iter.next());
    }

    @Test
    public void nextAccountExpandGroupsWalksEmptyGroupsThenDomain() throws Exception {
        // Arrange — expand-groups path enters the dl/group branch. The harness'
        // getGroupMembership() returns an empty membership, so the walk falls through to the
        // account's domain, then ends at the null global step.
        Account acct = prov.get(AccountBy.name, "user@example.com");
        Domain domain = prov.get(DomainBy.name, "example.com");
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, acct, true);

        // Act — first next() after self drains the empty groups and yields the domain.
        Entry next = iter.next();

        // Assert
        assertNotNull(next);
        assertTrue(next instanceof Domain);
        assertEquals(domain.getId(), ((Domain) next).getId());

        // After the domain, the global step yields null (harness getGlobalGrant() is null).
        assertNull("global grant is null in the harness", iter.next());
    }

    // ---- factory branch coverage for the simpler entity target types ----

    @Test
    public void getTargetIeteratorZimletReturnsZimletIterator() throws Exception {
        // Arrange — a real Zimlet entity (concrete, no LDAP needed).
        com.zimbra.cs.account.Zimlet zimlet = new com.zimbra.cs.account.Zimlet(
                "myzimlet", "zimlet-id", new HashMap<String, Object>(), prov);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, zimlet, false);

        // Assert — base iterator: self consumed, global step is null.
        assertTrue(iter instanceof TargetIterator.ZimletTargetIterator);
        assertNull(iter.next());
    }

    @Test
    public void getTargetIeteratorXmppComponentReturnsXmppIterator() throws Exception {
        // Arrange
        com.zimbra.cs.account.XMPPComponent xmpp = new com.zimbra.cs.account.XMPPComponent(
                "xmpp1", "xmpp-id", new HashMap<String, Object>(), prov);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, xmpp, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.XMPPComponentTargetIterator);
        assertNull(iter.next());
    }

    @Test
    public void getTargetIeteratorUcServiceReturnsUcServiceIterator() throws Exception {
        // Arrange
        com.zimbra.cs.account.UCService uc = new com.zimbra.cs.account.UCService(
                "uc1", "uc-id", new HashMap<String, Object>(), prov);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, uc, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.UCServiceTargetIterator);
        assertNull(iter.next());
    }

    @Test
    public void getTargetIeteratorAlwaysOnClusterReturnsClusterIterator() throws Exception {
        // Arrange
        com.zimbra.cs.account.AlwaysOnCluster cluster = new com.zimbra.cs.account.AlwaysOnCluster(
                "cluster1", "cluster-id", new HashMap<String, Object>(), null, prov);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, cluster, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.AlwaysOnClusterTargetIterator);
        assertNull(iter.next());
    }

    @Test
    public void getTargetIeteratorGlobalGrantReturnsGlobalGrantIterator() throws Exception {
        // Arrange — a real GlobalGrant entity.
        com.zimbra.cs.account.GlobalGrant gg = new com.zimbra.cs.account.GlobalGrant(
                new HashMap<String, Object>(), prov);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, gg, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.GlobalGrantTargetIterator);
    }

    @Test
    public void nextGlobalGrantYieldsSelfOnceThenNull() throws Exception {
        // Arrange — the factory consumes self (the only entry the GlobalGrant iterator yields).
        com.zimbra.cs.account.GlobalGrant gg = new com.zimbra.cs.account.GlobalGrant(
                new HashMap<String, Object>(), prov);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, gg, false);

        // Act / Assert — after self, the iterator is exhausted (mNoMore guard returns null).
        assertNull("global grant iterator yields nothing past self", iter.next());
        assertNull("repeated next() after exhaustion stays null", iter.next());
    }

    @Test
    public void getTargetIeteratorUnsupportedEntryThrowsFailure() throws Exception {
        // Arrange — an Entry subtype the factory has no branch for.
        Entry unsupported = new Entry(new HashMap<String, Object>(),
                new HashMap<String, Object>(), prov) {
        };

        // Act / Assert
        try {
            TargetIterator.getTargetIeterator(prov, unsupported, false);
            fail("expected FAILURE for an unsupported target type");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("no TargetIterator for"));
        }
    }

    // ---- pseudo-target walks: exercise the group/domain/pseudo-domain branches ----

    @Test
    public void nextPseudoAccountExpandGroupsWalksEmptyGroupsThenPseudoDomain() throws Exception {
        // Arrange — a pseudo account carries an empty group membership and a pseudo domain.
        // expand-groups enters the dl branch, finds no groups, then falls through to the
        // pseudo-domain branch (which recurses straight to the global step).
        Account pseudo = (Account) PseudoTarget.createPseudoTarget(prov, TargetType.account,
                null, null, true, null, null);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudo, true);

        // Act — factory consumed self; the empty-group + pseudo-domain walk ends at null global.
        Entry next = iter.next();

        // Assert
        assertNull("pseudo account with empty groups + pseudo domain ends at null global", next);
    }

    @Test
    public void nextPseudoAccountNoExpandPseudoDomainBranchEndsAtGlobal() throws Exception {
        // Arrange — no-expand pseudo account goes self -> domain; the pseudo-domain branch
        // recurses directly into the (null) global step.
        Account pseudo = (Account) PseudoTarget.createPseudoTarget(prov, TargetType.account,
                null, null, true, null, null);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudo, false);

        // Act / Assert
        assertNull("pseudo domain branch lands on null global", iter.next());
    }

    @Test
    public void getTargetIeteratorPseudoDistributionListReturnsDlIterator() throws Exception {
        // Arrange — a pseudo DL is a concrete DistributionList, so the DL branch is selected.
        Entry pseudoDl = PseudoTarget.createPseudoTarget(prov, TargetType.dl,
                null, null, true, null, null);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudoDl, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.DistributionListTargetIterator);
    }

    @Test
    public void nextPseudoDistributionListNoExpandPseudoDomainBranchEndsAtGlobal() throws Exception {
        // Arrange — no-expand DL goes self -> domain; pseudo DL has a pseudo domain so the
        // domain branch recurses straight to the null global step.
        Entry pseudoDl = PseudoTarget.createPseudoTarget(prov, TargetType.dl,
                null, null, true, null, null);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudoDl, false);

        // Act / Assert
        assertNull(iter.next());
    }

    @Test
    public void nextPseudoDistributionListExpandEmptyGroupsThenPseudoDomain() throws Exception {
        // Arrange — expand path enters the dl/group branch; pseudo DL yields an empty membership
        // then falls through to the pseudo-domain branch.
        Entry pseudoDl = PseudoTarget.createPseudoTarget(prov, TargetType.dl,
                null, null, true, null, null);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudoDl, true);

        // Act / Assert
        assertNull(iter.next());
    }

    @Test
    public void getTargetIeteratorPseudoDynamicGroupReturnsDynamicGroupIterator() throws Exception {
        // Arrange — a pseudo dynamic group (TargetType.group) selects the dynamic-group branch.
        Entry pseudoGroup = PseudoTarget.createPseudoTarget(prov, TargetType.group,
                null, null, true, null, null);

        // Act
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudoGroup, false);

        // Assert
        assertTrue(iter instanceof TargetIterator.DynamicGroupTargetIterator);
    }

    @Test
    public void nextPseudoDynamicGroupPseudoDomainBranchEndsAtGlobal() throws Exception {
        // Arrange — dynamic group walks self -> domain -> global; pseudo group has a pseudo
        // domain so the domain branch recurses to the null global step.
        Entry pseudoGroup = PseudoTarget.createPseudoTarget(prov, TargetType.group,
                null, null, true, null, null);
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, pseudoGroup, false);

        // Act / Assert
        assertNull(iter.next());
    }

    @Test
    public void nextBaseIteratorDrainTwiceStaysExhausted() throws Exception {
        // Arrange — a base-class iterator (COS) over the global-only chain. After the global step
        // yields null, a second next() must still return null (the mNoMore guard).
        com.zimbra.cs.account.Cos cos = prov.createCos("itercos2", new HashMap<String, Object>());
        TargetIterator iter = TargetIterator.getTargetIeterator(prov, cos, false);

        // Act
        Entry first = iter.next();
        Entry second = iter.next();

        // Assert
        assertNull("global step yields null", first);
        assertNull("exhausted iterator stays null", second);
    }

    // ============================================================================================
    // Direct-construction tests. Unlike getTargetIeterator(), constructing an iterator directly
    // does NOT consume the perspective target, so the FIRST next() yields the target itself. This
    // lets us assert the exact entry returned at every step of the chain — pinning down the
    // conditional/return/math behavior that the survived mutations alter.
    // ============================================================================================

    @Test
    public void baseIteratorFirstNextIsSelfThenGlobalNullThenStaysNull() throws Exception {
        // Covers base TargetIterator.next() L56 (mNoMore guard), L61 (!mCheckedSelf), L71 (return
        // grantedOn): the very first next() MUST return the perspective target itself, not null.
        Config config = prov.getConfig();
        TargetIterator.ConfigTargetIterator iter =
                new TargetIterator.ConfigTargetIterator(prov, config);

        // First next() returns self (kills L61-negate / L71 null-return: a null here would fail).
        Entry self = iter.next();
        assertNotNull("first next() must return the target itself", self);
        assertSame("first step is the perspective target", config, self);

        // Second next() is the global step; harness global grant is null.
        assertNull("global step yields null", iter.next());

        // Third next() must stay null via the mNoMore guard (kills L56-negate: if the guard were
        // flipped, the exhausted iterator would re-enter the chain and return self again).
        assertNull("exhausted base iterator stays null", iter.next());
    }

    @Test
    public void accountIteratorDirectNoExpandSelfThenDomainThenGlobal() throws Exception {
        // Covers AccountTargetIterator.next() L149 (mNoMore guard), L154 (!mCheckedSelf),
        // L155 (mExpandGroups branch -> domain not dl), L206 (domain branch yields the domain).
        Account acct = prov.get(AccountBy.name, "user@example.com");
        Domain domain = prov.get(DomainBy.name, "example.com");
        TargetIterator.AccountTargetIterator iter =
                new TargetIterator.AccountTargetIterator(prov, acct, false);

        // Step 1: self.
        Entry self = iter.next();
        assertSame("first step is the account itself", acct, self);

        // Step 2: the account's domain (no-expand skips the dl/group branch entirely).
        Entry dom = iter.next();
        assertNotNull("no-expand account must yield its domain", dom);
        assertTrue(dom instanceof Domain);
        assertEquals(domain.getId(), ((Domain) dom).getId());

        // Step 3: global (null), then exhausted.
        assertNull("global step null", iter.next());
        assertNull("exhausted account iterator stays null", iter.next());
    }

    @Test
    public void accountIteratorDirectExpandSelfThenEmptyGroupsThenDomain() throws Exception {
        // Covers L155 (mExpandGroups true -> mCurTargetType=dl) and the dl branch fall-through:
        // empty membership (harness) means after self the next entry is the DOMAIN, not a group.
        Account acct = prov.get(AccountBy.name, "user@example.com");
        Domain domain = prov.get(DomainBy.name, "example.com");
        TargetIterator.AccountTargetIterator iter =
                new TargetIterator.AccountTargetIterator(prov, acct, true);

        // Step 1: self.
        assertSame(acct, iter.next());

        // Step 2: with an empty group membership the dl branch recurses to the domain.
        Entry dom = iter.next();
        assertNotNull("expand account with empty groups must yield its domain", dom);
        assertTrue(dom instanceof Domain);
        assertEquals(domain.getId(), ((Domain) dom).getId());
    }

    @Test
    public void dlIteratorDirectNoExpandPseudoSelfThenPseudoDomainRecursesToGlobal()
            throws Exception {
        // Covers DistributionListTargetIterator.next() L227 (mNoMore guard), L232 (!mCheckedSelf),
        // L233 (mExpandGroups), L262 (domain branch), L266/L269 (pseudo-domain != null), L279
        // (return grantedOn). Use a pseudo DL so the domain branch hits the pseudo-domain path.
        Entry pseudoDl = PseudoTarget.createPseudoTarget(prov, TargetType.dl,
                null, null, true, null, null);
        TargetIterator.DistributionListTargetIterator iter =
                new TargetIterator.DistributionListTargetIterator(prov, pseudoDl, false);

        // Step 1: self (kills L232-negate / L279 null-return).
        assertSame("first DL step is the pseudo DL itself", pseudoDl, iter.next());

        // Step 2: no-expand -> domain branch; pseudo DL has a pseudo domain so it recurses straight
        // into the (null) global step.
        assertNull("pseudo-domain branch recurses to null global", iter.next());
        assertNull("exhausted DL iterator stays null", iter.next());
    }

    @Test
    public void dynamicGroupIteratorDirectNoExpandSelfThenPseudoDomainThenGlobal()
            throws Exception {
        // Covers DynamicGroupTargetIterator.next() L293 (mNoMore guard), L298 (!mCheckedSelf),
        // L304 (domain branch), L308/L311 (pseudo-domain != null), L321 (return grantedOn).
        Entry pseudoGroup = PseudoTarget.createPseudoTarget(prov, TargetType.group,
                null, null, true, null, null);
        TargetIterator.DynamicGroupTargetIterator iter =
                new TargetIterator.DynamicGroupTargetIterator(prov, pseudoGroup);

        // Step 1: self.
        assertSame("first dynamic-group step is the group itself", pseudoGroup, iter.next());

        // Step 2: domain branch; pseudo group has a pseudo domain so it recurses to null global.
        assertNull("pseudo-domain branch recurses to null global", iter.next());
        assertNull("exhausted dynamic-group iterator stays null", iter.next());
    }

    @Test
    public void globalGrantIteratorDirectFirstNextIsSelfThenNull() throws Exception {
        // Covers GlobalGrantTargetIterator.next() L428 (mNoMore guard), L433 (!mCheckedSelf),
        // L439 (return grantedOn): the first next() returns self exactly once, then null forever.
        com.zimbra.cs.account.GlobalGrant gg = new com.zimbra.cs.account.GlobalGrant(
                new HashMap<String, Object>(), prov);
        TargetIterator.GlobalGrantTargetIterator iter =
                new TargetIterator.GlobalGrantTargetIterator(prov, gg);

        // Step 1: self (kills L433-negate / L439 null-return).
        assertSame("global-grant iterator yields self first", gg, iter.next());

        // Step 2+: exhausted (mNoMore set when self returned). Kills L428-negate: a flipped guard
        // would re-run the !mCheckedSelf branch — but mCheckedSelf is now true, so it would return
        // null anyway; the decisive check is that self is returned exactly ONCE.
        assertNull("global-grant iterator yields nothing past self", iter.next());
        assertNull(iter.next());
    }

    @Test
    public void domainIteratorDirectMultipleSuperDomainsYieldsInOrderSelfFirst() throws Exception {
        // Covers DomainTargetIterator.next() L363 (mNoMore guard), L368 (!mCheckedSelf),
        // L374 (mCurSuperDomain++ MATH): with two provisioned super-domains the walk must yield
        // self, then parent, then grandparent IN ORDER. A broken increment (e.g. -- or no-op) would
        // repeat or skip a super-domain, which these exact-identity assertions detect.
        // Use an isolated ".gptld" hierarchy so no intermediate label (e.g. example.com) is also
        // a provisioned super-domain — keeping the expected super-domain list exactly [parent, gp].
        if (prov.get(DomainBy.name, "gp") == null) {
            prov.createDomain("gp", new HashMap<String, Object>());
        }
        if (prov.get(DomainBy.name, "parent.gp") == null) {
            prov.createDomain("parent.gp", new HashMap<String, Object>());
        }
        if (prov.get(DomainBy.name, "leaf.parent.gp") == null) {
            prov.createDomain("leaf.parent.gp", new HashMap<String, Object>());
        }
        Domain grandparent = prov.get(DomainBy.name, "gp");
        Domain parent = prov.get(DomainBy.name, "parent.gp");
        Domain leaf = prov.get(DomainBy.name, "leaf.parent.gp");

        TargetIterator.DomainTargetIterator iter =
                new TargetIterator.DomainTargetIterator(prov, leaf);

        // Step 1: self.
        Entry s0 = iter.next();
        assertSame("first domain step is the leaf itself", leaf, s0);

        // Step 2: first super-domain (immediate parent).
        Entry s1 = iter.next();
        assertNotNull(s1);
        assertTrue(s1 instanceof Domain);
        assertEquals("first super-domain is the immediate parent", parent.getId(),
                ((Domain) s1).getId());

        // Step 3: second super-domain (grandparent) — proves mCurSuperDomain advanced by exactly 1.
        Entry s2 = iter.next();
        assertNotNull(s2);
        assertTrue(s2 instanceof Domain);
        assertEquals("second super-domain is the grandparent", grandparent.getId(),
                ((Domain) s2).getId());

        // The two super-domains must be DISTINCT (a no-op increment would return parent twice).
        assertFalse("super-domains must differ", parent.getId().equals(grandparent.getId()));
        assertFalse("walk must not repeat the same super-domain",
                ((Domain) s1).getId().equals(((Domain) s2).getId()));

        // Step 4: global step (null), then exhausted.
        assertNull("domain walk ends at null global", iter.next());
    }

    @Test
    public void domainIteratorDirectNoSuperDomainSelfThenGlobalNull() throws Exception {
        // Covers L368 (!mCheckedSelf) and the else-branch where there are no super-domains: after
        // self the next step is the (null) global, never a super-domain.
        Domain domain = prov.get(DomainBy.name, "example.com");
        TargetIterator.DomainTargetIterator iter =
                new TargetIterator.DomainTargetIterator(prov, domain);

        assertSame("first domain step is self", domain, iter.next());
        assertNull("no super-domain -> global null", iter.next());
        assertNull("exhausted domain iterator stays null", iter.next());
    }
}
