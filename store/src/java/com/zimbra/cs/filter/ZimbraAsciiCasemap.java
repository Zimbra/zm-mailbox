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

import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.EQ_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.NE_OP;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.jsieve.comparators.AsciiCasemap;
import org.apache.jsieve.comparators.ComparatorUtils;
import org.apache.jsieve.exception.FeatureException;
import org.apache.jsieve.exception.SievePatternException;

/**
 * Class ZimbraAsciiCasecomp enhances the jsieve's AsciiCasemap class to
 * support the RFC 5231: Relational Extension.
 */
public class ZimbraAsciiCasemap extends AsciiCasemap implements ZimbraComparator {

    public boolean values(String operator, String left, String right)
            throws FeatureException {
        switch (operator) {
        case GT_OP:
            return (left.compareToIgnoreCase(right) > 0);
        case GE_OP:
            return (left.compareToIgnoreCase(right) >= 0);
        case LT_OP:
            return (left.compareToIgnoreCase(right) < 0);
        case LE_OP:
            return (left.compareToIgnoreCase(right) <= 0);
        case EQ_OP:
            return (left.compareToIgnoreCase(right) == 0);
        case NE_OP:
            return (left.compareToIgnoreCase(right) != 0);
        }
        return false;
    }

    @Override
    public boolean counts(String operator, List<String> lhs, String rhs)
            throws FeatureException {
        throw new FeatureException("Substring counts unsupported by ascii-casemap");
    }

    /**
     * @see org.apache.jsieve.comparators.AsciiCasemap.matches#matches(String, String)
     */
    public boolean matches(String string, String glob)
            throws SievePatternException {
        try {
            String regex = FilterUtil.sieveToJavaRegex(glob.toUpperCase());
            final Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(string.toUpperCase());
            return matcher.matches();
        } catch (PatternSyntaxException e) {
            throw new SievePatternException(e.getMessage());
        }
    }

    /**
     * @see org.apache.jsieve.comparators.AsciiCasemap#equals(String, String)
     */
    @Override
    public boolean equals2(String string1, String string2) throws FeatureException {
        return equals(string1, string2);
    }
}
