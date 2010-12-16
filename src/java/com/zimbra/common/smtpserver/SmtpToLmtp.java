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

package com.zimbra.common.smtpserver;

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

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

public class SmtpToLmtp {

    private int smtpPort;
    private String lmtpHost;
    private int lmtpPort;
    
    public SmtpToLmtp(int smtpPort, String lmtpHost, int lmtpPort) {
        this.smtpPort = smtpPort;
        this.lmtpHost = lmtpHost;
        this.lmtpPort = lmtpPort;
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
    public static void startup(int smtpPort, String lmtpHost, int lmtpPort) {
        SmtpToLmtp server = new SmtpToLmtp(smtpPort, lmtpHost, lmtpPort);
        server.start();
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
        private int numRecipients = 0;
        
        private SmtpHandler(InputStream smtpIn, OutputStream smtpOut) {
            this.smtpIn = smtpIn;
            this.smtpOut = new PrintWriter(smtpOut);
        }

        public void run() {
            InputStream lmtpIn = null;
            PrintWriter lmtpOut = null;
            String ehloSuffix = "";
            
            try {
                send(smtpOut, "220 " + SmtpToLmtp.class.getSimpleName());
                
                while (true) {
                    String command = readLine(smtpIn);
                    String uc = command.toUpperCase();
                    
                    if (uc.startsWith("MAIL FROM")) {
                        // Establish connection with the LMTP server.
                        Socket lmtpSocket = new Socket(lmtpHost, lmtpPort);
                        lmtpIn = lmtpSocket.getInputStream();
                        lmtpOut = new PrintWriter(lmtpSocket.getOutputStream());
                        String response = readLine(lmtpIn);
                        if (!response.startsWith("2")) {
                            ZimbraLog.smtp.error("Unexpected response from LMTP server: " + response);
                            return;
                        }

                        send(lmtpOut, "LHLO" + ehloSuffix);
                        
                        // Skip "250-" and wait for "250 ".
                        String line = "";
                        while (!line.startsWith("250 ")) {
                            line = readLine(lmtpIn);
                        }
                    }
                    
                    if (uc.startsWith("HELO") || uc.startsWith("EHLO")) {
                        if (command.length() >= 6) {
                            ehloSuffix = command.substring(4);
                        }
                        send(smtpOut, "250 " + getClass().getSimpleName());
                    } else if (uc.startsWith("RCPT")) {
                        if (!isNull(lmtpIn)) {
                            rcpt(lmtpIn, lmtpOut, command);
                        }
                    } else if (uc.startsWith("DATA")) {
                        if (!isNull(lmtpIn)) {
                            data(lmtpIn, lmtpOut, command);
                            lmtpIn = null;
                            lmtpOut = null;
                        }
                    } else if (uc.startsWith("QUIT")) {
                        send(smtpOut, "221 2.0.0 Bye");
                        return;
                    } else {
                        if (!isNull(lmtpIn)) {
                            send(lmtpOut, command);
                            String response = readLine(lmtpIn);
                            send(smtpOut, response);
                        }
                    }
                }
            } catch (IOException e) {
                ZimbraLog.smtp.info("", e);
            } finally {
                ByteUtil.closeStream(smtpIn);
                ByteUtil.closeWriter(smtpOut);
                ByteUtil.closeStream(lmtpIn);
                ByteUtil.closeWriter(lmtpOut);
            }
        }
        
        private boolean isNull(InputStream lmtpIn) {
            if (lmtpIn == null) {
                send(smtpOut, "503 need MAIL command");
                return true;
            }
            return false;
        }

        private void rcpt(InputStream lmtpIn, PrintWriter lmtpOut, String line)
        throws IOException {
            send(lmtpOut, line);
            line = readLine(lmtpIn);
            if (line.startsWith("2")) {
                numRecipients++;
            }
            send(smtpOut, line);
        }
        
        private void data(InputStream lmtpIn, PrintWriter lmtpOut, String line)
        throws IOException {
            // Send DATA line.
            send(lmtpOut, line);
            line = readLine(lmtpIn);
            send(smtpOut, line);
            
            if (line.startsWith("3")) {
                // Write content to a temp file.
                File tempFile = File.createTempFile(getClass().getSimpleName(), ".tmp");
                FileOutputStream fileOut = null;
                
                try {
                    fileOut = new FileOutputStream(tempFile);                
                    while (!line.equals(".")) {
                        line = readLine(smtpIn);
                        fileOut.write(line.getBytes());
                        fileOut.write('\r');
                        fileOut.write('\n');
                    }
                } finally {
                    ByteUtil.closeStream(fileOut);
                }
                
                // Start data handler thread to send the data, so that the SMTP connection
                // doesn't block.
                LmtpDataHandler handler = new LmtpDataHandler(tempFile, lmtpIn, lmtpOut, numRecipients);
                Thread thread = new Thread(handler);
                thread.setName(LmtpDataHandler.class.getSimpleName());
                thread.setDaemon(true);
                thread.start();

                send(smtpOut, "250 OK");
            }
        }
        
        /**
         * Sends {@code DATA} to the LMTP server in a separate thread, so
         * that the {@code SmtpHandler} thread doesn't block.  This is necessary for
         * edge cases in calendar and mail filtering, where mail delivery causes another
         * message to be sent to the same mailbox.
         */
        private class LmtpDataHandler
        implements Runnable {
            File tempFile;
            InputStream lmtpIn;
            PrintWriter lmtpOut;
            int numRecipients;
            
            LmtpDataHandler(File tempFile, InputStream lmtpIn, PrintWriter lmtpOut, int numRecipients) {
                this.tempFile = tempFile;
                this.lmtpIn = lmtpIn;
                this.lmtpOut = lmtpOut;
                this.numRecipients = numRecipients;
            }
            
            public void run() {
                InputStream dataIn = null;
                
                try {
                    dataIn = new FileInputStream(tempFile);
                    String line = "";
                    while (line != ".") {
                        line = readLine(dataIn);
                        send(lmtpOut, line);
                    }
                    for (int i = 1; i <= numRecipients; i++) {
                        String response = readLine(lmtpIn);
                        if (!response.startsWith("2")) {
                            ZimbraLog.smtp.warn("Unable to deliver to recpient %d: %s", i, response);
                        }
                    }
                    send(lmtpOut, "QUIT");
                    readLine(lmtpIn);
                } catch (EOFException e) {
                    ZimbraLog.smtp.info("Client disconnected");
                } catch (IOException e) {
                    ZimbraLog.smtp.warn("Error occurred", e);
                } finally {
                    ByteUtil.closeStream(dataIn);
                    tempFile.delete();
                    ByteUtil.closeStream(lmtpIn);
                    ByteUtil.closeWriter(lmtpOut);
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
