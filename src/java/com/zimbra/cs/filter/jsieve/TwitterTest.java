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

import com.google.common.base.Strings;
import com.zimbra.cs.filter.ZimbraMailAdapter;
import com.zimbra.cs.mime.ParsedAddress;

/**
 * SIEVE test for Twitter notifications.
 * <p>
 * Built-in test for Twitter notifications:
 * <ul>
 *  <li>direct message email - {@code From: <dm-*@postmaster.twitter.com>}
 *  <li>mention email - {@code From: <mention-*@postmaster.twitter.com>}
 * </ul>
 *
 * @author ysasaki
 */
public final class TwitterTest extends AbstractTest {

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;
        ParsedAddress sender = adapter.getParsedMessage().getParsedSender();
        if (!Strings.isNullOrEmpty(sender.emailPart)) {
            String email = sender.emailPart.toLowerCase();
            if (email.endsWith("@postmaster.twitter.com") &&
                    (email.startsWith("dm-") || email.startsWith("mention-"))) {
                return true;
            }
        }
        return false;
    }

}
