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
package com.zimbra.cs.mailclient.smtp;

import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.ParseException;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public final class SmtpReply {
    private int code;
    private List<String> lines;

    public static SmtpReply read(MailInputStream is) throws IOException {
        SmtpReply reply = new SmtpReply();
        reply.lines = new ArrayList<String>();
        String line;
        do {
            line = is.readLine();
            int code = parseCode(line);
            if (reply.code == 0) {
                reply.code = code;
            } else if (code != reply.code) {
                throw new ParseException("Inconsistent reply code: expected " +
                                         reply.code + " but got " + code);
            }
            if (line.length() > 3) {
                reply.lines.add(line.substring(4));
            }
        } while (line.length() > 3 && line.charAt(3) != ' ');
        return reply;
    }

    private static int parseCode(String line) throws ParseException {
        int i = 0;
        while (i < line.length() && Character.isDigit(line.charAt(i))) {
            i++;
        }
        String s = line.substring(0, i);
        if (i == 3) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        throw new ParseException("Invalid reply code: " + s);
    }
    
    private SmtpReply() {}
    
    public int getCode() {
        return code;
    }

    public List<String> getLines() {
        return lines;
    }
}
