/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

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
                try {
                    serverSocket.close();
                } catch (IOException ignore) {
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
        private final Deque<String> record = new LinkedBlockingDeque<String>();

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

        public Scenario reply(Pattern regex, String format) {
            commands.add(new Reply(regex, format, record));
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

    private static final class Send implements Command {
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

    private static final class Reply implements Command {
        private final Pattern regex;
        private final String format;
        private final Deque<String> record;

        Reply(Pattern regex, String format, Deque<String> record) {
            this.regex = regex;
            this.format = format;
            this.record = record;
        }

        @Override
        public void exec(InputStream in, OutputStream out) throws IOException {
            String prev = record.peekLast();
            Matcher matcher = regex.matcher(prev);
            Assert.assertTrue(matcher.find());
            int count = matcher.groupCount();
            Object[] args = new Object[count];
            for (int i = 0; i < count; i++) {
                args[i] = matcher.group(i + 1);
            }
            out.write(MessageFormat.format(format, args).getBytes(Charsets.UTF_8));
            out.flush();
        }
    }

    private static final class Recv implements Command {
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

    private static final class Swallow implements Command {
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
