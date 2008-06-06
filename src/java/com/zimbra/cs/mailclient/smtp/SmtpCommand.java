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

import com.zimbra.cs.mailclient.MailOutputStream;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;

public class SmtpCommand {
    private final String name;
    private final List<String> params;

    public static final String HELO = "HELO";
    public static final String EHLO = "EHLO";
    public static final String MAIL = "MAIL";
    public static final String RCPT = "RCPT";
    public static final String DATA = "DATA";
    public static final String RSET = "RSET";
    public static final String VRFY = "VRFY";
    public static final String EXPN = "EXPN";
    public static final String NOOP = "NOOP";
    public static final String HELP = "HELP";
    public static final String QUIT = "QUIT";

    public static SmtpCommand helo(String domain) {
        return new SmtpCommand(HELO, Arrays.asList(domain));
    }

    public static SmtpCommand ehlo(String domain) {
        return new SmtpCommand(EHLO, Arrays.asList(domain));
    }
    
    public static SmtpCommand mail(String from, String... mailParams) {
        List<String> params = new ArrayList<String>();
        params.add("FROM:" + address(from));
        if (mailParams != null) {
            params.addAll(Arrays.asList(mailParams));
        }
        return new SmtpCommand(MAIL, params);
    }

    public static SmtpCommand rcpt(String to, String... rcptParams) {
        List<String> params = new ArrayList<String>();
        params.add("TO:" + address(to));
        if (rcptParams != null) {
            params.addAll(Arrays.asList(rcptParams));
        }
        return new SmtpCommand(RCPT, params);
    }
    
    public static SmtpCommand data() {
        return new SmtpCommand(DATA, null);
    }
    
    public static SmtpCommand rset() {
        return new SmtpCommand(RSET, null);
    }
    
    public static SmtpCommand vrfy(String arg) {
        return new SmtpCommand(VRFY, Arrays.asList(arg));
    }
    
    public static SmtpCommand expn(String arg) {
        return new SmtpCommand(EXPN, Arrays.asList(arg));
    }
    
    public static SmtpCommand noop(String... params) {
        return new SmtpCommand(NOOP, Arrays.asList(params));
    }

    public static SmtpCommand help(String... params) {
        return new SmtpCommand(HELP, Arrays.asList(params));
    }

    public static SmtpCommand quit() {
        return new SmtpCommand(QUIT, null);
    }

    private static String address(String addr) {
        return !addr.startsWith("<") && !addr.endsWith(">") ?
               "<" + addr + ">" : addr;
    }
    
    public SmtpCommand(String name, List<String> params) {
        this.name = name;
        this.params = params;
    }

    public void write(MailOutputStream os) throws IOException {
        os.write(name);
        if (params != null && !params.isEmpty()) {
            for (String param : params) {
                os.write(' ');
                os.write(param);
            }
        }
        os.newLine();
    }
}
