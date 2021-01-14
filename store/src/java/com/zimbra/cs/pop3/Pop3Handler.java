/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.pop3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.security.sasl.Authenticator;
import com.zimbra.cs.security.sasl.AuthenticatorUser;
import com.zimbra.cs.security.sasl.PlainAuthenticator;
import com.zimbra.cs.server.ServerThrottle;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * @since Nov 25, 2004
 */
abstract class Pop3Handler {
    static final int MIN_EXPIRE_DAYS = 31;
    static final int MAX_RESPONSE = 512;
    static final int MAX_RESPONSE_TEXT = MAX_RESPONSE - 7; // "-ERR" + " " + "\r\n"

    static final byte[] LINE_SEPARATOR = {'\r', '\n'};
    private static final String TERMINATOR = ".";
    private static final int TERMINATOR_C = '.';
    private static final byte[] TERMINATOR_BYTE = {'.'};

    // Connection specific data
    final Pop3Config config;
    OutputStream output;
    boolean startedTLS;

    private String user;
    private String query;
    private String accountId;
    private String accountName;
    private Pop3Mailbox mailbox;
    private String command;
    private long startTime;
    int state;
    private int errorCount = 0;

    private boolean dropConnection;
    Authenticator authenticator;
    private String clientAddress;
    private String origRemoteAddress;

    static final int STATE_AUTHORIZATION = 1;
    static final int STATE_TRANSACTION = 2;
    static final int STATE_UPDATE = 3;

    // Message specific data
    private String currentCommandLine;
    private int expire;

    private final ServerThrottle throttle;

    Pop3Handler(Pop3Config config) {
        this.config = config;
        startedTLS = config.isSslEnabled();
        throttle = ServerThrottle.getThrottle(config.getProtocol());
    }

    abstract void startTLS() throws IOException;
    abstract void completeAuthentication() throws IOException;
    abstract InetSocketAddress getLocalAddress();

    String getOrigRemoteIpAddr() {
        return origRemoteAddress;
    }

    void setOrigRemoteIpAddr(String ip) {
        origRemoteAddress = ip;
    }

    boolean startConnection(InetAddress remoteAddr) throws IOException {
        // Set the logging context for anything logged before the first command.
        ZimbraLog.clearContext();
        clientAddress = remoteAddr.getHostAddress();
        setLoggingContext();

        ZimbraLog.pop.info("connected");
        if (!config.isServiceEnabled()) {
            return false;
        }
        sendOK(config.getGreeting());
        state = STATE_AUTHORIZATION;
        dropConnection = false;
        return true;
    }

    public void setLoggingContext() {
        ZimbraLog.clearContext();
        ZimbraLog.addAccountNameToContext(accountName);
        ZimbraLog.addIpToContext(clientAddress);
        ZimbraLog.addOrigIpToContext(origRemoteAddress);
    }

    boolean processCommand(String line) throws IOException {
        // TODO: catch IOException too?
        if (line != null && authenticator != null && !authenticator.isComplete()) {
            return continueAuthentication(line);
        }

        command = null;
        startTime = 0;
        currentCommandLine = line;

        try {
            boolean result = processCommandInternal();

            // Track stats if the command completed successfully
            if (startTime > 0) {
                long elapsed = ZimbraPerf.STOPWATCH_POP.stop(startTime);
                if (command != null) {
                    ZimbraPerf.POP_TRACKER.addStat(command.toUpperCase(), startTime);
                    ZimbraLog.pop.info("%s elapsed=%d", command.toUpperCase(), elapsed);
                } else {
                    ZimbraLog.pop.info("(unknown) elapsed=%d", elapsed);
                }
            }
            errorCount = 0;
            return result;
        } catch (Pop3CmdException e) {
            ZimbraLog.pop.debug(e.getMessage(), e);
            errorCount++;
            int errorLimit = LC.pop3_max_consecutive_error.intValue();
            if (errorLimit > 0 && errorCount >= errorLimit) {
                ZimbraLog.pop.warn("dropping connection due to too many errors");
                sendERR(e.getResponse() +" : Dropping connection due to too many bad commands");
                dropConnection = true;
            } else {
                sendERR(e.getResponse());
            }
            return !dropConnection;
        } catch (ServiceException e) {
            sendERR(Pop3CmdException.getResponse(e.getMessage()));
            if (MailServiceException.NO_SUCH_BLOB.equals(e.getCode())) {
                ZimbraLog.pop.warn(e.getMessage(), e);
            } else {
                ZimbraLog.pop.debug(e.getMessage(), e);
            }
            return !dropConnection;
        }
    }

