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
package com.zimbra.cs.filter;

import java.math.BigInteger;
import java.util.List;

import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.EQ_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.NE_OP;

import org.apache.jsieve.comparators.AsciiNumeric;
import org.apache.jsieve.exception.FeatureException;

import com.zimbra.common.util.ZimbraLog;

public class ZimbraAsciiNumeric extends AsciiNumeric implements ZimbraComparator {

    public boolean values(String operator, String lhs, String rhs) {
        BigInteger left = null, right = null;
        try {
            left = toInteger(lhs);
            right = toInteger(rhs);
        } catch (NumberFormatException e) {
            return false;
        }

        switch (operator) {
        case GT_OP:
            return (left.compareTo(right) > 0);
        case GE_OP:
            return (left.compareTo(right) >= 0);
        case LT_OP:
            return (left.compareTo(right) < 0);
        case LE_OP:
            return (left.compareTo(right) <= 0);
        case EQ_OP:
            return (left.compareTo(right) == 0);
        case NE_OP:
            return (left.compareTo(right) != 0);
        }
        return false;
    }

    private BigInteger toInteger(final String value) {
        int i;
        for (i=0;i<value.length();i++) {
            final char next = value.charAt(i);
            if (!isDigit(next)) {
                break;
            }
        }
        final BigInteger result = new BigInteger(value.substring(0,i));
        return result;
    }

    /**
     * This is a modified version of org.apache.jsieve.comparators.AsciiNumeric.isPositiveInfinity(String)
     * It accepts an empty string as well.
     * @param value not null
     * @return true when the value should represent positive infinity,
     * false otherwise
     */
    private boolean isPositiveInfinity(final String value) {
        if (null != value && value.isEmpty()) {
            return true;
        }
        final char initialCharacter = value.charAt(0);
        final boolean result = !isDigit(initialCharacter);
        return result;
    }

    /**
     * Is the given character an ASCII digit?
     * @param character character to be tested
     * @return true when the given character is an ASCII digit,
     * false otherwise
     */
    private boolean isDigit(final char character) {
        return character>=0x30 && character<=0x39;
    }

    @Override
    public boolean counts(String operator, List<String> targetList, String value)
            throws FeatureException {
        BigInteger count = null, filterValue = null;
        try {
            count = BigInteger.valueOf(targetList.size());
            filterValue = toInteger(value);
        } catch (NumberFormatException e) {
            return false;
        }

        switch (operator) {
        case GT_OP:
            return (count.compareTo(filterValue) > 0);
        case GE_OP:
            return (count.compareTo(filterValue) >= 0);
        case LT_OP:
            return (count.compareTo(filterValue) < 0);
        case LE_OP:
            return (count.compareTo(filterValue) <= 0);
        case EQ_OP:
            return (count.compareTo(filterValue) == 0);
        case NE_OP:
            return (count.compareTo(filterValue) != 0);
        }
        return false;
    }

    /**
     * @see org.apache.jsieve.comparators.AsciiNumeric#equals(String, String)
     */
    @Override
    public boolean equals(String string1, String string2) {
        final boolean result;
        if (isPositiveInfinity(string1)) {
            if (isPositiveInfinity(string2)) {
                result = true;
            } else {
                result = false;
            }
        } else {
            if (isPositiveInfinity(string2)) {
                result = false;
            } else {
                final BigInteger integer1 = toInteger(string1);
                final BigInteger integer2 = toInteger(string2);
                result = integer1.equals(integer2);
            }
        }
        return result;
    }
}
