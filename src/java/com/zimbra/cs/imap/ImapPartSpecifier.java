/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.zimbra.cs.mime.Mime;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

class ImapPartSpecifier {
    static class BinaryDecodingException extends Exception { private static final long serialVersionUID = 8158363540973909369L; }

    private String mCommand;
    private String mPart;
    private String mModifier;
    private int    mOctetStart = -1, mOctetEnd = -1;
    private List<String> mHeaders;

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

    ImapPartSpecifier setHeaders(List<String> headers) {
        mHeaders = headers;  return this;
    }
    private String[] getHeaders() {
        if (mHeaders == null || mHeaders.isEmpty())
            return NO_HEADERS;
        String[] headers = new String[mHeaders.size()];
        for (int i = 0; i < mHeaders.size(); i++)
            headers[i] = mHeaders.get(i);
        return headers;
    }

    private static final String[] NO_HEADERS = new String[0];

    @Override public String toString() {
        StringBuilder response = new StringBuilder(mCommand);
        if (mCommand.equals("BODY") || mCommand.equals("BINARY") || mCommand.equals("BINARY.SIZE")) {
            response.append('[');  getSectionPart(response);  response.append(']');
            // 6.4.5: "BODY[]<0.2048> of a 1500-octet message will return
            //         BODY[]<0> with a literal of size 1500, not BODY[]."
            if (mOctetStart != -1)
                response.append('<').append(mOctetStart).append('>');
        }
        return response.toString();
    }

    String getSectionSpec() {
        return getSectionPart(null);
    }

    private String getSectionPart(StringBuilder sb) {
        if (sb == null)
            sb = new StringBuilder();
        sb.append(mPart).append(mPart.equals("") || mModifier.equals("") ? "" : ".").append(mModifier);
        if (mHeaders != null) {
            boolean first = true;  sb.append(" (");
            for (String header : mHeaders) {
                sb.append(first ? "" : " ").append(header.toUpperCase());  first = false;
            }
            sb.append(')');
        }
        return sb.toString();
    }

    void writeMessage(PrintStream ps, OutputStream os, InputStream is, long length) throws IOException, ServiceException {
        if (!isEntireMessage())
            throw ServiceException.FAILURE("called writeMessage on non-toplevel part", null);

        if (mOctetStart < 0) {
            write(ps, os, is, length);
        } else {
            length = Math.max(0, Math.min(length, mOctetEnd) - mOctetStart);
            write(ps, os, ByteUtil.SegmentInputStream.create(is, mOctetStart, mOctetStart + length), length);
        }
    }

    private void write(PrintStream ps, OutputStream os, InputStream is, long length) throws IOException {
        ps.print(this);  ps.write(' ');

        if (is == null) {
            ps.print("NIL");
        } else if (mCommand.equals("BINARY.SIZE")) {
            ps.print(length);
        } else {
            // FIXME: need to check for NUL bytes in message content
            // boolean binary = mCommand.startsWith("BINARY") && hasNULs(content);
            ps.print("{");  ps.print(length);  ps.write('}');
            if (os != null) {
                os.write(ImapHandler.LINE_SEPARATOR_BYTES);  ByteUtil.copy(is, false, os, false);
            }
        }
    }

    void write(PrintStream ps, OutputStream os, MimeMessage mm) throws IOException, BinaryDecodingException {
        write(ps, os, getContent(mm));
    }

    private void write(PrintStream ps, OutputStream os, byte[] content) throws IOException {
        ps.print(this);  ps.write(' ');

        if (content == null) {
            ps.print("NIL");
        } else if (mCommand.equals("BINARY.SIZE")) {
            ps.print(content.length);
        } else {
            boolean binary = mCommand.startsWith("BINARY") && hasNULs(content);
            ps.print(binary ? "~{" : "{");  ps.print(content.length);  ps.write('}');
            if (os != null) {
                os.write(ImapHandler.LINE_SEPARATOR_BYTES);  os.write(content);
            }
        }
    }

    private boolean hasNULs(byte[] buffer) {
        if (buffer == null)
            return false;
        for (int i = 0, end = buffer.length; i < end; i++) {
            if (buffer[i] == '\0')
                return true;
        }
        return false;
    }

    private static final byte[] NO_CONTENT = new byte[0];

    byte[] getContent(MimeMessage msg) throws BinaryDecodingException {
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
                    if (mCommand.startsWith("BINARY")) {
                        try {
                            return getContent(((MimeBodyPart) mp).getInputStream(), -1);
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    } else {
                        return getContent(((MimeBodyPart) mp).getRawInputStream(), mp.getSize());
                    }
                } else if (mp instanceof MimeMessage) {
                    if (mCommand.startsWith("BINARY")) {
                        try {
                            return getContent(((MimeMessage) mp).getInputStream(), mp.getSize());
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    } else {
                        return getContent(((MimeMessage) mp).getRawInputStream(), mp.getSize());
                    }
                }
                ZimbraLog.imap.debug("getting content of part; not MimeBodyPart: " + this);
                return NO_CONTENT;
            } else if (mModifier.startsWith("HEADER")) {
                MimeMessage mm = (MimeMessage) mp;
                Enumeration headers;
                if (mModifier.equals("HEADER"))              headers = mm.getAllHeaderLines();
                else if (mModifier.equals("HEADER.FIELDS"))  headers = mm.getMatchingHeaderLines(getHeaders());
                else                                         headers = mm.getNonMatchingHeaderLines(getHeaders());
                StringBuilder result = new StringBuilder();
                while (headers.hasMoreElements())
                    result.append(headers.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                return result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
            } else if (mModifier.equals("MIME")) {
                Enumeration mime = mp.getAllHeaderLines();
                StringBuilder result = new StringBuilder();
                while (mime.hasMoreElements())
                    result.append(mime.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                return result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
            } else if (mModifier.equals("TEXT")) {
                MimeMessage mm = (MimeMessage) mp;
                return getContent(mm.getRawInputStream(), mp.getSize());
            }
            return null;
        } catch (IOException e) {
            return null;
        } catch (MessagingException e) {
            return null;
        }
    }

    /** Takes an <code>InputStream</code> and reads it into a <tt>byte[]</tt>
     *  array.  If there is a "partial" octet start/length constraint on this
     *  part specifier, it's applied while generating the array. */
    private byte[] getContent(InputStream is, int sizeHint) throws IOException {
        try {
            if (mOctetStart > 0)
                is.skip(mOctetStart);
        } catch (IOException ioe) {
            ByteUtil.closeStream(is);
            throw ioe;
        }
        int limit = mOctetStart < 0 ? -1 : Math.max(0, mOctetEnd - mOctetStart);
        return ByteUtil.getPartialContent(is, limit, sizeHint);
    }
}
