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

import java.util.List;

import org.apache.jsieve.comparators.Octet;
import org.apache.jsieve.exception.FeatureException;
import org.apache.jsieve.exception.SievePatternException;

import com.zimbra.common.soap.HeaderConstants;

/**
 * Class ZimbraOctet enhances the jsieve's Octet class to
 * support the :values match type for the i;octet
 */
public class ZimbraOctet extends Octet implements ZimbraComparator {

    public boolean values(String operator, String left, String right)
        throws FeatureException {
        switch (operator) {
        case HeaderConstants.GT_OP:
            return (left.compareTo(right) > 0);
        case HeaderConstants.GE_OP:
            return (left.compareTo(right) >= 0);
        case HeaderConstants.LT_OP:
            return (left.compareTo(right) < 0);
        case HeaderConstants.LE_OP:
            return (left.compareTo(right) <= 0);
        case HeaderConstants.EQ_OP:
            return (left.compareTo(right) == 0);
        case HeaderConstants.NE_OP:
            return (left.compareTo(right) != 0);
        }
        return false;
    }

    @Override
    public boolean counts(String operator, List<String> lhs, String rhs)
            throws FeatureException {
        throw new FeatureException("Substring counts unsupported by octet");
    }

    /**
     * @see org.apache.jsieve.comparators.Octet.matches#matches(String, String)
     */
    public boolean matches(String string, String glob)
            throws SievePatternException {
        return ZimbraComparatorUtils.matches(string, glob);
    }

    /**
     * @see org.apache.jsieve.comparators.Octet#equals(String, String)
     */
    @Override
    public boolean equals2(String string1, String string2) throws FeatureException {
        return equals(string1, string2);
    }
}
