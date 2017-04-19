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
import static org.apache.jsieve.comparators.ComparatorNames.ASCII_CASEMAP_COMPARATOR;
import static org.apache.jsieve.comparators.MatchTypeTags.IS_TAG;
import static org.apache.jsieve.tests.AddressPartTags.ALL_TAG;
import static org.apache.jsieve.tests.AddressPartTags.LOCALPART_TAG;
import static org.apache.jsieve.tests.AddressPartTags.DOMAIN_TAG;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.COUNT_TAG;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.VALUE_TAG;

import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.optional.Envelope;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraComparatorUtils;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class EnvelopeTest extends Envelope {

    /**
     * <p>
     * From RFC 5228, Section 5.4...
     * </p>
     * <code>
     * Syntax: envelope [COMPARATOR] [ADDRESS-PART] [MATCH-TYPE]
     *         &lt;envelope-part: string-list&gt; &lt;key-list: string-list&gt;
     * </code>
     */
    protected boolean executeBasic(MailAdapter mail, Arguments arguments,
            SieveContext context) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }

        ZimbraComparatorUtils.TestParameters params = ZimbraComparatorUtils.parseTestArguments(mail, arguments, context);
        params.setKeys(HeaderTest.replaceVariables(params.getKeys(), mail));
        params.setHeaderNames(HeaderTest.replaceVariables(params.getHeaderNames(), mail));
        for (String headerName : params.getHeaderNames()) {
            FilterUtil.headerNameHasSpace(headerName);
        }

        if (MatchTypeTags.MATCHES_TAG.equals(params.getMatchType())) {
            ZimbraMailAdapter zma  = (ZimbraMailAdapter) mail;
            try {
                HeaderTest.evaluateVarExp(zma, params.getHeaderNames(), HeaderTest.SourceType.ENVELOPE, params.getKeys());
            } catch (MessagingException e) {
                throw new SieveException("Exception occured while evaluating variable expression.", e);
            }
        }
        
        if (COUNT_TAG.equals(params.getMatchType()) || VALUE_TAG.equals(params.getMatchType()) || IS_TAG.equals(params.getMatchType())) {
            return match(mail,
                    (params.getAddressPart() == null ? ALL_TAG : params.getAddressPart()),
                    ZimbraComparatorUtils.getComparator(params.getComparator(), params.getMatchType()),
                    params.getMatchType(),
                    params.getOperator(),
                    params.getHeaderNames(),
                    params.getKeys(), context);
        } else {
            return match(mail,
                    (params.getAddressPart() == null ? ALL_TAG : params.getAddressPart()),
                    ZimbraComparatorUtils.getComparator(params.getComparator(), params.getMatchType()),
                    (params.getMatchType() == null ? IS_TAG : params.getMatchType()),
                    params.getHeaderNames(),
                    params.getKeys(), context);
        }
    }

    /**
     * Compares the address with operator
     *
     * @param operator "gt" / "ge" / "lt" / "le" / "eq" / "ne"
     */
    private boolean match(MailAdapter mail, String addressPart, String comparator,
            String matchType, String operator, List<String> headerNames,
            List<String> keys, SieveContext context) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        // Iterate over the address fields looking for a match
        boolean isMatched = false;
        List<String> headerValues = Lists.newArrayListWithExpectedSize(2);
        for (final String headerName: headerNames) {
            if ("to".equalsIgnoreCase(headerName)) {
                // RFC 5231 4.2. ... The envelope "to" will always have only one
                // entry, which is the address of the user for whom the Sieve script
                // is running.
                String recipient = null;
                try {
                    recipient = ((ZimbraMailAdapter)mail).getMailbox().getAccount().getMail();
                } catch (ServiceException e) {
                    recipient = "";
                }
                headerValues.add(getMatchAddressPart(addressPart, recipient));
            } else if ("from".equalsIgnoreCase(headerName)) {
                List<String> values = getMatchingValues(mail, headerName);
                if (values != null) {
                    if (matchType.equalsIgnoreCase(COUNT_TAG)) {
                        // RFC 5231 Section 4.2 Match Type COUNT says:
                        // | The envelope "from" will be 0 if the MAIL FROM is empty, or 1 if MAIL
                        // | FROM is not empty.
                        // This method could be called for other match type, such as :value or :is,
                        // remove the empty element only if the match type is :count.
                        values.removeIf(s -> Strings.isNullOrEmpty(s));
                    }
                    for (String value : values) {
                        headerValues.add(getMatchAddressPart(addressPart, value));
                    }
                }
            } else {
                throw new SyntaxException("Unexpected header name as a value for <envelope-part>: '" + headerName + "'");
            }
        }

        if (COUNT_TAG.equals(matchType)) {
            for (final String key: keys) {
                isMatched = ZimbraComparatorUtils.counts(comparator,
                        operator, headerValues, ZimbraComparatorUtils.getMatchKey(addressPart, key), context);
                if (isMatched) {
                    break;
                }
            }
        } else {
            Iterator headerValuesIter = headerValues.iterator();
            while (!isMatched && headerValuesIter.hasNext()) {
                List<String> normalizedKeys = Lists.newArrayListWithExpectedSize(keys.size());
                if (DOMAIN_TAG.equalsIgnoreCase(addressPart)) {
                    for (String key : keys) {
                        normalizedKeys.add(key.toLowerCase());
                    }
                } else {
                    normalizedKeys = keys;
                }
                isMatched = match(comparator, matchType, operator, (String)headerValuesIter.next(), normalizedKeys,
                        context);
            }
        }
        
       
        return isMatched;
    }

    /*
     * Please refer to the org.apache.jsieve.tests.optional.Envelope.match(String, String, String, String, String, SieveContext) 
     * for the parsing logic.
     */
    private String getMatchAddressPart(String addressPart, String email) {
        if (StringUtil.isNullOrEmpty(email)) {
            return "";
        }

        // Extract the part of the address we are matching on
        String matchAddress = null;
        if (ALL_TAG.equalsIgnoreCase(addressPart)) {
            matchAddress = email;
        } else {
            int localStart = 0;
            int localEnd = 0;
            int domainStart = 0;
            int domainEnd = email.length();
            int splitIndex = email.indexOf('@');
            // If there is no domain part (-1), treat it as an empty String
            if (splitIndex == -1) {
                localEnd = domainEnd;
                domainStart = domainEnd;
            } else {
                localEnd = splitIndex;
                domainStart = splitIndex + 1;
            }
            matchAddress = (addressPart.equals(LOCALPART_TAG) ? email
                    .substring(localStart, localEnd) : email.substring(
                    domainStart, domainEnd));
        }

        if (addressPart.equals(DOMAIN_TAG)) {
            matchAddress = matchAddress.toLowerCase();
        }
        return matchAddress;
    }

    private boolean match(String comparator, String matchType, String operator,
            String headerValue, List<String> keys, SieveContext context)
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
}
