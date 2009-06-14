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

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.JMSession;

public class TestMailSender
extends TestCase {

    private static final int TEST_SMTP_PORT = 6025;
    private static final String NAME_PREFIX = TestMailSender.class.getSimpleName();
    private static final String SENDER_NAME = "user1";
    private static final String RECIPIENT_NAME = "user2";
    private String mOriginalSmtpPort = null;
    
    private static class SmtpRejectSender
    implements Runnable
    {
        private PrintWriter mOut;
        
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
                
                send("220 " + SmtpRejectSender.class.getSimpleName());
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("EHLO")) {
                        send("500 Use HELO instead");
                    } else if (line.startsWith("HELO")) {
                        send("250 OK");
                    } else if (line.startsWith("MAIL FROM")) {
                        send("250 OK");
                    } else if (line.startsWith("RSET")) {
                        send("250 OK");
                    } else if (line.startsWith("QUIT")) {
                        send("221 Buh-bye.");
                        break;
                    } else {
                        // Reject sender
                        assertTrue("Unexpected line: " + line, line.startsWith("RCPT TO"));
                        send("550 Sender address rejected: User unknown in relay recipient table");
                    }
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Error in %s.", SmtpRejectSender.class.getSimpleName(), e);
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
    }
    
    public void testRejectSender()
    throws Exception {
        Thread smtpServerThread = new Thread(new SmtpRejectSender());
        smtpServerThread.start();
        Provisioning.getInstance().getLocalServer().setSmtpPort(TEST_SMTP_PORT);
        
        String content = TestUtil.getTestMessage(NAME_PREFIX + " testRejectSender", RECIPIENT_NAME, SENDER_NAME, null);
        MimeMessage msg = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(content.getBytes()));
        Mailbox mbox = TestUtil.getMailbox(SENDER_NAME);
        
        boolean sendFailed = false;
        try {
            mbox.getMailSender().sendMimeMessage(null, mbox, msg, null, null, null, null, null, false, false);
        } catch (MailServiceException e) {
            assertEquals(e.getCode(), MailServiceException.SEND_ABORTED_ADDRESS_FAILURE);
            String errorMsg = e.getMessage();
            assertTrue("Unexpected error message", errorMsg.contains("Sender address rejected"));
            sendFailed = true;
        }
        assertTrue(sendFailed);
    }
    
    public void tearDown()
    throws Exception {
        Provisioning.getInstance().getLocalServer().setSmtpPortAsString(mOriginalSmtpPort);
    }
    
    public static void main(String[] args)
    throws Exception {
        // Simply starts the test SMTP server for ad-hoc testing.  The test needs
        // to run inside the mailbox server.
        Thread thread = new Thread(new SmtpRejectSender());
        thread.start();
    }
}
