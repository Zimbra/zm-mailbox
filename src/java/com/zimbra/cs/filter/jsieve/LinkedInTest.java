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

import java.util.Set;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.AbstractTest;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
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
    private static final Set<String> ADDRESSES = ImmutableSet.of("connections@linkedin.com", "member@linkedin.com");

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
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
