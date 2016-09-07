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

import com.zimbra.cs.filter.jsieve.Values;

/**
 * Class ZimbraOctet enhances the jsieve's Octet class not to support the values() and counts() methods for the i;octet.
 */
public class ZimbraOctet extends Octet implements ZimbraComparator {

    public boolean values(String operator, String lhs, String rhs)
        throws FeatureException {
        throw new FeatureException("Substring compare value unsupported by octet");
    }

    @Override
    public boolean counts(String operator, List<String> lhs, String rhs)
            throws FeatureException {
        throw new FeatureException("Substring counts unsupported by octet");
    }
}
