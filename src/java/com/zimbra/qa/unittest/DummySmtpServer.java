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

package com.zimbra.qa.unittest;

import java.io.BufferedReader;
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

import com.zimbra.common.util.ZimbraLog;

public class DummySmtpServer
implements Runnable
{
    private int mPort;
    private String mRejectRcpt;
    private String mErrorMsg;
    private PrintWriter mOut;
    private List<String> mDataLines = new ArrayList<String>();
    private String mMailFrom;
    private static final Pattern PAT_RCPT = Pattern.compile("RCPT TO:<(.*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_MAIL_FROM = Pattern.compile("MAIL FROM:<(.*)>", Pattern.CASE_INSENSITIVE);

    public DummySmtpServer(int port) {
        mPort = port;
    }
    
    void setRejectedRecipient(String rcpt, String error) {
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
            server = new ServerSocket(mPort);
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