    boolean processCommandInternal() throws Pop3CmdException, IOException, ServiceException {
        startTime = System.currentTimeMillis();
        command = currentCommandLine;
        String arg = null;

        if (command == null) {
            dropConnection = true;
            ZimbraLog.pop.info("disconnected without quit");
            return false;
        }

        if (ZimbraLog.pop.isTraceEnabled()) {
            if ("PASS ".regionMatches(true, 0, command, 0, 5)) {
                ZimbraLog.pop.trace("C: PASS ****");
            } else {
                ZimbraLog.pop.trace("C: %s", command);
            }
        }

        if (!config.isServiceEnabled()) {
            dropConnection = true;
            sendERR("Temporarily unavailable");
            return false;
        }

        int space = command.indexOf(" ");
        if (space > 0) {
            arg = command.substring(space + 1);
            command = command.substring(0, space);
        }

        if (command.length() < 1) {
            throw new Pop3CmdException("invalid request. please specify a command");
        }
        // check account status before executing command
        if (accountId != null) {
            try {
                Provisioning prov = Provisioning.getInstance();
                Account acct = prov.get(AccountBy.id, accountId);
                if (acct == null || !acct.getAccountStatus(prov).equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                    return false;
            } catch (ServiceException e) {
                ZimbraLog.pop.warn("ServiceException checking account status",e);
                return false;
            }
            if (throttle.isAccountThrottled(accountId, origRemoteAddress, clientAddress)) {
                ZimbraLog.pop.warn("throttling POP3 connection for account %s due to too many requests", accountId);
                dropConnection = true;
                return false;
            }
        }

        if (throttle.isIpThrottled(origRemoteAddress)) {
            ZimbraLog.pop.warn("throttling POP3 connection for original remote IP %s", origRemoteAddress);
            dropConnection = true;
            return false;
        } else if (throttle.isIpThrottled(clientAddress)) {
            ZimbraLog.pop.warn("throttling POP3 connection for remote IP %s", clientAddress);
            dropConnection = true;
            return false;
        }

        int ch = command.charAt(0);

        // Breaking out of this switch causes a syntax error to be returned
        // So if you process a command then return immediately (even if the
        // command handler reported a syntax error or failed otherwise)

        switch (ch) {
        case 'a':
        case 'A':
            if ("AUTH".equalsIgnoreCase(command)) {
                doAUTH(arg);
                return true;
            }
            break;
        case 'c':
        case 'C':
            if ("CAPA".equalsIgnoreCase(command)) {
                doCAPA();
                return true;
            }
            break;
        case 'd':
        case 'D':
            if ("DELE".equalsIgnoreCase(command)) {
                doDELE(arg);
                return true;
            }
            break;
        case 'l':
        case 'L':
            if ("LIST".equalsIgnoreCase(command)) {
                doLIST(arg);
                return true;
            }
            break;
        case 'n':
        case 'N':
            if ("NOOP".equalsIgnoreCase(command)) {
                doNOOP();
                return true;
            }
            break;
        case 'p':
        case 'P':
            if ("PASS".equalsIgnoreCase(command)) {
                doPASS(arg);
                return true;
            }
            break;
        case 'q':
        case 'Q':
            if ("QUIT".equalsIgnoreCase(command)) {
                doQUIT();
                return false;
            }
            break;
        case 'r':
        case 'R':
            if ("RETR".equalsIgnoreCase(command)) {
                doRETR(arg);
                return true;
            } else if ("RSET".equalsIgnoreCase(command)) {
                doRSET();
                return true;
            }
            break;
        case 's':
        case 'S':
            if ("STAT".equalsIgnoreCase(command)) {
                doSTAT();
                return true;
            } else if ("STLS".equalsIgnoreCase(command)) {
                doSTLS();
                return true;
            }
            break;
        case 't':
        case 'T':
            if ("TOP".equalsIgnoreCase(command)) {
                doTOP(arg);
                return true;
            }
            break;
        case 'u':
        case 'U':
            if ("UIDL".equalsIgnoreCase(command)) {
                doUIDL(arg);
                return true;
            } else if ("USER".equalsIgnoreCase(command)) {
                doUSER(arg);
                return true;
            }
            break;
        case 'x':
        case 'X':
            if ("XOIP".equalsIgnoreCase(command)) {
                doXOIP(arg);
                return true;
            }
            break;
        default:
            break;
        }
        throw new Pop3CmdException("unknown command");
    }

    void sendERR(String response) throws IOException {
        sendResponse("-ERR", response, true);
    }

    void sendOK(String response) throws IOException {
        sendResponse("+OK", response, true);
    }

    private void sendOK(String response, boolean flush) throws IOException {
        sendResponse("+OK", response, flush);
    }

    void sendContinuation(String s) throws IOException {
        sendLine("+ " + s, true);
    }

    private void sendResponse(String status, String msg, boolean flush) throws IOException {
        sendLine((msg == null || msg.length() == 0) ? status : status + " " + msg, flush);
    }

    abstract void sendLine(String line, boolean flush) throws IOException;

    /*
     * state:
     *   in a line
     *   hit end of a line
     */
    private void sendMessage(InputStream is, int maxNumBodyLines) throws IOException {
        boolean inBody = false;
        int numBodyLines = 0;

        PushbackInputStream stream = new PushbackInputStream(is);
        int c;

        boolean startOfLine = true;
        int lineLength = 0;

        while ((c = stream.read()) != -1) {
            if (c == '\r' || c == '\n') {
                if (c == '\r') {
                    int peek = stream.read();
                    if (peek != '\n' && peek != -1)
                        stream.unread(peek);
                }

                if (!inBody) {
                    if (lineLength == 0)
                        inBody = true;
                } else {
                    numBodyLines++;
                }
                startOfLine = true;
                lineLength = 0;
                output.write(LINE_SEPARATOR);

                if (inBody && numBodyLines >= maxNumBodyLines) {
                    break;
                }
                continue;
            } else if (c == TERMINATOR_C && startOfLine) {
                output.write(c); // we'll end up writing it twice
            }
            if (startOfLine)
                startOfLine = false;
            lineLength++;
            output.write(c);
        }
        if (lineLength != 0) {
            output.write(LINE_SEPARATOR);
        }
        output.write(TERMINATOR_BYTE);
        output.write(LINE_SEPARATOR);
        output.flush();
    }

    private void doQUIT() throws IOException, ServiceException, Pop3CmdException {
        dropConnection = true;
        if (mailbox != null) {
            state = STATE_UPDATE;
            int count = mailbox.expungeDeletes();
            if (count > 0) {
                sendOK("deleted " + count + " message(s)");
            } else {
                sendOK(config.getGoodbye());
            }
        } else {
            sendOK(config.getGoodbye());
        }
        ZimbraLog.pop.info("quit from client");
    }

    private void doNOOP() throws IOException {
        sendOK("yawn");
    }

    private void doRSET() throws Pop3CmdException, IOException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        int numUndeleted = mailbox.undeleteMarked();
        sendOK(numUndeleted+ " message(s) undeleted");
    }

