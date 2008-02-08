package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailTest;

import java.io.PrintStream;

public class Pop3Test extends MailTest {
    private static final String[] USAGE = {
        "Usage: java " + Pop3Test.class.getName() + " [options] hostname",
        "  -p port  : port to use (default is " + Pop3Client.DEFAULT_PORT +
                    " or " + Pop3Client.DEFAULT_SSL_PORT + " for SSL)",
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

    protected Pop3Test() {
        super(new Pop3Client());
        mClient.setPort(Pop3Client.DEFAULT_PORT);
    }

    public static void main(String[] args) throws Exception {
        new Pop3Test().run(args);
    }
}
