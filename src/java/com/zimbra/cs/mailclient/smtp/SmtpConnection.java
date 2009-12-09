/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;

public class SmtpConnection extends MailConnection {
    
    public static final String EHLO = "EHLO";
    public static final String HELO = "HELO";
    public static final String MAIL = "MAIL";
    public static final String RCPT = "RCPT";
    public static final String DATA = "DATA";
    public static final String QUIT = "QUIT";

    // Same headers that SMTPTransport passes to MimeMessage.writeTo().
    private static final String[] IGNORE_HEADERS = new String[] { "Bcc", "Content-Length" };
    
    private static final Logger LOGGER = Logger.getLogger(SmtpConnection.class);
    
    private Set<String> invalidRecipients = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private Set<String> validRecipients = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    
    public SmtpConnection(SmtpConfig config) {
        super(config);
    }
    
    public Set<String> getValidRecipients() {
        return Collections.unmodifiableSet(validRecipients);
    }
    
    public Set<String> getInvalidRecipients() {
        return Collections.unmodifiableSet(invalidRecipients);
    }

    private class SmtpDataOutputStream
    extends FilterOutputStream {
        
        private byte[] lastTwoBytes = new byte[2];

        private SmtpDataOutputStream(OutputStream out) {
            super(out);
        }
        
        @Override
        public void write(int b)
        throws IOException {
            if (b == '.' && lastTwoBytes[0] == '\r' && lastTwoBytes[1] == '\n') {
                // SMTP transparency per RFC 2821 4.5.2.  Add a dot prefix to
                // lines that start with a dot.
                super.write('.');
            }
            super.write(b);
            lastTwoBytes[0] = lastTwoBytes[1];
            lastTwoBytes[1] = (byte) (b & 0xFF);
        }
    }
    
    @Override
    protected MailInputStream newMailInputStream(InputStream is) {
        return new MailInputStream(is);
    }