    private void doUSER(String username) throws Pop3CmdException, IOException {
        checkIfLoginPermitted();

        if (state != STATE_AUTHORIZATION) {
            throw new Pop3CmdException("this command is only valid in authorization state");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new Pop3CmdException("please specify a user");
        }
        if (username.length() > 1024) {
            throw new Pop3CmdException("username length too long");
        }
        if (username.endsWith("}")) {
            int p = username.indexOf('{');
            if (p != -1) {
                user = username.substring(0, p);
                query = username.substring(p + 1, username.length() - 1);
            } else {
                user = username;
            }
        } else {
            user = username;
        }

        sendOK("hello " + user + ", please enter your password");
    }

    private void doPASS(String password) throws Pop3CmdException, IOException {
        checkIfLoginPermitted();

        if (state != STATE_AUTHORIZATION) {
            throw new Pop3CmdException("this command is only valid in authorization state");
        }
        if (user == null) {
            throw new Pop3CmdException("please specify username first with the USER command");
        }
        if (password == null) {
            throw new Pop3CmdException("please specify a password");
        }
        if (password.length() > 1024) {
            throw new Pop3CmdException("password length too long");
        }
        authenticate(user, null, password, null);
        sendOK("server ready");
    }

    private void doAUTH(String arg) throws IOException {
        if (isAuthenticated()) {
            sendERR("command only valid in AUTHORIZATION state");
            return;
        }
        if (arg == null || arg.length() == 0) {
            sendERR("mechanism not specified");
            return;
        }

        int i = arg.indexOf(' ');
        String mechanism = i > 0 ? arg.substring(0, i) : arg;
        String initialResponse = i > 0 ? arg.substring(i + 1) : null;

        AuthenticatorUser authUser = new Pop3AuthenticatorUser(this);
        Authenticator auth = Authenticator.getAuthenticator(mechanism, authUser);
        // auth is null if you're not permitted to use that mechanism (including needing TLS layer)
        if (auth == null) {
            sendERR("mechanism not supported");
            return;
        }

        authenticator = auth;
        authenticator.setLocalAddress(getLocalAddress().getAddress());
        if (!authenticator.initialize()) {
            authenticator = null;
            return;
        }

        if (initialResponse != null) {
            continueAuthentication(initialResponse);
        } else {
            sendContinuation("");
        }
    }

