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

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

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
