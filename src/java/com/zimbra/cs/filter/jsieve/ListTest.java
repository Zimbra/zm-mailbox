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

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * SIEVE test whether or not the message is to a mailing list or distribution list the user belongs to.
 * <p>
 * The presence of List-Id header (RFC 2919) is a clear indicator, however some mailing list distribution software
 * including Zimbra haven't adopted it. {@link ListTest} returns true if any of the following conditions are met:
 * <ul>
 *  <li>{@code X-Zimbra-DL} header exists
 *  <li>{@code List-Id} header exists
 * </ul>
 *
 * @see http://www.ietf.org/rfc/rfc3685.txt
 * @author ysasaki
 */
public final class ListTest extends AbstractTest {

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }

        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;
        if (!adapter.getHeader("X-Zimbra-DL").isEmpty() || !adapter.getHeader("List-Id").isEmpty()) {
            return true;
        }
        return false;
    }

}
