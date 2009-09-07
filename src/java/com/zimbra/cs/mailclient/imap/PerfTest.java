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
