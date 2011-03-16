/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;
import com.zimbra.cs.mailclient.util.Ascii;

public final class SmtpConnection extends MailConnection {

    public static final String EHLO = "EHLO";
    public static final String HELO = "HELO";
    public static final String MAIL = "MAIL";
    public static final String RCPT = "RCPT";
    public static final String DATA = "DATA";
    public static final String QUIT = "QUIT";
    public static final String AUTH = "AUTH";
    public static final String STARTTLS = "STARTTLS";
    public static final String RSET = "RSET";

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
        // Server greeting.
        String reply = mailIn.readLine();
        mailIn.trace();
        if (reply == null) {
            throw new MailException("Did not receive greeting from server");
        }
        if (getReplyCode(reply) != 220) {
            throw new IOException("Expected greeting, but got: " + reply);
        }

        // Send hello, read extensions and auth mechanisms.
        if (ehlo() != 250) {
            helo();
        }

        SmtpConfig config = getSmtpConfig();
        if (config.needAuth()) {
            if (!serverExtensions.contains(AUTH)) {
                throw new MailException("The server doesn't support authentication.");
            }
            //TODO Only LOGIN is supported so far.
            if (serverAuthMechanisms.contains("LOGIN")) {
                config.setMechanism("LOGIN");
            } else {
                throw new MailException("No authentication mechansims supported: " + serverAuthMechanisms);
            }
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
        String reply = sendCommand(EHLO, getSmtpConfig().getDomain());
        return readHelloReplies(EHLO, reply);
    }

    /**
     * Sends the {@code HELO} command and processes the reply.
     *
     * @throws IOException if the server did not respond with reply code {@code 250}
     */
    private void helo() throws IOException {
        String reply = sendCommand(HELO, getSmtpConfig().getDomain());
        int replyCode = readHelloReplies(HELO, reply);
        if (replyCode != 250) {
            throw new CommandFailedException(HELO, reply);
        }
    }

    private static final Pattern PAT_EXTENSION = Pattern.compile("250[ -]([^\\s]+)(.*)");

    /**
     * Reads replies to {@code EHLO} or {@code HELO}.
     * <p>
     * Returns the reply code.  Handles multiple {@code 250} responses.
     *
     * @param command the command name
     * @param firstReply the first reply line returned from sending {@code EHLO} or {@code HELO}
     */
    private int readHelloReplies(String command, String firstReply) throws IOException {

        String reply = firstReply;
        int replyCode;
        int line = 1;
        serverExtensions.clear();
        serverAuthMechanisms.clear();

        while (true) {
            if (reply.length() < 4) {
                throw new CommandFailedException(command, "Invalid server response at line " + line + ": " + reply);
            }
            replyCode = getReplyCode(reply);
            if (replyCode != 250) {
                return replyCode;
            }

            if (line > 1) {
                // Parse server extensions.
                Matcher matcher = PAT_EXTENSION.matcher(reply);
                if (matcher.matches()) {
                    String extName = matcher.group(1).toUpperCase();
                    serverExtensions.add(extName);
                    if (extName.equals(AUTH)) {
                        // Parse auth mechanisms.
                        Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
                        for (String mechanism : splitter.split(matcher.group(2))) {
                            serverAuthMechanisms.add(mechanism.toUpperCase());
                        }
                    }
                }
            }

            char fourthChar = reply.charAt(3);
            if (fourthChar == '-') {
                // Multiple response lines.
                reply = mailIn.readLine();
            } else if (fourthChar == ' ') {
                mailIn.trace();
                // Last 250 response.
                return 250;
            } else {
                throw new CommandFailedException(command, "Invalid server response at line " + line + ": " + reply);
            }
            line++;
        }
    }

    @Override
    protected void sendLogin(String user, String pass) throws IOException {
        // Send AUTH LOGIN command.
        String reply = sendCommand(AUTH, "LOGIN");
        if (getReplyCode(reply) != 334) {
            throw new CommandFailedException(AUTH, reply);
        }

        // Send username.
        reply = sendCommand(Base64.encodeBase64(user.getBytes()), null);
        if (getReplyCode(reply) != 334) {
            if (isPositive(reply)) {
                return;
            } else {
                throw new CommandFailedException(AUTH, "AUTH LOGIN failed: " + reply);
            }
        }

        // Send password.
        reply = sendCommand(Base64.encodeBase64(pass.getBytes()), null);
        if (!isPositive(reply)) {
            throw new CommandFailedException(AUTH, "AUTH LOGIN failed: " + reply);
        }
    }

