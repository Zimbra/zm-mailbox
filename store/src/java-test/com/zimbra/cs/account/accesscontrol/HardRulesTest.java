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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.HardRules.HardRule;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link HardRules}: the forbidden-attribute gate, the {@code checkHardRules}
 * delegated-admin policy (global admin pass-through, delegated-admin requirement, the
 * delegated-cannot-touch-global rule, and the non-account rejection), and the
 * {@link HardRule#ruleVolated} exception-decoding round trip. Uses real {@link Account} objects
 * built through the in-memory MockProvisioning harness — no domain-object mocking.
 */
public class HardRulesTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name, boolean globalAdmin, boolean delegatedAdmin)
            throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        if (globalAdmin) {
            attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        }
        if (delegatedAdmin) {
            attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        }
        return prov.createAccount(name, "test123", attrs);
    }

    // ---------- isForbiddenAttr / checkForbiddenAttr ----------

    @Test
    public void isForbiddenAttrZimbraIsAdminAccountReturnsTrue() {
        // Act / Assert — the one always-forbidden attr, case-insensitively
        assertTrue(HardRules.isForbiddenAttr(Provisioning.A_zimbraIsAdminAccount));
        assertTrue("matching must be case-insensitive",
                HardRules.isForbiddenAttr(Provisioning.A_zimbraIsAdminAccount.toUpperCase()));
    }

    @Test
    public void isForbiddenAttrOrdinaryAttrReturnsFalse() {
        // Act / Assert
        assertFalse(HardRules.isForbiddenAttr(Provisioning.A_displayName));
    }

    @Test
    public void checkForbiddenAttrForbiddenThrowsPermDenied() throws Exception {
        // Act / Assert
        try {
            HardRules.checkForbiddenAttr(Provisioning.A_zimbraIsAdminAccount);
            fail("expected PERM_DENIED for a forbidden attr");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void checkForbiddenAttrAllowedDoesNotThrow() throws Exception {
        // Act — must complete without throwing
        HardRules.checkForbiddenAttr(Provisioning.A_displayName);

        // Assert — reaching here means no exception was thrown
        assertTrue(true);
    }

    // ---------- checkHardRules ----------

    @Test
    public void checkHardRulesGlobalAdminReturnsTrue() throws Exception {
        // Arrange
        Account globalAdmin = createAccount("ghard@example.com", true, false);

        // Act — global admin short-circuits to TRUE regardless of right/target
        Boolean result = HardRules.checkHardRules(globalAdmin, true, globalAdmin, null);

        // Assert
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void checkHardRulesDelegatedAdminOnNonAdminTargetReturnsNull() throws Exception {
        // Arrange — admin right (right == null => admin right), delegated grantee, plain target
        Account delegated = createAccount("dhard@example.com", false, true);
        Account target = createAccount("thard@example.com", false, false);

        // Act
        Boolean result = HardRules.checkHardRules(delegated, true, target, null);

        // Assert — eligible delegated admin, ordinary target: hard rules not applicable
        assertNull("an eligible delegated admin on a non-admin target yields null", result);
    }

    @Test
    public void checkHardRulesNotAdminAccountThrowsNotEligible() throws Exception {
        // Arrange — neither global nor delegated admin, admin right requested
        Account plain = createAccount("phard@example.com", false, false);
        Account target = createAccount("ptarget@example.com", false, false);

        // Act / Assert
        try {
            HardRules.checkHardRules(plain, true, target, null);
            fail("expected PERM_DENIED for a non-eligible admin");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertEquals(HardRule.NOT_EFFECTIVE_DELEGATED_ADMIN_ACCOUNT, HardRule.ruleVolated(e));
        }
    }

    @Test
    public void checkHardRulesDelegatedAdminTargetingGlobalAdminThrowsCannotAccess() throws Exception {
        // Arrange — eligible delegated admin attempting to act on a global admin's account
        Account delegated = createAccount("dhard2@example.com", false, true);
        Account globalTarget = createAccount("gtarget@example.com", true, false);

        // Act / Assert
        try {
            HardRules.checkHardRules(delegated, true, globalTarget, null);
            fail("expected PERM_DENIED: delegated admin cannot access a global admin");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertEquals(HardRule.DELEGATED_ADMIN_CANNOT_ACCESS_GLOBAL_ADMIN, HardRule.ruleVolated(e));
        }
    }

    // ---------- HardRule.ruleVolated ----------

    @Test
    public void ruleVolatedNonPermDeniedExceptionReturnsNull() {
        // Arrange — a non-PERM_DENIED exception carries no hard-rule signal
        ServiceException e = ServiceException.FAILURE("some failure", null);

        // Act / Assert
        assertNull(HardRule.ruleVolated(e));
    }

    @Test
    public void ruleVolatedPermDeniedWithoutRuleArgReturnsNull() {
        // Arrange — PERM_DENIED but no hard-rule argument attached
        ServiceException e = ServiceException.PERM_DENIED("plain perm denied");

        // Act / Assert
        assertNull("no recognizable rule argument means null", HardRule.ruleVolated(e));
    }
}
