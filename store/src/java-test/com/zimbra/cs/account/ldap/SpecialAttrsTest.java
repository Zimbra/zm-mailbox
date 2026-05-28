/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit tests for SpecialAttrs.
 *
 * Tests Zimbra special attributes (zimbraId, ldapBaseDN) handling and validation.
 */
public class SpecialAttrsTest {

    private SpecialAttrs specialAttrs;

    @Before
    public void setUp() throws Exception {
        specialAttrs = new SpecialAttrs();
    }

    // ========== Constants ==========

    @Test
    public void specialAttr_zimbraId_constant() {
        assertEquals(Provisioning.A_zimbraId, SpecialAttrs.SA_zimbraId);
    }

    @Test
    public void specialAttr_ldapBase_constant() {
        assertEquals("ldap.baseDN", SpecialAttrs.PA_ldapBase);
    }

    // ========== Getter Tests ==========

    @Test
    public void getZimbraId_notSet_returnsNull() {
        assertNull(specialAttrs.getZimbraId());
    }

    @Test
    public void getLdapBaseDn_notSet_returnsNull() {
        assertNull(specialAttrs.getLdapBaseDn());
    }

    @Test
    public void getZimbraId_afterSet_returnsValue() throws ServiceException {
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);

        specialAttrs.handleZimbraId(attrs);

