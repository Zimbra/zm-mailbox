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
 * Functional tests for {@link MailSignature} attribute callback. Verifies signature length
 * enforcement using context limits, attrsToModify limits, and account defaults.
 */
public class MailSignatureTest {

    private static final String ATTR = Provisioning.A_zimbraPrefMailSignature;

    private Provisioning prov;

    private Account account;

    private MailSignature callback;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        account = prov.createAccount("sig@example.com", "test123", new HashMap<String, Object>());
        callback = new MailSignature();
    }

    @Test
    public void preModifyUnsettingValueReturnsWithoutValidation() throws Exception {
        // Arrange — null value means unsetting
        Map<String, Object> mod = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — unsetting short-circuits, no exception
        callback.preModify(ctx, ATTR, null, mod, account);

        // Assert — nothing added
        assertFalse(mod.containsKey(ATTR));
    }

    @Test
    public void preModifyNonSignatureBearingEntryReturnsWithoutValidation() throws Exception {
        // Arrange — Cos entry is neither Account, Identity nor Signature
        Entry cos = prov.createCos("sigcos", new HashMap<String, Object>());
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = repeat("x", 50000);
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — skipped because entry is not a signature-bearing type
        callback.preModify(ctx, ATTR, value, mod, cos);

        // Assert — value untouched even though it is huge
        assertEquals(value, mod.get(ATTR));
    }

    @Test
    public void preModifyWithinAccountDefaultLimitSucceeds() throws Exception {
        // Arrange — short signature, account default limit is 10240
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = "Regards, Test User";
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, ATTR, value, mod, account);

        // Assert — value retained, no exception
        assertEquals(value, mod.get(ATTR));
    }

    @Test
    public void preModifyExceedsAccountDefaultLimitThrowsInvalidRequest() throws Exception {
        // Arrange — exceed the 10240 default
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = repeat("a", 10241);
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, value, mod, account);
            fail("expected INVALID_REQUEST for signature exceeding the default limit");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyMaxLenFromContextEnforcesContextLimit() throws Exception {
        // Arrange — context says max 5; value is longer
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = "123456";
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.MAX_SIGNATURE_LEN, "5");

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, value, mod, account);
            fail("expected INVALID_REQUEST when context signature limit is exceeded");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyMaxLenFromContextWithinLimitSucceeds() throws Exception {
        // Arrange — context max 100, short value
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = "short sig";
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.MAX_SIGNATURE_LEN, "100");

        // Act
        callback.preModify(ctx, ATTR, value, mod, account);

        // Assert — value retained
        assertEquals(value, mod.get(ATTR));
    }

    @Test
    public void preModifyMaxLenFromAttrsToModifyEnforcesThatLimit() throws Exception {
        // Arrange — no context limit, but attrsToModify carries a small max length
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = "abcdefghij";
        mod.put(ATTR, value);
        mod.put(Provisioning.A_zimbraMailSignatureMaxLength, "5");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, ATTR, value, mod, account);
            fail("expected INVALID_REQUEST when attrsToModify max length is exceeded");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyUnlimitedWhenMaxIsZeroSucceeds() throws Exception {
        // Arrange — context max 0 means unlimited; a long value must pass
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = repeat("z", 20000);
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.MAX_SIGNATURE_LEN, "0");

        // Act — 0 == unlimited, no length enforcement
        callback.preModify(ctx, ATTR, value, mod, account);

        // Assert — long value retained, no exception
        assertEquals(value, mod.get(ATTR));
    }

    @Test
    public void preModifyInvalidContextMaxLenFallsBackToAccountDefault() throws Exception {
        // Arrange — bad context value is ignored; short value fits the 10240 default
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = "fits";
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.MAX_SIGNATURE_LEN, "garbage");

        // Act — invalid context value ignored, account default applies
        callback.preModify(ctx, ATTR, value, mod, account);

        // Assert — value retained
        assertEquals(value, mod.get(ATTR));
    }

    @Test
    public void preModifyNullEntryWithoutLimitsReturnsWithoutValidation() throws Exception {
        // Arrange — entry null and no context/attrs limit available
        Map<String, Object> mod = new HashMap<String, Object>();
        String value = repeat("q", 30000);
        mod.put(ATTR, value);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — with null entry and no usable max, callback returns before validating
        callback.preModify(ctx, ATTR, value, mod, null);

        // Assert — long value retained, no exception
        assertEquals(value, mod.get(ATTR));
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(n * s.length());
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
