/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailClient;

import java.io.PrintStream;

public class Pop3Client extends MailClient {
    private static final String[] USAGE = {
        "Usage: java " + Pop3Client.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is " + Pop3Config.DEFAULT_PORT +
                    " or " + Pop3Config.DEFAULT_SSL_PORT + " for SSL)",
        "  -u user  : authentication id to use",
        "  -z user  : authorization id to use",
        "  -w pass  : password to use",
        "  -r realm : authentication realm",
        "  -m mech  : SASL mechanism to use (\"login\" for IMAP LOGIN)",
        "  -k #     : Minimum QOP to use (0=auth, 1=auth-int, 2=auth-conf)",
        "  -l #     : Maximum QOP to use (0=auth, 1=auth-int, 2=auth-conf)",
        "  -s       : enable IMAP over SSL (imaps)",
        "  -t       : enable IMAP over TLS",
        "  -f mbox  : select specified mailbox",
        "  -d       : enable debug output",
        "  -q       : enable silent mode",
        "  -h       : print this help message"
    };

    protected Pop3Client() {
        super(new Pop3Config());
    }

    @Override
    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }
    
    public static void main(String[] args) throws Exception {
        new Pop3Client().run(args);
    }
}
