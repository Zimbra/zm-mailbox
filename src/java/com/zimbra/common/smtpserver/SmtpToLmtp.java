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
    private boolean debug = false;
    
    public SmtpToLmtp(int smtpPort, String lmtpHost, int lmtpPort) {
        this.smtpPort = smtpPort;
        this.lmtpHost = lmtpHost;
        this.lmtpPort = lmtpPort;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
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
                    Socket lmtpSocket = new Socket(lmtpHost, lmtpPort);
                    SmtpHandler handler = new SmtpHandler(smtpSocket.getInputStream(), smtpSocket.getOutputStream(),
                        lmtpSocket.getInputStream(), lmtpSocket.getOutputStream());
                    Thread thread = new Thread(handler);
                    thread.start();
                }
            } catch (IOException e) {
                ZimbraLog.smtp.error("", e);
            }
        }
    }
    
    private class SmtpHandler
    implements Runnable {
        
        private InputStream smtpIn;
        private PrintWriter smtpOut;
        private InputStream lmtpIn;
        private PrintWriter lmtpOut;
        private int numRecipients = 0;
        
        private SmtpHandler(InputStream smtpIn, OutputStream smtpOut, InputStream lmtpIn, OutputStream lmtpOut) {
            this.smtpIn = smtpIn;
            this.smtpOut = new PrintWriter(smtpOut);
            this.lmtpIn = lmtpIn;
            this.lmtpOut = new PrintWriter(lmtpOut);
        }

        public void run() {
            try {
                String response = readLine(lmtpIn);
                if (!response.startsWith("2")) {
                    ZimbraLog.smtp.error("Unexpected response from LMTP server: " + response);
                    close();
                    return;
                }
                send(smtpOut, "220 " + SmtpToLmtp.class.getSimpleName());
                
                while (true) {
                    String command = readLine(smtpIn);
                    String uc = command.toUpperCase();
                    
                    if (uc.startsWith("HELO") || uc.startsWith("EHLO")) {
                        ehlo(command);
                    } else if (uc.startsWith("RCPT")) {
                        rcpt(command);
                    } else if (uc.startsWith("DATA")) {
                        data(command);
                    } else {
                        send(lmtpOut, command);
                        response = readLine(lmtpIn);
                        send(smtpOut, response);
                    }
                    if (uc.startsWith("QUIT")) {
                        close();
                        return;
                    }
                }
            } catch (IOException e) {
                ZimbraLog.smtp.info("", e);
                e.printStackTrace(System.err);
                close();
            }
        }
        
        private void ehlo(String line)
        throws IOException {
            String suffix = "";
            if (line.length() > 4) {
                suffix = line.substring(4);
            }
            send(lmtpOut, "LHLO" + suffix);
            while (true) {
                line = readLine(lmtpIn);
                send(smtpOut, line);
                if (line.charAt(3) == ' ') {
                    break;
                }
            }
        }
        
        private void rcpt(String line)
        throws IOException {
            send(lmtpOut, line);
            line = readLine(lmtpIn);
            if (line.startsWith("2")) {
                numRecipients++;
            }
            send(smtpOut, line);
        }
        
        private void data(String line)
        throws IOException {
            // Send DATA line.
            send(lmtpOut, line);
            line = readLine(lmtpIn);
            send(smtpOut, line);
            
            if (line.startsWith("3")) {
                // Send content.
                while (!line.equals(".")) {
                    line = readLine(smtpIn);
                    send(lmtpOut, line);
                }
                for (int i = 1; i <= numRecipients; i++) {
                    String response = readLine(lmtpIn);
                    if (!response.startsWith("2")) {
                        ZimbraLog.smtp.warn("Unable to deliver to recpient %d: %s", i, response);
                    }
                }
                send(smtpOut, "250 OK");
            }
        }
        
        private void send(PrintWriter out, String response) {
            if (debug) {
                System.out.println("Sending " + response + " to " + out);
            }
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
            if (debug) {
                System.out.println("Read " + buf.toString() + " from " + in);
            }
            return buf.toString();
        }
        
        private void close() {
            ByteUtil.closeStream(lmtpIn);
            ByteUtil.closeWriter(lmtpOut);
            ByteUtil.closeStream(smtpIn);
            ByteUtil.closeWriter(smtpOut);
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
