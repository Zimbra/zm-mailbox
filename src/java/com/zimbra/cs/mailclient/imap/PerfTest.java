/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.imap;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class PerfTest {
    private final int count;
    private final List<ImapConnection> connections;

    public PerfTest(int count) {
        this.count = count;
        connections = new ArrayList<ImapConnection>(count);
    }

    public void run() throws IOException {
        for (int i = 0; i < count; i++) {
            ImapConnection connection = newConnection();
            if (connection == null) break;
            connections.add(connection);
        }
        p("Created %d out of %d connections", connections.size(), count);
    }
    
    private static ImapConnection newConnection() throws IOException {
        ImapConfig config = new ImapConfig();
        config.setHost("localhost");
        config.setPort(7143);
        config.setAuthenticationId("user1");
        ImapConnection connection = new ImapConnection(config);
        try {
            connection.connect();
        } catch (IOException e) {
            return null;
        }
        connection.login("test123");
        connection.select("INBOX");
        connection.noop();
        return connection;
    }

    private static void p(String fmt, Object... args) {
        System.out.println(String.format(fmt, args));
    }

    public static void main(String[] args) throws Throwable {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        PerfTest test = new PerfTest(count);
        test.run();
        Thread.sleep(1000000000);
    }
}
