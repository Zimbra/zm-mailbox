package com.zimbra.cs.mailtest;

import static com.zimbra.cs.mailtest.ClientAuthenticator.*;

import javax.security.sasl.Sasl;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class MailTest {
    protected MailClient mClient;
    private StringBuilder mLineBuffer;
    private boolean mEnableTLS;
    private boolean mGotEndOfFile;

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
                System.err.printf("ERROR: %s\n", msg);
                printUsage(System.err);
                System.exit(1);
            }
        }
        mClient.setLogStream(System.out);
        mClient.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        mClient.connect();
        if (mEnableTLS) mClient.startTLS();
        mClient.authenticate();
        mClient.setLogStream(null);
        String qop = mClient.getNegotiatedQop();
        if (qop != null) System.out.printf("[Negotiated QOP is %s]\n", qop); 
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
            if (!mGotEndOfFile) e.printStackTrace();
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
            try {
                mGotEndOfFile = true;
                mClient.getInputStream().close();
            } catch (IOException e) {}
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
            throw new IllegalArgumentException("Missing required host name");
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
        int minQop = -1;
        int maxQop = -1;
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
                case 's':
                    mClient.setSslEnabled(true);
                    break;
                case 'k':
                    minQop = parseQop(arg, it.next());
                    break;
                case 'l':
                    maxQop = parseQop(arg, it.next());
                    break;
                case 't':
                    mEnableTLS = true;
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
        // If SSL is enabled then only QOP_AUTH is supported
        if (!mClient.isSSLEnabled()) {
            mClient.setSaslProperty(Sasl.QOP, getQop(minQop, maxQop));
        }
        return true;
    }

    private static int parseQop(String arg, String value) {
        if (value.equalsIgnoreCase(QOP_AUTH) || value.equals("0")) return 0;
        if (value.equalsIgnoreCase(QOP_AUTH_INT) || value.equals("1")) return 1;
        if (value.equalsIgnoreCase(QOP_AUTH_CONF) || value.equals("2")) return 2;
        throw new IllegalArgumentException("Invalid value for option '" + arg + "'");
    }

    private static String getQop(int minQop, int maxQop) {
        if (minQop == -1) minQop = 0;
        if (maxQop == -1) maxQop = 2;
        if (minQop > maxQop) maxQop = minQop;
        StringBuilder sb = new StringBuilder();
        for (int i = maxQop; i >= minQop; --i) {
            switch (i) {
                case 0: sb.append(QOP_AUTH); break;
                case 1: sb.append(QOP_AUTH_INT); break;
                case 2: sb.append(QOP_AUTH_CONF); break;
                default: throw new AssertionError();
            }
            if (i > minQop) sb.append(',');
        }
        return sb.toString();
    }
}
