/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import static org.junit.Assert.*;
import org.junit.Test;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

/**
 * Full functional tests for {@link RightModifier}.
 *
 * Tests verify RightModifier enum behavior including modifier parsing,
 * character conversion, SOAP attribute mapping, and modifier descriptions.
 */
public class RightModifierTest {

    /**
     * Test: Create all RightModifier enum values → verify values accessible.
     *
     * Verifies: All enum constants are defined and accessible without error.
     */
    @Test
    public void rightModifier_allEnumValues_defined() {
        // Act & Assert - Verify all enum values are defined
        assertNotNull(RightModifier.RM_DENY);
        assertNotNull(RightModifier.RM_CAN_DELEGATE);
        assertNotNull(RightModifier.RM_SUBDOMAIN);
        assertNotNull(RightModifier.RM_DISINHERIT_SUB_GROUPS);
    }

    /**
     * Test: Convert deny modifier character to RightModifier → verify returns RM_DENY.
     *
     * Verifies: fromChar() correctly maps '-' character to RM_DENY.
     */
    @Test
    public void fromChar_denyModifier_returnsDenyModifier() throws ServiceException {
        // Act
        RightModifier modifier = RightModifier.fromChar('-');

        // Assert
        assertNotNull(modifier);
        assertEquals(RightModifier.RM_DENY, modifier);
    }

    /**
     * Test: Convert can-delegate modifier character to RightModifier → verify returns RM_CAN_DELEGATE.
     *
     * Verifies: fromChar() correctly maps '+' character to RM_CAN_DELEGATE.
     */
    @Test
    public void fromChar_canDelegateModifier_returnsDelegateModifier() throws ServiceException {
        // Act
        RightModifier modifier = RightModifier.fromChar('+');

        // Assert
        assertNotNull(modifier);
        assertEquals(RightModifier.RM_CAN_DELEGATE, modifier);
    }

    /**
     * Test: Convert subdomain modifier character to RightModifier → verify returns RM_SUBDOMAIN.
     *
     * Verifies: fromChar() correctly maps '*' character to RM_SUBDOMAIN.
     */
    @Test
    public void fromChar_subdomainModifier_returnsSubdomainModifier() throws ServiceException {
        // Act
        RightModifier modifier = RightModifier.fromChar('*');

        // Assert
        assertNotNull(modifier);
        assertEquals(RightModifier.RM_SUBDOMAIN, modifier);
    }

    /**
     * Test: Convert disinherit-subgroups modifier character to RightModifier → verify returns RM_DISINHERIT_SUB_GROUPS.
     *
     * Verifies: fromChar() correctly maps '^' character to RM_DISINHERIT_SUB_GROUPS.
     */
    @Test
    public void fromChar_disinheritSubgroupsModifier_returnsDisinheritModifier() throws ServiceException {
        // Act
        RightModifier modifier = RightModifier.fromChar('^');

        // Assert
        assertNotNull(modifier);
        assertEquals(RightModifier.RM_DISINHERIT_SUB_GROUPS, modifier);
    }

    /**
     * Test: Convert unknown character to RightModifier → verify returns null.
     *
     * Verifies: fromChar() returns null for invalid/unknown characters.
     */
    @Test
    public void fromChar_unknownCharacter_returnsNull() throws ServiceException {
        // Act
        RightModifier modifier = RightModifier.fromChar('?');

        // Assert
        assertNull(modifier);
    }

    /**
     * Test: Get modifier character from RM_DENY → verify returns '-'.
     *
     * Verifies: getModifier() returns correct character for deny modifier.
     */
    @Test
    public void getModifier_denyModifier_returnsDashCharacter() {
        // Act
        char modifier = RightModifier.RM_DENY.getModifier();

        // Assert
        assertEquals('-', modifier);
    }

    /**
     * Test: Get modifier character from RM_CAN_DELEGATE → verify returns '+'.
     *
     * Verifies: getModifier() returns correct character for delegate modifier.
     */
    @Test
    public void getModifier_delegateModifier_returnsPlusCharacter() {
        // Act
        char modifier = RightModifier.RM_CAN_DELEGATE.getModifier();

        // Assert
        assertEquals('+', modifier);
    }

