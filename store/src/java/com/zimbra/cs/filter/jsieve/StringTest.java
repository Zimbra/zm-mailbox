/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016, 2017 Synacor, Inc.
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
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.EQ_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.GT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LE_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.LT_OP;
import static com.zimbra.cs.filter.jsieve.MatchRelationalOperators.NE_OP;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.COUNT_TAG;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.VALUE_TAG;
import static org.apache.jsieve.comparators.ComparatorNames.ASCII_CASEMAP_COMPARATOR;
import static org.apache.jsieve.comparators.MatchTypeTags.CONTAINS_TAG;
import static org.apache.jsieve.comparators.MatchTypeTags.IS_TAG;
import static org.apache.jsieve.comparators.MatchTypeTags.MATCHES_TAG;
import static org.apache.jsieve.tests.ComparatorTags.COMPARATOR_TAG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.Header;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraComparatorUtils;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * @author zimbra
 *
 */
public class StringTest extends Header {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jsieve.tests.AbstractTest#executeBasic(org.apache.jsieve.mail.
     * MailAdapter, org.apache.jsieve.Arguments, org.apache.jsieve.SieveContext)
     */
    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }

        ZimbraMailAdapter mailAdapter = (ZimbraMailAdapter) mail;
        if (!SetVariable.isVariablesExtAvailable(mailAdapter)) {
            return false;
        }

        String matchType = null;
        String comparator = null;
        String operator = null;
        List<String> sourceValues = null;
        List<String> keyValues = null;
        boolean nextArgumentIsRelationalSign = false;

        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();
        boolean stop = false;

        // RFC 5229 Section 5. Test String
        // Usage:  string [MATCH-TYPE] [COMPARATOR]
        //         <source: string-list> <key-list: string-list>
        while (!stop && argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof TagArgument) {
                final String tag = ((TagArgument) argument).getTag();

                // [COMPARATOR]?
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
                            || COUNT_TAG.equalsIgnoreCase(tag)
                            || VALUE_TAG.equalsIgnoreCase(tag))) {
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
            final Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                List<String> sourceStringList = ((StringListArgument) argument).getList();
                if (null == sourceStringList || 0 == sourceStringList.size()) {
                    throw context.getCoordinate().syntaxException(
                            "Expecting a StringListof header names");
                }
                sourceValues = new ArrayList<String>();
                for (String source : sourceStringList) {
                    sourceValues.add(FilterUtil.replaceVariables(mailAdapter, source));
                }
            }
        }

        // The next argument MUST be a string-list of keys
        if (argumentsIter.hasNext()) {
            final Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                List<String> keyStringList = ((StringListArgument) argument).getList();
                if (null == keyStringList || 0 == keyStringList.size()) {
                    throw context.getCoordinate().syntaxException(
                            "Expecting a StringList of keys");
                }
                keyValues = new ArrayList<String>();
                for (String key : keyStringList) {
                    keyValues.add(FilterUtil.replaceVariables(mailAdapter, key));
                }
            }
        }

        if (argumentsIter.hasNext()) {
            throw context.getCoordinate().syntaxException(
                    "Found unexpected arguments");
        }

        if (null == matchType) {
            matchType = IS_TAG;
        }
        if (null == comparator) {
            if (matchType.equalsIgnoreCase(VALUE_TAG) || matchType.equalsIgnoreCase(COUNT_TAG)) {
                comparator = ASCII_NUMERIC_COMPARATOR;
            } else {
                comparator = ASCII_CASEMAP_COMPARATOR;
            }
        }

        boolean result = match(mail, comparator,
                matchType, operator, sourceValues, keyValues, context);

        if (result) {
            if (matchType.equals(MatchTypeTags.MATCHES_TAG)) {
                try {
                    HeaderTest.evaluateVarExp(mailAdapter, sourceValues, HeaderTest.SourceType.LITERAL, keyValues);
                } catch (MessagingException e) {
                    throw new SieveException("Exception occured while evaluating variable expression.", e);
                }
            }
        }
        return result;
    }

    private boolean match(MailAdapter mail, String comparator, String matchType, String operator,
            List<String> sourceValues, List<String> keyValues, SieveContext context)
        throws SieveException {
        // Iterate over the header names looking for a match
        boolean isMatched = false;

        Iterator<String> keyIter = null;
        if (COUNT_TAG.equalsIgnoreCase(matchType)) {
            // RFC 5229 Section 5. Test String
            //   The "relational" extension [RELATIONAL] adds a match type called
            //   ":count".  The count of a single string is 0 if it is the empty
            //   string, or 1 otherwise.  The count of a string list is the sum of the
            //   counts of the member strings.
            sourceValues.removeAll(Arrays.asList("", null));
            keyIter = keyValues.iterator();
            while (!isMatched && keyIter.hasNext()) {
                isMatched = ZimbraComparatorUtils.counts(comparator,
                    operator, sourceValues, keyIter.next(), context);
            }
        } else {
            Iterator<String> sourceIter = sourceValues.iterator();
            while (!isMatched && sourceIter.hasNext()) {
                String source = sourceIter.next();
                keyIter = keyValues.iterator();
                while(!isMatched && keyIter.hasNext()) {
                    isMatched = ZimbraComparatorUtils.match(comparator, matchType, operator,
                            source, keyIter.next(), context);
                }
            }
        }
        return isMatched;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
        if (arguments.getArgumentList().size() < 3) {
            throw new SyntaxException("At least 3 argument are needed. Found " + arguments);
        }
        for (Argument a : arguments.getArgumentList()) {
            System.out.println(a);
            ZimbraLog.filter.debug(a);
        }
    }
}
