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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.PseudoTarget.PseudoDistributionList;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link GroupACLs}, the helper that collects ACLs from the groups a target
 * entry belongs to. Tests run in-package so they can reach the package-private ctor/methods and the
 * package-visible {@link PseudoTarget.PseudoDistributionList}. Real {@link Account} and
 * {@link Group} entries are built via the in-memory {@link Provisioning} harness. The
 * collect/aggregate behaviour is exercised against groups that carry no ACL, so the three
 * allowed/denied sets stay empty and {@link GroupACLs#getAllACLs()} returns {@code null}.
 */
public class GroupACLsFunctionalTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        RightManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Group pseudoGroup(String name) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        // pseudoDomain may be null — PseudoDistributionList only stores it for getPseudoDomain().
        return new PseudoDistributionList(name, UUID.randomUUID().toString(), attrs, prov, null);
    }

    @Test
    public void constructorDistributionListTargetBuildsInstance() throws Exception {
        // Arrange
        Group dl = pseudoGroup("dlt@example.com");

        // Act — DL target branch stores the entry and does not touch getDirectDistributionLists
        GroupACLs groupACLs = new GroupACLs(dl);

        // Assert — fresh instance has nothing collected yet
        assertNull("no ACLs collected yet", groupACLs.getAllACLs());
    }

    @Test
    public void constructorAccountTargetFailsBecauseDirectDlsUnsupported() throws Exception {
        // Arrange — Account target branch calls getDirectDistributionLists, unsupported by the mock
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Account acct = prov.createAccount("acctgacl@example.com", "test123", attrs);

        // Act / Assert — the account branch is reached; mock throws for direct DL lookup
        try {
            new GroupACLs(acct);
            fail("expected failure resolving direct distribution lists in the mock harness");
        } catch (UnsupportedOperationException e) {
            assertTrue("account branch reached getDirectDistributionLists", true);
        }
    }

    @Test
    public void constructorUnsupportedTargetTypeThrowsFailure() throws Exception {
        // Arrange — a Cos is neither Account nor DistributionList
        Map<String, Object> cosAttrs = new HashMap<>();
        cosAttrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());

        // Act / Assert — the else branch throws an internal-error ServiceException
        try {
            new GroupACLs(prov.createCos("gaclcos", cosAttrs));
            fail("expected ServiceException for unsupported target type");
        } catch (ServiceException e) {
            assertTrue("internal error message", e.getMessage().toLowerCase().contains("internal"));
        }
    }

    @Test
    public void collectACLGroupWithNoAclCollectsNothing() throws Exception {
        // Arrange — DL target plus a granted-on group that carries no ACL attributes
        Group target = pseudoGroup("collecttarget@example.com");
        Group grantedOn = pseudoGroup("grantedon@example.com");
        GroupACLs groupACLs = new GroupACLs(target);

        // Act — collect from a group with empty ACL; no ACEs are added to any bucket
        groupACLs.collectACL(grantedOn, false);

        // Assert — all buckets empty => aggregate is null
        assertNull("no ACEs to aggregate", groupACLs.getAllACLs());
    }

    @Test
    public void collectACLSkipPositiveGrantsGroupWithNoAclCollectsNothing() throws Exception {
        // Arrange — same setup, but skipPositiveGrants=true exercises that branch
        Group target = pseudoGroup("collecttarget2@example.com");
        Group grantedOn = pseudoGroup("grantedon2@example.com");
        GroupACLs groupACLs = new GroupACLs(target);

        // Act — skip positive grants; denied bucket still scanned (empty here)
        groupACLs.collectACL(grantedOn, true);

        // Assert
        assertNull(groupACLs.getAllACLs());
    }

    @Test
    public void getAllACLsEmptyAfterMultipleCollectsReturnsNull() throws Exception {
        // Arrange
        Group target = pseudoGroup("multi@example.com");
        GroupACLs groupACLs = new GroupACLs(target);

        // Act — repeated collects over ACL-free groups keep every bucket empty
        groupACLs.collectACL(pseudoGroup("g1@example.com"), false);
        groupACLs.collectACL(pseudoGroup("g2@example.com"), true);

        // Assert
        assertNull("empty buckets across collects yields null", groupACLs.getAllACLs());
    }

    @Test
    public void getAllACLsFreshInstanceForDlTargetReturnsNull() throws Exception {
        // Arrange / Act — no collect at all
        GroupACLs groupACLs = new GroupACLs(pseudoGroup("fresh@example.com"));
        List<ZimbraACE> all = groupACLs.getAllACLs();

        // Assert
        assertNull(all);
    }
}
