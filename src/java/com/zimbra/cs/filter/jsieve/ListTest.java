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

import java.util.List;
import java.util.Set;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.filter.ZimbraSieveException;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.util.AccountUtil;

/**
 * SIEVE test whether or not the message is to a mailing list or distribution list the user belongs to.
 * <p>
 * The presence of List-Id header (RFC 2919) is a clear indicator, however some mailing list distribution software
 * haven't adopted it, so we also test if To/CC does not contain any of *my* addresses. As a side effect, this test may
 * falsely detect messages being auto-forwarded or BCCed. We may delete this problematic logic later.
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

        // test if List-Id header exists
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;
        if (!adapter.getHeader("List-Id").isEmpty()) {
            return true;
        }

        // test if To/CC does not contain me
        List<ParsedAddress> rcpts = adapter.getParsedMessage().getParsedRecipients();
        if (rcpts.isEmpty()) {
            return false;
        }
        Set<String> me;
        try {
            me = AccountUtil.getEmailAddresses(adapter.getMailbox().getAccount());
        } catch (ServiceException e) {
            throw new ZimbraSieveException(e);
        }
        for (ParsedAddress rcpt : rcpts) {
            if (!Strings.isNullOrEmpty(rcpt.emailPart) && me.contains(rcpt.emailPart)) {
                return false;
            }
        }
        return true;
    }

}
