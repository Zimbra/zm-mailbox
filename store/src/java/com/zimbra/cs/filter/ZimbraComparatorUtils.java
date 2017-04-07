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

import static org.apache.jsieve.comparators.MatchTypeTags.CONTAINS_TAG;
import static org.apache.jsieve.comparators.MatchTypeTags.IS_TAG;
import static org.apache.jsieve.comparators.MatchTypeTags.MATCHES_TAG;
import static org.apache.jsieve.tests.AddressPartTags.ALL_TAG;
import static org.apache.jsieve.tests.AddressPartTags.DOMAIN_TAG;
import static org.apache.jsieve.tests.AddressPartTags.LOCALPART_TAG;
import static org.apache.jsieve.tests.ComparatorTags.COMPARATOR_TAG;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.EQ_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.NE_OP;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.COUNT_TAG;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.VALUE_TAG;

import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.comparators.ComparatorUtils;
import org.apache.jsieve.comparators.Equals;
import org.apache.jsieve.exception.FeatureException;
import org.apache.jsieve.exception.LookupException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SievePatternException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;

import com.zimbra.cs.filter.jsieve.Counts;
import com.zimbra.cs.filter.jsieve.Values;

public class ZimbraComparatorUtils {

    /**
     * Parses the address test string in the 'if' part of the filter rule
     *
     * @see org.apache.jsieve.tests.AbstractComparatorTest#executeBasic(MailAdapter mail, Arguments arguments, SieveContext context)
     *
     * @param mail
     * @param arguments
     * @param context
     * @return TestParameters
     */
    public static TestParameters parseTestArguments(MailAdapter mail,
            Arguments arguments, SieveContext context) throws SieveException {
        String addressPart = null;
        String comparator = null;
        String matchType = null;
        String operator = null;
        List<String> headerNames = null;
        List<String> keys = null;
        boolean nextArgumentIsRelationalSign = false;

        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();
        boolean stop = false;

        // Tag processing
        while (!stop && argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof TagArgument) {
                String tag = ((TagArgument) argument).getTag();

                // [ADDRESS-PART]?
                if (addressPart == null
                        && (LOCALPART_TAG.equalsIgnoreCase(tag)
                            || DOMAIN_TAG.equalsIgnoreCase(tag)
                            || ALL_TAG.equalsIgnoreCase(tag))) {
                    addressPart = tag;
                }
                // [COMPARATOR]?
                else if (comparator == null && COMPARATOR_TAG.equalsIgnoreCase(tag)) {
                    // The next argument must be a string list
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            List stringList = ((StringListArgument) argument).getList();
                            if (stringList.size() != 1) {
                                throw new SyntaxException("Expecting exactly one String");
                            }
                            comparator = (String) stringList.get(0);
                        } else {
                            throw new SyntaxException("Expecting a StringList");
                        }
                    }
                }
                // [MATCH-TYPE]?
                else if (matchType == null
                        && (IS_TAG.equalsIgnoreCase(tag)
                            || CONTAINS_TAG.equalsIgnoreCase(tag)
                            || MATCHES_TAG.equalsIgnoreCase(tag)
                            || COUNT_TAG.equalsIgnoreCase(tag)
                            || VALUE_TAG.equalsIgnoreCase(tag))) {
                    matchType = tag;
                    nextArgumentIsRelationalSign = true;
                } else {
                    throw context.getCoordinate().syntaxException("Found unexpected TagArgument");
                }
            } else {
                if (nextArgumentIsRelationalSign && argument instanceof StringListArgument) {
                    String symbol = ((StringListArgument) argument).getList().get(0);
                    if (matchType != null
                            && (GT_OP.equalsIgnoreCase(symbol)
                                || GE_OP.equalsIgnoreCase(symbol)
                                || LT_OP.equalsIgnoreCase(symbol)
                                || LE_OP.equalsIgnoreCase(symbol)
                                || EQ_OP.equalsIgnoreCase(symbol)
                                || NE_OP.equalsIgnoreCase(symbol))) {
                        operator = symbol;
                    } else {
                        argumentsIter.previous();
                        stop = true;
                    }
                    nextArgumentIsRelationalSign = false;
                } else {
                    // Stop when a non-tag argument is encountered
                    argumentsIter.previous();
                    stop = true;
                }
            }
        }

        // The next argument MUST be a string-list of header names
        if (argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                headerNames = ((StringListArgument) argument).getList();
            }
        }
        if (headerNames == null) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a StringList of header names");
        }
        for (String headerName : headerNames) {
            if (headerName != null) {
                FilterUtil.headerNameHasSpace(headerName);
            }
        }

        // The next argument MUST be a string-list of keys
        if (argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                keys = ((StringListArgument) argument).getList();
            }
        }
        if (keys == null) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a StringList of keys");
        }

        if (argumentsIter.hasNext()) {
            throw context.getCoordinate().syntaxException(
                    "Found unexpected arguments");
        }

        TestParameters result = new TestParameters(mail, addressPart,
                comparator, matchType, operator,
                headerNames, keys);
        return result;
    }

    /**
     * Calles the appropriate comparison action based on the matchType value.
     *
     * @param comparatorName comparator name (ASCII_CASEMAP_COMPARATOR, ASCII_NUMERIC_COMPARATOR, OCTET_COMPARATOR)
     * @param matchType match type name (IS_TAG, CONTAINS_TAG, MATCHES_TAG, VALUE_TAG, COUNT_TAG)
     * @param operator operator name (GT_OP, GE_OP, LT_OP, LE_OP, EQ_OP, NE_OP)
     * @param matchTarget value contained in the target message
     * @param matchArgument condition value in the filter rule
     * @param context context
     * @return true if the target value matches the condition
     * @throws SieveException
     */
    public static boolean match(String comparatorName, String matchType, String operator,
            String matchTarget, String matchArgument, SieveContext context)
            throws SieveException {
        boolean isMatched = false;
        if (IS_TAG.equals(matchType)) {
            isMatched = ComparatorUtils.is(comparatorName, matchTarget, matchArgument, context);
        } else if (CONTAINS_TAG.equals(matchType)) {
            isMatched = ComparatorUtils.contains(comparatorName, matchTarget, matchArgument,
                    context);
        } else if (MATCHES_TAG.equals(matchType)) {
            isMatched = ComparatorUtils.matches(comparatorName, matchTarget, matchArgument,
                    context);
        } else if (VALUE_TAG.equals(matchType)) {
            isMatched = values(comparatorName, operator, matchTarget, matchArgument,
                    context);
        }
        return isMatched;
    }

    /**
     * Calles the comparator specific count() method.
     *
     * @param comparatorName Comparator name
     * @param operator "gt" / "ge" / "lt" / "le" / "eq" / "ne"
     * @param matchTarget digits (list of the values of the target fields in the message)
     * @param matchArgument digits (value specified in the filter rule)
     * @param context context
     * @return boolean result of "[# of matchTarget] [operator] [matchArgument]"
     * @throws LookupException
     * @throws FeatureException
     */
    public static boolean counts(String comparatorName,
            String operator, List<String> matchTarget, String matchArgument,
            SieveContext context) throws LookupException, FeatureException {
        Counts comparatorObj = (Counts) context.getComparatorManager().getComparator(comparatorName);
        return comparatorObj.counts(operator, matchTarget, matchArgument);
    }

    /**
     * Calls the comparator specific value() method.
     *
     * @param comparatorName Comparator name
     * @param operator "gt" / "ge" / "lt" / "le" / "eq" / "ne"
     * @param lhs left hand side value
     * @param rhs right hand side value
     * @param context context
     * @return boolean result of "[lhs] [operator] [rhs]"
     * @throws LookupException
     * @throws FeatureException
     */
    public static boolean values(String comparatorName, String operator,
            String lhs, String rhs, SieveContext context) throws LookupException, FeatureException {
        Values comparatorObj = (Values) context.getComparatorManager().getComparator(comparatorName);
        return comparatorObj.values(operator, lhs, rhs);
    }

    /**
     * Makes the domain name lowercase
     * @param addressPart ADDRESS-PART (":localpart" / ":domain" / ":all")
     * @param key key string in the key-list
     */
    public static String getMatchKey(String addressPart, String key) {
        // domain matches MUST ignore case, others should not
        String matchKey = null;
        if (DOMAIN_TAG.equals(addressPart)) {
            matchKey = key.toLowerCase();
        } else {
            matchKey = key;
        }
        return matchKey;
    }

    public static class TestParameters {
        public TestParameters(MailAdapter mail, String addressPart,
                String comparator, String matchType, String operator,
                List<String> headerNames, List<String> keys) {
            this.mail = mail;
            this.addressPart = normalize(addressPart);
            this.comparator = normalize(comparator);
            this.matchType = normalize(matchType);
            this.operator = normalize(operator);
            this.headerNames = headerNames;
            this.keys = keys;
        }
        private MailAdapter mail = null;
        private String addressPart = null;
        private String comparator = null;
        private String matchType = null;
        private String operator = null;
        private List<String> headerNames = null;
        private List<String> keys = null;

        public MailAdapter getMail() { return mail;}
        public String getAddressPart() { return addressPart;}
        public String getComparator() { return comparator; }
        public String getMatchType() { return matchType;}
        public String getOperator() { return operator;}
        public List<String> getHeaderNames() { return headerNames;}
        public List<String> getKeys() { return keys;}
        public void setMail(MailAdapter mail) { this.mail = mail;}
        public void setAddressPart(String addressPart) { this.addressPart = normalize(addressPart);}
        public void setComparator(String comparator) { this.comparator = normalize(comparator);}
        public void setMatchType(String matchType) { this.matchType = normalize(matchType);}
        public void setOperator(String operator) { this.operator = normalize(operator);}
        public void setHeaderNames(List<String> headerNames) { this.headerNames = headerNames;}
        public void setKeys(List<String> keys) { this.keys = keys;}

        private static String normalize(String operator) {
            return operator == null ? null : operator.toLowerCase();
        }
    }

    /**
     * @see org.apache.jsieve.comparators.ComparatorUtils#matches(String, String)
     */
    static public boolean matches(String string, String glob)
            throws SievePatternException {
        try {
            String regex = FilterUtil.sieveToJavaRegex(glob);
            final Matcher matcher = Pattern.compile(regex).matcher(string);
            return matcher.matches();
        } catch (PatternSyntaxException e) {
            throw new SievePatternException(e.getMessage());
        }
    }
}
