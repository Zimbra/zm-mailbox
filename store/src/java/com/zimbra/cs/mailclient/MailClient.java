/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient;

import static com.zimbra.cs.mailclient.auth.SaslAuthenticator.*;

import com.zimbra.common.util.Log;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.smtp.SmtpConfig;
import com.zimbra.cs.mailclient.smtp.SmtpConnection;
import com.zimbra.cs.mailclient.util.SSLUtil;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.pop3.Pop3Config;
import com.zimbra.cs.mailclient.pop3.Pop3Connection;

import javax.security.sasl.Sasl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import javax.security.auth.login.LoginException;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;

public abstract class MailClient {
    private final MailConfig config;
    protected MailConnection connection;
    private final StringBuilder sbuf = new StringBuilder(132);
    private String password;
    private boolean eof;
    
    private static final Logger LOG = LogManager.getLogger(MailClient.class);

    protected MailClient(MailConfig config) {
        this.config = config;
    }

    public void run(String[] args) throws LoginException, IOException {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel(LOG.getName(), Level.INFO);
        parseArguments(args, config);
        connect();
        authenticate();
        startCommandLoop();
    }

    protected abstract void printUsage(PrintStream ps);

    protected void setPassword(String value) {
        password = value;
    }

    protected void connect() throws IOException {
        config.setConnectTimeout(30);
        config.setSSLSocketFactory(SSLUtil.getDummySSLContext().getSocketFactory());
        connection = newConnection(config);
        connection.connect();
    }

    protected void authenticate() throws LoginException, IOException {
        Console console = System.console();
        if (console != null) {
            if (config.getAuthenticationId() == null) {
                config.setAuthenticationId(console.readLine("Username: "));
            }
            if (password == null && isPasswordRequired()) {
                password = new String(console.readPassword("Password: "));
            }
        }
        connection.authenticate(password);
        String qop = connection.getNegotiatedQop();
        if (qop != null) System.out.printf("[Negotiated QOP is %s]\n", qop);
    }

    private boolean isPasswordRequired() {
        String mech = config.getMechanism();
        if (mech == null) {
            return true;
        }
        AuthenticatorFactory af = config.getAuthenticatorFactory();
        return af != null && af.isPasswordRequired(mech);
    }

    private static MailConnection newConnection(MailConfig config) {
        if (config instanceof ImapConfig) {
            return new ImapConnection((ImapConfig) config);
        } else if (config instanceof Pop3Config) {
            return new Pop3Connection((Pop3Config) config);
        } else if (config instanceof SmtpConfig) {
            return new SmtpConnection((SmtpConfig) config);
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + config.getProtocol());
        }
    }

    private void startCommandLoop() {
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
        @Override
        public void run() {
            try {
                MailOutputStream os = connection.getOutputStream();
                String line;
                while ((line = readLine(System.in)) != null) {
                    if (!processCommand(line)) {
                        os.writeLine(line);
                        os.flush();
                    }
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

    protected void parseArguments(String[] args, MailConfig config) {
        ListIterator<String> itr = Arrays.asList(args).listIterator();
        try {
            while (itr.hasNext() && parseArgument(itr));
            if (!itr.hasNext()) {
                throw new IllegalArgumentException("Missing required hostname");
            }
            config.setHost(itr.next());
            if (itr.hasNext()) {
                throw new IllegalArgumentException("Extra arguments found after hostname");
            }
        } catch (IllegalArgumentException e) {
            System.err.printf("ERROR: %s\n", e);
            printUsage(System.err);
            System.exit(1);
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
                config.setSecurity(MailConfig.Security.SSL);
                break;
            case 't':
                config.setSecurity(MailConfig.Security.TLS_IF_AVAILABLE);
                break;
            case 'd':
                config.getLogger().setLevel(Log.Level.trace);
                break;
            case 'q':
                config.getLogger().setLevel(Log.Level.error);
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
        if (config.getSecurity() != MailConfig.Security.SSL) {
            Map<String, String> props = new HashMap<String, String>();
            props.put(Sasl.QOP, getQop(minQop, maxQop));
            config.setSaslProperties(props);
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

    private boolean processCommand(String line) throws IOException {
        if (!line.startsWith("!")) return false;
        line = line.substring(1);
        String[] cmdLine = line.split(" ");
        boolean success = false;
        try {
            success = processCommand(cmdLine);
        } catch (IndexOutOfBoundsException e) {
            // Fall through
        }
        if (!success) {
            System.out.println(">> ERROR: Invalid command: " + line);
        }
        return true;
    }

    protected boolean processCommand(String[] cmdLine) throws IOException {
        String cmd = cmdLine[0];
        if (isMatch(cmd, "SHow")) {
            return processShow(cmdLine);
        } else if (isMatch(cmd, "SET")) {
            return processSet(cmdLine);
        }
        return false;
    }

    protected boolean processShow(String[] cmdLine) throws IOException {
        return false;
    }

    protected boolean processSet(String[] cmdLine) throws IOException {
        return false;
    }

    protected static boolean isMatch(String word, String fullword) {
        if (word.length() > fullword.length()) return false;
        boolean enough = false;
        for (int i = 0; i < fullword.length(); i++) {
            char c = fullword.charAt(i);
            if (Character.isLowerCase(c)) {
                enough = true;
            } else {
                c = Character.toLowerCase(c);
            }
            if (i >= word.length()) return enough;
            if (Character.toLowerCase(word.charAt(i)) != c) {
                return false;
            }
        }
        return word.length() == fullword.length();
    }
}