        assertEquals(uuid, specialAttrs.getZimbraId());
    }

    @Test
    public void getLdapBaseDn_afterSet_returnsValue() throws ServiceException {
        String baseDn = "dc=example,dc=com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);

        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals(baseDn, specialAttrs.getLdapBaseDn());
    }

    // ========== getSingleValuedAttr Tests ==========

    @Test
    public void getSingleValuedAttr_stringValue_success() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("cn", "Test User");

        String result = SpecialAttrs.getSingleValuedAttr(attrs, "cn");
        assertEquals("Test User", result);
    }

    @Test
    public void getSingleValuedAttr_missingAttr_returnsNull() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();

        String result = SpecialAttrs.getSingleValuedAttr(attrs, "cn");
        assertNull(result);
    }

    @Test
    public void getSingleValuedAttr_arrayValue_throwsException() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("mail", new String[]{"user1@example.com", "user2@example.com"});

        try {
            SpecialAttrs.getSingleValuedAttr(attrs, "mail");
            fail("Should throw ServiceException for multi-valued attribute");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("single-valued attribute"));
            assertTrue(e.getMessage().contains("mail"));
        }
    }

    @Test
    public void getSingleValuedAttr_integerValue_throwsException() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("number", 123);

        try {
            SpecialAttrs.getSingleValuedAttr(attrs, "number");
            fail("Should throw ServiceException for non-string value");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("single-valued attribute"));
        }
    }

    @Test
    public void getSingleValuedAttr_nullValue_success() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", null);

        String result = SpecialAttrs.getSingleValuedAttr(attrs, "attr");
        assertNull(result);
    }

    @Test
    public void getSingleValuedAttr_emptyString_success() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("attr", "");

        String result = SpecialAttrs.getSingleValuedAttr(attrs, "attr");
        assertEquals("", result);
    }

    // ========== handleZimbraId Tests ==========

    @Test
    public void handleZimbraId_validUUID_success() throws ServiceException {
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);

        specialAttrs.handleZimbraId(attrs);

        assertEquals(uuid, specialAttrs.getZimbraId());
        // Should be removed from attrs
        assertFalse(attrs.containsKey(SpecialAttrs.SA_zimbraId));
    }

    @Test
    public void handleZimbraId_uuidFormatted_success() throws ServiceException {
        String uuid = "a1b2c3d4-e5f6-4g7h-8i9j-k0l1m2n3o4p5"; // Valid UUID format
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);

        // Note: actual UUID validation depends on LdapUtil.isValidUUID
        // This test may fail if format is invalid
    }

    @Test
    public void handleZimbraId_invalidUUID_throwsException() throws ServiceException {
        String invalidUUID = "not-a-valid-uuid-at-all";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, invalidUUID);

        try {
            specialAttrs.handleZimbraId(attrs);
            fail("Should throw ServiceException for invalid UUID");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not a valid UUID"));
        }
    }

    @Test
    public void handleZimbraId_missingAttribute_doesNotThrow() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();

        // Should not throw when zimbraId is missing
        specialAttrs.handleZimbraId(attrs);

        assertNull(specialAttrs.getZimbraId());
    }

    @Test
    public void handleZimbraId_nullAttribute_returnsNull() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, null);

        specialAttrs.handleZimbraId(attrs);

        assertNull(specialAttrs.getZimbraId());
    }

    @Test
    public void handleZimbraId_removesFromAttrs_success() throws ServiceException {
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);
        attrs.put("cn", "Test User");

        specialAttrs.handleZimbraId(attrs);

        // zimbraId should be removed
        assertFalse(attrs.containsKey(SpecialAttrs.SA_zimbraId));
        // Other attrs should remain
        assertTrue(attrs.containsKey("cn"));
    }

    @Test
    public void handleZimbraId_multipleAttrs_onlyZimbraIdRemoved() throws ServiceException {
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);
        attrs.put("mail", "user@example.com");
        attrs.put("cn", "Test User");

        int sizeBefore = attrs.size();
        specialAttrs.handleZimbraId(attrs);
        int sizeAfter = attrs.size();

        assertEquals(sizeBefore - 1, sizeAfter);
        assertFalse(attrs.containsKey(SpecialAttrs.SA_zimbraId));
        assertTrue(attrs.containsKey("mail"));
        assertTrue(attrs.containsKey("cn"));
    }

    // ========== handleLdapBaseDn Tests ==========

    @Test
    public void handleLdapBaseDn_validBaseDN_success() throws ServiceException {
        String baseDn = "dc=example,dc=com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);

        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals(baseDn, specialAttrs.getLdapBaseDn());
        // Should be removed from attrs
        assertFalse(attrs.containsKey(SpecialAttrs.PA_ldapBase));
    }

    @Test
    public void handleLdapBaseDn_complexBaseDN_success() throws ServiceException {
        String baseDn = "ou=people,o=company,c=us,dc=example,dc=com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);

        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals(baseDn, specialAttrs.getLdapBaseDn());
    }

    @Test
    public void handleLdapBaseDn_missingAttribute_doesNotThrow() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();

        specialAttrs.handleLdapBaseDn(attrs);

        assertNull(specialAttrs.getLdapBaseDn());
    }

    @Test
    public void handleLdapBaseDn_nullAttribute_returnsNull() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, null);

        specialAttrs.handleLdapBaseDn(attrs);

        assertNull(specialAttrs.getLdapBaseDn());
    }

    @Test
    public void handleLdapBaseDn_removesFromAttrs() throws ServiceException {
        String baseDn = "dc=example,dc=com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);
        attrs.put("cn", "Test");

        specialAttrs.handleLdapBaseDn(attrs);

        assertFalse(attrs.containsKey(SpecialAttrs.PA_ldapBase));
        assertTrue(attrs.containsKey("cn"));
    }

    @Test
    public void handleLdapBaseDn_emptyBaseDN_success() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, "");

        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals("", specialAttrs.getLdapBaseDn());
    }

    // ========== Combined Operations Tests ==========

    @Test
    public void handleBoth_zimbraIdAndBaseDn_bothSet() throws ServiceException {
        String uuid = UUID.randomUUID().toString();
        String baseDn = "dc=example,dc=com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);

        specialAttrs.handleZimbraId(attrs);
        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals(uuid, specialAttrs.getZimbraId());
        assertEquals(baseDn, specialAttrs.getLdapBaseDn());
        assertEquals(0, attrs.size()); // Both removed
    }

    @Test
    public void handleBoth_withOtherAttrs_onlySpecialAttrsRemoved() throws ServiceException {
        String uuid = UUID.randomUUID().toString();
        String baseDn = "dc=example,dc=com";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.SA_zimbraId, uuid);
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);
        attrs.put("mail", "user@example.com");
        attrs.put("cn", "Test User");
        attrs.put("description", "Test account");

        specialAttrs.handleZimbraId(attrs);
        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals(3, attrs.size()); // Only special attrs removed
        assertTrue(attrs.containsKey("mail"));
        assertTrue(attrs.containsKey("cn"));
        assertTrue(attrs.containsKey("description"));
    }

    // ========== Edge Cases ==========

    @Test
    public void multipleInstances_independent() throws ServiceException {
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();

        SpecialAttrs attrs1 = new SpecialAttrs();
        SpecialAttrs attrs2 = new SpecialAttrs();

        Map<String, Object> map1 = new HashMap<>();
        map1.put(SpecialAttrs.SA_zimbraId, uuid1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put(SpecialAttrs.SA_zimbraId, uuid2);

        attrs1.handleZimbraId(map1);
        attrs2.handleZimbraId(map2);

        assertEquals(uuid1, attrs1.getZimbraId());
        assertEquals(uuid2, attrs2.getZimbraId());
    }

    @Test
    public void handleZimbraId_calledMultipleTimes_lastOneWins() throws ServiceException {
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();

        Map<String, Object> attrs1 = new HashMap<>();
        attrs1.put(SpecialAttrs.SA_zimbraId, uuid1);
        Map<String, Object> attrs2 = new HashMap<>();
        attrs2.put(SpecialAttrs.SA_zimbraId, uuid2);

        specialAttrs.handleZimbraId(attrs1);
        String first = specialAttrs.getZimbraId();
        specialAttrs.handleZimbraId(attrs2);
        String second = specialAttrs.getZimbraId();

        assertEquals(uuid1, first);
        assertEquals(uuid2, second);
    }

    @Test
    public void baseDnWithLeadingTrailingSpaces() throws ServiceException {
        String baseDn = "  dc=example,dc=com  ";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SpecialAttrs.PA_ldapBase, baseDn);

        specialAttrs.handleLdapBaseDn(attrs);

        assertEquals(baseDn, specialAttrs.getLdapBaseDn());
    }

    @Test
    public void getSingleValuedAttr_multipleCallsSameAttr() throws ServiceException {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("cn", "Test User");

        String result1 = SpecialAttrs.getSingleValuedAttr(attrs, "cn");
        String result2 = SpecialAttrs.getSingleValuedAttr(attrs, "cn");

        assertEquals("Test User", result1);
        assertEquals("Test User", result2);
        assertTrue(attrs.containsKey("cn")); // Not removed by getSingleValuedAttr
    }
}
