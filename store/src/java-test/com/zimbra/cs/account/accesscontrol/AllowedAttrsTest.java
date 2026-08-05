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
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AllowedAttrs.Result;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AllowedAttrs}. Exercises the three factory results
 * (ALLOW_ALL / DENY_ALL / ALLOW_SOME) and the real attribute-access decision logic
 * {@code allowAttr} and the package-private {@code canAccessAttrs} against a real
 * {@link Account} target created through the in-memory {@link Provisioning} harness.
 * The ALLOW_SOME +/- attr-name normalization, the dump rendering, and the PERM_DENIED
 * failure path are all covered.
 */
public class AllowedAttrsTest {

    private static Provisioning prov;

    private Account target;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        // overwrite-on-duplicate contract: safe to recreate every method
        target = prov.createAccount("allowed@example.com", "test123",
                new HashMap<String, Object>());
    }

    @Test
    public void allowAllAttrsFactoryResultIsAllowAllAndAllowsAnyAttr() {
        // Arrange / Act
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();

        // Assert
        assertEquals(Result.ALLOW_ALL, aa.getResult());
        assertNull("ALLOW_ALL carries no specific set", aa.getAllowed());
        assertTrue("ALLOW_ALL must allow any attr", aa.allowAttr("displayName"));
    }

    @Test
    public void denyAllAttrsFactoryResultIsDenyAllAndDeniesAnyAttr() {
        // Arrange / Act
        AllowedAttrs aa = AllowedAttrs.DENY_ALL_ATTRS();

        // Assert
        assertEquals(Result.DENY_ALL, aa.getResult());
        assertFalse("DENY_ALL must deny any attr", aa.allowAttr("displayName"));
    }

    @Test
    public void allowSomeAttrsFactoryResultIsAllowSomeWithProvidedSet() {
        // Arrange
        Set<String> some = new HashSet<String>();
        some.add("displayName");

        // Act
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        // Assert
        assertEquals(Result.ALLOW_SOME, aa.getResult());
        assertTrue(aa.getAllowed().contains("displayName"));
    }

    @Test
    public void allowAttrAllowSomeContainsAttrReturnsTrue() {
        // Arrange
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        // Act / Assert
        assertTrue("listed attr must be allowed", aa.allowAttr("displayName"));
        assertFalse("unlisted attr must be denied", aa.allowAttr("description"));
    }

    @Test
    public void allowAttrAllowSomeWithPlusPrefixNormalizesName() {
        // Arrange - allowed set holds the bare name, query uses '+' add-modifier prefix
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        // Act / Assert - '+'/'-' prefix is stripped before lookup
        assertTrue("'+attr' must normalize to 'attr'", aa.allowAttr("+displayName"));
        assertTrue("'-attr' must normalize to 'attr'", aa.allowAttr("-displayName"));
    }

    @Test
    public void canAccessAttrsAllowAllReturnsTrueForAnyNeed() throws Exception {
        // Arrange
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();
        Set<String> needed = new HashSet<String>();
        needed.add("displayName");

        // Act / Assert
        assertTrue(aa.canAccessAttrs(needed, target));
    }

    @Test
    public void canAccessAttrsDenyAllReturnsFalse() throws Exception {
        // Arrange
        AllowedAttrs aa = AllowedAttrs.DENY_ALL_ATTRS();
        Set<String> needed = new HashSet<String>();
        needed.add("displayName");

        // Act / Assert
        assertFalse(aa.canAccessAttrs(needed, target));
    }

    @Test
    public void canAccessAttrsAllowSomeNeedAllReturnsFalse() throws Exception {
        // Arrange - ALLOW_SOME but caller needs ALL attrs (null)
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        // Act / Assert - need-all against allow-some is denied
        assertFalse("need-all against allow-some must be false", aa.canAccessAttrs(null, target));
    }

    @Test
    public void canAccessAttrsAllowSomeAllNeededPresentReturnsTrue() throws Exception {
        // Arrange
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        some.add("description");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        Set<String> needed = new HashSet<String>();
        needed.add("displayName");

        // Act / Assert
        assertTrue(aa.canAccessAttrs(needed, target));
    }

    @Test
    public void canAccessAttrsAllowSomeMissingNeededThrowsPermDenied() throws Exception {
        // Arrange - needed attr not in the allowed set
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        Set<String> needed = new HashSet<String>();
        needed.add("description");

        // Act / Assert - throws PERM_DENIED rather than returning false
        try {
            aa.canAccessAttrs(needed, target);
            fail("expected PERM_DENIED for a non-allowed attr");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("description"));
        }
    }

    @Test
    public void dumpAllowSomeRendersResultAndAllowedAttrs() {
        // Arrange
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        // Act
        String dump = aa.dump();

        // Assert - the rendering mentions the result and the allowed attr
        assertTrue(dump.contains("ALLOW_SOME"));
        assertTrue(dump.contains("displayName"));
    }

    @Test
    public void dumpAllowAllRendersResultOnly() {
        // Arrange
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();

        // Act
        String dump = aa.dump();

        // Assert
        assertTrue(dump.contains("ALLOW_ALL"));
        assertFalse("ALLOW_ALL dump must not list allowed attrs", dump.contains("allowed = ("));
    }

    @Test
    public void canSetAttrsWithinConstraintsNullAttrsThrowsFailure() throws Exception {
        // Arrange
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();

        // Act / Assert - null attrsNeeded is an internal error
        try {
            aa.canSetAttrsWithinConstraints(null, target, null);
            fail("expected FAILURE for null attrsNeeded");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void canSetAttrsWithinConstraintsDenyAllReturnsFalse() throws Exception {
        // Arrange - DENY_ALL short-circuits before any constraint lookup
        AllowedAttrs aa = AllowedAttrs.DENY_ALL_ATTRS();
        Map<String, Object> attrsNeeded = new HashMap<String, Object>();
        attrsNeeded.put("displayName", "Bob");

        // Act / Assert
        assertFalse(aa.canSetAttrsWithinConstraints(null, target, attrsNeeded));
    }

    @Test
    public void canSetAttrsWithinConstraintsAllowAllNoConstraintsReturnsTrue() throws Exception {
        // Arrange - ALLOW_ALL drives the allowAll branch through the per-attr loop; the account's
        // default COS carries no zimbraConstraint so hasConstraints stays false.
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();
        Map<String, Object> attrsNeeded = new HashMap<String, Object>();
        attrsNeeded.put("displayName", "Bob");
        attrsNeeded.put("description", "a user");

        // Act
        boolean allowed = aa.canSetAttrsWithinConstraints(null, target, attrsNeeded);

        // Assert - every needed attr is allowed under ALLOW_ALL with no constraints
        assertTrue("ALLOW_ALL with no constraints permits any attr", allowed);
    }

    @Test
    public void canSetAttrsWithinConstraintsAllowSomeAllNeededAllowedReturnsTrue() throws Exception {
        // Arrange - ALLOW_SOME where every needed attr is in the allowed set
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        some.add("description");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        Map<String, Object> attrsNeeded = new HashMap<String, Object>();
        attrsNeeded.put("displayName", "Bob");
        attrsNeeded.put("description", "a user");

        // Act
        boolean allowed = aa.canSetAttrsWithinConstraints(null, target, attrsNeeded);

        // Assert
        assertTrue("all needed attrs present in allow set", allowed);
    }

    @Test
    public void canSetAttrsWithinConstraintsAllowSomePlusPrefixNormalizesAndAllows() throws Exception {
        // Arrange - allowed set holds the bare name; the needed key carries a '+' add modifier
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        Map<String, Object> attrsNeeded = new HashMap<String, Object>();
        attrsNeeded.put("+displayName", "Bob");

        // Act
        boolean allowed = aa.canSetAttrsWithinConstraints(null, target, attrsNeeded);

        // Assert - '+' prefix stripped before the allow lookup
        assertTrue("'+attr' normalizes to 'attr' for the allow check", allowed);
    }

    @Test
    public void canSetAttrsWithinConstraintsAllowSomeMissingNeededThrowsPermDenied() throws Exception {
        // Arrange - ALLOW_SOME but the needed attr is not in the allowed set
        Set<String> some = new HashSet<String>();
        some.add("displayName");
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

        Map<String, Object> attrsNeeded = new HashMap<String, Object>();
        attrsNeeded.put("description", "denied attr");

        // Act / Assert - throws PERM_DENIED naming the offending attr
        try {
            aa.canSetAttrsWithinConstraints(null, target, attrsNeeded);
            fail("expected PERM_DENIED for a non-allowed attr");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("description"));
        }
    }

    @Test
    public void canSetAttrsWithinConstraintsForbiddenAttrThrowsPermDenied() throws Exception {
        // Arrange - zimbraIsAdminAccount is an ALWAYS_FORBIDDEN attr; even ALLOW_ALL cannot set it
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();
        Map<String, Object> attrsNeeded = new HashMap<String, Object>();
        attrsNeeded.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");

        // Act / Assert - HardRules.checkForbiddenAttr rejects it
        try {
            aa.canSetAttrsWithinConstraints(null, target, attrsNeeded);
            fail("expected PERM_DENIED for a forbidden attr");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessAttrsForbiddenAttrThrowsPermDenied() throws Exception {
        // Arrange - ALLOW_SOME that even lists the forbidden attr; HardRules still rejects it
        Set<String> some = new HashSet<String>();
        some.add(Provisioning.A_zimbraIsAdminAccount);
        AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);
        Set<String> needed = new HashSet<String>();
        needed.add(Provisioning.A_zimbraIsAdminAccount);

        // Act / Assert
        try {
            aa.canAccessAttrs(needed, target);
            fail("expected PERM_DENIED for a forbidden attr");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessAttrsDebugEnabledLogsAndStillReturnsTrue() throws Exception {
        // Arrange - turn on acl debug logging so the debug-dump branch (attrs needed + allowed) runs
        Log acl = ZimbraLog.acl;
        Log.Level prior = acl.getLevel();
        acl.setLevel(Log.Level.debug);
        try {
            Set<String> some = new HashSet<String>();
            some.add("displayName");
            AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);
            Set<String> needed = new HashSet<String>();
            needed.add("displayName");

            // Act
            boolean allowed = aa.canAccessAttrs(needed, target);

            // Assert - the debug path does not alter the decision
            assertTrue(allowed);
        } finally {
            acl.setLevel(prior);
        }
    }

    @Test
    public void canAccessAttrsDebugEnabledNeedAllLogsAllAttributesMarker() throws Exception {
        // Arrange - debug on, attrsNeeded == null exercises the "<all attributes>" debug branch
        Log acl = ZimbraLog.acl;
        Log.Level prior = acl.getLevel();
        acl.setLevel(Log.Level.debug);
        try {
            Set<String> some = new HashSet<String>();
            some.add("displayName");
            AllowedAttrs aa = AllowedAttrs.ALLOW_SOME_ATTRS(some);

            // Act - need-all against allow-some is denied, after the debug dump
            boolean allowed = aa.canAccessAttrs(null, target);

            // Assert
            assertFalse("need-all against allow-some is denied", allowed);
        } finally {
            acl.setLevel(prior);
        }
    }
}
