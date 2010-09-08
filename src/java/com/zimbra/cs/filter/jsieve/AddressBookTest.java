/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Nov 11, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.exception.SyntaxException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;

public class AddressBookTest extends AbstractTest {

    static final String IN = ":in";
    static final String CONTACTS = "contacts";
    static final String GAL = "GAL";
    static final byte[] SEARCH_TYPE = { MailItem.TYPE_CONTACT };
    private static Log mLog = LogFactory.getLog(AddressBookTest.class);

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context)
            throws SieveException {
        String comparator = null;
        Set<String> abooks = null;
        @SuppressWarnings("unchecked")
        ListIterator<Argument> argumentsIter = arguments.getArgumentList().listIterator();

        // First argument MUST be a tag of ":in"
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof TagArgument)
            {
                String tag = ((TagArgument) argument).getTag();
                if (tag.equals(IN))
                    comparator = tag;
                else
                    throw new SyntaxException(
                        "Found unexpected: \"" + tag + "\"");
            }
        }
        if (null == comparator)
            throw new SyntaxException("Expecting \":in\"");

        // Second argument MUST be header names
        String[] headers = null;
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                StringListArgument strList = (StringListArgument) argument;
                headers = new String[strList.getList().size()];
                for (int i=0; i< headers.length; i++) {
                    headers[i] = (String) strList.getList().get(i);
                }
            }
        }
        if (headers == null) {
            throw new SyntaxException("No headers are found");
        }
        // Third argument MUST be either contacts or GAL
        if (argumentsIter.hasNext())
        {
            Object argument = argumentsIter.next();
            if (argument instanceof StringListArgument) {
                StringListArgument strList = (StringListArgument) argument;
                abooks = new HashSet<String>();
                for (int i=0; i< strList.getList().size(); i++) {
                    String abookName = (String) strList.getList().get(i);
                    if (!CONTACTS.equals(abookName) && !GAL.equals(abookName))
                        throw new SyntaxException("Unknown address book name: " + abookName);
                    // eliminate duplicates by adding it to the set
                    abooks.add(abookName);
                }
            }
        }
        if (abooks == null || abooks.isEmpty())
            throw new SyntaxException("Expecting address book name(s)");

        // There MUST NOT be any further arguments
        if (argumentsIter.hasNext())
            throw new SyntaxException("Found unexpected argument(s)");

        if (! (mail instanceof ZimbraMailAdapter))
            return false;
        return test(mail, comparator, headers, abooks);
    }

    private boolean test(MailAdapter mail, String comparator, String[] headers, Set<String> abooks) throws SieveException {
        ZimbraMailAdapter zimbraMail = (ZimbraMailAdapter) mail;
        for (String abookName : abooks) {
            if (CONTACTS.equals(abookName)) {
                Mailbox mbox = zimbraMail.getMailbox();
                // searching contacts
                for (int i=0; i<headers.length; i++) {
                    // get values for header that should contains address, like From, To, etc.
                    @SuppressWarnings("unchecked")
                    List<String> headerVals = mail.getHeader(headers[i]);
                    for (int k=0; k<headerVals.size(); k++) {
                        // each header may contain multiple vaules; e.g., To: may contain many recipients
                        String headerVal = (headerVals.get(k)).toLowerCase();
                        ZimbraQueryResults results = null;
                        try {
                            String iaddrStr = headerVal;
                            try {
                                InternetAddress iaddr = new InternetAddress(headerVal);
                                iaddrStr = iaddr.getAddress();
                            } catch (AddressException e1) {
                            }
                            results = mbox.search(new OperationContext(mbox), "To:" + iaddrStr,
                                    SEARCH_TYPE, SortBy.DATE_ASCENDING, 100);
                            mLog.debug("searching for " + iaddrStr);
                            if (results.hasNext()) {
                                mLog.debug("found " + iaddrStr + " in contacts");
                                return true;
                            }
                        } catch (IOException e) {
                        } catch (ServiceException e) {
                        } finally {
                            if (results != null) {
                                try {
                                    results.doneWithSearchResults();
                                } catch (ServiceException e) {
                                }
                            }
                        }
                    }
                }

            } // searching other address database like GAL
        }
        return false;
    }

    @Override
    protected void validateArguments(Arguments arguments, SieveContext context) {
    }
}
