/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

/**
 * A programmable TCP server for testing.
 *
 * @author ysasaki
 */
public final class MockTcpServer {

    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Scenario scenario;

    private MockTcpServer(Scenario scenario) {
        this.scenario = scenario;
    }

    public static Scenario scenario() {
        return new Scenario();
    }

    public String replay() {
        Preconditions.checkState(executor.isTerminated());
        return scenario.replay();
    }

    public MockTcpServer start(int port) throws IOException {
        Preconditions.checkState(serverSocket == null);

        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(1000);
        serverSocket.setReuseAddress(true);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sock = serverSocket.accept();
                    sock.setSoTimeout(1000);
                    scenario.play(sock);
                    sock.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
        return this;
    }

    public void shutdown(long timeout) {
        Preconditions.checkState(serverSocket != null);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
    }

    public void destroy() {
        executor.shutdownNow();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static boolean end(ByteArrayOutputStream buf, byte[] with) {
        if (buf.size() < with.length) {
            return false;
        }
        byte[] array = buf.toByteArray();
        for (int i = 0; i < with.length; i++) {
            if (array[array.length - with.length + i] != with[i]) {
                return false;
            }
        }
        return true;
    }

    public static final class Scenario {
        private final Queue<Command> commands = new ConcurrentLinkedQueue<Command>();
        private final Queue<String> record = new ConcurrentLinkedQueue<String>();

        private Scenario() {
        }

        private String replay() {
            return record.poll();
        }

        private void play(Socket sock) throws IOException {
            InputStream in = sock.getInputStream();
            OutputStream out = sock.getOutputStream();
            for (Command cmd : commands) {
                cmd.exec(in, out);
            }
        }

        public MockTcpServer build() {
            return new MockTcpServer(this);
        }

        public Scenario sendLine(String line) {
            return send(line + "\r\n");
        }

        public Scenario send(String text) {
            return send(text.getBytes(Charsets.UTF_8));
        }

        public Scenario send(byte[] data) {
            commands.add(new Send(data));
            return this;
        }

        public Scenario recvLine() {
            commands.add(new Recv(new byte[]{'\n'}, record));
            return this;
        }

        public Scenario recvUntil(String until) {
            commands.add(new Recv(until.getBytes(Charsets.UTF_8), record));
            return this;

        }

        public Scenario swallowUntil(String until) {
            commands.add(new Swallow(until.getBytes(Charsets.UTF_8)));
            return this;
        }
    }

    private interface Command {
        void exec(InputStream in, OutputStream out) throws IOException;
    }

    private static class Send implements Command {
        private final byte[] data;

        Send(byte[] data) {
            this.data = data;
        }

        @Override
        public void exec(InputStream in, OutputStream out) throws IOException {
            out.write(data);
            out.flush();
        }
    }

    private static class Recv implements Command {
        private final byte[] until;
        private final Queue<String> record;

        Recv(byte[] until, Queue<String> record) {
            this.until = until;
            this.record = record;
        }

        @Override
        public void exec(InputStream in, OutputStream out) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            while (true) {
                int b = in.read();
                if (b < 0) {
                    break;
                }
                buf.write(b);
                if (end(buf, until)) {
                    break;
                }
            }
            if (buf.size() > 0) {
                record.add(buf.toString(Charsets.UTF_8.name()));
            }
            buf.close();
        }

    }

    private static class Swallow implements Command {
        private final byte[] until;

        Swallow(byte[] until) {
            this.until = until;
        }

        @Override
        public void exec(InputStream in, OutputStream out) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            while (true) {
                int b = in.read();
                if (b < 0) {
                    break;
                }
                buf.write(b);
                if (end(buf, until)) {
                    break;
                }
            }
            buf.close();
        }
    }

}
