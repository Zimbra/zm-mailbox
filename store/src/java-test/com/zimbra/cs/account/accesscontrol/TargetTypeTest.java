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

import com.zimbra.common.service.ServiceException;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TargetType} enum.
 *
 * Covers: fromCode(), getCode(), getPrettyName(), isInheritedBy(),
 * subTargetTypes(), inheritFrom(), isGroup(), needsTargetIdentity().
 * All tests run without LDAP.
 */
public class TargetTypeTest {

    // ---------------------------------------------------------------
    // fromCode
    // ---------------------------------------------------------------

    @Test
    public void testFromCode_validCodes_returnMatchingEnum() throws ServiceException {
        assertEquals(TargetType.account,        TargetType.fromCode("account"));
        assertEquals(TargetType.calresource,    TargetType.fromCode("calresource"));
        assertEquals(TargetType.cos,            TargetType.fromCode("cos"));
        assertEquals(TargetType.dl,             TargetType.fromCode("dl"));
        assertEquals(TargetType.group,          TargetType.fromCode("group"));
        assertEquals(TargetType.domain,         TargetType.fromCode("domain"));
        assertEquals(TargetType.server,         TargetType.fromCode("server"));
        assertEquals(TargetType.alwaysoncluster,TargetType.fromCode("alwaysoncluster"));
        assertEquals(TargetType.ucservice,      TargetType.fromCode("ucservice"));
        assertEquals(TargetType.xmppcomponent,  TargetType.fromCode("xmppcomponent"));
        assertEquals(TargetType.zimlet,         TargetType.fromCode("zimlet"));
        assertEquals(TargetType.config,         TargetType.fromCode("config"));
        assertEquals(TargetType.global,         TargetType.fromCode("global"));
    }

    @Test(expected = ServiceException.class)
    public void testFromCode_invalidCode_throwsServiceException() throws ServiceException {
        TargetType.fromCode("nonexistent");
    }

    @Test(expected = ServiceException.class)
    public void testFromCode_emptyString_throwsServiceException() throws ServiceException {
        TargetType.fromCode("");
    }

    // ---------------------------------------------------------------
    // getCode / getPrettyName
    // ---------------------------------------------------------------

    @Test
    public void testGetCode_returnsEnumName() {
        for (TargetType tt : TargetType.values()) {
            assertEquals(tt.name(), tt.getCode());
        }
    }

    @Test
    public void testGetPrettyName_knownValues() {
        assertEquals("Account",      TargetType.account.getPrettyName());
        assertEquals("Domain",       TargetType.domain.getPrettyName());
        assertEquals("GlobalGrant",  TargetType.global.getPrettyName());
        assertEquals("DistributionList", TargetType.dl.getPrettyName());
        assertEquals("DynamicGroup", TargetType.group.getPrettyName());
    }

    // ---------------------------------------------------------------
    // isInheritedBy
    // ---------------------------------------------------------------

    @Test
    public void testIsInheritedBy_account_onlyByAccount() {
        assertTrue(TargetType.account.isInheritedBy(TargetType.account));
        assertFalse(TargetType.account.isInheritedBy(TargetType.calresource));
        assertFalse(TargetType.account.isInheritedBy(TargetType.domain));
        assertFalse(TargetType.account.isInheritedBy(TargetType.global));
    }

    @Test
    public void testIsInheritedBy_calresource_onlyByCalresource() {
        assertTrue(TargetType.calresource.isInheritedBy(TargetType.calresource));
        assertFalse(TargetType.calresource.isInheritedBy(TargetType.account));
    }

    @Test
    public void testIsInheritedBy_dl_byAccountCalresourceAndDl() {
        assertTrue(TargetType.dl.isInheritedBy(TargetType.account));
        assertTrue(TargetType.dl.isInheritedBy(TargetType.calresource));
        assertTrue(TargetType.dl.isInheritedBy(TargetType.dl));
        assertFalse(TargetType.dl.isInheritedBy(TargetType.domain));
        assertFalse(TargetType.dl.isInheritedBy(TargetType.group));
    }

    @Test
    public void testIsInheritedBy_group_byAccountCalresourceAndGroup() {
        assertTrue(TargetType.group.isInheritedBy(TargetType.account));
        assertTrue(TargetType.group.isInheritedBy(TargetType.calresource));
        assertTrue(TargetType.group.isInheritedBy(TargetType.group));
        assertFalse(TargetType.group.isInheritedBy(TargetType.dl));
        assertFalse(TargetType.group.isInheritedBy(TargetType.domain));
    }

