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

import javax.mail.MessagingException;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.google.common.base.Strings;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * SIEVE test for Socialcast notifications.
 * <p>
 * Built-in test for Socialcast notifications excluding bulk messages:
 * <ul>
 *  <li>from {@code *@socialcast.com}
 *  <li>has {@code Reply-To} header (this should exclude bulk messages)
 * </ul>
 *
 * @author ysasaki
 */
public final class SocialcastTest extends AbstractTest {

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;

        ParsedMessage pm = adapter.getParsedMessage();
        ParsedAddress sender = pm.getParsedSender();
        if (!Strings.isNullOrEmpty(sender.emailPart) && sender.emailPart.endsWith("@socialcast.com")) {
            try {
                if (pm.getMimeMessage().getHeader("Reply-To", null) != null) { // test if Reply-To exists
                    return true;
                }
            } catch (MessagingException ignore) {
            }
        }
        return false;
    }

}
