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

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AttributeConstraint}.
 *
 * Tests that can run without LDAP:
 * <ul>
 *   <li>Direct construction of the base class (package-private constructor).</li>
 *   <li>{@code toString()} with no min/max/values.</li>
 *   <li>{@code violateConstraint()} static method with various map configurations.</li>
 * </ul>
 *
 * Tests requiring {@link com.zimbra.cs.account.AttributeManager} (LDAP) –
 * namely {@code newConstraint()} and {@code fromString()} – are not covered here.
 */
public class AttributeConstraintTest {

    // ---------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_setsAttrName_visibleViaToString() {
        AttributeConstraint c = new AttributeConstraint("zimbraPasswordMinLength");
        // toString() includes the attr name as the first element
        assertTrue(c.toString().startsWith("zimbraPasswordMinLength"));
    }

    // ---------------------------------------------------------------
    // toString – base class with no constraints
    // ---------------------------------------------------------------

    @Test
    public void testToString_noConstraints_returnsAttrNameOnly() {
        AttributeConstraint c = new AttributeConstraint("zimbraMailQuota");
        assertEquals("zimbraMailQuota", c.toString());
    }

    @Test
    public void testToString_differentAttrName_returnsCorrectName() {
        AttributeConstraint c = new AttributeConstraint("zimbraPrefGroupMailBy");
        assertEquals("zimbraPrefGroupMailBy", c.toString());
    }

    // ---------------------------------------------------------------
    // violated – base class: no values, no min/max
    // ---------------------------------------------------------------

    @Test
    public void testViolated_noConstraints_stringValue_returnsFalse() throws ServiceException {
        AttributeConstraint c = new AttributeConstraint("zimbraAttr");
        // base class: mValues==null → violateValues returns false; violateMinMax returns false
        assertFalse(c.violated("anyValue"));
    }

    @Test
    public void testViolated_noConstraints_stringArrayValue_returnsFalse() throws ServiceException {
        AttributeConstraint c = new AttributeConstraint("zimbraAttr");
        assertFalse(c.violated(new String[]{"a", "b", "c"}));
    }

    @Test(expected = ServiceException.class)
    public void testViolated_nonStringValue_throwsServiceException() throws ServiceException {
        AttributeConstraint c = new AttributeConstraint("zimbraAttr");
        // Object value that is neither String nor String[] → FAILURE
        c.violated(Integer.valueOf(42));
    }

    // ---------------------------------------------------------------
    // violateConstraint – static helper
    // ---------------------------------------------------------------

    @Test
    public void testViolateConstraint_emptyMap_returnsFalse() throws ServiceException {
        Map<String, AttributeConstraint> empty = new HashMap<String, AttributeConstraint>();
        assertFalse(AttributeConstraint.violateConstraint(empty, "zimbraMailQuota", "1000"));
    }

    @Test
    public void testViolateConstraint_constraintPresentButNoViolation_returnsFalse()
            throws ServiceException {
        // Constraint with no min/max/values – base class always returns false
        AttributeConstraint c = new AttributeConstraint("zimbraAttr");
        Map<String, AttributeConstraint> map = new HashMap<String, AttributeConstraint>();
        map.put("zimbraAttr", c);
        assertFalse(AttributeConstraint.violateConstraint(map, "zimbraAttr", "anyValue"));
    }

    @Test
    public void testViolateConstraint_missingAttr_returnsFalse() throws ServiceException {
        // attr "zimbraA" is in the map but we are querying "zimbraB" → no constraint
        Map<String, AttributeConstraint> map = new HashMap<String, AttributeConstraint>();
        map.put("zimbraA", new AttributeConstraint("zimbraA"));
        assertFalse(AttributeConstraint.violateConstraint(map, "zimbraB", "value"));
    }

    @Test
    public void testViolateConstraint_ignoredAttr_zimbraCOSId_returnsFalse()
            throws ServiceException {
        // ignoreConstraint() returns true for zimbraCOSId → constraint is silently ignored
        AttributeConstraint c = new AttributeConstraint("zimbraCOSId");
        Map<String, AttributeConstraint> map = new HashMap<String, AttributeConstraint>();
        map.put("zimbraCOSId", c);
        assertFalse(AttributeConstraint.violateConstraint(map, "zimbraCOSId", "some-cos-uuid"));
    }

    @Test
    public void testViolateConstraint_ignoredAttr_zimbraDomainDefaultCOSId_returnsFalse()
            throws ServiceException {
        // ignoreConstraint() also returns true for zimbraDomainDefaultCOSId
        AttributeConstraint c = new AttributeConstraint("zimbraDomainDefaultCOSId");
        Map<String, AttributeConstraint> map = new HashMap<String, AttributeConstraint>();
        map.put("zimbraDomainDefaultCOSId", c);
        assertFalse(AttributeConstraint.violateConstraint(
                map, "zimbraDomainDefaultCOSId", "some-cos-uuid"));
    }

    // ---------------------------------------------------------------
    // getMin / getMax – base class always returns null
    // ---------------------------------------------------------------

    @Test
    public void testGetMin_baseClass_returnsNull() {
        AttributeConstraint c = new AttributeConstraint("zimbraAttr");
        assertNull(c.getMin());
    }

    @Test
    public void testGetMax_baseClass_returnsNull() {
        AttributeConstraint c = new AttributeConstraint("zimbraAttr");
        assertNull(c.getMax());
    }

    // ---------------------------------------------------------------
    // setMin / setMax – base class throws PARSE_ERROR
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testSetMin_baseClass_throwsParseError() throws ServiceException {
        new AttributeConstraint("zimbraAttr").setMin("10");
    }

    @Test(expected = ServiceException.class)
    public void testSetMax_baseClass_throwsParseError() throws ServiceException {
        new AttributeConstraint("zimbraAttr").setMax("100");
    }
}
