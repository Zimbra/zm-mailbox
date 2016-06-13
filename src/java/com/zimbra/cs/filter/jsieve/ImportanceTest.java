/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.filter.Sieve;
import com.zimbra.soap.mail.type.FilterTest;
import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.comparators.MatchTypeTags;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.Header;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Checks values of "Importance" and "X-Priority" headers.
 */
public final class ImportanceTest extends Header {

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context) throws SieveException {
        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();
        FilterTest.Importance importance;
        if (argumentsIter.hasNext()) {
            Argument argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                importance = FilterTest.Importance.valueOf(((StringListArgument) argument).getList().get(0));
            } else {
                throw new SyntaxException("Expecting a string");
            }
        } else {
            throw new SyntaxException("Unexpected end of arguments");
        }

        // First check "Importance" header
        List<String> headers = Arrays.asList("Importance");
        List<String> values = null;
        switch (importance) {
            case high:
                values = Arrays.asList("High");
                break;
            case low:
                values = Arrays.asList("Low");
                break;
            case normal:
                values = Arrays.asList("High", "Low");
        }
        boolean result1 =
                match(mail, Sieve.Comparator.iasciicasemap.toString(), MatchTypeTags.IS_TAG, headers, values, context);

        // Now check "X-Priority" header
        headers = Arrays.asList("X-Priority");
        values = null;
        switch (importance) {
            case high:
                values = Arrays.asList("1");
                break;
            case low:
                values = Arrays.asList("5");
                break;
            case normal:
                // normal is when it is neither high importance nor low importance
                values = Arrays.asList("1", "5");
        }
        boolean result2 =
                match(mail, Sieve.Comparator.iasciicasemap.toString(), MatchTypeTags.IS_TAG, headers, values, context);

        // normal is when it is neither high importance nor low importance
        return importance == FilterTest.ImportanceTest.Importance.normal ? !(result1 || result2) : result1 || result2;
    }
}
