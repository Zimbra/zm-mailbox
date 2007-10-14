package com.zimbra.cs.mailtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class MailTest {
    protected MailClient mClient;
    private StringBuilder mLineBuffer;

    protected MailTest(MailClient client) {
        mClient = client;
        mLineBuffer = new StringBuilder(132);
    }

    protected abstract void printUsage(PrintStream ps);
    
    public void run(String[] args) throws Exception {
        try {
            parseArguments(args);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null) {
                System.err.println(msg);
                printUsage(System.err);
                System.exit(1);
            }
        }
        mClient.setLogPrintStream(System.out);
        mClient.connect();
        mClient.authenticate();
        mClient.setLogPrintStream(null);
        startCommandLoop();
    }

    private void startCommandLoop() throws IOException {
        Thread t = new ReaderThread();
        t.setDaemon(true);
        t.start();
        final MailInputStream is = mClient.getInputStream();
        String line;
        try {
            while ((line = is.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReaderThread extends Thread {
        public void run() {
            try {
                MailOutputStream os = mClient.getOutputStream();
                String line;
                while ((line = readLine(System.in)) != null) {
                    os.writeLine(line);
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String readLine(InputStream is) throws IOException {
        mLineBuffer.setLength(0);
        int c;
        while ((c = is.read()) != -1 && c != '\n') {
            if (c != '\r') mLineBuffer.append((char) c);
        }
        return c != -1 ? mLineBuffer.toString() : null;
    }

    protected void parseArguments(String[] args) {
        ListIterator<String> it = Arrays.asList(args).listIterator();
        while (it.hasNext() && parseArgument(it)) {}
        if (!it.hasNext()) {
            throw new IllegalArgumentException("ERROR: Missing required host name");
        }
        mClient.setHost(it.next());
        if (it.hasNext()) {
            throw new IllegalArgumentException();
        }
    }

    private boolean parseArgument(ListIterator<String> it) {
        String arg = it.next();
        if (!arg.startsWith("-")) {
            it.previous();
            return false;
        }
        if (arg.length() != 2) {
            throw new IllegalArgumentException("Illegal option: " + arg);
        }
        try {
            switch (arg.charAt(1)) {
                case 'p':
                    mClient.setPort(Integer.parseInt(it.next()));
                    break;
                case 'u':
                    mClient.setAuthorizationId(it.next());
                    break;
                case 'a':
                    mClient.setAuthenticationId(it.next());
                    break;
                case 'w':
                    mClient.setPassword(it.next());
                    break;
                case 'v':
                    mClient.setDebug(true);
                    break;
                case 'm':
                    mClient.setMechanism(it.next().toUpperCase());
                    break;
                case 'r':
                    mClient.setRealm(it.next());
                    break;
                case 'h':
                    printUsage(System.out);
                    System.exit(0);
                default:
                    throw new IllegalArgumentException("Illegal option: " + arg);
            }
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Option requires argument: " + arg);
        }
        return true;
    }
}