    /**
     * Test: Get modifier character from RM_SUBDOMAIN → verify returns '*'.
     *
     * Verifies: getModifier() returns correct character for subdomain modifier.
     */
    @Test
    public void getModifier_subdomainModifier_returnsAsteriskCharacter() {
        // Act
        char modifier = RightModifier.RM_SUBDOMAIN.getModifier();

        // Assert
        assertEquals('*', modifier);
    }

    /**
     * Test: Get modifier character from RM_DISINHERIT_SUB_GROUPS → verify returns '^'.
     *
     * Verifies: getModifier() returns correct character for disinherit modifier.
     */
    @Test
    public void getModifier_disinheritModifier_returnsCaretCharacter() {
        // Act
        char modifier = RightModifier.RM_DISINHERIT_SUB_GROUPS.getModifier();

        // Assert
        assertEquals('^', modifier);
    }

    /**
     * Test: Get SOAP attribute mapping for RM_DENY → verify returns deny attribute constant.
     *
     * Verifies: getSoapAttrMapping() returns correct SOAP attribute for deny.
     */
    @Test
    public void getSoapAttrMapping_denyModifier_returnsDenyAttribute() {
        // Act
        String soapAttr = RightModifier.RM_DENY.getSoapAttrMapping();

        // Assert
        assertNotNull(soapAttr);
        assertEquals(AdminConstants.A_DENY, soapAttr);
    }

    /**
     * Test: Get SOAP attribute mapping for RM_CAN_DELEGATE → verify returns delegate attribute constant.
     *
     * Verifies: getSoapAttrMapping() returns correct SOAP attribute for delegate.
     */
    @Test
    public void getSoapAttrMapping_delegateModifier_returnsDelegateAttribute() {
        // Act
        String soapAttr = RightModifier.RM_CAN_DELEGATE.getSoapAttrMapping();

        // Assert
        assertNotNull(soapAttr);
        assertEquals(AdminConstants.A_CAN_DELEGATE, soapAttr);
    }

    /**
     * Test: Get SOAP attribute mapping for RM_SUBDOMAIN → verify returns subdomain attribute constant.
     *
     * Verifies: getSoapAttrMapping() returns correct SOAP attribute for subdomain.
     */
    @Test
    public void getSoapAttrMapping_subdomainModifier_returnsSubdomainAttribute() {
        // Act
        String soapAttr = RightModifier.RM_SUBDOMAIN.getSoapAttrMapping();

        // Assert
        assertNotNull(soapAttr);
        assertEquals(AdminConstants.A_SUB_DOMAIN, soapAttr);
    }

    /**
     * Test: Get SOAP attribute mapping for RM_DISINHERIT_SUB_GROUPS → verify returns disinherit attribute constant.
     *
     * Verifies: getSoapAttrMapping() returns correct SOAP attribute for disinherit.
     */
    @Test
    public void getSoapAttrMapping_disinheritModifier_returnsDisinheritAttribute() {
        // Act
        String soapAttr = RightModifier.RM_DISINHERIT_SUB_GROUPS.getSoapAttrMapping();

        // Assert
        assertNotNull(soapAttr);
        assertEquals(AdminConstants.A_DISINHERIT_SUB_GROUPS, soapAttr);
    }

    /**
     * Test: Get description from RM_DENY → verify returns deny description.
     *
     * Verifies: getDescription() returns correct description for deny.
     */
    @Test
    public void getDescription_denyModifier_returnsDescription() {
        // Act
        String desc = RightModifier.RM_DENY.getDescription();

        // Assert
        assertNotNull(desc);
        assertTrue(desc.length() > 0);
        assertTrue(desc.contains("denied"));
    }

    /**
     * Test: Get description from RM_CAN_DELEGATE → verify returns delegate description.
     *
     * Verifies: getDescription() returns correct description for delegate.
     */
    @Test
    public void getDescription_delegateModifier_returnsDescription() {
        // Act
        String desc = RightModifier.RM_CAN_DELEGATE.getDescription();

        // Assert
        assertNotNull(desc);
        assertTrue(desc.length() > 0);
        assertTrue(desc.contains("delegated"));
    }

