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
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link PrefMailForwardingAddress} attribute callback. Verifies length
 * and address-count enforcement using both context-supplied limits and account defaults.
 */
public class PrefMailForwardingAddressTest {

    private static final String ATTR = Provisioning.A_zimbraPrefMailForwardingAddress;

    private Provisioning prov;

    private Account account;

    private PrefMailForwardingAddress callback;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        account = prov.createAccount("fwd@example.com", "test123", attrs);
        callback = new PrefMailForwardingAddress();
    }

    @Test
    public void preModifyNonAccountEntryReturnsWithoutValidation() throws Exception {
        // Arrange — Cos entry; callback only validates Account entries
        Entry cos = prov.createCos("fwdcos", new HashMap<String, Object>());
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "anything@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — non-Account entries skip validation entirely
        callback.preModify(ctx, ATTR, "anything@x.com", mod, cos);

        // Assert — value untouched, no exception
        assertEquals("anything@x.com", mod.get(ATTR));
    }

    @Test
    public void preModifyUnsettingValueReturnsWithoutValidation() throws Exception {
        // Arrange — no value present means unsetting
        Map<String, Object> mod = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — unsetting short-circuits; no exception regardless of limits
        callback.preModify(ctx, ATTR, null, mod, account);

        // Assert — nothing added to the mod map
        assertFalse(mod.containsKey(ATTR));
    }

    @Test
    public void preModifyWithinAccountDefaultsSucceeds() throws Exception {
        // Arrange — short single address, account default max len 4096 / max addrs 100
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "a@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, "a@x.com", mod, account);

        // Assert — value retained, no exception
        assertEquals("a@x.com", mod.get(ATTR));
    }

    @Test
    public void preModifyValueExceedsAccountMaxLengthThrowsInvalidRequest() throws Exception {
        // Arrange — set a tiny max length on the account, then exceed it
        Map<String, Object> shrink = new HashMap<String, Object>();
        shrink.put(Provisioning.A_zimbraMailForwardingAddressMaxLength, "5");
        prov.modifyAttrs(account, shrink);

        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "toolong@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, "toolong@x.com", mod, account);
            fail("expected INVALID_REQUEST for value exceeding max length");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyTooManyAddressesThrowsInvalidRequest() throws Exception {
        // Arrange — allow generous length but only 1 address
        Map<String, Object> limits = new HashMap<String, Object>();
        limits.put(Provisioning.A_zimbraMailForwardingAddressMaxLength, "4096");
        limits.put(Provisioning.A_zimbraMailForwardingAddressMaxNumAddrs, "1");
        prov.modifyAttrs(account, limits);

        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "a@x.com,b@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, "a@x.com,b@x.com", mod, account);
            fail("expected INVALID_REQUEST for too many addresses");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyMaxLenFromContextOverridesAccountDefault() throws Exception {
        // Arrange — context supplies a small max length, value exceeds it
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "abcdefghij@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_LEN, "5");

        // Act + Assert — context value is honored over the account default
        try {
            callback.preModify(ctx, ATTR, "abcdefghij@x.com", mod, account);
            fail("expected INVALID_REQUEST when context max len is exceeded");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyInvalidContextMaxLenFallsBackToAccountDefault() throws Exception {
        // Arrange — non-numeric context value is logged and ignored, account default applies
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "a@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_LEN, "not-a-number");

        // Act — bad context value ignored; short address fits the 4096 default
        callback.preModify(ctx, ATTR, "a@x.com", mod, account);

        // Assert — no exception, value retained
        assertEquals("a@x.com", mod.get(ATTR));
    }

    @Test
    public void preModifyNullEntryWithoutContextLimitsReturnsWithoutValidation() throws Exception {
        // Arrange — entry is null (create-style) and no context limits provided
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "anylengthvalue@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — with null entry and no usable max, the callback returns before validating
        callback.preModify(ctx, ATTR, "anylengthvalue@x.com", mod, null);

        // Assert — value untouched, no exception
        assertEquals("anylengthvalue@x.com", mod.get(ATTR));
    }

    @Test
    public void preModifyMultipleAddressesWithinLimitSucceeds() throws Exception {
        // Arrange — two addresses, default max addrs is 100
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(ATTR, "a@x.com,b@x.com");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, "a@x.com,b@x.com", mod, account);

        // Assert — within limits, value retained
        assertEquals("a@x.com,b@x.com", mod.get(ATTR));
    }
}
