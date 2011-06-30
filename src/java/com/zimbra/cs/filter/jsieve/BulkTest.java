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

import com.google.common.collect.ImmutableSet;
import com.zimbra.cs.filter.ZimbraMailAdapter;

/**
 * SIEVE test whether or not the message is a bulk mail (legitimate mass marketing mail).
 *
 * @author ysasaki
 */
public final class BulkTest extends AbstractTest {
    private static final Set<String> HEADERS = ImmutableSet.of("X-ANTIABUSE", "X-REPORT-ABUSE", "X-ABUSE-REPORTS-TO",
            "X-MAIL_ABUSE_INQUIRES", "X-JOB", "UNSUBSCRIBE", "REMOVEEMAIL");

    @Override
    protected boolean executeBasic(MailAdapter mail, Arguments args, SieveContext ctx) throws SieveException {
        if (!(mail instanceof ZimbraMailAdapter)) {
            return false;
        }
        ZimbraMailAdapter adapter = (ZimbraMailAdapter) mail;

        for (String name : adapter.getHeaderNames()) {
            name = name.toUpperCase(); // compare in all upper-case
            if (HEADERS.contains(name)) { // test common bulk headers
                return true;
            } else if ("PRECEDENCE".equals(name)) { // test "Precedence: bulk"
                for (String precedence : adapter.getHeader("Precedence")) {
                    if ("bulk".equalsIgnoreCase(precedence)) {
                        return true;
                    }
                }
            } else if (name.contains("CAMPAIGN")) { // test *CAMPAIGN*
                return true;
            }
        }
        return false;
    }

}