    /**
     * Test: Get description from RM_SUBDOMAIN → verify returns subdomain description.
     *
     * Verifies: getDescription() returns correct description for subdomain.
     */
    @Test
    public void getDescription_subdomainModifier_returnsDescription() {
        // Act
        String desc = RightModifier.RM_SUBDOMAIN.getDescription();

        // Assert
        assertNotNull(desc);
        assertTrue(desc.length() > 0);
        assertTrue(desc.contains("sub domain"));
    }

    /**
     * Test: Get description from RM_DISINHERIT_SUB_GROUPS → verify returns disinherit description.
     *
     * Verifies: getDescription() returns correct description for disinherit.
     */
    @Test
    public void getDescription_disinheritModifier_returnsDescription() {
        // Act
        String desc = RightModifier.RM_DISINHERIT_SUB_GROUPS.getDescription();

        // Assert
        assertNotNull(desc);
        assertTrue(desc.length() > 0);
        assertTrue(desc.contains("sub-group"));
    }

    /**
     * Test: Round-trip conversion: modifier → character → modifier → character.
     *
     * Verifies: fromChar() and getModifier() are inverses for all modifiers.
     */
    @Test
    public void roundTripConversion_allModifiers_consistent() throws ServiceException {
        // Arrange
        RightModifier[] modifiers = {
            RightModifier.RM_DENY,
            RightModifier.RM_CAN_DELEGATE,
            RightModifier.RM_SUBDOMAIN,
            RightModifier.RM_DISINHERIT_SUB_GROUPS
        };

        // Act & Assert - Round-trip each modifier
        for (RightModifier original : modifiers) {
            char ch = original.getModifier();
            RightModifier recovered = RightModifier.fromChar(ch);
            assertEquals("Round-trip failed for " + original.name(), original, recovered);
        }
    }

    /**
     * Test: Parse each modifier character and verify correct mapping workflow.
     *
     * Verifies: fromChar() correctly parses all valid modifier characters.
     */
    @Test
    public void fromChar_allValidCharacters_correctMappings() throws ServiceException {
        // Act & Assert - Verify each character maps correctly
        RightModifier deny = RightModifier.fromChar('-');
        assertNotNull(deny);
        assertEquals(RightModifier.RM_DENY, deny);
        assertEquals(AdminConstants.A_DENY, deny.getSoapAttrMapping());

        RightModifier delegate = RightModifier.fromChar('+');
        assertNotNull(delegate);
        assertEquals(RightModifier.RM_CAN_DELEGATE, delegate);
        assertEquals(AdminConstants.A_CAN_DELEGATE, delegate.getSoapAttrMapping());

        RightModifier subdomain = RightModifier.fromChar('*');
        assertNotNull(subdomain);
        assertEquals(RightModifier.RM_SUBDOMAIN, subdomain);
        assertEquals(AdminConstants.A_SUB_DOMAIN, subdomain.getSoapAttrMapping());

        RightModifier disinherit = RightModifier.fromChar('^');
        assertNotNull(disinherit);
        assertEquals(RightModifier.RM_DISINHERIT_SUB_GROUPS, disinherit);
        assertEquals(AdminConstants.A_DISINHERIT_SUB_GROUPS, disinherit.getSoapAttrMapping());
    }

    /**
     * Test: Parse sequence of invalid characters → all return null.
     *
     * Verifies: fromChar() returns null for all invalid characters.
     */
    @Test
    public void fromChar_invalidCharacters_allReturnNull() throws ServiceException {
        // Arrange
        char[] invalidChars = {'@', '#', '$', '%', '&', 'a', '1', ' ', '\0', '\n'};

        // Act & Assert
        for (char invalidChar : invalidChars) {
            RightModifier result = RightModifier.fromChar(invalidChar);
            assertNull("Should return null for character '" + invalidChar + "'", result);
        }
    }
}
