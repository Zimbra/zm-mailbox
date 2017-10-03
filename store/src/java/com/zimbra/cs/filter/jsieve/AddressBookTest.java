/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * SIEVE test that returns true if the email address in the specified header exists in the address book.
 *
 * @since Nov 11, 2004
 */
public final class AddressBookTest extends AbstractTest {
    private static final String IN = ":in";
    private List<String> headers;

    @Override
    protected void validateArguments(Arguments args, SieveContext ctx) throws SieveException {
        Iterator<Argument> itr = args.getArgumentList().iterator();
        if (itr.hasNext()) {
            Argument arg = itr.next();
            if (arg instanceof TagArgument) {
                TagArgument tag = (TagArgument) arg;
                if (tag.is(IN)) {
                    if (itr.hasNext()) {
                        arg = itr.next();
                        if (arg instanceof StringListArgument) {
                            headers = ((StringListArgument) arg).getList();
                        } else {
                            throw ctx.getCoordinate().syntaxException(IN + " is missing an argument");
                        }
                    } else {
                        throw ctx.getCoordinate().syntaxException(IN + " is missing an argument");
                    }
                } else {
                    throw ctx.getCoordinate().syntaxException("Unknown tag: " + tag.getTag());
                }
            } else {
                throw ctx.getCoordinate().syntaxException("Unexpected argument: " + arg.getValue());
            }
        }
    }

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments arguments, SieveContext context) throws SieveException {
        assert(headers != null);
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        Mailbox mbox = ((ZimbraMailAdapter) mail).getMailbox();
        List<InternetAddress> addrs = new ArrayList<InternetAddress>();
        for (String header : headers) {
            for (String value : mail.getHeader(header)) {
                addrs.add(new InternetAddress(value));
            }
        }
        try {
            return mbox.index.existsInContacts(addrs);
        } catch (IOException | ServiceException e) {
            ZimbraLog.filter.error("Failed to lookup contacts", e);
        }
        return false;
    }

}
