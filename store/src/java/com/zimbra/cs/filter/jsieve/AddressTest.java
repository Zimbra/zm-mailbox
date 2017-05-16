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
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.COUNT_TAG;
import static com.zimbra.cs.filter.jsieve.MatchTypeTags.VALUE_TAG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.Address;

import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.ZimbraComparatorUtils;
import com.zimbra.cs.filter.ZimbraMailAdapter;

public class AddressTest extends Address {

    /**
     * <p>
     * From RFC 5228, Section 5.1...
     * </p>
     * <code>
     * Syntax: address [ADDRESS-PART] [COMPARATOR] [MATCH-TYPE]
     *         &lt;header-list: string-list&gt; &lt;key-list: string-list&gt;
     * </code>
     *
     * @see org.apache.jsieve.tests.AbstractComparatorTest#executeBasic(MailAdapter,
     *      Arguments, SieveContext)
     */
    @Override
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
                HeaderTest.evaluateVarExp(zma, params.getHeaderNames(), HeaderTest.SourceType.HEADER, params.getKeys());
            } catch (MessagingException e) {
                throw new SieveException("Exception occured while evaluating variable expression.", e);
            }
        }
        if (COUNT_TAG.equals(params.getMatchType()) || VALUE_TAG.equals(params.getMatchType()) || IS_TAG.equalsIgnoreCase(params.getMatchType())) {
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
     */
    private boolean match(MailAdapter mail, String addressPart, String comparator,
            String matchType, String operator, List<String> headerNames,
            List<String> keys, SieveContext context) throws SieveException {
        // Iterate over the address fields looking for a match
        boolean isMatched = false;

        Iterator headerNamesIter = headerNames.iterator();
        List<String> headerValues = new ArrayList<String>();
        while(headerNamesIter.hasNext()) {
            final MailAdapter.Address[] addresses = mail.parseAddresses((String) headerNamesIter.next());
            final int length = addresses.length;
            int i = 0;
            while (i < length) {
                MailAdapter.Address address = addresses[i++];
                final String localPart = address.getLocalPart();
                final String domain = address.getDomain();
                headerValues.add(getMatchAddress(addressPart, localPart, domain));
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
                isMatched = match(comparator, matchType,
                    operator, (String)headerValuesIter.next(), keys, context);
            }
        }
        return isMatched;
    }

    /**
     * Compares the value of the specified address field with the operator
     */
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

    private String getMatchAddress(String addressPart, String localPart, String domain) {
        // Extract the part of the address we are matching on
        final String matchAddress;
        if (ALL_TAG.equals(addressPart))
            matchAddress = localPart + "@" + domain;
        else {
            matchAddress = (LOCALPART_TAG.equals(addressPart) ? localPart
                    : domain.toLowerCase());
        }
        return matchAddress;
    }
}