    @Override
    protected MailOutputStream newMailOutputStream(OutputStream os) {
        return new MailOutputStream(os);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void processGreeting() throws IOException {
        // Server greeting.
        String reply = mailIn.readLine();
        if (reply == null) {
            throw new MailException("Did not receive greeting from server");
        }
        if (getReplyCode(reply) != 220) {
            throw new IOException("Expected greeting, but got: " + reply);
        }
        
        // Send hello.
        if (ehlo() != 250) {
            helo();
        }
        
        setState(State.AUTHENTICATED);
    }
    
    /**
     * Sends the <tt>EHLO</tt> command and processes the reply.
     * @return the server reply code
     * @throws IOException if the server response was invalid
     */
    private int ehlo() throws IOException {
        String reply = sendCommand(EHLO, getSmtpConfig().getDomain());
        return readHelloReplies(EHLO, reply);
    }
    
    /**
     * Sends the <tt>HELO</tt> command and processes the reply.
     * @throws IOException if the server did not respond with reply
     * code <tt>250</tt>
     */
    private void helo() throws IOException {
        String reply = sendCommand(HELO, getSmtpConfig().getDomain());
        int replyCode = readHelloReplies(HELO, reply);
        if (replyCode != 250) {
            throw new CommandFailedException(HELO, reply);
        }
    }
    
    /**
     * Reads replies to <tt>EHLO</tt> or <tt>HELO</tt>.  Returns
     * the reply code.  Handles multiple <tt>250</tt> responses.
     * @param command the command name
     * @param firstReply the first reply line returned from sending
     * <tt>EHLO</tt> or <tt>HELO</tt> 
     */
    private int readHelloReplies(String command, String firstReply)
    throws IOException {
        String reply = firstReply;
        int replyCode;
        int line = 1;
        
        while (true) {
            if (reply.length() < 4) {
                throw new CommandFailedException(command, "Invalid server response at line " + line + ": " + reply);
            }
            replyCode = getReplyCode(reply);
            if (replyCode != 250) {
                return replyCode;
            }
            char fourthChar = reply.charAt(3);
            if (fourthChar == '-') {
                // Multiple response lines.
                reply = mailIn.readLine(); 
            } else if (fourthChar == ' ') {
                // Last 250 response.
                return 250;
            } else {
                throw new CommandFailedException(command, "Invalid server response at line " + line + ": " + reply);
            }
        }
    }

    @Override
    protected void sendLogin(String user, String pass) {
    }

    @Override
    protected void sendAuthenticate(boolean ir) {
    }
    
    @Override
    public void logout() {
    }

    @Override
    protected void sendStartTls() {
    }

    /**
     * Sends the given message.  Implicitly connects to the MTA, if necessary, and disconnects.
     * Gets the sender and recipients from the headers in the <tt>MimeMessage</tt>.
     */
    public void sendMessage(MimeMessage msg)
    throws IOException, MessagingException {
        // Determine sender.
        String sender = null;
        if (msg instanceof SMTPMessage) {
            sender = ((SMTPMessage) msg).getEnvelopeFrom();
            if (sender != null && sender.length() >= 2 && sender.startsWith("<") && sender.endsWith(">")) {
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
        
        // Determine recipients.
        Address[] recipAddrs = msg.getAllRecipients();
        List<String> recipList = new ArrayList<String>();
        for (Address address : recipAddrs) {
            String addrString = getAddress(address);
            if (!StringUtil.isNullOrEmpty(addrString)) {
                recipList.add(addrString);
            }
        }
        String[] recipients = new String[recipList.size()];
        recipList.toArray(recipients);
        
        sendMessage(sender, recipients, msg);
    }
    
    private String getAddress(Address address) {
        if (address instanceof InternetAddress) {
            return ((InternetAddress) address).getAddress();
        } else {
            return null;
        }
    }


    /**
     * Sends the given message.  Implicitly connects to the MTA, if necessary, and disconnects.
     * @param sender the envelope sender
     */
    public void sendMessage(String sender, String[] recipients, MimeMessage msg)
    throws IOException, MessagingException {
        connect();
        try {
            sendInternal(sender, recipients, msg, null);
        } finally {
            close();
        }
    }
    
    /**
     * Sends the given message.  Implicitly connects to the MTA, if necessary, and disconnects.
     * @param sender the envelope sender
     */
    public void sendMessage(String sender, String[] recipients, String msg)
    throws IOException, MessagingException {
        connect();
        try {
            sendInternal(sender, recipients, null, msg);
        } finally {
            close();
        }
    }
    
    private void sendInternal(String sender, String[] recipients, MimeMessage javaMailMessage, String messageString)
    throws IOException, MessagingException {
        invalidRecipients.clear();
        
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
        if (smtpData.lastTwoBytes[0] != '\r' && smtpData.lastTwoBytes[1] != '\n') {
            // Message data doesn't end with <CRLF>.
            mailOut.write('\r');
            mailOut.write('\n');
        }
        mailOut.writeLine(".");
        mailOut.flush();
        reply = mailIn.readLine();
        quit();
        if (reply == null) {
            throw new CommandFailedException(DATA, "No response");
        }
        if (!isPositive(reply)) {
            throw new CommandFailedException(DATA, reply);
        }
        close();
    }
    
    /**
     * Sends the given command and returns the first line from the server reply.
     * @throws CommandFailedException if the server did not respond 
     */
    private String sendCommand(String command, String args)
    throws IOException {
        String line = command;
        if (!StringUtil.isNullOrEmpty(args)) {
            line += " " + args;
        }
        mailOut.writeLine(line);
        mailOut.flush();
        String reply = mailIn.readLine();
        if (reply == null) {
            throw new CommandFailedException(command, "No response from server");
        }
        return reply;
    }
    
    private void mail(String from) 
    throws IOException {
        if (from == null) {
            from = "";
        }
        String reply = sendCommand(MAIL, "FROM:<" + from + ">");
        if (!isPositive(reply)) {
            throw new CommandFailedException(MAIL, reply);
        }
    }
    
    private void rcpt(String[] recipients)
    throws IOException {
        for (String recipient : recipients) {
            if (recipient == null) {
                recipient = "";
            }
            String reply = sendCommand(RCPT, "TO:<" + recipient + ">");
            if (isPositive(reply)) {
                validRecipients.add(recipient);
            } else {
                if (getSmtpConfig().isPartialSendAllowed()) {
                    invalidRecipients.add(recipient);
                } else {
                    throw new InvalidRecipientException(recipient, reply);
                }
            }
        }
        if (validRecipients.isEmpty()) {
            throw new CommandFailedException(RCPT, "No valid recipients");
        }
    }

    /**
     * Returns <tt>true</tt> if the code from the given reply
     * is between 200 and 299.
     */
    private boolean isPositive(String reply)
    throws IOException {
        int replyCode = getReplyCode(reply);
        if (200 <= replyCode && replyCode <= 299) {
            return true;
        }
        return false;
    }
    
    private void quit() throws IOException {
        sendCommand(QUIT, null);
    }
    
    private static int getReplyCode(String line)
    throws IOException {
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
