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
package com.zimbra.cs.filter.jsieve;

import static com.zimbra.cs.filter.jsieve.ComparatorName.ASCII_NUMERIC_COMPARATOR;
import static org.apache.jsieve.comparators.ComparatorNames.ASCII_CASEMAP_COMPARATOR;
import static org.apache.jsieve.comparators.MatchTypeTags.CONTAINS_TAG;
import static org.apache.jsieve.comparators.MatchTypeTags.IS_TAG;
import static org.apache.jsieve.comparators.MatchTypeTags.MATCHES_TAG;
import static org.apache.jsieve.tests.ComparatorTags.COMPARATOR_TAG;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.cs.filter.ZimbraComparatorUtils;

public class NotifyMethodCapabilityTest extends AbstractTest {

    private static final String CAPABILITY_ONLINE = "online";
    private static final String CAPABILITY_YES = "yes";
    private static final String CAPABILITY_NO = "no";
    private static final String CAPABILITY_MAYBE = "maybe";
    private static final String CAPABILITY_PROTOCOL = "mailto";

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context)
            throws SieveException {
        String comparator = null;
        String matchType = null;
        String uri = null;
        String capability = null;
        List<String> keys = null;
        String operator = null;
        boolean nextArgumentIsRelationalSign = false;

        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();
        boolean stop = false;

        /*
         * Test notify_method_capability
         * Usage:  notify_method_capability [COMPARATOR] [MATCH-TYPE]
         *         <notification-uri: string>
         *         <notification-capability: string>
         *         <key-list: string-list>
         */
        while (!stop && argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof TagArgument) {
                final String tag = ((TagArgument) argument).getTag();

                if (comparator == null && COMPARATOR_TAG.equalsIgnoreCase(tag)) {
                    // The next argument must be a stringlist
                    if (argumentsIter.hasNext()) {
                        argument = argumentsIter.next();
                        if (argument instanceof StringListArgument) {
                            List<String> stringList = ((StringListArgument) argument)
                                    .getList();
                            if (stringList.size() != 1) {
                                throw context.getCoordinate().syntaxException(
                                        "Expecting exactly one String");
                            }
                            comparator = stringList.get(0);
                        } else {
                            throw context.getCoordinate().syntaxException(
                                    "Expecting a StringList");
                        }
                    }
                }
                // [MATCH-TYPE]?
                else if (matchType == null
                        && (IS_TAG.equalsIgnoreCase(tag)
                            || CONTAINS_TAG.equalsIgnoreCase(tag)
                            || MATCHES_TAG.equalsIgnoreCase(tag)
                            || HeaderConstants.COUNT.equalsIgnoreCase(tag))) {
                    matchType = tag;
                    nextArgumentIsRelationalSign = true;
                } else {
                    throw context.getCoordinate().syntaxException(
                            "Found unexpected TagArgument: \"" + tag + "\"");
                }
            } else {
                if (nextArgumentIsRelationalSign && argument instanceof StringListArgument) {
                    String symbol = ((StringListArgument) argument).getList().get(0);
                    if (matchType != null
                            && (HeaderConstants.GT_OP.equalsIgnoreCase(symbol)
                                || HeaderConstants.GE_OP.equalsIgnoreCase(symbol)
                                || HeaderConstants.LT_OP.equalsIgnoreCase(symbol)
                                || HeaderConstants.LE_OP.equalsIgnoreCase(symbol)
                                || HeaderConstants.EQ_OP.equalsIgnoreCase(symbol)
                                || HeaderConstants.NE_OP.equalsIgnoreCase(symbol))) {
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

        // The next argument MUST be a string of notification-uri
        if (argumentsIter.hasNext()) {
            final Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                uri = ((StringListArgument) argument).getList().get(0);
            }
        }
        if (null == uri) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a String of uri");
        }

        // The next argument MUST be a string of notification-capability
        if (argumentsIter.hasNext()) {
            final Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                capability = ((StringListArgument) argument).getList().get(0);
            }
        }
        if (null == capability) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a String of capability");
        }

        // The next argument MUST be a string-list of keys
        if (argumentsIter.hasNext()) {
            final Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                keys = ((StringListArgument) argument).getList();
            }
        }

        if (null == comparator) {
            if (HeaderConstants.COUNT.equalsIgnoreCase(matchType)) {
                comparator = ASCII_NUMERIC_COMPARATOR;
            } else {
                comparator = ASCII_CASEMAP_COMPARATOR;
            }
        }
        if (null == matchType) {
            matchType = IS_TAG;
        }
        if (null == keys || null == matchType) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a StringList of keys");
        } else if (!HeaderConstants.COUNT.equalsIgnoreCase(matchType)) {
            for (String key : keys) {
                if (!CAPABILITY_YES.equalsIgnoreCase(key) &&
                    !CAPABILITY_NO.equalsIgnoreCase(key) &&
                    !CAPABILITY_MAYBE.equalsIgnoreCase(key)) {
                    throw context.getCoordinate().syntaxException(
                            "Invalid key value: [" + key + "]");
                }
            }
        }
        if (HeaderConstants.COUNT.equalsIgnoreCase(matchType) && null == operator) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a String of operator");
        }

        if (argumentsIter.hasNext()) {
            throw context.getCoordinate().syntaxException(
                    "Found unexpected arguments");
        }
        return test(mail, comparator, matchType, operator, uri, capability, keys, context);
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) {
        // override validation -- it's already done in executeBasic above
    }

    private boolean test(MailAdapter mail, String comparator, String matchType, String operator,
            String uri, String capability, List<String> keys, SieveContext context) throws SieveException {
        if (null == uri || null == capability) {
            return false;
        }
        try {
            URL url = new URL(uri);
            String protocol = url.getProtocol();
            if (!CAPABILITY_PROTOCOL.equalsIgnoreCase(protocol)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        if (HeaderConstants.COUNT.equalsIgnoreCase(matchType)) {
            return testCount(mail, keys, comparator, operator, context);
        }
        // There is no way to detect the online/offline status of the recipient.
        // The test always returns "maybe" for the "mailto" notification method
        // (See RFC 5435 Section 5. and RFC 5436 Section 2.2. for more details).
        for (String key : keys) {
            if (CAPABILITY_ONLINE.equalsIgnoreCase(capability) &&
                CAPABILITY_MAYBE.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean testCount(MailAdapter mail, List<String> keys, String comparator, String operator, SieveContext context) throws SieveException {
        List<String> values = Arrays.asList(CAPABILITY_MAYBE);
        boolean isMatched = false;
        for (String key : keys) {
            isMatched = ZimbraComparatorUtils.counts(mail, comparator,
                operator, values, key, context);
            if (isMatched) {
                break;
            }
        }
        return isMatched;
    }

}
