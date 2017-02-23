/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class QuotedStringParser {
    private String mInput;

    //the parser flips between these two sets of delimiters
    private static final String DELIM_WHITESPACE_AND_QUOTES = " \t\r\n\"";
    private static final String DELIM_QUOTES_ONLY ="\"";

    public QuotedStringParser(String input) {
        if (input == null)
            throw new IllegalArgumentException("Search Text cannot be null.");
        mInput = input;
    }

    public List<String> parse() {
        List<String> result = new ArrayList<String>();

        boolean returnTokens = true;
        String currentDelims = DELIM_WHITESPACE_AND_QUOTES;
        StringTokenizer parser = new StringTokenizer(mInput, currentDelims, returnTokens);

        boolean openDoubleQuote = false;
        boolean gotContent = false;
        String token = null;
        while (parser.hasMoreTokens()) {
            token = parser.nextToken(currentDelims);
            if (!isDoubleQuote(token)) {
                if (!currentDelims.contains(token)) {
                    result.add(token);
                    gotContent = true;
                }
            } else {
                currentDelims = flipDelimiters(currentDelims);
                // allow empty string in double quotes
                if (openDoubleQuote && !gotContent)
                    result.add("");
                openDoubleQuote = !openDoubleQuote;
                gotContent = false;
            }
        }
        return result;
    }

    private boolean isDoubleQuote(String token) {
        return token.equals("\"");
    }

    private String flipDelimiters(String curDelims) {
        if (curDelims.equals(DELIM_WHITESPACE_AND_QUOTES))
            return DELIM_QUOTES_ONLY;
        else
            return DELIM_WHITESPACE_AND_QUOTES;
    }
}

