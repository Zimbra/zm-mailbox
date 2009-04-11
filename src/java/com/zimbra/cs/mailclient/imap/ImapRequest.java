/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.util.TraceOutputStream;
import com.zimbra.cs.mailclient.util.DateUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Date;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
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
        if (data == null) {
            throw new NullPointerException();
        }
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
             throw failed(res.getResponseText().getText());
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
        } else if (data instanceof IDInfo) {
            writeData(out, ((IDInfo) data).toRequestParam());
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
    private static String toInternalDate(Date date) {
        return DateUtil.toImapDateTime(date);
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

    public CommandFailedException failed(String error) {
        return failed(error, null);
    }
    
    public CommandFailedException failed(String error, Throwable cause) {
        CommandFailedException cfe = new CommandFailedException(cmd.getName(), error);
        try {
            cfe.setRequest(toString());
        } catch (Exception e) {
        }
        cfe.initCause(cause);
        return cfe;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(tag).append(' ').append(cmd);
        if (params != null) {
            if (cmd.getCAtom() == CAtom.LOGIN && params.size() > 1) {
                sb.append(' ');
                append(sb, params.get(0));
                sb.append(" <password>");
            } else {
                for (Object param : params) {
                    sb.append(' ');
                    append(sb, param);
                }
            }
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, Object param) {
        if (param instanceof String) {
            String s = (String) param;
            sb.append(s.length() > 0 ? s : "\"\"");
        } else if (param instanceof Quoted) { 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ((Quoted) param).write(baos);
            } catch (IOException e) {
                throw new AssertionError();
            }
            sb.append(baos.toString());
        } else if (param instanceof Literal) {
            sb.append("<literal ");
            sb.append(((Literal) param).getSize());
            sb.append(" bytes>");
        } else if (param instanceof MailboxName) {
            String encoded = ((MailboxName) param).encode();
            append(sb, ImapData.asAString(encoded));
        } else if (param instanceof IDInfo) {
            append(sb, ((IDInfo) param).toRequestParam());
        } else if (param instanceof Date) {
            append(sb, new Quoted(toInternalDate((Date) param)));
        } else if (param instanceof Object[]) {
            append(sb, Arrays.asList((Object[]) param));
        } else if (param instanceof List) {
            sb.append('(');
            Iterator it = ((List) param).iterator();
            if (it.hasNext()) {
                append(sb, it.next());
                while (it.hasNext()) {
                    sb.append(' ');
                    append(sb, it.next());
                }
            }
            sb.append(')');
        } else { // Atom, Flags, Object 
            sb.append(param.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        Date date = new Date();
        System.out.println("new = " + toInternalDate(date));
    }
}
