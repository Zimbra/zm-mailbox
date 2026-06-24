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
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DefaultCalendarIdCallback}. Covers the value-validation branches
 * (null / empty / non-integer / zero / wrong type) and the COS-not-allowed branch. The
 * Account-with-mailbox branch needs a real mailbox folder and is exercised elsewhere.
 */
public class DefaultCalendarIdCallbackTest {

    private static final String ATTR = "zimbraPrefDefaultCalendarId";

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private CallbackContext modifyCtx() {
        return new CallbackContext(CallbackContext.Op.MODIFY);
    }

    @Test
    public void preModifyNullValueThrowsInvalidRequest() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, null, new HashMap<String, Object>(), null);
            fail("expected ServiceException for null value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Invalid value received"));
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyEmptyStringThrowsInvalidRequest() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, "", new HashMap<String, Object>(), null);
            fail("expected ServiceException for empty string value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Invalid value received"));
        }
    }

    @Test
    public void preModifyEmptyStringArrayThrowsInvalidRequest() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, new String[0], new HashMap<String, Object>(), null);
            fail("expected ServiceException for empty array value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Invalid value received"));
        }
    }

    @Test
    public void preModifyArrayWithEmptyFirstElementThrowsInvalidRequest() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, new String[] {""}, new HashMap<String, Object>(), null);
            fail("expected ServiceException for empty first array element");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Invalid value received"));
        }
    }

    @Test
    public void preModifyNonIntegerStringThrowsMustBeValidInteger() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, "notANumber", new HashMap<String, Object>(), null);
            fail("expected ServiceException for non-integer value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be valid integer"));
        }
    }

    @Test
    public void preModifyNonIntegerInArrayThrowsMustBeValidInteger() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, new String[] {"12x"}, new HashMap<String, Object>(), null);
            fail("expected ServiceException for non-integer array value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be valid integer"));
        }
    }

    @Test
    public void preModifyZeroValueThrowsInvalidRequest() throws Exception {
        // Arrange - value parses to 0, which the callback rejects
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, "0", new HashMap<String, Object>(), null);
            fail("expected ServiceException for zero value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Invalid value received"));
        }
    }

    @Test
    public void preModifyNonStringNonArrayValueThrowsInvalidRequest() throws Exception {
        // Arrange - an Integer is neither String nor String[]
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, Integer.valueOf(5), new HashMap<String, Object>(), null);
            fail("expected ServiceException for unsupported value type");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Invalid value received"));
        }
    }

    @Test
    public void preModifyValidValueButCosEntryThrowsNotAllowedOnCos() throws Exception {
        // Arrange - a valid integer passes value checks, then the Cos branch rejects it
        Cos cos = prov.createCos("calid-cos", new HashMap<String, Object>());
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ATTR, "10", new HashMap<String, Object>(), cos);
            fail("expected ServiceException - changing calendar id on COS not allowed");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("on COS is not allowed"));
        }
    }

    @Test
    public void preModifyValidValueWithNullEntryDoesNotThrow() throws Exception {
        // Arrange - null entry means create-time; value validation passes and no entry branch runs
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert - completes without exception
        callback.preModify(modifyCtx(), ATTR, "10", new HashMap<String, Object>(), null);
        assertTrue("valid value with null entry must pass", true);
    }

    @Test
    public void postModifyIsNoopDoesNotThrow() throws Exception {
        // Arrange
        DefaultCalendarIdCallback callback = new DefaultCalendarIdCallback();

        // Act / Assert
        callback.postModify(modifyCtx(), ATTR, null);
        assertNotNull(callback);
    }
}
