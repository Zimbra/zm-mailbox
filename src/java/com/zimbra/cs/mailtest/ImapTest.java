package com.zimbra.cs.mailtest;

import java.io.PrintStream;

/**
 * IMAP protocol test client, like Cyrus IMTEST.
 */
public class ImapTest extends MailTest {
    private static final String[] USAGE = {
        "Usage: java " + ImapTest.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is port " + ImapClient.DEFAULT_PORT + ")",
        "  -u user  : authorization name to use",
        "  -a user  : authentication name to use",
        "  -w pass  : password to use",
        "  -v       : enable verbose output",
        "  -m mech  : mechanism to use (plain, gssapi, or login)",
        "  -r realm : realm",
        "  -h       : print this help message"
    };

    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }
    
    protected ImapTest() {
        super(new ImapClient());
        mClient.setPort(ImapClient.DEFAULT_PORT);
    }

    public static void main(String[] args) throws Exception {
        new ImapTest().run(args);
    }
}
