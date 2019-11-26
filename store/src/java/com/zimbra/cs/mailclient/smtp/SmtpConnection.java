/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.smtp;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.security.sasl.SaslException;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;
import com.zimbra.cs.mailclient.auth.SaslAuthenticator;
import com.zimbra.cs.mailclient.util.Ascii;

public final class SmtpConnection extends MailConnection {

    static final String EHLO = "EHLO";
    static final String HELO = "HELO";
    static final String MAIL = "MAIL";
    static final String RCPT = "RCPT";
    static final String DATA = "DATA";
    static final String QUIT = "QUIT";
    static final String AUTH = "AUTH";
    static final String STARTTLS = "STARTTLS";
    static final String RSET = "RSET";
    private static final String LOGIN = "LOGIN";

    // Same headers that SMTPTransport passes to MimeMessage.writeTo().
    private static final String[] IGNORE_HEADERS = new String[] { "Bcc", "Resent-Bcc", "Content-Length" };

    private Set<String> invalidRecipients = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private Set<String> validRecipients = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private Set<String> serverAuthMechanisms = new HashSet<String>();
    private Set<String> serverExtensions = new HashSet<String>();

    public SmtpConnection(SmtpConfig config) {
        super(config);
        Preconditions.checkNotNull(config.getHost());
        Preconditions.checkArgument(config.getPort() >= 0);
    }

    public Set<String> getValidRecipients() {
        return Collections.unmodifiableSet(validRecipients);
    }

    public Set<String> getInvalidRecipients() {
        return Collections.unmodifiableSet(invalidRecipients);
    }

    /**
     * Transform message data transmission.
     * <ul>
     *  <li>Bare CR or LF characters are converted to {@code <CRLF>}.
     *  See RFC 2821 2.3.7 Lines.
     *  <li>If the first character of the line is a period, one additional
     *  period is inserted at the beginning of the line.
     *  See RFC 2821 4.5.2 Transparency.
     * </ul>
     */
    private static final class SmtpDataOutputStream extends FilterOutputStream {

        private int last = -1;

        private SmtpDataOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            switch (b) {
                case '\r':
                    crlf();
                    break;
                case '\n':
                    if (last != '\r') {
                        crlf();
                    }
                    break;
                case '.':
                    switch (last) {
                        case '\r':
                        case '\n':
                        case -1:
                            dot();
                            break;
                    }
                    dot();
                    break;
                default:
                    super.write(b);
                    break;
            }
            last = b;
        }

        /**
         * End of data.
         * <p>
         * RFC 2822 4.1.1.4 DATA (DATA)
         * <p>
         * The mail data is terminated by a line containing only a period, that
         * is, the character sequence {@code <CRLF>.<CRLF>}. This is the end of
         * mail data indication. Note that the first {@code <CRLF>} of this
         * terminating sequence is also the {@code <CRLF>} that ends the final
         * line of the data (message text) or, if there was no data, ends the
         * DATA command itself. An extra {@code <CRLF>} MUST NOT be added, as
         * that would cause an empty line to be added to the message. The only
         * exception to this rule would arise if the message body were passed to
         * the originating SMTP-sender with a final "line" that did not end in
         * {@code <CRLF>}; in that case, the originating SMTP system MUST either
         * reject the message as invalid or add {@code <CRLF>} in order to have
         * the receiving SMTP server recognize the "end of data" condition.
         */
        public void end() throws IOException {
            switch (last) {
                case '\r':
                case '\n':
                case -1:
                    break;
                default:
                    crlf();
                    break;
            }
            dot();
            crlf();
        }

        private void dot() throws IOException {
            super.write('.');
        }

        private void crlf() throws IOException {
            super.write('\r');
            super.write('\n');
        }

