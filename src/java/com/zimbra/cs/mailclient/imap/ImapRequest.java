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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.util.TraceOutputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Date;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ImapRequest {
    private final ImapConnection connection;
    private final String tag;
    private final Atom cmd;
    private List<Object> params;
    private ResponseHandler handler;

    public ImapRequest(ImapConnection connection, Atom cmd) {
        this.connection = connection;
        this.tag = connection.newTag();
        this.cmd = cmd;
    }

    public ImapRequest(ImapConnection connection, Atom cmd, Object... params) {
        this(connection, cmd);
        for (Object param : params) {
            addParam(param);
        }
    }

    public void addParam(Object data) {
        if (params == null) {
            params = new ArrayList<Object>();
        }
        params.add(data);
    }

    public void setResponseHandler(ResponseHandler handler) {
        this.handler = handler;
    }
    
    public String getTag() { return tag; }
    public Atom getCommand() { return cmd; }
    public List<Object> getParams() { return params; }
    public ResponseHandler getResponseHandler() { return handler; }
    
    public boolean isAuthenticate() {
        return CAtom.AUTHENTICATE.atom().equals(cmd);
    }
    
    public ImapResponse send() throws IOException {
        return connection.sendRequest(this);
    }

    public ImapResponse sendCheckStatus() throws IOException {
        ImapResponse res = send();
        checkStatus(res);
        return res;
    }

    public void checkStatus(ImapResponse res) throws IOException {
        if (!res.isTagged()) {
             throw new MailException("Expected a tagged response");
         }
         if (!tag.equalsIgnoreCase(res.getTag())) {
             throw new MailException(
                 "Unexpected tag in response(expected " + tag + " but got " +
                 res.getTag() + ")");
         }
         if (!res.isOK()) {
             throw new CommandFailedException(
                 cmd.getName(), res.getResponseText().getText());
         }
    }

    public void write(ImapOutputStream out) throws IOException {
        out.write(tag);
        out.write(' ');
        out.write(cmd.getName());
        if (params != null && params.size() > 0) {
            out.write(' ');
            if (cmd.getCAtom() == CAtom.LOGIN && params.size() > 1) {
                writeData(out, params.get(0));
                out.write(' ');
                writeUntracedList(
                    out, params.subList(1, params.size()), "<password>");
            } else {
                writeList(out, params);
            }
        }
        out.newLine();
        out.flush();
    }

    private void writeUntracedList(ImapOutputStream out, List<Object> data,
                                   String msg) throws IOException {
        TraceOutputStream os = connection.getTraceOutputStream();
        if (os != null && os.suspendTrace(msg)) {
            try {
                writeList(out, data);
            } finally {
                os.resumeTrace();
            }
        } else {
            writeList(out, data);
        }
    }
    
    private void writeData(ImapOutputStream out, Object data)
        throws IOException {
        if (data instanceof String) {
            String s = (String) data;
            out.write(s.length() > 0 ? s : "\"\"");
        } else if (data instanceof Atom) {
            ((Atom) data).write(out);
        } else if (data instanceof Quoted) {
            ((Quoted) data).write(out);
        } else if (data instanceof Literal) {
            connection.writeLiteral((Literal) data);
        } else if (data instanceof Flags) {
            ((Flags) data).write(out);
        } else if (data instanceof MailboxName) {
            String encoded = ((MailboxName) data).encode();
            writeData(out, ImapData.asAString(encoded));
        } else if (data instanceof Date) {
            writeData(out, new Quoted(toInternalDate((Date) data)));
        } else if (data instanceof Object[]) {
            writeData(out, Arrays.asList((Object[]) data));
        } else if (data instanceof List) {
            out.write('(');
            writeList(out, (List) data);
            out.write(')');
        } else {
            writeData(out, data.toString());
        }
    }

    // Format is dd-MMM-yyyy HH:mm:ss Z
    private String toInternalDate(Date date) {
        return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z").format(date);
    }
    
    private void writeList(ImapOutputStream out, List list)
        throws IOException {
        Iterator it = list.iterator();
        if (it.hasNext()) {
            writeData(out, it.next());
            while (it.hasNext()) {
                out.write(' ');
                writeData(out, it.next());
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(tag);
        sb.append(' ').append(cmd);
        if (params != null) sb.append(" <data>");
        return sb.toString();
    }
}