    @Override
    protected void sendAuthenticate(boolean ir) throws IOException {
        StringBuilder sb = new StringBuilder(authenticator.getMechanism());
        if (ir) {
            sb.append(' ');
            sb.append(Ascii.toString(Base64.encodeBase64(authenticator.getInitialResponse())));
        }
        String reply = sendCommand(AUTH, sb.toString());
        if (isNegative(reply)) {
            throw new CommandFailedException(AUTH, reply);
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
            close();
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
            close();
        }
    }

    private void sendInternal(String sender, String[] recipients, MimeMessage javaMailMessage, String messageString)
            throws IOException, MessagingException {

        invalidRecipients.clear();
        validRecipients.clear();
        Collections.addAll(validRecipients, recipients);

        mail(sender);
        rcpt(recipients);
        String reply = sendCommand(DATA, null);
        if (getReplyCode(reply) != 354) {
            throw new CommandFailedException(DATA, reply);
        }

        SmtpDataOutputStream smtpData = new SmtpDataOutputStream(mailOut);
        if (javaMailMessage != null) {
            javaMailMessage.writeTo(smtpData, IGNORE_HEADERS);
        } else {
            smtpData.write(messageString.getBytes());
        }
        smtpData.end();
        mailOut.flush();
        mailOut.trace();
        reply = mailIn.readLine();
        mailIn.trace();
        if (reply == null) {
            throw new CommandFailedException(DATA, "No response");
        }
        if (!isPositive(reply)) {
            throw new CommandFailedException(DATA, reply);
        }
        quit();
        close();
    }

    /**
     * Sends the given command and returns the first line from the server reply.
     *
     * @throws CommandFailedException if the server did not respond
     */
    private String sendCommand(String command, String args) throws IOException {
        return sendCommand(command.getBytes(), args);
    }

    /**
     * Sends the given command and returns the first line from the server reply.
     *
     * @throws CommandFailedException if the server did not respond
     */
    private String sendCommand(byte[] command, String args) throws IOException {
        mailOut.write(command);
        if (!Strings.isNullOrEmpty(args)) {
            mailOut.write(' ');
            mailOut.write(args);
        }
        mailOut.newLine();
        mailOut.flush();
        mailOut.trace();
        String reply = mailIn.readLine();
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
        String reply = sendCommand(MAIL, "FROM:" + normalizeAddress(from));
        if (!isPositive(reply)) {
            validRecipients.clear();
            throw new CommandFailedException(MAIL, reply);
        }
    }

    private String normalizeAddress(String addr) {
        if ((!addr.startsWith("<")) && (!addr.endsWith(">")))
            return "<" + addr + ">";
        else
            return addr;
    }

    /**
     * Sends {@code RSET} command to the server.
     *
     * @throws CommandFailedException SMTP error
     * @throws IOException socket error
     */
    void rset() throws IOException {
        String reply = sendCommand(RSET, null);
        if (!isPositive(reply)) {
            throw new CommandFailedException(RSET, reply);
        }
    }

    private void rcpt(String[] recipients) throws IOException {
        for (String recipient : recipients) {
            if (recipient == null) {
                recipient = "";
            }
            String reply = sendCommand(RCPT, "TO:" + normalizeAddress(recipient));
            if (!isPositive(reply)) {
                validRecipients.remove(recipient);
                invalidRecipients.add(recipient);
                if (!getSmtpConfig().isPartialSendAllowed()) {
                    throw new InvalidRecipientException(recipient, reply);
                }
            }
        }
        if (validRecipients.isEmpty()) {
            throw new CommandFailedException(RCPT, "No valid recipients");
        }
    }

    /**
     * Returns {@code true} if the code from the given reply is between {@code 200} and {@code 299}.
     */
    private boolean isPositive(String reply) throws IOException {
        int replyCode = getReplyCode(reply);
        if (200 <= replyCode && replyCode <= 299) {
            return true;
        }
        return false;
    }

    private boolean isNegative(String reply) throws IOException {
        return (getReplyCode(reply) >= 400);
    }

    private void quit() throws IOException {
        try {
            sendCommand(QUIT, null);
        } catch (CommandFailedException e) { // no reason to make it an error
            getLogger().warn(e.getMessage());
        }
    }

    private static int getReplyCode(String line) throws IOException {
        if (line == null || line.length() < 3) {
            throw new IOException("Invalid server response: '" + line + "'");
        }
        String replyCodeString = line.substring(0, 3);
        try {
            return Integer.parseInt(replyCodeString);
        } catch (NumberFormatException e) {
            throw new IOException("Could not parse reply code: " + line);
        }
    }

    private SmtpConfig getSmtpConfig() {
        return (SmtpConfig) config;
    }
}
