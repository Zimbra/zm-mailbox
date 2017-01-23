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

import java.util.Set;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.zimbra.cs.filter.DummyMailAdapter;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mime.ParsedAddress;

/**
 * SIEVE test for LinkedIn notifications.
 * <p>
 * Built-in test for LinkedIn notifications:
 * <ul>
 *  <li>from {@code member@linkedin.com}
 *  <li>from {@code connections@linkedin.com}
 * </ul>
 *
 * @author ysasaki
 */
public final class LinkedInTest extends AbstractTest {
    private static final Set<String> ADDRESSES = ImmutableSet.of("connections@linkedin.com", "member@linkedin.com", "hit-reply@linkedin.com");

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        if (mail instanceof DummyMailAdapter) {
            return true;
        }
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;

        ParsedAddress sender = adapter.getParsedMessage().getParsedSender();
        if (!Strings.isNullOrEmpty(sender.emailPart) && ADDRESSES.contains(sender.emailPart.toLowerCase())) {
            return true;
        }
        return false;
    }

}
