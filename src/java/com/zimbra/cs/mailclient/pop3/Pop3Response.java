/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailInputStream;

import java.io.IOException;
import java.io.EOFException;

/**
 * POP3 server response message.
 */
public class Pop3Response {
    private final String mCommand;
    private String mStatus;
    private String mMessage;
    private ContentInputStream mContentInputStream;
    
    private static final String OK = "+OK";
    private static final String ERR = "-ERR";
    private static final String CONTINUATION = "+";
    
    private static final String LIST = "LIST";
    private static final String RETR = "RETR";
    private static final String UIDL = "UIDL";

    public static Pop3Response read(String cmd, MailInputStream is)
            throws IOException {
        Pop3Response res = new Pop3Response(cmd);
        res.readResponse(is);
        return res;
    }

    private Pop3Response(String cmd) {
        mCommand = cmd;
    }

    private void readResponse(MailInputStream is) throws IOException {
        String line = is.readLine();
        if (line == null) {
            throw new EOFException("Unexpected end of stream");
        }
        int i = line.indexOf(' ');
        if (i == -1) {
            mStatus = line;
            mMessage = "";
        } else {
            mStatus = line.substring(0, i);
            mMessage = line.substring(i).trim();
        }
        if (isOK() && hasContent(mCommand)) {
            mContentInputStream = new ContentInputStream(is);
        }
    }

    private static boolean hasContent(String cmd) {
        return LIST.equalsIgnoreCase(cmd) || RETR.equalsIgnoreCase(cmd) ||
               UIDL.equalsIgnoreCase(cmd);
    }
    
    public boolean isOK() { return mStatus.equals(OK); }
    public boolean isERR() { return mStatus.equals(ERR); }
    public boolean isContinuation() { return mStatus.equals(CONTINUATION); }

    public String getMessage() { return mMessage; }
        
    public ContentInputStream getContentInputStream() {
        return mContentInputStream;
    }
}
