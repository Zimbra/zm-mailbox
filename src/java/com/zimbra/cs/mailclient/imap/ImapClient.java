/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
    private IDInfo idInfo;
    
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
        "  -i id    : send ID info (name=value,...)",
        "  -h       : print this help message"
    };

    protected ImapClient() {
        super(new ImapConfig());
    }

    @Override
    protected void authenticate() throws LoginException, IOException {
        if (idInfo != null) {
            getImapConnection().id(idInfo);
        }
        super.authenticate();
        if (mailbox != null) {
            getImapConnection().select(mailbox);
        }
    }

    @Override
    protected boolean parseArgument(ListIterator<String> args) {
        String arg = args.next();
        if (arg.equals("-f") && args.hasNext()) {
            mailbox = args.next();
            return true;
        } else if (arg.equals("-i") && args.hasNext()) {
            parseIDInfo(args.next());
            return true;
        }
        args.previous();
        return super.parseArgument(args);
    }

    private void parseIDInfo(String id) {
        if (idInfo == null) {
            idInfo = new IDInfo();
        }
        for (String s : id.split(",")) {
            int i = s.indexOf('=');
            if (i > 0) {
                idInfo.put(s.substring(0, i), s.substring(i + 1));
            }
        }
    }
    
    @Override
    protected void printUsage(PrintStream ps) {
        for (String line : USAGE) ps.println(line);
    }

    @Override
    protected boolean processCommand(String[] cmdLine) throws IOException {
        ImapConnection ic = getImapConnection();
        String cmd = cmdLine[0];
        if (isMatch(cmd, "SELect")) {
            MailboxInfo mbox = ic.select(cmdLine[1]);
            System.out.printf(">> Selected mailbox: %s\n", mbox);
        } else if (isMatch(cmd, "CAPability")) {
            ImapCapabilities cap = ic.capability();
            System.out.printf(">> Capabilities: %s\n", cap);
        } else {
            super.processCommand(cmdLine);
        }
        return true;
    }
    
    @Override
    protected boolean processShow(String[] cmdLine) throws IOException {
        ImapConnection ic = getImapConnection();
        String arg = cmdLine[1];
        if (isMatch(arg, "CAPability")) {
            System.out.printf(">> Current capabilities: %s\n", ic.getCapabilities());
        } else if (isMatch(arg, "MAILbox") || isMatch(arg, "MBox")) {
            System.out.printf(">> Current mailbox: %s\n", ic.getMailbox());
        } else {
            return false;
        }
        return true;
    }

    public ImapConnection getImapConnection() {
        return (ImapConnection) connection;
    }
    
    public static void main(String[] args) throws Exception {
        new ImapClient().run(args);
    }
}
