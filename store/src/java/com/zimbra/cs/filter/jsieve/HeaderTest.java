/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016, 2017 Synacor, Inc.
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SievePatternException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.SieveMailException;
import org.apache.jsieve.tests.Header;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraComparatorUtils;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class HeaderTest extends Header {

    /**
     * <p>
     * From RFC 5228, Section 5.7...
     * </p>
     * <code>
     * Syntax: header [COMPARATOR] [MATCH-TYPE]
     *       &lt;header-names: string-list&gt; &lt;key-list: string-list&gt;
     *
     * @see org.apache.jsieve.tests.Header#executeBasic(MailAdapter,
     *      Arguments, SieveContext)
     */
    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments,
            SieveContext context) throws SieveException {
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
                headerNames = ((StringListArgument) argument).getList();
            }
        }
        headerNames = replaceVariables(headerNames, mail);
        if (null == headerNames) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a StringListof header names");
        }

        // The next argument MUST be a string-list of keys
        if (argumentsIter.hasNext()) {
            final Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                keys = ((StringListArgument) argument).getList();
            }
        }
        keys = replaceVariables(keys, mail);
        if (null == keys) {
            throw context.getCoordinate().syntaxException(
                    "Expecting a StringList of keys");
        }

        if (argumentsIter.hasNext()) {
            throw context.getCoordinate().syntaxException(
                    "Found unexpected arguments");
        }

        if (matchType != null
           && (COUNT_TAG.equalsIgnoreCase(matchType) || VALUE_TAG.equalsIgnoreCase(matchType))) {
            return match(mail,
                         (comparator == null ? ASCII_NUMERIC_COMPARATOR : comparator),
                         matchType, operator, headerNames, keys, context);
        } else {
            return match(mail,
                         (comparator == null ? ASCII_CASEMAP_COMPARATOR : comparator),
                         (matchType == null ? IS_TAG : matchType),
                         headerNames, keys, context);
        }
    }

    /**
     * @param keys
     * @param mail
     * @return
     */
    public static List<String> replaceVariables(List<String> keys, MailAdapter mail) {
        List<String> replacedVariables = new ArrayList<String>();
        if (!(mail instanceof ZimbraMailAdapter)) {
            return replacedVariables;
        }
        ZimbraMailAdapter zma  = (ZimbraMailAdapter) mail;
        for (String key : keys) {
            String temp = FilterUtil.replaceVariables(zma, key);
            replacedVariables.add(temp);
        }
        
        return replacedVariables;
    }

    /**
     * Compares the header field with operator sign
     */
    protected boolean match(MailAdapter mail, String comparator, String matchType,
            String operator,
            List<String> headerNames, List<String> keys, SieveContext context)
        throws SieveException {
        // Iterate over the header names looking for a match
        boolean isMatched = false;

        Iterator headerNamesIter = headerNames.iterator();
        if (COUNT_TAG.equalsIgnoreCase(matchType)) {
            // RFC 5231: 4.2. "... if more than one (header) field name is specified, the counts for
            // all specified fields are added together to obtain the number for comparison."
            List<String> headerValues = new ArrayList<String>();
            while(headerNamesIter.hasNext()) {
                headerValues.addAll(mail.getMatchingHeader((String) headerNamesIter.next()));
            }
            for (final String key: keys) {
                isMatched = ZimbraComparatorUtils.counts(comparator,
                    operator, headerValues, key, context);
                if (isMatched) {
                    break;
                }
            }
        } else {
            while (!isMatched && headerNamesIter.hasNext()) {
                isMatched = match(comparator, matchType, operator,
                    mail.getMatchingHeader((String) headerNamesIter.next()),
                    keys, context);
            }
        }
        return isMatched;
    }

    /**
     * Traverses the values set of the specific header field(s) to check the filter key
     */
    protected boolean match(String comparator, String matchType, String operator,
        List<String> headerValues, List<String> keys, SieveContext context)
            throws SieveException {
        if (headerValues.isEmpty()) {
            return false;
        }

        // Iterate over the header values looking for a match
        boolean isMatched = false;
        Iterator headerValuesIter = headerValues.iterator();
        while (!isMatched && headerValuesIter.hasNext()) {
            isMatched = match(comparator, matchType, operator,
                              (String) headerValuesIter.next(), keys, context);
        }
        return isMatched;
    }

    /**
     * Compares each header value to each key.
     */
    protected boolean match(String comparator, String matchType,
        String operator, String headerValue, List<String> keys, SieveContext context)
            throws SieveException {
        // Iterate over the keys looking for a match
        boolean isMatched = false;
        for (final String key: keys) {
            isMatched = ZimbraComparatorUtils.match(comparator, matchType, operator,
                headerValue, key, context);
            if (isMatched) {
                break;
            }
        }
        return isMatched;
    }
    
    protected boolean match(MailAdapter mail, String comparator, String matchType, List<String> headerNames, List<String> keys, 
            SieveContext context) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        
        ZimbraMailAdapter zma  = (ZimbraMailAdapter) mail;
        if (matchType.equals(MatchTypeTags.MATCHES_TAG)) {
            try {
                evaluateVarExp(zma, headerNames, SourceType.HEADER, keys);
            } catch (MessagingException e) {
                throw new SieveException("Exception occured while evaluating variable expression.", e);
            }
        }

        // Iterate over the header names looking for a match
        boolean isMatched = false;
        Iterator<String> headerNamesIter = headerNames.iterator();
        while (!isMatched && headerNamesIter.hasNext()) {
            List<String> values = zma.getMatchingHeader(headerNamesIter.next());
            if (MatchTypeTags.CONTAINS_TAG.equals(matchType) && values != null && values.isEmpty()) {
                isMatched = false;
            } else {
                isMatched = match(comparator, matchType, values, keys, context);
            }
        }
        return isMatched;
    }
    
    /**
     * Matches the wildcard ("?" and "*") in the Address, Envelope, Header or String test,
     * and stores one string for each wildcard in the variable ${n} (n=1,2,...,9).
     *
     * @param mailAdapter triggering mail object
     * @param sourceNames target header name or literal value to be matched
     * @param sourceType  where the value should be looked up, message header, envelope, or use as literal
     * @param keys matching key string
     * @throws SieveMailException
     * @throws SievePatternException
     * @throws MessagingException 
     */
    public static enum SourceType {HEADER, ENVELOPE, LITERAL};
    public static void evaluateVarExp(ZimbraMailAdapter mailAdapter, List<String> sourceNames, SourceType sourceType, List<String> keys) throws SieveMailException, SievePatternException, MessagingException {
        List<String> varValues = new ArrayList<String>();

        String firstMatchedInputSubsequence = null;

        for (Object obj : sourceNames) {
            String name = (String) obj;
            List<String> values = null;
            switch (sourceType) {
            case ENVELOPE:
                values = mailAdapter.getEnvelope(name);
                break;
            case HEADER:
                String[] headerValues = mailAdapter.getMimeMessage().getHeader(name);
                if (headerValues != null && headerValues.length > 0) {
                    values = Arrays.asList(headerValues);
                } else {
                    values = new ArrayList<String>();
                }
                List<String> decodedValues = new ArrayList<>();
                for (String value : values) {
                    try {
                        decodedValues.add(MimeUtility.decodeText(MimeUtility.unfold(value)));
                    } catch (UnsupportedEncodingException e) {
                        throw new SieveMailException("Exception occured while decoding header value", e);
                    }
                }
                values = decodedValues;
                break;
            case LITERAL:
            default:
                values = new ArrayList<String>();
                values.add(name);
                break;
            }
            for (String sourceStr : values) {
                for (Object key : keys) {
                    String keyStr = ((String) key);
                    String regex = FilterUtil.sieveToJavaRegex(keyStr);
                    Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(sourceStr);
                    int grpCount = matcher.groupCount();
                    if (matcher.find() && grpCount > 0) {
                        mailAdapter.clearMatchedValues();
                        if (firstMatchedInputSubsequence == null) {
                            // The varValues holds the substring from the source value
                            // that the corresponding wildcard expands to. Although RFC 5229 says that
                            // "Index 0 contains the matched part of the source value", it does not
                            // define the value of index 0 when the matching operation performs to
                            // the multiple header fields, or multiple values of a single header field.
                            // In such cases, ZCS will put the first matched part of the source value.
                            firstMatchedInputSubsequence = matcher.group();
                        }
                        for (int i = 1; i<=grpCount; ++i) {
                            String matchGrp =  matcher.group(i);
                            varValues.add(matchGrp);
                            ZimbraLog.filter.debug("The matched variable are: %s", matchGrp );
                        }
                    }
                }
            }
        }
        if (firstMatchedInputSubsequence != null) {
            varValues.add(0, firstMatchedInputSubsequence);
        }
        ZimbraLog.filter.debug("The matched variables are: %s", varValues );
        mailAdapter.setMatchedValues(varValues);
    }
}