    @Test
    public void testIsInheritedBy_domain_byFiveTypes() {
        assertTrue(TargetType.domain.isInheritedBy(TargetType.account));
        assertTrue(TargetType.domain.isInheritedBy(TargetType.calresource));
        assertTrue(TargetType.domain.isInheritedBy(TargetType.dl));
        assertTrue(TargetType.domain.isInheritedBy(TargetType.group));
        assertTrue(TargetType.domain.isInheritedBy(TargetType.domain));
        assertFalse(TargetType.domain.isInheritedBy(TargetType.cos));
        assertFalse(TargetType.domain.isInheritedBy(TargetType.server));
        assertFalse(TargetType.domain.isInheritedBy(TargetType.global));
    }

    @Test
    public void testIsInheritedBy_global_byAllThirteenTypes() {
        for (TargetType tt : TargetType.values()) {
            assertTrue("global.isInheritedBy(" + tt + ") should be true",
                    TargetType.global.isInheritedBy(tt));
        }
    }

    @Test
    public void testIsInheritedBy_cos_onlyByCos() {
        assertTrue(TargetType.cos.isInheritedBy(TargetType.cos));
        assertFalse(TargetType.cos.isInheritedBy(TargetType.account));
        assertFalse(TargetType.cos.isInheritedBy(TargetType.domain));
    }

    @Test
    public void testIsInheritedBy_server_onlyByServer() {
        assertTrue(TargetType.server.isInheritedBy(TargetType.server));
        assertFalse(TargetType.server.isInheritedBy(TargetType.account));
    }

    // ---------------------------------------------------------------
    // subTargetTypes
    // ---------------------------------------------------------------

    @Test
    public void testSubTargetTypes_account_isEmpty() {
        assertTrue(TargetType.account.subTargetTypes().isEmpty());
    }

    @Test
    public void testSubTargetTypes_domain_doesNotContainSelf_containsMembers() {
        Set<TargetType> subs = TargetType.domain.subTargetTypes();
        assertFalse("domain.subTargetTypes() must not contain domain", subs.contains(TargetType.domain));
        assertTrue(subs.contains(TargetType.account));
        assertTrue(subs.contains(TargetType.calresource));
        assertTrue(subs.contains(TargetType.dl));
        assertTrue(subs.contains(TargetType.group));
    }

    @Test
    public void testSubTargetTypes_global_doesNotContainGlobal() {
        Set<TargetType> subs = TargetType.global.subTargetTypes();
        assertFalse(subs.contains(TargetType.global));
        // global inherits all 13; sub-targets exclude self → 12 entries
        assertEquals(12, subs.size());
    }

    // ---------------------------------------------------------------
    // inheritFrom
    // ---------------------------------------------------------------

    @Test
    public void testInheritFrom_account_containsExpectedTypes() {
        Set<TargetType> from = TargetType.account.inheritFrom();
        // account can inherit from: account, dl, group, domain, global
        assertTrue(from.contains(TargetType.account));
        assertTrue(from.contains(TargetType.dl));
        assertTrue(from.contains(TargetType.group));
        assertTrue(from.contains(TargetType.domain));
        assertTrue(from.contains(TargetType.global));
        assertFalse(from.contains(TargetType.cos));
        assertFalse(from.contains(TargetType.server));
    }

    @Test
    public void testInheritFrom_global_containsOnlyGlobal() {
        // global only inherits from itself
        Set<TargetType> from = TargetType.global.inheritFrom();
        assertTrue(from.contains(TargetType.global));
        assertEquals(1, from.size());
    }

    // ---------------------------------------------------------------
    // isGroup / needsTargetIdentity
    // ---------------------------------------------------------------

    @Test
    public void testIsGroup_dlAndGroup_returnTrue() {
        assertTrue(TargetType.dl.isGroup());
        assertTrue(TargetType.group.isGroup());
    }

    @Test
    public void testIsGroup_nonGroupTypes_returnFalse() {
        assertFalse(TargetType.account.isGroup());
        assertFalse(TargetType.domain.isGroup());
        assertFalse(TargetType.global.isGroup());
        assertFalse(TargetType.cos.isGroup());
        assertFalse(TargetType.server.isGroup());
    }

    @Test
    public void testNeedsTargetIdentity_config_returnsFalse() {
        assertFalse(TargetType.config.needsTargetIdentity());
    }

    @Test
    public void testNeedsTargetIdentity_global_returnsFalse() {
        assertFalse(TargetType.global.needsTargetIdentity());
    }

    @Test
    public void testNeedsTargetIdentity_account_returnsTrue() {
        assertTrue(TargetType.account.needsTargetIdentity());
    }

    @Test
    public void testNeedsTargetIdentity_domain_returnsTrue() {
        assertTrue(TargetType.domain.needsTargetIdentity());
    }

    // ---------------------------------------------------------------
    // Enum completeness
    // ---------------------------------------------------------------

    @Test
    public void testValues_exactlyThirteenTypes() {
        assertEquals(13, TargetType.values().length);
    }
}
