/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.util.JMSession;

public class TestMailSender
extends TestCase {

    private static final int TEST_SMTP_PORT = 6025;
    private static final String NAME_PREFIX = TestMailSender.class.getSimpleName();
    private static final String SENDER_NAME = "user1";
    private static final String RECIPIENT_NAME = "user2";
    private String mOriginalSmtpPort = null;
    private String mOriginalSmtpSendPartial;
    private String mOriginalAllowAnyFrom;
    
    private static class DummySmtpServer
    implements Runnable
    {
        private String mRejectRcpt;
        private String mErrorMsg;
        private PrintWriter mOut;
        private List<String> mDataLines = new ArrayList<String>();
        private String mMailFrom;
        private static final Pattern PAT_RCPT = Pattern.compile("RCPT TO:<(.*)>", Pattern.CASE_INSENSITIVE);
        private static final Pattern PAT_MAIL_FROM = Pattern.compile("MAIL FROM:<(.*)>", Pattern.CASE_INSENSITIVE);
        
        private void setRejectedRecipient(String rcpt, String error) {
            mRejectRcpt = rcpt;
            mErrorMsg = error;
        }
        
        public String getMailFrom() {
            return mMailFrom;
        }
        
        public List<String> getDataLines() {
            return mDataLines;
        }
        
        public void run() {
            ServerSocket server = null;
            Socket socket = null;
            InputStream in = null;
            try {
                server = new ServerSocket(TEST_SMTP_PORT);
                socket = server.accept();
                in = socket.getInputStream();
                mOut = new PrintWriter(socket.getOutputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line = null;
                
                send("220 " + DummySmtpServer.class.getSimpleName());
                while ((line = reader.readLine()) != null) {
                    String uc = line.toUpperCase();
                    if (uc.startsWith("MAIL FROM")) {
                        Matcher m = PAT_MAIL_FROM.matcher(line);
                        if (m.matches()) {
                            mMailFrom = m.group(1);
                        }
                        send("250 OK");
                    } else if (uc.startsWith("DATA")) {
                        send("354 OK");
                        line = reader.readLine();
                        while (!line.equals(".")) {
                            mDataLines.add(line);
                            line = reader.readLine();
                        }
                        send("250 OK");
                    } else if (uc.startsWith("QUIT")) {
                        send("221 Buh-bye.");
                        break;
                    } else if (uc.startsWith("RCPT")){
                        Matcher m = PAT_RCPT.matcher(line);
                        if (m.matches() && m.group(1).equals(mRejectRcpt)) {
                            send("550 " + mErrorMsg);
                        } else {
                            send("250 OK");
                        }
                    } else {
                        send("250 OK");
                    }
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Error in %s.", DummySmtpServer.class.getSimpleName(), e);
            } finally {
                try {
                    if (mOut != null) {
                        mOut.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                    if (server != null) {
                        server.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void send(String response) {
            mOut.print(response + "\r\n");
            mOut.flush();
        }
    }
    
    public void setUp()
    throws Exception {
        mOriginalSmtpPort = Provisioning.getInstance().getLocalServer().getSmtpPortAsString();
        mOriginalSmtpSendPartial = TestUtil.getServerAttr(Provisioning.A_zimbraSmtpSendPartial);
        mOriginalAllowAnyFrom = TestUtil.getAccountAttr(SENDER_NAME, Provisioning.A_zimbraAllowAnyFromAddress);
    }
    
    // XXX bburtin: commenting out test until we're back to using our own SMTP client
    public void xtestRejectRecipient()
    throws Exception {
        String errorMsg = "Sender address rejected: User unknown in relay recipient table";
        String bogusAddress = TestUtil.getAddress("bogus");
        startDummySmtpServer(bogusAddress, errorMsg);
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSmtpPort(TEST_SMTP_PORT);

        String content = TestUtil.getTestMessage(NAME_PREFIX + " testRejectSender", bogusAddress, SENDER_NAME, null);
        MimeMessage msg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        Mailbox mbox = TestUtil.getMailbox(SENDER_NAME);

        // Test reject first recipient, get partial send value from LDAP.
        boolean sendFailed = false;
        server.setSmtpSendPartial(false);
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_ABORTED_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        assertTrue(sendFailed);
        
        // Test reject first recipient, set partial send value explicitly.
        startDummySmtpServer(bogusAddress, errorMsg);
        sendFailed = false;
        server.setSmtpSendPartial(true);
        MailSender sender = mbox.getMailSender().setIgnoreFailedAddresses(false);
        
        try {
            sender.sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_ABORTED_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        assertTrue(sendFailed);
        
        // Test reject second recipient, get partial send value from LDAP.
        startDummySmtpServer(bogusAddress, errorMsg);
        sendFailed = false;
        String validAddress = TestUtil.getAddress(RECIPIENT_NAME);
        InternetAddress[] recipients = new InternetAddress[2];
        recipients[0] = new InternetAddress(validAddress);
        recipients[1] = new InternetAddress(bogusAddress);
        msg.setRecipients(MimeMessage.RecipientType.TO, recipients);
        server.setSmtpSendPartial(false);
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_ABORTED_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        assertTrue(sendFailed);
        
        // Test partial send, get value from LDAP.
        startDummySmtpServer(bogusAddress, errorMsg);
        server.setSmtpSendPartial(true);
        sendFailed = false;
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        assertTrue(sendFailed);
        
        // Test partial send, specify value explicitly.
        server.setSmtpSendPartial(false);
        startDummySmtpServer(bogusAddress, errorMsg);
        server.setSmtpSendPartial(true);
        sendFailed = false;
        sender = mbox.getMailSender().setIgnoreFailedAddresses(true);
        try {
            sender.sendMimeMessage(null, mbox, msg);
        } catch (MailServiceException e) {
            validateException(e, MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE, bogusAddress, errorMsg);
            sendFailed = true;
        }
        assertTrue(sendFailed);
    }
    
    public void testRestrictEnvelopeSender()
    throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSmtpPort(TEST_SMTP_PORT);
        
        Mailbox mbox = TestUtil.getMailbox(SENDER_NAME);
        Account account = mbox.getAccount();

        // Create a message with a different From header value.
        String from = TestUtil.getAddress("testRestrictEnvelopeSender");
        String subject = NAME_PREFIX + " testRestrictEnvelopeSender";
        MessageBuilder builder = new MessageBuilder().withFrom(from).withRecipient(RECIPIENT_NAME)
            .withSubject(subject).withBody("Who are you?");
        String content = builder.create();
        MimeMessage msg = new FixedMimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));

        account.setSmtpRestrictEnvelopeFrom(true);

        // Restrict envelope sender, disallow custom from.
        account.setAllowAnyFromAddress(false);
        DummySmtpServer smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        assertEquals(account.getName(), smtp.getMailFrom()); 
        // Test contains to handle personal name
        assertTrue(getHeaderValue(smtp.getDataLines(), "From").contains(account.getName()));
        
        // Restrict envelope sender, allow custom from.
        msg = new FixedMimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        account.setAllowAnyFromAddress(true);
        smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        assertEquals(account.getName(), smtp.getMailFrom()); 
        assertEquals(from, getHeaderValue(smtp.getDataLines(), "From"));
        
        account.setSmtpRestrictEnvelopeFrom(false);

        // Don't restrict envelope sender, disallow custom from.
        account.setAllowAnyFromAddress(false);
        smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        assertEquals(account.getName(), smtp.getMailFrom()); 
        assertTrue(getHeaderValue(smtp.getDataLines(), "From").contains(account.getName()));
        
        // Don't restrict envelope sender, allow custom from.
        msg = new FixedMimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        account.setAllowAnyFromAddress(true);
        smtp = startDummySmtpServer(null, null);
        mbox.getMailSender().sendMimeMessage(null, mbox, msg);
        assertEquals(from, smtp.getMailFrom()); 
        assertEquals(from, getHeaderValue(smtp.getDataLines(), "From"));
    }
    
    private String getHeaderValue(List<String> dataLines, String headerName) {
        if (dataLines == null) {
            return null;
        }
        
        Pattern pat = Pattern.compile(headerName + ":\\s+(.*)");
        for (String line : dataLines) {
            Matcher m = pat.matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }
    
    private DummySmtpServer startDummySmtpServer(String rejectedRecipient, String errorMsg) {
        DummySmtpServer smtp = new DummySmtpServer();
        smtp.setRejectedRecipient(rejectedRecipient, errorMsg);
        Thread smtpServerThread = new Thread(smtp);
        smtpServerThread.start();
        return smtp;
    }
    
    private void validateException(MailServiceException e, String expectedCode, String invalidRecipient, String errorSubstring) {
        assertEquals(expectedCode, e.getCode());
        
        boolean foundRecipient = false;
        for (Argument arg : e.getArgs()) {
            if (arg.mName.equals("invalid")) {
                assertEquals(invalidRecipient, arg.mValue);
                foundRecipient = true;
            }
        }
        assertTrue(foundRecipient);
    }
    
    public void tearDown()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraSmtpPort, mOriginalSmtpPort);
        TestUtil.setServerAttr(Provisioning.A_zimbraSmtpSendPartial, mOriginalSmtpSendPartial);
        TestUtil.setAccountAttr(SENDER_NAME, Provisioning.A_zimbraAllowAnyFromAddress, mOriginalAllowAnyFrom);
    }
    
    public static void main(String[] args)
    throws Exception {
        // Simply starts the test SMTP server for ad-hoc testing.  The test needs
        // to run inside the mailbox server.
        DummySmtpServer smtp = new DummySmtpServer();
        if (args.length >= 2) {
            smtp.setRejectedRecipient(args[0], args[1]);
        }
        Thread thread = new Thread(smtp, DummySmtpServer.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }
}
