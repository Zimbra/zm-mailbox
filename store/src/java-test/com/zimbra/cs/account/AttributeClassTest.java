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

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Entry.EntryType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AttributeClass} enum: object-class names, provisionable
 * flag, entry-type mapping, the reverse OC lookup map, and string parsing.
 */
public class AttributeClassTest {

    @Test
    public void getOCNameAccountReturnsZimbraAccount() {
        // Act
        String ocName = AttributeClass.account.getOCName();

        // Assert
        assertEquals("zimbraAccount", ocName);
    }

    @Test
    public void isProvisionableAccountReturnsTrue() {
        // Act + Assert
        assertTrue("account must be provisionable", AttributeClass.account.isProvisionable());
    }

    @Test
    public void isProvisionableMailRecipientReturnsFalse() {
        // Act + Assert
        assertFalse("mailRecipient must NOT be provisionable",
                AttributeClass.mailRecipient.isProvisionable());
    }

    @Test
    public void getEntryTypeAccountReturnsAccountEntryType() {
        // Act
        EntryType type = AttributeClass.account.getEntryType();

        // Assert
        assertEquals(EntryType.ACCOUNT, type);
    }

    @Test
    public void getEntryTypeTwoArgConstructorValueIsNull() {
        // mimeEntry was declared with the 2-arg constructor (no EntryType)
        // Act
        EntryType type = AttributeClass.mimeEntry.getEntryType();

        // Assert
        assertNull("2-arg ctor entries have null entry type", type);
    }

    @Test
    public void getEntryTypeDomainReturnsDomainEntryType() {
        // Act + Assert
        assertEquals(EntryType.DOMAIN, AttributeClass.domain.getEntryType());
    }

    @Test
    public void getAttributeClassKnownOCNameReturnsMatchingEnum() {
        // Act — reverse lookup by registered object-class name
        AttributeClass clazz = AttributeClass.getAttributeClass("zimbraCalendarResource");

        // Assert
        assertNotNull("known OC name must resolve", clazz);
        assertSame(AttributeClass.calendarResource, clazz);
        assertEquals("zimbraCalendarResource", clazz.getOCName());
    }

    @Test
    public void getAttributeClassUnknownOCNameReturnsNull() {
        // Act + Assert
        assertNull("unknown OC name must resolve to null",
                AttributeClass.getAttributeClass("zimbraNoSuchObjectClass"));
    }

    @Test
    public void getAttributeClassMatchesPublicConstant() {
        // The OC_* public constants are derived from getOCName(); both views must agree.
        // Act
        AttributeClass byConstant = AttributeClass.getAttributeClass(AttributeClass.OC_zimbraDomain);

        // Assert
        assertSame(AttributeClass.domain, byConstant);
        assertEquals(AttributeClass.OC_zimbraDomain, AttributeClass.domain.getOCName());
    }

    @Test
    public void fromStringValidNameReturnsEnumValue() throws Exception {
        // Act
        AttributeClass clazz = AttributeClass.fromString("server");

        // Assert
        assertSame(AttributeClass.server, clazz);
        assertEquals("zimbraServer", clazz.getOCName());
        assertEquals(EntryType.SERVER, clazz.getEntryType());
    }

    @Test
    public void fromStringUnknownNameThrowsParseError() {
        // Act + Assert
        try {
            AttributeClass.fromString("definitelyNotAValue");
            fail("expected ServiceException for unknown attribute class name");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue("message should name the bad value",
                    e.getMessage().contains("definitelyNotAValue"));
        }
    }

    @Test
    public void fromStringRoundTripsValueOf() throws Exception {
        // Workflow: valueOf-style name resolves and the round-trip is stable.
        // Act
        AttributeClass first = AttributeClass.fromString("addressList");
        AttributeClass second = AttributeClass.fromString(first.name());

        // Assert
        assertSame(first, second);
        assertEquals("zimbraAddressList", first.getOCName());
        assertEquals(EntryType.ADDRESS_LIST, first.getEntryType());
    }

    @Test
    public void publicConstantsMatchEnumOCNamesForSampling() {
        // Assert several derived OC_* constants stay aligned with their enum's getOCName().
        assertEquals(AttributeClass.cos.getOCName(), AttributeClass.OC_zimbraCOS);
        assertEquals(AttributeClass.alias.getOCName(), AttributeClass.OC_zimbraAlias);
        assertEquals(AttributeClass.group.getOCName(), AttributeClass.OC_zimbraGroup);
        assertEquals(AttributeClass.signature.getOCName(), AttributeClass.OC_zimbraSignature);
    }
}
