/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.Header;

import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * Acts just like the original header test, but tests the headers
 * of attachments instead of the top-level message.
 */
public class AttachmentHeaderTest extends Header {

    @SuppressWarnings("unchecked")
    @Override
    protected boolean match(MailAdapter mail, String comparator,
                            String matchType, List headerNames, List keys, SieveContext context)
    throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter zma = (ZimbraMailAdapter) mail;
        // Iterate over the header names looking for a match
        boolean isMatched = false;
        Iterator<String> headerNamesIter = headerNames.iterator();
        while (!isMatched && headerNamesIter.hasNext()) {
            Set<String> values = zma.getMatchingAttachmentHeader(headerNamesIter.next());
            isMatched = match(comparator, matchType, new ArrayList<String>(values), keys, context);
        }
        return isMatched;
    }
}
