/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link TrustedSenderList} attribute callback. Verifies max-entry
 * enforcement and CoS-default copying behavior against a real {@link Account}.
 */
public class TrustedSenderListTest {

    private static final String ATTR = Provisioning.A_zimbraPrefMailTrustedSenderList;

    private Provisioning prov;

    private Account account;

    private TrustedSenderList callback;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        // cap the list at 3 so the "too many" branch is easy to trigger
        attrs.put(Provisioning.A_zimbraMailTrustedSenderListMaxNumEntries, "3");
        account = prov.createAccount("tsl@example.com", "test123", attrs);
        // The attribute has no defaultCOSValue, so without a CoS the account's
        // getAttrDefault(ATTR) is null. Production accounts always resolve a (possibly
        // empty) default through their CoS; the in-memory account does not. Seed an
        // empty default so the callback's "copy CoS values" branch is reachable
        // (it calls getMultiValue/getMultiValueSet on the default, which reject null).
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put(ATTR, new String[] {});
        account.setOverrideDefaults(defaults);
        callback = new TrustedSenderList();
    }

    @Test
    public void preModifyCreateOpReturnsWithoutChange() throws Exception {
        // Arrange
        Map<String, Object> mod = new HashMap<String, Object>();
        String[] big = new String[] {"a@x.com", "b@x.com", "c@x.com", "d@x.com"};
        mod.put(ATTR, big);
        CallbackContext ctx = new CallbackContext(Op.CREATE);

        // Act — on create the callback returns immediately, no enforcement
        callback.preModify(ctx, ATTR, big, mod, account);

        // Assert — mod map untouched (value still the original array)
        assertEquals(big, mod.get(ATTR));
    }

    @Test
    public void preModifyNonAccountEntryReturnsWithoutChange() throws Exception {
        // Arrange — Cos entry, not an Account
        Entry cos = prov.createCos("tslcos", new HashMap<String, Object>());
        Map<String, Object> mod = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com", "c@x.com", "d@x.com"};
        mod.put(ATTR, vals);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — non-Account entries are skipped
        callback.preModify(ctx, ATTR, vals, mod, cos);

        // Assert
        assertEquals(vals, mod.get(ATTR));
    }

    @Test
    public void preModifyAlreadyDoneReturnsWithoutChange() throws Exception {
        // Arrange — pre-mark the callback as done
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.isDoneAndSetIfNot(TrustedSenderList.class);
        Map<String, Object> mod = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com", "c@x.com", "d@x.com"};
        mod.put(ATTR, vals);

        // Act — second invocation short-circuits
        callback.preModify(ctx, ATTR, vals, mod, account);

        // Assert — no enforcement happened
        assertEquals(vals, mod.get(ATTR));
    }

    @Test
    public void preModifyReplaceWithinLimitSucceeds() throws Exception {
        // Arrange — exactly 3 entries, limit is 3
        Map<String, Object> mod = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com", "c@x.com"};
        mod.put(ATTR, vals);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, vals, mod, account);

        // Assert — value retained, no exception
        assertEquals(vals, mod.get(ATTR));
    }

    @Test
    public void preModifyReplaceExceedingLimitThrowsTooManyTrustedSenders() throws Exception {
        // Arrange — 4 entries against a limit of 3
        Map<String, Object> mod = new HashMap<String, Object>();
        String[] vals = new String[] {"a@x.com", "b@x.com", "c@x.com", "d@x.com"};
        mod.put(ATTR, vals);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, vals, mod, account);
            fail("expected TOO_MANY_TRUSTED_SENDERS for 4 > 3");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.TOO_MANY_TRUSTED_SENDERS,
                    ((AccountServiceException) e).getCode());
        }
    }

    @Test
    public void preModifyAddToEmptyWithinLimitCopiesAndKeepsAddValue() throws Exception {
        // Arrange — account has no current values; add 2 entries (limit 3)
        Map<String, Object> mod = new HashMap<String, Object>();
        Set<String> add = new HashSet<String>();
        add.add("a@x.com");
        add.add("b@x.com");
        mod.put("+" + ATTR, add);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, add, mod, account);

        // Assert — the +attr value is re-put as a Set (CoS defaults merged in, none here)
        Object result = mod.get("+" + ATTR);
        assertTrue("expected a Set result", result instanceof Set);
        assertTrue(((Set<?>) result).contains("a@x.com"));
        assertTrue(((Set<?>) result).contains("b@x.com"));
    }

    @Test
    public void preModifyAddToEmptyExceedingLimitThrowsTooManyTrustedSenders() throws Exception {
        // Arrange — adding 4 entries to an empty list with limit 3
        Map<String, Object> mod = new HashMap<String, Object>();
        Set<String> add = new HashSet<String>();
        add.add("a@x.com");
        add.add("b@x.com");
        add.add("c@x.com");
        add.add("d@x.com");
        mod.put("+" + ATTR, add);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, add, mod, account);
            fail("expected TOO_MANY_TRUSTED_SENDERS when add exceeds limit");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.TOO_MANY_TRUSTED_SENDERS,
                    ((AccountServiceException) e).getCode());
        }
    }

    @Test
    public void preModifyAddToExistingExceedingLimitThrowsTooManyTrustedSenders() throws Exception {
        // Arrange — account already has 2 values, add 2 more (limit 3 => 4 > 3)
        Map<String, Object> existing = new HashMap<String, Object>();
        existing.put(ATTR, new String[] {"x@x.com", "y@x.com"});
        prov.modifyAttrs(account, existing);

        Map<String, Object> mod = new HashMap<String, Object>();
        Set<String> add = new HashSet<String>();
        add.add("a@x.com");
        add.add("b@x.com");
        mod.put("+" + ATTR, add);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, add, mod, account);
            fail("expected TOO_MANY_TRUSTED_SENDERS when current+add exceeds limit");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.TOO_MANY_TRUSTED_SENDERS,
                    ((AccountServiceException) e).getCode());
        }
    }

    @Test
    public void preModifyAddToExistingWithinLimitSucceeds() throws Exception {
        // Arrange — account already has 1 value, add 1 more (total 2 <= 3)
        Map<String, Object> existing = new HashMap<String, Object>();
        existing.put(ATTR, new String[] {"x@x.com"});
        prov.modifyAttrs(account, existing);

        Map<String, Object> mod = new HashMap<String, Object>();
        Set<String> add = new HashSet<String>();
        add.add("a@x.com");
        mod.put("+" + ATTR, add);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, add, mod, account);

        // Assert — add value preserved, no exception
        assertEquals(add, mod.get("+" + ATTR));
    }

    @Test
    public void preModifyRemoveFromEmptyConvertsToAddWithCosDefaultsMinusRemoved() throws Exception {
        // Arrange — no current values, remove "a@x.com"; callback copies CoS defaults (none),
        // removes, and rewrites as "+attr" while dropping the "-attr" key
        Map<String, Object> mod = new HashMap<String, Object>();
        Set<String> remove = new HashSet<String>();
        remove.add("a@x.com");
        mod.put("-" + ATTR, remove);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, remove, mod, account);

        // Assert — "-attr" removed, "+attr" present and does not contain the removed value
        assertFalse("minus key should be removed", mod.containsKey("-" + ATTR));
        assertTrue("plus key should be added", mod.containsKey("+" + ATTR));
        assertFalse(((Set<?>) mod.get("+" + ATTR)).contains("a@x.com"));
    }
}
