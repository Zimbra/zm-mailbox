package com.zimbra.cs.mailclient;

import static com.zimbra.cs.mailclient.ClientAuthenticator.*;
import com.zimbra.cs.mailclient.util.SSLUtil;

import javax.security.sasl.Sasl;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class MailClient {
    private MailConfig config;
    protected MailConnection connection;
    private StringBuilder sbuf;
    private String password;
    private boolean eof;

    protected MailClient(MailConfig config) {
        this.config = config;
        sbuf = new StringBuilder(132);
    }

    public void run(String[] args) throws LoginException, IOException {
        config.setTrace(true);
        parseArguments(args);
        connect();
        startCommandLoop();
    }

    protected abstract void printUsage(PrintStream ps);
    
    protected void connect() throws LoginException, IOException {
        if (config.getAuthenticationId() == null) {
            // Authentication id defaults to login username
            config.setAuthenticationId(System.getProperty("user.name"));
        }
        config.setTraceStream(System.out);
        config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        connection = MailConnection.getInstance(config);
        connection.connect();
        connection.authenticate(password);
        String qop = connection.getNegotiatedQop();
        if (qop != null) System.out.printf("[Negotiated QOP is %s]\n", qop); 
    }

    private void startCommandLoop() throws IOException {
        connection.setTraceEnabled(false);
        Thread t = new ReaderThread();
        t.setDaemon(true);
        t.start();
        final MailInputStream is = connection.getInputStream();
        String line;
        try {
            while ((line = is.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            if (!eof) e.printStackTrace();
        }
    }

    private class ReaderThread extends Thread {
        public void run() {
            try {
                MailOutputStream os = connection.getOutputStream();
                String line;
                while ((line = readLine(System.in)) != null) {
                    os.writeLine(line);
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            eof = true;
            connection.close();
        }
    }

    private String readLine(InputStream is) throws IOException {
        sbuf.setLength(0);
        int c;
        while ((c = is.read()) != -1 && c != '\n') {
            if (c != '\r') sbuf.append((char) c);
        }
        return c != -1 ? sbuf.toString() : null;
    }

    public void parseArguments(String[] args) {
        try {
            parseArguments(Arrays.asList(args).listIterator());
        } catch (IllegalArgumentException e) {
            System.err.printf("ERROR: %s\n", e);
            printUsage(System.err);
            System.exit(1);
        }
    }

    private void parseArguments(ListIterator<String> args) {
        while (args.hasNext() && parseArgument(args)) {
            // Continue parsing...
        }
        if (!args.hasNext()) {
            throw new IllegalArgumentException("Missing required host name");
        }
        config.setHost(args.next());
        if (args.hasNext()) {
            throw new IllegalArgumentException();
        }
    }

    protected boolean parseArgument(ListIterator<String> args) {
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
                config.setPort(Integer.parseInt(args.next()));
                break;
            case 'u':
                config.setAuthenticationId(args.next());
                break;
            case 'z':
                config.setAuthorizationId(args.next());
                break;
            case 'w':
                password = args.next();
                break;
            case 'r':
                config.setRealm(args.next());
                break;
            case 'm':
                config.setMechanism(args.next().toUpperCase());
                break;
            case 'k':
                minQop = parseQop(arg, args.next());
                break;
            case 'l':
                maxQop = parseQop(arg, args.next());
                break;
            case 's':
                config.setSslEnabled(true);
                break;
            case 't':
                config.setTlsEnabled(true);
                break;
            case 'd':
                config.setDebug(true);
                break;
            case 'q':
                config.setTrace(false);
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
        if (!config.isSslEnabled()) {
            config.setSaslProperty(Sasl.QOP, getQop(minQop, maxQop));
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
