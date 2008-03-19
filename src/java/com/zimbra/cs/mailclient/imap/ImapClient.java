package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.MailClient;

import java.io.PrintStream;

/**
 * IMAP protocol test client, like Cyrus IMTEST.
 */
public class ImapClient extends MailClient {
    private static final String[] USAGE = {
        "Usage: java " + ImapClient.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is " + ImapConfig.DEFAULT_PORT +
                    " or " + ImapConfig.DEFAULT_SSL_PORT + " for SSL)",
        "  -u user  : authorization name to use",
        "  -a user  : authentication name to use",
        "  -w pass  : password to use",
        "  -v       : enable verbose output",
        "  -m mech  : SASL mechanism to use (\"login\" for IMAP LOGIN)",
        "  -k #     : Minimum QOP to use (0=auth, 1=auth-int, 2=auth-conf)",
        "  -l #     : Maximum QOP to use (0=auth, 1=auth-int, 2=auth-conf)",
        "  -r realm : realm",
        "  -s       : enable IMAP over SSL (imaps)",
        "  -h       : print this help message"
    };

    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }
    
    protected ImapClient() {
        super(new ImapConfig());
    }

    public static void main(String[] args) throws Exception {
        new ImapClient().run(args);
    }
}