    private boolean continueAuthentication(String response) throws IOException {
        byte[] b = Base64.decodeBase64(response.getBytes("us-ascii"));
        authenticator.handle(b);
        if (authenticator.isComplete()) {
            if (authenticator.isAuthenticated()) {
                completeAuthentication();
            } else {
                authenticator = null;
            }
        }
        return true;
    }

    private boolean isAuthenticated() {
        return state != STATE_AUTHORIZATION && accountId != null;
    }

    void authenticate(String username, String authenticateId, String password, Authenticator auth)
            throws Pop3CmdException {
        String mechanism = auth != null ? auth.getMechanism() : "LOGIN";
        try {
            // LOGIN is just another form of AUTH PLAIN with authcid == authzid
            if (auth == null) {
                auth = new PlainAuthenticator(new Pop3AuthenticatorUser(this));
                authenticateId = username;
            }
            // for some authenticators, actually do the authentication here
            // for others (e.g. GSSAPI), auth is already done -- this is just a lookup and authorization check
            Account acct = auth.authenticate(username, authenticateId, password, AuthContext.Protocol.pop3, getOrigRemoteIpAddr(), clientAddress, null);
            // auth failure was represented by Authenticator.authenticate() returning null
            if (acct == null) {
                throw new Pop3CmdException("LOGIN failed");
            }
            if (!acct.getBooleanAttr(Provisioning.A_zimbraPop3Enabled, false) || !acct.isPrefPop3Enabled()) {
                throw new Pop3CmdException("pop access not enabled for account");
            }
            accountId = acct.getId();
            accountName = acct.getName();

            ZimbraLog.addAccountNameToContext(accountName);
            ZimbraLog.pop.info("user %s authenticated, mechanism=%s %s",
                    accountName, mechanism, startedTLS ? "[TLS]" : "");

            mailbox = new Pop3Mailbox(MailboxManager.getInstance().getMailboxByAccount(acct), acct, query);
            state = STATE_TRANSACTION;
            expire = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailMessageLifetime, 0) / Constants.MILLIS_PER_DAY);
            if (expire > 0 && expire < MIN_EXPIRE_DAYS) {
                expire = MIN_EXPIRE_DAYS;
            }
        } catch (ServiceException e) {
            String code = e.getCode();
            if (code.equals(AccountServiceException.NO_SUCH_ACCOUNT) || code.equals(AccountServiceException.AUTH_FAILED)
                    || code.equals(AccountServiceException.TWO_FACTOR_AUTH_FAILED)) {
                throw new Pop3CmdException("LOGIN failed");
            } else if (code.equals(AccountServiceException.CHANGE_PASSWORD)) {
                throw new Pop3CmdException("your password has expired");
            } else if (code.equals(AccountServiceException.MAINTENANCE_MODE)) {
                throw new Pop3CmdException("your account is having maintenance peformed. please try again later");
            } else {
                throw new Pop3CmdException(e.getMessage());
            }
        }
    }

    private void checkIfLoginPermitted() throws Pop3CmdException {
        if (!startedTLS && !config.isCleartextLoginsEnabled()) {
            throw new Pop3CmdException("only valid after entering TLS mode");
        }
    }

    boolean isSSLEnabled() {
        return startedTLS;
    }

    private void doSTAT() throws Pop3CmdException, IOException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        sendOK(mailbox.getNumMessages() + " " + mailbox.getSize());
    }

    private void doSTLS() throws Pop3CmdException, IOException {
        if (config.isSslEnabled()) {
            throw new Pop3CmdException("command not valid over SSL");
        }
        if (state != STATE_AUTHORIZATION) {
            throw new Pop3CmdException("this command is only valid prior to login");
        }
        if (startedTLS) {
            throw new Pop3CmdException("command not valid while in TLS mode");
        }
        startTLS();
        startedTLS = true;
    }

    private void doLIST(String msg) throws Pop3CmdException, IOException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        if (msg != null) {
            Pop3Message pm = mailbox.getPop3Msg(msg);
            sendOK(msg + " " + pm.getSize());
        } else {
            sendOK(mailbox.getNumMessages()+" messages", false);
            int totNumMsgs = mailbox.getTotalNumMessages();
            for (int n=0; n < totNumMsgs; n++) {
                Pop3Message pm = mailbox.getMsg(n);
                if (!pm.isDeleted()) {
                    sendLine((n + 1) + " " + pm.getSize(), false);
                }
            }
            sendLine(TERMINATOR, true);
        }
    }

    private void doUIDL(String msg) throws Pop3CmdException, IOException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        if (msg != null) {
            Pop3Message pm = mailbox.getPop3Msg(msg);
            sendOK(msg + " " + pm.getId() + "." + pm.getDigest());
        } else {
            sendOK(mailbox.getNumMessages() + " messages", false);
            int totNumMsgs = mailbox.getTotalNumMessages();
            for (int n=0; n < totNumMsgs; n++) {
                Pop3Message pm = mailbox.getMsg(n);
                if (!pm.isDeleted()) {
                    sendLine((n+1) + " " + pm.getId() + "." + pm.getDigest(), false);
                }
            }
            sendLine(TERMINATOR, true);
        }
    }

    private int parseInt(String s, String message) throws Pop3CmdException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Pop3CmdException(message);
        }
    }

    private void doRETR(String msg) throws Pop3CmdException, IOException, ServiceException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        if (msg == null) {
            throw new Pop3CmdException("please specify a message");
        }
        Message m = mailbox.getMessage(msg);
        InputStream is = null;
        try {
            is = m.getContentStream();
            sendOK("message follows", false);
            sendMessage(is, Integer.MAX_VALUE);
        } finally {
            ByteUtil.closeStream(is);
        }
        mailbox.getPop3Msg(msg).setRetrieved(true);
    }

    private void doTOP(String arg) throws Pop3CmdException, IOException, ServiceException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        int space = arg == null ? -1 : arg.indexOf(" ");
        if (space == -1) {
            throw new Pop3CmdException("please specify a message and number of lines");
        }
        String msg = arg.substring(0, space);
        int n = parseInt(arg.substring(space + 1), "unable to parse number of lines");

        if (n < 0) {
            throw new Pop3CmdException("please specify a non-negative value for number of lines");
        }
        if (msg == null || msg.equals("")) {
            throw new Pop3CmdException("please specify a message");
        }
        Message m = mailbox.getMessage(msg);
        InputStream is = null;
        try {
            is = m.getContentStream();
            sendOK("message top follows", false);
            sendMessage(is, n);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    private void doDELE(String msg) throws Pop3CmdException, IOException {
        if (state != STATE_TRANSACTION) {
            throw new Pop3CmdException("this command is only valid after a login");
        }
        if (msg == null) {
            throw new Pop3CmdException("please specify a message");
        }
        Pop3Message pm = mailbox.getPop3Msg(msg);
        mailbox.delete(pm);
        sendOK("message "+msg+" marked for deletion");
    }

    private void doCAPA() throws IOException {
        // [SASL]       RFC 5034: POP3 Simple Authentication and Security Layer (SASL) Authentication Mechanism
        sendOK("Capability list follows", false);
        sendLine("TOP", false);
        sendLine("USER", false);
        sendLine("UIDL", false);
        if (!config.isSslEnabled()) {
            sendLine("STLS", false);
        }
        sendLine("SASL" + getSaslCapabilities(), false);
        if (state != STATE_TRANSACTION) {
            sendLine("EXPIRE " + MIN_EXPIRE_DAYS + " USER", false);
        } else {
            if (expire == 0) {
                sendLine("EXPIRE NEVER", false);
            } else {
                sendLine("EXPIRE " + expire, false);
            }
        }
        sendLine("XOIP", false);
        // TODO: VERSION INFO
        sendLine("IMPLEMENTATION ZimbraInc", false);
        sendLine(TERMINATOR, true);
    }

    private void doXOIP(String origIp) throws Pop3CmdException, IOException {
        if (origIp == null)
            throw new Pop3CmdException("please specify an ip address");

        String curOrigRemoteIp = getOrigRemoteIpAddr();
        if (curOrigRemoteIp == null) {
            setOrigRemoteIpAddr(origIp);
            ZimbraLog.addOrigIpToContext(origIp);
            ZimbraLog.pop.info("POP3 client identified as: %s", origIp);
        } else {
            if (curOrigRemoteIp.equals(origIp)) {
                ZimbraLog.pop.warn("POP3 XOIP is allowed only once per session, command ignored");
            } else {
                ZimbraLog.pop.error("POP3 XOIP is allowed only once per session, received different IP: %s, command ignored",
                        origIp);
            }
        }

        sendOK("");
    }

    private String getSaslCapabilities() {
        AuthenticatorUser authUser = new Pop3AuthenticatorUser(this);
        StringBuilder sb = new StringBuilder();
        for (String mechanism : Authenticator.listMechanisms()) {
            if (Authenticator.getAuthenticator(mechanism, authUser) != null)
                sb.append(' ').append(mechanism);
        }
        return sb.toString();
    }

}
