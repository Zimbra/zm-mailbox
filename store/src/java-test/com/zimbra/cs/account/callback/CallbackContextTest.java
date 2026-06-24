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

import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link CallbackContext}. This is a pure in-memory state holder shared
 * between pre/post attribute-callback invocations; the tests drive every public method and both
 * enum values through realistic create/modify workflows and assert the resulting state.
 */
public class CallbackContextTest {

    @Test
    public void isCreateCreateOpReturnsTrue() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.CREATE);

        // Act
        boolean create = ctx.isCreate();

        // Assert
        assertTrue("CREATE op must report isCreate()==true", create);
    }

    @Test
    public void isCreateModifyOpReturnsFalse() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        boolean create = ctx.isCreate();

        // Assert
        assertFalse("MODIFY op must report isCreate()==false", create);
    }

    @Test
    public void getCreatingEntryNameNotSetReturnsNull() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.CREATE);

        // Act + Assert -- unset name defaults to null
        assertNull("creating entry name defaults to null", ctx.getCreatingEntryName());
    }

    @Test
    public void setCreatingEntryNameThenGetReturnsStoredName() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.CREATE);

        // Act
        ctx.setCreatingEntryName("user@example.com");

        // Assert
        assertEquals("user@example.com", ctx.getCreatingEntryName());
    }

    @Test
    public void setCreatingEntryNameCalledTwiceLastValueWins() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- simulate two phases of a modify pipeline overwriting the name
        ctx.setCreatingEntryName("first@example.com");
        ctx.setCreatingEntryName("second@example.com");

        // Assert
        assertEquals("second@example.com", ctx.getCreatingEntryName());
    }

    @Test
    public void getDataNotSetReturnsNull() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        assertNull("unset data key returns null", ctx.getData(DataKey.MAX_SIGNATURE_LEN));
    }

    @Test
    public void setDataThenGetReturnsStoredValue() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        ctx.setData(DataKey.MAX_SIGNATURE_LEN, "1024");

        // Assert
        assertEquals("1024", ctx.getData(DataKey.MAX_SIGNATURE_LEN));
    }

    @Test
    public void setDataMultipleKeysStoredIndependently() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- distinct keys must not collide
        ctx.setData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_LEN, "256");
        ctx.setData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_NUM_ADDRS, "10");

        // Assert
        assertEquals("256", ctx.getData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_LEN));
        assertEquals("10", ctx.getData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_NUM_ADDRS));
    }

    @Test
    public void setDataSameKeyTwiceOverwrites() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.setData(DataKey.PREV_EPHEMERAL_BACKEND_URL, "ldap://old");

        // Act
        ctx.setData(DataKey.PREV_EPHEMERAL_BACKEND_URL, "ldap://new");

        // Assert
        assertEquals("ldap://new", ctx.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL));
    }

    @Test
    public void isDoneAndSetIfNotFirstCallReturnsFalseAndMarksDone() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        Class<? extends AttributeCallback> cb = MailHost.class;

        // Act -- first invocation reports not-yet-done but records it
        boolean firstSeen = ctx.isDoneAndSetIfNot(cb);

        // Assert
        assertFalse("first invocation must report not-done", firstSeen);
        assertTrue("after first call the callback must be marked done", ctx.isDoneAndSetIfNot(cb));
    }

    @Test
    public void isDoneAndSetIfNotSecondCallSameCallbackReturnsTrue() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        Class<? extends AttributeCallback> cb = DataSourceQuota.class;
        ctx.isDoneAndSetIfNot(cb);

        // Act
        boolean secondSeen = ctx.isDoneAndSetIfNot(cb);

        // Assert
        assertTrue("repeat invocation of the same callback reports done", secondSeen);
    }

    @Test
    public void isDoneAndSetIfNotDifferentCallbacksTrackedIndependently() {
        // Arrange
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- mark one callback done; a different one must still be not-done
        ctx.isDoneAndSetIfNot(MailHost.class);
        boolean otherSeen = ctx.isDoneAndSetIfNot(BackupCSDDedupe.class);

        // Assert
        assertFalse("distinct callbacks are tracked separately", otherSeen);
        assertTrue("the originally-marked callback is still done", ctx.isDoneAndSetIfNot(MailHost.class));
    }

    @Test
    public void opEnumValuesContainsCreateAndModify() {
        // Act
        Op[] ops = Op.values();

        // Assert
        assertEquals("Op has exactly two values", 2, ops.length);
        assertEquals(Op.CREATE, Op.valueOf("CREATE"));
        assertEquals(Op.MODIFY, Op.valueOf("MODIFY"));
    }

    @Test
    public void dataKeyEnumValuesContainAllExpectedKeys() {
        // Act
        DataKey[] keys = DataKey.values();

        // Assert -- the six documented data keys
        assertEquals("DataKey has six values", 6, keys.length);
        assertEquals(DataKey.PREV_EPHEMERAL_BACKEND_URL, DataKey.valueOf("PREV_EPHEMERAL_BACKEND_URL"));
    }
}
