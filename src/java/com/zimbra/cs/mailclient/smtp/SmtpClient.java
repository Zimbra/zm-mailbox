/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.mailclient.smtp;

import java.io.PrintStream;

import com.zimbra.cs.mailclient.MailClient;

public final class SmtpClient extends MailClient {

    private static final String[] USAGE = {
        "Usage: java " + SmtpClient.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is " + SmtpConfig.DEFAULT_PORT +
            " or " + SmtpConfig.DEFAULT_SSL_PORT + " for SSL)",
        "  -u user  : authentication id to use",
        "  -z user  : authorization id to use",
        "  -w pass  : password to use",
        "  -r realm : authentication realm",
        "  -m mech  : SASL mechanism to use (\"login\" for SMTP AUTH)",
        "  -k #     : Minimum QOP to use (0=auth, 1=auth-int, 2=auth-conf)",
        "  -l #     : Maximum QOP to use (0=auth, 1=auth-int, 2=auth-conf)",
        "  -s       : enable SMTP over SSL (smtps)",
        "  -t       : enable SMTP over TLS",
        "  -d       : enable debug output",
        "  -q       : enable silent mode",
        "  -h       : print this help message"
    };

    protected SmtpClient() {
        super(new SmtpConfig());
    }

    @Override
    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }

    public static void main(String[] args) throws Exception {
        new SmtpClient().run(args);
    }
}
