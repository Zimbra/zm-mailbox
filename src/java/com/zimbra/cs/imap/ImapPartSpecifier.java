/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;

class ImapPartSpecifier {
    static class BinaryDecodingException extends Exception { }

    private String mCommand;
    private String mPart;
    private String mModifier;
    private List   mHeaders;
    private int    mOctetStart = -1, mOctetEnd = -1;

    ImapPartSpecifier(String cmd, String part, String modifier) {
        mCommand = cmd;  mPart = part;  mModifier = modifier;
    }
    ImapPartSpecifier(String cmd, String part, String modifier, int start, int count) {
        mCommand = cmd;  mPart = part;  mModifier = modifier;  setPartial(start, count);
    }

    void setPartial(int start, int count) {
        if (start >= 0 && count >= 0) {
            mOctetStart = start;  mOctetEnd = start + count;
        }
    }

    boolean isEntireMessage()  { return mPart.equals("") && mModifier.equals(""); }

    ImapPartSpecifier setHeaders(List headers)  { mHeaders = headers;  return this; }
    private String[] getHeaders() {
        if (mHeaders == null || mHeaders.isEmpty())
            return NO_HEADERS;
        String[] headers = new String[mHeaders.size()];
        for (int i = 0; i < mHeaders.size(); i++)
            headers[i] = (String) mHeaders.get(i);
        return headers;
    }

    private int getOctetStart(byte[] content) {
        return (mOctetStart == -1 ? 0 : Math.min(mOctetStart, content.length));
    }
    private int getOctetEnd(byte[] content) {
        return (mOctetEnd == -1 ? content.length : Math.min(mOctetEnd, content.length));
    }

    private static final String[] NO_HEADERS = new String[0];

    public String toString() {
        StringBuffer response = new StringBuffer(mCommand);
        if (mCommand.equals("BODY") || mCommand.equals("BINARY")) {
            response.append('[').append(mPart).append(mPart.equals("") || mModifier.equals("") ? "" : ".").append(mModifier);
            if (mHeaders != null) {
                boolean first = true;  response.append(" (");
                for (Iterator it = mHeaders.iterator(); it.hasNext(); first = false)
                    response.append(first ? "" : " ").append(((String) it.next()).toUpperCase());
                response.append(')');
            }
            response.append(']');
            // 6.4.5: "BODY[]<0.2048> of a 1500-octet message will return
            //         BODY[]<0> with a literal of size 1500, not BODY[]."
            if (mOctetStart != -1)
                response.append('<').append(mOctetStart).append('>');
        }
        return response.toString();
    }

    void write(PrintStream ps, OutputStream os, byte[] content) throws IOException {
        ps.print(this);  ps.write(' ');

        if (content == null)
            ps.print("NIL");
        else if (mCommand.equals("BINARY.SIZE"))
            ps.print(content.length);
        else {
            int start = getOctetStart(content);
            int end   = getOctetEnd(content);
            boolean binary = mCommand.startsWith("BINARY") ? hasNULs(content, start, end) : false;
            ps.print(binary ? "~{" : "{");  ps.print(end - start);  ps.write('}');
            if (os != null) {
                os.write(ImapHandler.LINE_SEPARATOR_BYTES);  os.write(content, start, end - start);
            }
        }
    }

    void write(PrintStream ps, byte[] content) {
        ps.print(this);  ps.write(' ');

        if (content == null)
            ps.print("NIL");
        else if (mCommand.equals("BINARY.SIZE"))
            ps.print(content.length);
        else {
            int start = getOctetStart(content);
            int end   = getOctetEnd(content);
            boolean binary = mCommand.startsWith("BINARY") ? hasNULs(content, start, end) : false;
            ps.print(binary ? "~{" : "{");  ps.print(end - start);  ps.write('}');
            ps.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, ImapHandler.LINE_SEPARATOR_BYTES.length);
            ps.write(content, start, end - start);
        }
    }

    void write(PrintStream ps, OutputStream os, MimeMessage mm) throws IOException, BinaryDecodingException {
        write(ps, os, getContent(mm));
    }

    void write(PrintStream ps, MimeMessage mm) throws BinaryDecodingException {
        write(ps, getContent(mm));
    }

    private boolean hasNULs(byte[] buffer, int start, int end) {
        if (buffer == null)
            return false;
        for (int i = start; i < end; i++)
            if (buffer[i] == 0)
                return true;
        return false;
    }

    private static final byte[] NO_CONTENT = new byte[0];

    private byte[] getContent(MimeMessage msg) throws BinaryDecodingException {
        try {
            MimePart mp = Mime.getMimePart(msg, mPart);
            if (mp == null)
                return null;
            // TEXT and HEADER* modifiers operate on rfc822 messages
            if ((mModifier.equals("TEXT") || mModifier.startsWith("HEADER")) && !(mp instanceof MimeMessage)) {
                Object content = Mime.getMessageContent(mp);
                if (!(content instanceof MimeMessage))
                    return null;
                mp = (MimeMessage) content;
            }
            // get the content of the requested part
            if (mModifier.equals("")) {
                if (mp instanceof MimeBodyPart) {
                    if (mCommand.startsWith("BINARY"))
                        try {
                            return ByteUtil.getContent(((MimeBodyPart) mp).getInputStream(), -1);
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    else
                        return ByteUtil.getRawContent((MimeBodyPart) mp);
                } else if (mp instanceof MimeMessage) {
                    if (mCommand.startsWith("BINARY"))
                        try {
                            return ByteUtil.getContent(((MimeMessage) mp).getInputStream(), mp.getSize());
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    else
                        return ByteUtil.getContent(((MimeMessage) mp).getRawInputStream(), mp.getSize());
                }
                ZimbraLog.imap.debug("getting content of part; not MimeBodyPart: " + this);
                return NO_CONTENT;
            } else if (mModifier.startsWith("HEADER")) {
                MimeMessage mm = (MimeMessage) mp;
                Enumeration headers;
                if (mModifier.equals("HEADER"))              headers = mm.getAllHeaderLines();
                else if (mModifier.equals("HEADER.FIELDS"))  headers = mm.getMatchingHeaderLines(getHeaders());
                else                                         headers = mm.getNonMatchingHeaderLines(getHeaders());
                StringBuffer result = new StringBuffer();
                while (headers.hasMoreElements())
                    result.append(headers.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                return result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
            } else if (mModifier.equals("MIME")) {
                Enumeration mime = mp.getAllHeaderLines();
                StringBuffer result = new StringBuffer();
                while (mime.hasMoreElements())
                    result.append(mime.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                return result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
            } else if (mModifier.equals("TEXT")) {
                MimeMessage mm = (MimeMessage) mp;
                return ByteUtil.getContent(mm.getRawInputStream(), mp.getSize());
            }
            return null;
        } catch (IOException e) {
            return null;
        } catch (MessagingException e) {
            return null;
        }
    }
}
