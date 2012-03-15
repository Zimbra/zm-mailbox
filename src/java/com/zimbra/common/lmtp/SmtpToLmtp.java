/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.common.lmtp;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

public class SmtpToLmtp {
    
    private static final Pattern PAT_MAIL_FROM = Pattern.compile("MAIL FROM:<(.*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_RCPT_TO = Pattern.compile("RCPT TO:<(.+)>", Pattern.CASE_INSENSITIVE);
    
    public interface RecipientValidator {
        /**
         * Validates the recipient passed in by the SMTP client.
         * @return one or more valid mailbox addresses, or an empty {@code Iterable}
         * if the recipient is invalid
         */
        public Iterable<String> validate(String recipient);
    }
    
    private static final RecipientValidator DUMMY_VALIDATOR = new RecipientValidator() {
        public Iterable<String> validate(String recipient) {
            return Arrays.asList(recipient);
        }
    };
    
    private class LmtpData {
        String sender;
        List<String> recipients = Lists.newArrayList();
        File file;
    }

    private int smtpPort;
    private String lmtpHost;
    private int lmtpPort;
    private RecipientValidator validator = DUMMY_VALIDATOR;
    
    public SmtpToLmtp(int smtpPort, String lmtpHost, int lmtpPort) {
        this.smtpPort = smtpPort;
        this.lmtpHost = lmtpHost;
        this.lmtpPort = lmtpPort;
    }
    
    public void setRecipientValidator(RecipientValidator validator) {
        assert(validator != null);
        this.validator = validator;
    }
    
    private void run() {
        SmtpServer server = new SmtpServer();
        server.run();
    }
    
    private void start() {
        SmtpServer server = new SmtpServer();
        Thread thread = new Thread(server);
        thread.setName(SmtpServer.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Starts the {@code SmtpServer} thread.
     */
    public static SmtpToLmtp startup(int smtpPort, String lmtpHost, int lmtpPort) {
        SmtpToLmtp server = new SmtpToLmtp(smtpPort, lmtpHost, lmtpPort);
        server.start();
        return server;
    }
    
    private class SmtpServer
    implements Runnable {
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(smtpPort);
                while (true) {
                    Socket smtpSocket = serverSocket.accept();
                    SmtpHandler handler = new SmtpHandler(smtpSocket.getInputStream(), smtpSocket.getOutputStream());
                    Thread thread = new Thread(handler);
                    thread.setName(SmtpHandler.class.getSimpleName());
                    thread.setDaemon(true);
                    thread.start();
                }
            } catch (IOException e) {
                ZimbraLog.smtp.error("", e);
            }
        }
    }
    
    /**
     * Handles an incoming SMTP connection and establishes the outbound
     * LMTP connection.
     */
    private class SmtpHandler
    implements Runnable {
        
        private InputStream smtpIn;
        private PrintWriter smtpOut;
        
        private SmtpHandler(InputStream smtpIn, OutputStream smtpOut) {
            this.smtpIn = smtpIn;
            this.smtpOut = new PrintWriter(smtpOut);
        }

        public void run() {
            LmtpData data = null;
            
            try {
                send(smtpOut, "220 " + SmtpToLmtp.class.getSimpleName());
                
                while (true) {
                    String command = readLine(smtpIn);
                    String uc = command.toUpperCase();

                    Matcher m = PAT_MAIL_FROM.matcher(command);
                    if (m.matches()) {
                        data = new LmtpData();
                        data.sender = m.group(1);
                        send(smtpOut, "250 OK");
                        continue;
                    }
                    
                    m = PAT_RCPT_TO.matcher(command);
                    if (m.matches()) {
                        Iterable<String> validRecipients = validator.validate(m.group(1));
                        if (validRecipients == null) {
                            validRecipients = Collections.emptyList();
                        }
                        Iterables.addAll(data.recipients, validRecipients);
                        
                        if (Iterables.isEmpty(validRecipients)) {
                            send(smtpOut, "550 No such user here");
                        } else {
                            send(smtpOut, "250 OK");
                        }
                    } else if (uc.startsWith("DATA")) {
                        send(smtpOut, "354 Start mail input; end with <CRLF>.<CRLF>");
                        data.file = readData(smtpIn);
                        send(smtpOut, "250 OK");
                        
                        LmtpClientThread thread = new LmtpClientThread(data);
                        thread.start();
                    } else if (uc.startsWith("RSET")) {
                        data = new LmtpData();
                        send(smtpOut, "250 OK");
                    } else if (uc.startsWith("QUIT")) {
                        send(smtpOut, "221 2.0.0 Bye");
                        return;
                    } else {
                        // EHLO or unrecognized command.
                        send(smtpOut, "250 OK");
                    }
                }
            } catch (Throwable t) {
                ZimbraLog.smtp.info("", t);
            } finally {
                ByteUtil.closeStream(smtpIn);
                ByteUtil.closeWriter(smtpOut);
            }
        }
        
        private File readData(InputStream in)
        throws IOException {
            // Write content to a temp file.
            File tempFile = File.createTempFile(getClass().getSimpleName(), ".tmp");
            FileOutputStream fileOut = null;

            try {
                fileOut = new FileOutputStream(tempFile);
                
                while (true) {
                    String line = readLine(smtpIn);
                    if (line.equals(".")) {
                        break;
                    }
                    fileOut.write(line.getBytes());
                    fileOut.write('\r');
                    fileOut.write('\n');
                }
            } catch (Exception e) {
                ByteUtil.closeStream(fileOut);
                tempFile.delete();
            } finally {
                ByteUtil.closeStream(fileOut);
            }
            
            return tempFile;
        }
        
        private class LmtpClientThread
        extends Thread {
            LmtpData data;
            
            LmtpClientThread(LmtpData data) {
                this.data = data;
            }
            
            public void run() {
                InputStream in = null;
                try {
                    LmtpClient client = new LmtpClient(lmtpHost, lmtpPort);
                    in = new FileInputStream(data.file);
                    client.sendMessage(in, data.recipients, data.sender, SmtpToLmtp.class.getSimpleName(), data.file.length());
                } catch (Throwable e) {
                    ZimbraLog.smtp.warn("Error occurred", e);
                } finally {
                    ByteUtil.closeStream(in);
                    data.file.delete();
                }
            }
        }
        
        private void send(PrintWriter out, String response) {
            ZimbraLog.smtp.trace("S: %s", response);
            out.print(response + "\r\n");
            out.flush();
        }

        private String readLine(InputStream in)
        throws IOException {
            StringBuilder buf = new StringBuilder();
            int c;
            while ((c = in.read()) >= 0) {
                if (c == '\n') {
                    if (buf.length() > 0 && buf.charAt(buf.length() - 1) == '\r') {
                        buf.setLength(buf.length() - 1);
                    }
                    break;
                }
                buf.append((char) c);
            }
            if (c < 0) {
                throw new EOFException("Client disconnected");
            }
            ZimbraLog.smtp.trace("C: %s", buf);
            return buf.toString();
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.format("Usage: java %s <SMTP port> <LMTP host> <LMTP port>\n", SmtpToLmtp.class.getName());
        }
        CliUtil.toolSetup();
        SmtpToLmtp server = new SmtpToLmtp(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
        server.run();
    }
}