        /**
         * For efficiency, don't flush until the last {@code <CRLF>.<CRLF>}.
         */
        @Override
        public void flush() {
        }
    }

    @Override
    protected MailInputStream newMailInputStream(InputStream is) {
        if (getLogger().isTraceEnabled()) {
            return new MailInputStream(is, getLogger());
        } else {
            return new MailInputStream(is);
        }
    }

    @Override
    protected MailOutputStream newMailOutputStream(OutputStream os) {
        if (getLogger().isTraceEnabled()) {
            return new MailOutputStream(os, getLogger());
        } else {
            return new MailOutputStream(os);
        }
    }

    @Override
    protected void processGreeting() throws IOException {
        // Server greeting, can be multiline.
        while (true) {
            Reply reply = Reply.parse(mailIn.readLine());
            mailIn.trace();
            if (reply == null) {
                throw new MailException("Did not receive greeting from server");
            }
            if (reply.code != 220) {
                throw new IOException("Expected greeting, but got: " + reply);
            }
            if (reply.last) {
                break;
            }
        }

        // Send hello, read extensions and auth mechanisms.
        if (ehlo() != 250) {
            helo();
        }
        
        // check auth extensions now if starttls is not being used
        if (config.getSecurity() != MailConfig.Security.TLS_IF_AVAILABLE ||
                !serverExtensions.contains(STARTTLS)) {
            checkAuthExtensions();
        }

        setState(State.NOT_AUTHENTICATED);
    }

    /**
     * Sends the {@code EHLO} command and processes the reply.
     *
     * @return the server reply code
     * @throws IOException if the server response was invalid
     */
    private int ehlo() throws IOException {
        Reply reply = sendCommand(EHLO, getSmtpConfig().getDomain());
        return readHelloReplies(EHLO, reply);
    }

    /**
     * Sends the {@code HELO} command and processes the reply.
     *
     * @throws IOException if the server did not respond with reply code {@code 250}
     */
    private void helo() throws IOException {
        Reply reply = sendCommand(HELO, getSmtpConfig().getDomain());
        int replyCode = readHelloReplies(HELO, reply);
        if (replyCode != 250) {
            throw new CommandFailedException(HELO, reply.toString());
        }
    }

    private static final Pattern PAT_EXTENSION = Pattern.compile("([^\\s]+)(.*)");

    /**
     * Reads replies to {@code EHLO} or {@code HELO}.
     * <p>
     * Returns the reply code.  Handles multiple {@code 250} responses.
     *
     * @param command the command name
     * @param firstReply the first reply line returned from sending {@code EHLO} or {@code HELO}
     */
    private int readHelloReplies(String command, Reply firstReply) throws IOException {

        Reply reply = firstReply;
        int line = 1;
        serverExtensions.clear();
        serverAuthMechanisms.clear();

        while (true) {
            if (reply.text == null) {
                throw new CommandFailedException(command, "Invalid server response at line " + line + ": " + reply);
            }
            if (reply.code != 250) {
                return reply.code;
            }

            if (line > 1) {
                // Parse server extensions.
                Matcher matcher = PAT_EXTENSION.matcher(reply.text);
                if (matcher.matches()) {
                    String extName = matcher.group(1).toUpperCase();
                    serverExtensions.add(extName);
                    if (extName.equals(AUTH)) {
                        // Parse auth mechanisms.
                        Splitter splitter = Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings();
                        for (String mechanism : splitter.split(matcher.group(2))) {
                            serverAuthMechanisms.add(mechanism.toUpperCase());
                        }
                    }
                }
            }
            if (reply.last) { // Last 250 response.
                mailIn.trace();
                return 250;
            } else { // Multiple response lines.
                reply = Reply.parse(mailIn.readLine());
            }
            line++;
        }
    }

    private void checkAuthExtensions() throws MailException {
        if (config.getAuthenticationId() == null) {
            return;
        }
        if (!serverExtensions.contains(AUTH)) {
            throw new MailException("The server doesn't support SMTP-AUTH.");
        }
        String mech = config.getMechanism();
        if (mech != null) {
            if (!serverAuthMechanisms.contains(mech.toUpperCase())) {
                throw new MailException("Auth mechanism mismatch client=" + mech + ",server="+ serverAuthMechanisms);
            }
        } else {
            if (serverAuthMechanisms.contains(LOGIN)) {
                config.setMechanism(LOGIN);
            } else if (serverAuthMechanisms.contains(SaslAuthenticator.PLAIN)) {
                config.setMechanism(SaslAuthenticator.PLAIN);
            } else if (serverAuthMechanisms.contains(SaslAuthenticator.CRAM_MD5)) {
                config.setMechanism(SaslAuthenticator.CRAM_MD5);
            } else if (serverAuthMechanisms.contains(SaslAuthenticator.DIGEST_MD5)) {
                config.setMechanism(SaslAuthenticator.DIGEST_MD5);
            } else if (serverAuthMechanisms.contains(SaslAuthenticator.XOAUTH2)) {
                config.setMechanism(SaslAuthenticator.XOAUTH2);
            } else {
                throw new MailException("No auth mechanism supported: " + serverAuthMechanisms);
            }
        }
    }

    @Override
    protected void sendLogin(String user, String pass) throws IOException {
        // Send AUTH LOGIN command.
        Reply reply = sendCommand(AUTH, LOGIN);
        if (reply.code != 334) {
            throw new CommandFailedException(AUTH, reply.toString());
        }

        // Send username.
        reply = sendCommand(Base64.encodeBase64(user.getBytes()), null);
        if (reply.code != 334) {
            if (reply.isPositive()) {
                return;
            } else {
                throw new CommandFailedException(AUTH, "AUTH LOGIN failed: " + reply);
            }
        }

        // Send password.
        reply = sendCommand(Base64.encodeBase64(pass.getBytes()), null);
        if (!reply.isPositive()) {
            throw new CommandFailedException(AUTH, "AUTH LOGIN failed: " + reply);
        }
    }

    @Override
    protected void sendAuthenticate(boolean ir) throws IOException {
        Reply reply;
        if (authenticator.hasInitialResponse()) {
            reply = sendCommand(AUTH, authenticator.getMechanism() + ' ' +
                    Ascii.toString(Base64.encodeBase64(authenticator.getInitialResponse())));
        } else {
            reply = sendCommand(AUTH, authenticator.getMechanism());
        }

        while (true) {
            switch (reply.code) {
                case 235: // success
                    if (authenticator.isComplete()) {
                        return;
                    } else {
                        throw new SaslException("SASL client auth not complete yet S: " + reply.toString());
                    }
                case 334: // continue
                    byte[] challenge = Strings.isNullOrEmpty(reply.text) ? new byte[0] : Base64.decodeBase64(reply.text);
                    byte[] response = authenticator.evaluateChallenge(challenge);
                    if (response != null) {
                        reply = sendCommand(Ascii.toString(Base64.encodeBase64(response)), null);
                    } else {
                        reply = sendCommand("", null);
                    }
                    continue;
                default:
                    throw new CommandFailedException(AUTH, reply.toString());
            }
        }
    }

    @Override
    public void logout() {
    }

    /**
     * Overrides the superclass implementation, in order to send {@code EHLO}
     * and reread the server extension list.
     */
    @Override
    protected void startTls() throws IOException {
        super.startTls();
        if (ehlo() != 250) {
            helo();
        }
        checkAuthExtensions();
    }

    @Override
    protected void sendStartTls() throws IOException {
        sendCommand(STARTTLS, null);
    }

    private String getSender(MimeMessage msg) throws MessagingException {
        String sender = null;
        if (msg instanceof SMTPMessage) {
            sender = ((SMTPMessage) msg).getEnvelopeFrom();
            if (sender != null && sender.length() >= 2 &&
                    sender.startsWith("<") && sender.endsWith(">")) {
                // Strip brackets.
                sender = sender.substring(1, sender.length() - 1);
            }
        }
        if (sender == null) {
            Address[] fromAddrs = msg.getFrom();
            if (fromAddrs != null && fromAddrs.length > 0) {
                sender = getAddress(fromAddrs[0]);
            }
        }
        return sender;
    }

    private String[] toString(Address[] addrs) {
        List<String> result = new ArrayList<String>();
        for (Address addr : addrs) {
            String str = getAddress(addr);
            if (!Strings.isNullOrEmpty(str)) {
                result.add(str);
            }
        }
        return Iterables.toArray(result, String.class);
    }

    private String getAddress(Address address) {
        if (address instanceof InternetAddress) {
            return ((InternetAddress) address).getAddress();
        } else {
            return null;
        }
    }

    /**
     * Sends the message.
     * <p>
     * Uses the sender and recipients from the headers in the {@link MimeMessage}.
     *
     * @see #sendMessage(String, String[], MimeMessage)
     */
    public void sendMessage(MimeMessage msg) throws IOException, MessagingException {
        sendMessage(getSender(msg), toString(msg.getAllRecipients()), msg);
    }

    /**
     * Sends the message.
     * <p>
     * Uses the sender from the headers in the {@link MimeMessage}.
     *
     * @see #sendMessage(String, String[], MimeMessage)
     */
    void sendMessage(Address[] rcpts, MimeMessage msg) throws IOException, MessagingException {
        sendMessage(getSender(msg), toString(rcpts), msg);
    }

    /**
     * Sends the message.
     *
     * @see #sendMessage(String, String[], MimeMessage)
     */
    void sendMessage(String sender, Address[] rcpts, MimeMessage msg) throws IOException, MessagingException {
        sendMessage(sender, toString(rcpts), msg);
    }

    /**
     * Sends the message.
     * <p>
     * Implicitly connects to the MTA, if necessary, and disconnects.
     *
     * @param sender envelope from
     * @param rcpts envelope recipients
     * @param msg message to send
     * @throws IOException SMTP error
     * @throws MessagingException MIME serialization error
     */
    public void sendMessage(String sender, String[] rcpts, MimeMessage msg) throws IOException, MessagingException {
        connect();
        try {
            sendInternal(sender, rcpts, msg, null);
        } finally {
            quit();
        }
    }

    /**
     * Sends the message.
     * <p>
     * Implicitly connects to the MTA, if necessary, and disconnects.
     *
     * @param sender envelope from
     * @param rcpts envelope recipients
     * @param msg message to send
     * @throws IOException SMTP error
     * @throws MessagingException MIME serialization error
     */
    public void sendMessage(String sender, String[] rcpts, String msg) throws IOException, MessagingException {
        connect();
        try {
            sendInternal(sender, rcpts, null, msg);
        } finally {
            quit();
        }
    }

    /**
     * Return notification options as an RFC 1891 string.
     * Returns null if no options set.
     */
    private String getDSNNotify(SMTPMessage message) {
        if (message.getNotifyOptions() == 0)
            return null;
        if (message.getNotifyOptions() == SMTPMessage.NOTIFY_NEVER)
            return "NEVER";
        StringBuffer sb = new StringBuffer();
        if ((message.getNotifyOptions() & SMTPMessage.NOTIFY_SUCCESS) != 0)
            sb.append("SUCCESS");
        if ((message.getNotifyOptions() & SMTPMessage.NOTIFY_FAILURE) != 0) {
            if (sb.length() != 0)
                sb.append(',');
            sb.append("FAILURE");
        }
        if ((message.getNotifyOptions() & SMTPMessage.NOTIFY_DELAY) != 0) {
            if (sb.length() != 0)
                sb.append(',');
            sb.append("DELAY");
        }
        return sb.toString();
    }
    
    private void sendInternal(String sender, String[] recipients, MimeMessage javaMailMessage, String messageString)
            throws IOException, MessagingException {

        invalidRecipients.clear();
        validRecipients.clear();
        Collections.addAll(validRecipients, recipients);

        mail(sender);
        
        String notify = null;
        if (serverExtensions.contains("DSN")) {
            if (javaMailMessage instanceof SMTPMessage)
                notify = getDSNNotify((SMTPMessage) javaMailMessage);
            if (notify == null)
                notify = getSmtpConfig().getDsn();
        }
        
        rcpt(recipients, notify);
        Reply reply = sendCommand(DATA, null);
        if (reply.code != 354) {
            throw new CommandFailedException(DATA, reply.toString());
        }

        SmtpDataOutputStream smtpData = new SmtpDataOutputStream(mailOut);
        try {
            if (javaMailMessage != null) {
                javaMailMessage.writeTo(smtpData, IGNORE_HEADERS);
            } else {
                smtpData.write(messageString.getBytes());
            }
        } catch (MessagingException e) { // close without QUIT
            close();
            throw e;
        } catch (IOException e) { // close without QUIT
            close();
            throw e;
        }
        smtpData.end();
        mailOut.flush();
        mailOut.trace();
        reply = Reply.parse(mailIn.readLine());
        mailIn.trace();
        if (reply == null) {
            throw new CommandFailedException(DATA, "No response");
        }
        if (!reply.isPositive()) {
            throw new CommandFailedException(DATA, reply.toString());
        }
    }

    /**
     * Sends the given command and returns the first line from the server reply.
     *
     * @throws CommandFailedException if the server did not respond
     */
    private Reply sendCommand(String command, String args) throws IOException {
        return sendCommand(command.getBytes(), args);
    }

    /**
     * Sends the given command and returns the first line from the server reply.
     *
     * @throws CommandFailedException if the server did not respond
     */
    private Reply sendCommand(byte[] command, String args) throws IOException {
        mailOut.write(command);
        if (!Strings.isNullOrEmpty(args)) {
            mailOut.write(' ');
            mailOut.write(args);
        }
        mailOut.newLine();
        mailOut.flush();
        mailOut.trace();
        Reply reply = Reply.parse(mailIn.readLine());
        mailIn.trace();
        if (reply == null) {
            throw new CommandFailedException(new String(command), "No response from server");
        }
        return reply;
    }

    /**
     * Sends {@code MAIL FROM} command to the server.
     *
     * @param from sender address
     * @throws CommandFailedException SMTP error
     * @throws IOException socket error
     */
    void mail(String from) throws IOException {
        if (from == null) {
            from = "";
        }
        Reply reply = sendCommand(MAIL, "FROM:" + normalizeAddress(from));
        if (!reply.isPositive()) {
            validRecipients.clear();
            throw new CommandFailedException(MAIL, reply.toString());
        }
    }

    private String normalizeAddress(String addr) {
        if ((!addr.startsWith("<")) && (!addr.endsWith(">"))) {
            return "<" + addr + ">";
        } else {
            return addr;
        }
    }

    /**
     * Sends {@code RSET} command to the server.
     *
     * @throws CommandFailedException SMTP error
     * @throws IOException socket error
     */
    void rset() throws IOException {
        Reply reply = sendCommand(RSET, null);
        if (!reply.isPositive()) {
            throw new CommandFailedException(RSET, reply.toString());
        }
    }

    private void rcpt(String[] recipients, String dsn) throws IOException {
        for (String recipient : recipients) {
            if (recipient == null) {
                recipient = "";
            }
            String cmd = "TO:" + normalizeAddress(recipient);
            if (dsn != null)
                cmd += " NOTIFY=" + dsn;
            Reply reply = sendCommand(RCPT, cmd);
            if (!reply.isPositive()) {
                validRecipients.remove(recipient);
                invalidRecipients.add(recipient);
                if (!getSmtpConfig().isPartialSendAllowed()) {
                    throw new InvalidRecipientException(recipient, reply.toString());
                }
            }
        }
        if (validRecipients.isEmpty()) {
            throw new CommandFailedException(RCPT, "No valid recipients");
        }
    }

    private void quit() throws IOException {
        if (isClosed()) {
            return;
        }
        try {
            sendCommand(QUIT, null);
        } catch (CommandFailedException e) { // no reason to make it an error
            getLogger().warn(e.getMessage());
        } finally {
            close();
        }
    }

    private SmtpConfig getSmtpConfig() {
        return (SmtpConfig) config;
    }

    private static final class Reply {
        final int code;
        String text;
        boolean last;

        private Reply(String line) throws IOException {
            if (line == null || line.length() < 3) {
                throw new IOException("Invalid server response: '" + line + "'");
            }
            try {
                code = Integer.parseInt(line.substring(0, 3));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid server response: '" + line + "'");
            }
            if (line.length() > 4) {
                switch (line.charAt(3)) {
                    case '-':
                        last = false;
                        break;
                    case ' ':
                        last = true;
                        break;
                    default:
                        throw new IOException("Invalid server response: '" + line + "'");
                }
                text = line.substring(4).trim();
            }
        }

        static Reply parse(String line) throws IOException {
            if (Strings.isNullOrEmpty(line)) {
                return null;
            } else {
                return new Reply(line);
            }
        }

        /**
         * Returns true if the reply code is between {@code 200} and {@code 299}.
         */
        boolean isPositive() {
            return 200 <= code && code <= 299;
        }

        @Override
        public String toString() {
            return text == null ? String.valueOf(code) : (code + " " + text);
        }
    }

}
