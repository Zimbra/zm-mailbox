/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
