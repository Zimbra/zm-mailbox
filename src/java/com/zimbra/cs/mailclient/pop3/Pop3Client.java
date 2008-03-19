/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailClient;

import java.io.PrintStream;

public class Pop3Client extends MailClient {
    private static final String[] USAGE = {
        "Usage: java " + Pop3Client.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is " + Pop3Config.DEFAULT_PORT +
                    " or " + Pop3Config.DEFAULT_SSL_PORT + " for SSL)",
        "  -u user  : authorization name to use",
        "  -a user  : authentication name to use",
        "  -w pass  : password to use",
        "  -v       : enable verbose output",
        "  -m mech  : SASL mechanism to use (\"user\" for POP3 USER/PASS)",
        "  -q qop   : Quality of protection to use (auth, auth-int, auth-conf",
        "  -r realm : realm",
        "  -h       : print this help message"
    };

    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }

    protected Pop3Client() {
        super(new Pop3Config());
    }

    public static void main(String[] args) throws Exception {
        new Pop3Client().run(args);
    }
}
