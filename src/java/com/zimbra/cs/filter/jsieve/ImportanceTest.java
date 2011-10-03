/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
