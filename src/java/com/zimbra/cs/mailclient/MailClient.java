package com.zimbra.cs.mailclient;

import static com.zimbra.cs.mailclient.ClientAuthenticator.*;
import com.zimbra.cs.mailclient.util.SSLUtil;

import javax.security.sasl.Sasl;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class MailClient {
    private MailConfig mConfig;
    protected MailConnection mConnection;
    private StringBuilder mLineBuffer;
    private boolean mEnableTLS;
    private boolean mGotEndOfFile;

    protected MailClient(MailConfig config) {
        mConfig = config;
        mLineBuffer = new StringBuilder(132);
    }

    public void run(String[] args) throws Exception {
        try {
            parseArguments(args);
        } catch (IllegalArgumentException e) {
            System.err.printf("ERROR: %s\n", e);
            printUsage(System.err);
            System.exit(1);
        }
        if (mConfig.getAuthenticationId() == null) {
            // Authentication id defaults to login username
            mConfig.setAuthenticationId(System.getProperty("user.name"));
        }
        mConfig.setTraceStream(System.out);
        mConfig.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        mConnection = MailConnection.getInstance(mConfig);
        mConnection.connect();
        if (mEnableTLS) mConnection.startTLS();
        mConnection.authenticate();
        mConnection.setTraceEnabled(false);
        String qop = mConnection.getNegotiatedQop();
        if (qop != null) System.out.printf("[Negotiated QOP is %s]\n", qop); 
        startCommandLoop();
    }

    protected abstract void printUsage(PrintStream ps);
    
    private void startCommandLoop() throws IOException {
        Thread t = new ReaderThread();
        t.setDaemon(true);
        t.start();
        final MailInputStream is = mConnection.getInputStream();
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
                MailOutputStream os = mConnection.getOutputStream();
                String line;
                while ((line = readLine(System.in)) != null) {
                    os.writeLine(line);
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mGotEndOfFile = true;
            mConnection.close();
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
        mConfig.setHost(it.next());
        if (it.hasNext()) {
            throw new IllegalArgumentException();
        }
    }

    private boolean parseArgument(ListIterator<String> args) {
        String arg = args.next();
        if (!arg.startsWith("-")) {
            args.previous();
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
                mConfig.setPort(Integer.parseInt(args.next()));
                break;
            case 'u':
                mConfig.setAuthorizationId(args.next());
                break;
            case 'a':
                mConfig.setAuthenticationId(args.next());
                break;
            case 'w':
                mConfig.setPassword(args.next());
                break;
            case 'v':
                mConfig.setDebug(true);
                mConfig.setTrace(true);
                break;
            case 'm':
                mConfig.setMechanism(args.next().toUpperCase());
                break;
            case 'r':
                mConfig.setRealm(args.next());
                break;
            case 's':
                mConfig.setSSLEnabled(true);
                break;
            case 'k':
                minQop = parseQop(arg, args.next());
                break;
            case 'l':
                maxQop = parseQop(arg, args.next());
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
        if (!mConfig.isSSLEnabled()) {
            mConfig.setSaslProperty(Sasl.QOP, getQop(minQop, maxQop));
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
