/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.Header;

import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * Acts just like the original header test, but tests the headers
 * of all MIME parts instead of just the top-level message.
 */
public class MimeHeaderTest extends Header {

    @SuppressWarnings("unchecked")
    @Override
    protected boolean match(MailAdapter mail, String comparator,
                            String matchType, List headerNames, List keys, SieveContext context)
    throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter zma = (ZimbraMailAdapter) mail;
        // Iterate over the header names looking for a match
        boolean isMatched = false;
        Iterator<String> headerNamesIter = headerNames.iterator();
        while (!isMatched && headerNamesIter.hasNext()) {
            Set<String> values = zma.getMatchingHeaderFromAllParts(headerNamesIter.next());
            isMatched = match(comparator, matchType, new ArrayList<String>(values), keys, context);
        }
        return isMatched;
    }
}
