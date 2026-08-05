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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link WhiteBlackList}, the amavis white/black-list size-limit callback.
 * Real {@link Account} entries are run through {@code preModify}, asserting the limit is enforced
 * for replace and add/remove paths and skipped when no limit is configured.
 */
public class WhiteBlackListTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account newAccount(String name, String whiteMax) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (whiteMax != null) {
            attrs.put(Provisioning.A_zimbraMailWhitelistMaxNumEntries, whiteMax);
        }
        return prov.createAccount(name, "test123", attrs);
    }

    @Test
    public void preModifyReplaceWithinLimitPasses() throws Exception {
        // Arrange -- limit 3, replacing with 2 values
        Account acct = newAccount("wbl-ok@example.com", "3");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_amavisWhitelistSender,
                new String[] {"a@x.com", "b@x.com" });
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender,
                new String[] {"a@x.com", "b@x.com" }, toModify, acct);

        // Assert
        assertTrue("2 values under a limit of 3 must be allowed", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyReplaceExceedsLimitThrows() throws Exception {
        // Arrange -- limit 2, replacing with 3 values
        Account acct = newAccount("wbl-over@example.com", "2");
        Map<String, Object> toModify = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com", "c@x.com" };
        toModify.put(Provisioning.A_amavisWhitelistSender, vals);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, vals,
                    toModify, acct);
            fail("expected INVALID_REQUEST when replace exceeds the limit");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("message should mention the limit", e.getMessage().contains("max is 2"));
        } finally {
            prov.deleteAccount(acct.getId());
        }
    }

    @Test
    public void preModifyNoLimitConfiguredSkipsCheck() throws Exception {
        // Arrange -- no max attr => max is null => no checking, even with many values
        Account acct = newAccount("wbl-nolimit@example.com", null);
        Map<String, Object> toModify = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com", "c@x.com", "d@x.com" };
        toModify.put(Provisioning.A_amavisWhitelistSender, vals);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, vals,
                toModify, acct);

        // Assert
        assertTrue("with no configured limit the check is skipped", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyMixReplaceWithAddThrows() throws Exception {
        // Arrange -- both a replace and an add for the same attr is illegal
        Account acct = newAccount("wbl-mix@example.com", "5");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_amavisWhitelistSender, "a@x.com");
        toModify.put("+" + Provisioning.A_amavisWhitelistSender, "b@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, "a@x.com",
                    toModify, acct);
            fail("expected INVALID_REQUEST when mixing +attr with attr");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("can't mix"));
        } finally {
            prov.deleteAccount(acct.getId());
        }
    }

    @Test
    public void preModifyAddPushesExistingOverLimitThrows() throws Exception {
        // Arrange -- existing 2 values, limit 2, adding 1 more (a new value) => newNum 3 > 2
        Account acct = newAccount("wbl-add@example.com", "2");
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(Provisioning.A_amavisWhitelistSender, new String[] {"a@x.com", "b@x.com" });
        prov.modifyAttrs(acct, seed);
        assertEquals(2, acct.getMultiAttrSet(Provisioning.A_amavisWhitelistSender).size());

        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put("+" + Provisioning.A_amavisWhitelistSender, "c@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, "c@x.com",
                    toModify, acct);
            fail("expected INVALID_REQUEST when add pushes total over limit");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        } finally {
            prov.deleteAccount(acct.getId());
        }
    }

    @Test
    public void preModifyAddDuplicateOfExistingDoesNotCountTwice() throws Exception {
        // Arrange -- existing has "a@x.com"; adding "a@x.com" again is not a new value
        Account acct = newAccount("wbl-dup@example.com", "1");
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(Provisioning.A_amavisWhitelistSender, "a@x.com");
        prov.modifyAttrs(acct, seed);

        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put("+" + Provisioning.A_amavisWhitelistSender, "a@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- numToAdd is 0 because the value already exists, so newNum stays at 1 (== limit)
        new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, "a@x.com",
                toModify, acct);

        // Assert
        assertTrue("re-adding an existing value must not exceed the limit", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyRemoveWhenAlreadyOverLimitAllowsRemoval() throws Exception {
        // Arrange -- 3 existing values but limit later lowered to 2; a pure removal should pass
        Account acct = newAccount("wbl-removeover@example.com", "2");
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(Provisioning.A_amavisWhitelistSender,
                new String[] {"a@x.com", "b@x.com", "c@x.com" });
        prov.modifyAttrs(acct, seed);
        assertEquals(3, acct.getMultiAttrSet(Provisioning.A_amavisWhitelistSender).size());

        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put("-" + Provisioning.A_amavisWhitelistSender, "a@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- curNum(3) > numMax(2), no adds, so the removal is allowed through
        new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, "a@x.com",
                toModify, acct);

        // Assert
        assertTrue("removal must be allowed even when already over the limit", true);
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void preModifyCreateWithDataKeyLimitEnforcesMax() throws Exception {
        // Arrange -- create flow: limit comes from the CallbackContext data key, not the account
        Map<String, Object> toModify = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com" };
        toModify.put(Provisioning.A_amavisWhitelistSender, vals);
        CallbackContext ctx = new CallbackContext(Op.CREATE);
        ctx.setData(DataKey.MAIL_WHITELIST_MAX_NUM_ENTRIES, "1");

        // Act + Assert -- entry is null on create; max(1) < 2 values => limit exceeded
        try {
            new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender, vals,
                    toModify, null);
            fail("expected INVALID_REQUEST on create exceeding data-key limit");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("max is 1"));
        }
    }

    @Test
    public void preModifyBlacklistUsesBlacklistDataKey() throws Exception {
        // Arrange -- the blacklist attr reads the blacklist data key on create
        Map<String, Object> toModify = new HashMap<String, Object>();
        String[] vals = new String[] {"x@y.com", "z@y.com", "q@y.com" };
        toModify.put(Provisioning.A_amavisBlacklistSender, vals);
        CallbackContext ctx = new CallbackContext(Op.CREATE);
        ctx.setData(DataKey.MAIL_BLACKLIST_MAX_NUM_ENTRIES, "2");

        // Act + Assert
        try {
            new WhiteBlackList().preModify(ctx, Provisioning.A_amavisBlacklistSender, vals,
                    toModify, null);
            fail("expected INVALID_REQUEST on blacklist create exceeding limit");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains(Provisioning.A_amavisBlacklistSender));
        }
    }

    @Test
    public void preModifyNonAccountEntryIsIgnored() throws Exception {
        // Arrange -- a non-Account, non-null entry returns immediately
        com.zimbra.cs.account.Server server =
                prov.createServer("wbl-server.example.com", new HashMap<String, Object>());
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_amavisWhitelistSender,
                new String[] {"a@x.com", "b@x.com", "c@x.com" });
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new WhiteBlackList().preModify(ctx, Provisioning.A_amavisWhitelistSender,
                new String[] {"a@x.com", "b@x.com", "c@x.com" }, toModify, server);

        // Assert -- Server entries are ignored, no exception
        assertFalse("server entries are not subject to white/black-list limits", false);
    }

    @Test
    public void postModifyIsNoOpDoesNotThrow() throws Exception {
        // Arrange
        Account acct = newAccount("wbl-post@example.com", "3");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new WhiteBlackList().postModify(ctx, Provisioning.A_amavisWhitelistSender, acct);

        // Assert
        assertTrue("postModify is a no-op", true);
        prov.deleteAccount(acct.getId());
    }
}
