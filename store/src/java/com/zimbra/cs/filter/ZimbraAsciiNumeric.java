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

import org.apache.commons.lang.StringUtils;
import org.apache.jsieve.comparators.AsciiNumeric;
import org.apache.jsieve.exception.FeatureException;

import com.zimbra.common.util.ZimbraLog;

public class ZimbraAsciiNumeric extends AsciiNumeric implements ZimbraComparator {

    public boolean values(String operator, String lhs, String rhs) throws FeatureException {
        if (getNumericType(lhs) == NumericType.NEGATIVE_NUMBER || getNumericType(rhs) == NumericType.NEGATIVE_NUMBER) {
            throw new FeatureException(":values found negative value: lhs[" + lhs + "] rhs[" + rhs + "]");
        }

        BigInteger left = null, right = null;
        try {
            left = toInteger(lhs);
            right = toInteger(rhs);
        } catch (NumberFormatException e) {
            return valuesInfinity(operator, lhs, rhs);
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

    /**
     * Compare with positive infinity.
     * <ul>
     * <li> The positive infinity greater than all finite values.
     * <li> Each positive infinity compares equal only to itself.
     * <li> Each positive infinity compares unequal to all other values.
     * </ul>
     * @param operator Operator name (GT_OP, GE_OP, LT_OP, LE_OP, EQ_OP, NE_OP)
     * @param lhs Left-hand side value
     * @param rhs Right-hand side value
     * @return true if the target value matches the condition
     */
    private boolean valuesInfinity(String operator, String lhs, String rhs) {
        boolean isPosInfLHS = isPositiveInfinity(lhs);
        boolean isPosInfRHS = isPositiveInfinity(rhs);

        if (isPosInfLHS && isPosInfRHS) {
            switch (operator) {
            case GT_OP: return false;
            case GE_OP: return true;
            case LT_OP: return false;
            case LE_OP: return true;
            case EQ_OP: return true;
            case NE_OP: return false;
            }
        }
        if (isPosInfLHS && !isPosInfRHS) {
            switch (operator) {
            case GT_OP: return true;
            case GE_OP: return true;
            case LT_OP: return false;
            case LE_OP: return false;
            case EQ_OP: return false;
            case NE_OP: return true;
            }
        }
        if (!isPosInfLHS && isPosInfRHS) {
            switch (operator) {
            case GT_OP: return false;
            case GE_OP: return false;
            case LT_OP: return true;
            case LE_OP: return true;
            case EQ_OP: return false;
            case NE_OP: return true;
            }
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
        if (getNumericType(value) == NumericType.NEGATIVE_NUMBER) {
            throw new FeatureException(":counts found negative value: [" + value + "]");
        }
        BigInteger filterValue = null;
        BigInteger count = BigInteger.valueOf(targetList.size());
        try {
            filterValue = toInteger(value);
        } catch (NumberFormatException e) {
            return countInfinity(operator, count, value);
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
     * Compare the number of count with positive infinity
     * <ul>
     * <li> The positive infinity greater than all finite values.
     * <li> The left-hand side value is always finite value.
     * </ul>
     * @param operator Operator name (GT_OP, GE_OP, LT_OP, LE_OP, EQ_OP, NE_OP)
     * @param count Left-hand side value
     * @param value Right-hand side value
     * @return true if the target value matches the condition
     */
    private boolean countInfinity(String operator, BigInteger count, String value) {
        if (isPositiveInfinity(value)) {
            switch (operator) {
            case GT_OP: return false;
            case GE_OP: return false;
            case LT_OP: return true;
            case LE_OP: return true;
            case EQ_OP: return false;
            case NE_OP: return true;
            }
        }
        return false;
    }

    /** 
     * Match type :is for i;ascii-numeric.
     */
    public boolean equals2(String lhs, String rhs) throws FeatureException{
        NumericType lhsType = getNumericType(lhs);
        NumericType rhsType = getNumericType(rhs);

        if (lhsType == NumericType.NEGATIVE_NUMBER || rhsType == NumericType.NEGATIVE_NUMBER) {
            throw new FeatureException(":is found negative value: lhs[" + lhs + "] rhs[" + rhs + "]");
        }
        if (lhsType == NumericType.POSITIVEINFINITY && rhsType == NumericType.POSITIVEINFINITY) {
            return true;
        }
        if (lhsType == NumericType.POSITIVEINFINITY || rhsType == NumericType.POSITIVEINFINITY) {
            return false;
        }

        BigInteger intLhs = toInteger(lhs);
        BigInteger intRhs = toInteger(rhs);
        return intLhs.equals(intRhs);
    }

    public enum NumericType {POSITIVE_NUMBER, NEGATIVE_NUMBER, POSITIVEINFINITY};
    /** 
     * Check the string type
     * <ul>
     * <li> If the value has a '-' (0x2d) followed by one or more digits, then it is negative number.
     * <li> If the value is empty or does not starts with number, then it is positive infinity (RFC 4790 Section 9.1.1)
     * <li> If the value starts with digit, then it is positive number
     * </ul>
     */
    public NumericType getNumericType(String value) {
        if (StringUtils.isEmpty(value)) {
            return NumericType.POSITIVEINFINITY;
        }
        if (value.length() > 1 && value.startsWith("-") && isDigit(value.charAt(1))) {
            return NumericType.NEGATIVE_NUMBER;
        }
        if (isPositiveInfinity(value)) {
            return NumericType.POSITIVEINFINITY;
        }
        return NumericType.POSITIVE_NUMBER;
    }

    /**
     * Unsupported, see <a href='http://tools.ietf.org/html/rfc4790#section-9.1.1'>RFC4790</a>.
     * @see org.apache.jsieve.comparators.Contains#contains(String, String)
     */
    public boolean contains(String container, String content) throws FeatureException {
        // TODO: Consider using finer grained exception
        throw new FeatureException("Substring contains unsupported by ascii-numeric");
    }

    /**
     * Unsupported operation.
     * <a href='http://tools.ietf.org/html/rfc5228#section-2.7.1'>RFC5228</a> limits
     * support to comparators that support <code>:contains</code>.
     * <a href='http://tools.ietf.org/html/rfc4790#section-9.1.1'>RFC4790</a> states
     * that substring matches are not supported.
     * @see org.apache.jsieve.comparators.Matches#matches(String, String)
     */
    public boolean matches(String string, String glob)
            throws FeatureException {
        // TODO: Consider using finer grained exception
        throw new FeatureException("Substring match unsupported by ascii-numeric");
    }
}
