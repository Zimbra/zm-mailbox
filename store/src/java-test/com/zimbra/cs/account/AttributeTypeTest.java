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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the {@link AttributeType} enum. The enum registers each value into a
 * static name-&gt;type map at class-load time; {@link AttributeType#getType(String)} reverses the
 * lookup. These tests verify the registration/lookup contract for every value plus the unknown
 * and null lookup edge cases.
 */
public class AttributeTypeTest {

    @Test
    public void getTypeKnownBooleanCodeReturnsBooleanType() {
        // Act
        AttributeType type = AttributeType.getType("boolean");

        // Assert
        assertSame("'boolean' must map to TYPE_BOOLEAN", AttributeType.TYPE_BOOLEAN, type);
    }

    @Test
    public void getTypeIntegerCodeReturnsIntegerType() {
        // Act
        AttributeType type = AttributeType.getType("integer");

        // Assert
        assertSame(AttributeType.TYPE_INTEGER, type);
    }

    @Test
    public void getTypeLongCodeReturnsLongType() {
        // Act
        AttributeType type = AttributeType.getType("long");

        // Assert
        assertSame(AttributeType.TYPE_LONG, type);
    }

    @Test
    public void getTypeEveryEnumValueRoundTripsThroughItsCode() {
        // Arrange / Act / Assert — each enum value's registered code must resolve back to itself.
        for (AttributeType expected : AttributeType.values()) {
            String code = codeOf(expected);
            AttributeType actual = AttributeType.getType(code);
            assertSame("code '" + code + "' must resolve to " + expected, expected, actual);
        }
    }

    @Test
    public void getTypeUnknownCodeReturnsNull() {
        // Act
        AttributeType type = AttributeType.getType("no_such_type");

        // Assert
        assertNull("unknown code must yield null", type);
    }

    @Test
    public void getTypeNullCodeReturnsNull() {
        // Act
        AttributeType type = AttributeType.getType(null);

        // Assert
        assertNull("null code must yield null, not throw", type);
    }

    @Test
    public void valuesContainsAllNineteenDeclaredTypes() {
        // Act
        AttributeType[] all = AttributeType.values();

        // Assert — exact count guards against accidental enum edits.
        assertEquals("AttributeType must declare 19 values", 19, all.length);
    }

    @Test
    public void valueOfByEnumNameReturnsSameAsConstant() {
        // Act
        AttributeType byName = AttributeType.valueOf("TYPE_EMAILP");

        // Assert
        assertSame(AttributeType.TYPE_EMAILP, byName);
    }

    @Test
    public void getTypeEmailVariantsResolveToDistinctTypes() {
        // Act
        AttributeType email = AttributeType.getType("email");
        AttributeType emailp = AttributeType.getType("emailp");
        AttributeType csEmailp = AttributeType.getType("cs_emailp");

        // Assert — the three email codes are distinct registrations.
        assertSame(AttributeType.TYPE_EMAIL, email);
        assertSame(AttributeType.TYPE_EMAILP, emailp);
        assertSame(AttributeType.TYPE_CS_EMAILP, csEmailp);
        assertTrue("email variants must be distinct enum values",
                email != emailp && emailp != csEmailp && email != csEmailp);
    }

    /* Returns the registered code for a type by probing each known mapping. */
    private static String codeOf(AttributeType t) {
        switch (t) {
            case TYPE_BOOLEAN:     return "boolean";
            case TYPE_BINARY:      return "binary";
            case TYPE_CERTIFICATE: return "certificate";
            case TYPE_DURATION:    return "duration";
            case TYPE_GENTIME:     return "gentime";
            case TYPE_EMAIL:       return "email";
            case TYPE_EMAILP:      return "emailp";
            case TYPE_CS_EMAILP:   return "cs_emailp";
            case TYPE_ENUM:        return "enum";
            case TYPE_ID:          return "id";
            case TYPE_INTEGER:     return "integer";
            case TYPE_PORT:        return "port";
            case TYPE_PHONE:       return "phone";
            case TYPE_STRING:      return "string";
            case TYPE_ASTRING:     return "astring";
            case TYPE_OSTRING:     return "ostring";
            case TYPE_CSTRING:     return "cstring";
            case TYPE_REGEX:       return "regex";
            case TYPE_LONG:        return "long";
            default: throw new IllegalStateException("unmapped type " + t);
        }
    }
}
