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
package com.zimbra.cs.account.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.callback.CallbackContext.Op;

/**
 * Tests {@link ModernCSPPolicy}.
 */
public class ModernCSPPolicyTest {

    private static final String ATTR = "zimbraModernCSPPolicy";

    private static void modify(Object value) throws ServiceException {
        new ModernCSPPolicy().preModify(new CallbackContext(Op.MODIFY), ATTR, value, new HashMap<String, Object>(),
                null);
    }

    private static void assertRejected(Object value) {
        try {
            modify(value);
            fail("expected the value to be rejected");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void acceptsAPolicyOnOneLine() throws Exception {
        modify("default-src 'self'; script-src 'self' 'nonce-{{nonce}}'; object-src 'none'");
    }

    @Test
    public void rejectsLineFeed() {
        assertRejected("default-src 'self'\nX-Injected: 1");
    }

    @Test
    public void rejectsCarriageReturn() {
        assertRejected("default-src 'self'\r\nX-Injected: 1");
    }

    @Test
    public void acceptsRemovingTheValue() throws Exception {
        modify(null);
        modify("");
    }

    @Test
    public void rejectsMultipleValuesForASingleValuedAttribute() {
        assertRejected(new String[] { "default-src 'self'", "object-src 'none'" });
    }
}
