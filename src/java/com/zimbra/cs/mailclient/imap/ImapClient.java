package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.MailClient;

import javax.security.auth.login.LoginException;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ListIterator;

/**
 * IMAP protocol test client.
 */
public class ImapClient extends MailClient {
    private String mailbox;
    
    private static final String[] USAGE = {
        "Usage: java " + ImapClient.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is " + ImapConfig.DEFAULT_PORT +
                    " or " + ImapConfig.DEFAULT_SSL_PORT + " for SSL)",
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

    protected ImapClient() {
        super(new ImapConfig());
    }

    @Override
    protected void connect() throws LoginException, IOException {
        super.connect();
        if (mailbox != null) {
            ((ImapConnection) connection).select(mailbox);
        }
    }

    @Override
    protected boolean parseArgument(ListIterator<String> args) {
        String arg = args.next();
        if (arg.equals("-f") && args.hasNext()) {
            mailbox = args.next();
            return true;
        }
        args.previous();
        return super.parseArgument(args);
    }

    @Override
    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }

    public static void main(String[] args) throws Exception {
        new ImapClient().run(args);
    }
}
