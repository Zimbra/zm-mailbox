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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Right.RightType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link InlineAttrRight}: the compose helpers, the looksLikeOne detector,
 * and the parse-and-build workflow in {@code newInlineAttrRight} (get/set ops, target-type
 * decoding, and each parse-error branch). {@code newInlineAttrRight} validates the attribute
 * against the real {@code AttributeManager}, so the harness is booted in @BeforeClass.
 */
public class InlineAttrRightTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();   // boots AttributeManager + in-memory MockProvisioning
    }

    // ---------- compose helpers (pure) ----------

    @Test
    public void composeGetRightAccountAttrBuildsGetDotTargetDotAttr() {
        // Act
        String right = InlineAttrRight.composeGetRight(TargetType.account, "displayName");

        // Assert
        assertEquals("get.account.displayName", right);
    }

    @Test
    public void composeSetRightAccountAttrBuildsSetDotTargetDotAttr() {
        // Act
        String right = InlineAttrRight.composeSetRight(TargetType.account, "displayName");

        // Assert
        assertEquals("set.account.displayName", right);
    }

    // ---------- looksLikeOne ----------

    @Test
    public void looksLikeOneDottedStringReturnsTrue() {
        // Act / Assert
        assertTrue(InlineAttrRight.looksLikeOne("get.account.displayName"));
    }

    @Test
    public void looksLikeOnePlainNameReturnsFalse() {
        // Act / Assert
        assertFalse("a name without a dot is not an inline attr right",
                InlineAttrRight.looksLikeOne("renameAccount"));
    }

    // ---------- newInlineAttrRight workflow ----------

    @Test
    public void newInlineAttrRightGetAccountAttrBuildsGetAttrsRight() throws Exception {
        // Arrange
        String name = InlineAttrRight.composeGetRight(TargetType.account, Provisioning.A_displayName);

        // Act
        InlineAttrRight iar = InlineAttrRight.newInlineAttrRight(name);

        // Assert — full resulting state
        assertNotNull(iar);
        assertEquals(name, iar.getName());
        assertEquals("description should mirror the right name", name, iar.getDesc());
        assertEquals(RightType.getAttrs, iar.getRightType());
        assertTrue("a getAttrs inline right is an attr right", iar.isAttrRight());
        assertTrue("the target type must include account",
                iar.getTargetTypes().contains(TargetType.account));
        assertTrue("the named attr must be in the right's attr set",
                iar.getAttrs().contains(Provisioning.A_displayName));
    }

    @Test
    public void newInlineAttrRightSetAccountAttrBuildsSetAttrsRight() throws Exception {
        // Arrange
        String name = InlineAttrRight.composeSetRight(TargetType.account, Provisioning.A_displayName);

        // Act
        InlineAttrRight iar = InlineAttrRight.newInlineAttrRight(name);

        // Assert
        assertEquals(RightType.setAttrs, iar.getRightType());
        assertEquals(name, iar.getName());
        assertTrue(iar.getAttrs().contains(Provisioning.A_displayName));
    }

    @Test
    public void newInlineAttrRightWrongPartCountThrowsParseError() throws Exception {
        // Act / Assert — only two parts, not three
        try {
            InlineAttrRight.newInlineAttrRight("get.account");
            fail("expected PARSE_ERROR for a 2-part right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    @Test
    public void newInlineAttrRightInvalidOpThrowsParseError() throws Exception {
        // Act / Assert — op is neither get nor set
        try {
            InlineAttrRight.newInlineAttrRight("del.account.displayName");
            fail("expected PARSE_ERROR for an invalid op");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("del"));
        }
    }

    @Test
    public void newInlineAttrRightGlobalTargetTypeThrowsParseError() throws Exception {
        // Act / Assert — global target type is explicitly disallowed
        try {
            InlineAttrRight.newInlineAttrRight("get.global.displayName");
            fail("expected PARSE_ERROR for global target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    @Test
    public void isTheSameRightSameNameReturnsTrue() throws Exception {
        // Arrange
        String name = InlineAttrRight.composeGetRight(TargetType.account, Provisioning.A_displayName);
        InlineAttrRight a = InlineAttrRight.newInlineAttrRight(name);
        InlineAttrRight b = InlineAttrRight.newInlineAttrRight(name);

        // Act / Assert — two rights built from the same name compare equal by name
        assertTrue("rights with identical names must be the same right", a.isTheSameRight(b));
    }

    @Test
    public void isTheSameRightDifferentNameReturnsFalse() throws Exception {
        // Arrange
        InlineAttrRight get = InlineAttrRight.newInlineAttrRight(
                InlineAttrRight.composeGetRight(TargetType.account, Provisioning.A_displayName));
        InlineAttrRight set = InlineAttrRight.newInlineAttrRight(
                InlineAttrRight.composeSetRight(TargetType.account, Provisioning.A_displayName));

        // Act / Assert
        assertFalse("get and set rights for the same attr are different rights",
                get.isTheSameRight(set));
    }
}
