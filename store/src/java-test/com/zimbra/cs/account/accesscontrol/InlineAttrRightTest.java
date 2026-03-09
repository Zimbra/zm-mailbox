/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link InlineAttrRight} static helpers.
 *
 * Only the static helper methods (composeGetRight, composeSetRight, looksLikeOne)
 * are tested here, as they require no LDAP or AttributeManager.
 * newInlineAttrRight() requires a live AttributeManager and is not tested.
 */
public class InlineAttrRightTest {

    // ---------------------------------------------------------------
    // composeGetRight
    // ---------------------------------------------------------------

    @Test
    public void testComposeGetRight_account_buildsCorrectKey() {
        assertEquals("get.account.zimbraId",
                InlineAttrRight.composeGetRight(TargetType.account, "zimbraId"));
    }

    @Test
    public void testComposeGetRight_domain_buildsCorrectKey() {
        assertEquals("get.domain.zimbraDomainName",
                InlineAttrRight.composeGetRight(TargetType.domain, "zimbraDomainName"));
    }

    @Test
    public void testComposeGetRight_dl_buildsCorrectKey() {
        assertEquals("get.dl.zimbraId",
                InlineAttrRight.composeGetRight(TargetType.dl, "zimbraId"));
    }

    @Test
    public void testComposeGetRight_cos_buildsCorrectKey() {
        assertEquals("get.cos.zimbraId",
                InlineAttrRight.composeGetRight(TargetType.cos, "zimbraId"));
    }

    @Test
    public void testComposeGetRight_server_buildsCorrectKey() {
        assertEquals("get.server.zimbraSmtpHostname",
                InlineAttrRight.composeGetRight(TargetType.server, "zimbraSmtpHostname"));
    }

    // ---------------------------------------------------------------
    // composeSetRight
    // ---------------------------------------------------------------

    @Test
    public void testComposeSetRight_account_buildsCorrectKey() {
        assertEquals("set.account.zimbraMailAlias",
                InlineAttrRight.composeSetRight(TargetType.account, "zimbraMailAlias"));
    }

    @Test
    public void testComposeSetRight_domain_buildsCorrectKey() {
        assertEquals("set.domain.zimbraDomainName",
                InlineAttrRight.composeSetRight(TargetType.domain, "zimbraDomainName"));
    }

    // ---------------------------------------------------------------
    // looksLikeOne
    // ---------------------------------------------------------------

    @Test
    public void testLooksLikeOne_composedGetRight_returnsTrue() {
        String composed = InlineAttrRight.composeGetRight(TargetType.account, "zimbraId");
        assertTrue(InlineAttrRight.looksLikeOne(composed));
    }

    @Test
    public void testLooksLikeOne_composedSetRight_returnsTrue() {
        String composed = InlineAttrRight.composeSetRight(TargetType.domain, "attr");
        assertTrue(InlineAttrRight.looksLikeOne(composed));
    }

    @Test
    public void testLooksLikeOne_stringContainingDot_returnsTrue() {
        assertTrue(InlineAttrRight.looksLikeOne("a.b"));
        assertTrue(InlineAttrRight.looksLikeOne("has.multiple.dots"));
    }

    @Test
    public void testLooksLikeOne_plainRightName_returnsFalse() {
        assertFalse(InlineAttrRight.looksLikeOne("loginAs"));
        assertFalse(InlineAttrRight.looksLikeOne("adminConsoleRights"));
        assertFalse(InlineAttrRight.looksLikeOne("somePlainRight"));
    }

    @Test
    public void testLooksLikeOne_emptyString_returnsFalse() {
        assertFalse(InlineAttrRight.looksLikeOne(""));
    }

    // ---------------------------------------------------------------
    // Format consistency
    // ---------------------------------------------------------------

    @Test
    public void testComposeGetRight_format_isGetDotCodeDotAttr() {
        String result = InlineAttrRight.composeGetRight(TargetType.calresource, "myAttr");
        assertEquals("get.calresource.myAttr", result);
        String[] parts = result.split("\\.");
        assertEquals(3, parts.length);
        assertEquals("get", parts[0]);
        assertEquals("calresource", parts[1]);
        assertEquals("myAttr", parts[2]);
    }

    @Test
    public void testComposeSetRight_format_isSetDotCodeDotAttr() {
        String result = InlineAttrRight.composeSetRight(TargetType.server, "myAttr");
        assertEquals("set.server.myAttr", result);
        String[] parts = result.split("\\.");
        assertEquals(3, parts.length);
        assertEquals("set", parts[0]);
        assertEquals("server", parts[1]);
        assertEquals("myAttr", parts[2]);
    }

    @Test
    public void testComposeGetRight_usesTargetTypeCode() {
        for (TargetType tt : TargetType.values()) {
            String composed = InlineAttrRight.composeGetRight(tt, "attr");
            assertTrue("Composed right must contain target type code",
                    composed.contains(tt.getCode()));
        }
    }
}
