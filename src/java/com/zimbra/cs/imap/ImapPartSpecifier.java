/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

class ImapPartSpecifier {
    static class BinaryDecodingException extends Exception { private static final long serialVersionUID = 8158363540973909369L; }

    /** Maximum size of byte[] buffer held in memory while processing a
     *  <tt>FETCH BINARY</tt> or <tt>FETCH BINARY.SIZE</tt>. */
    static final int MAX_PINNED_BINARY_CONTENT = 100000;

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

    boolean isEntireMessage() {
        return mPart.equals("") && mModifier.equals("");
    }

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
            response.append('[').append(getSectionSpec()).append(']');
            // 6.4.5: "BODY[]<0.2048> of a 1500-octet message will return
            //         BODY[]<0> with a literal of size 1500, not BODY[]."
            if (mOctetStart != -1)
                response.append('<').append(mOctetStart).append('>');
        }
        return response.toString();
    }

    String getCommand() {
        return mCommand;
    }

    String getSectionPart() {
        return mPart;
    }

    String getSectionSpec() {
        StringBuilder sb = new StringBuilder();
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

    void write(PrintStream ps, OutputStream os, Object obj) throws IOException, BinaryDecodingException, ServiceException {
        Pair<Long, InputStream> contents = getContent(obj);
        InputStream is = contents == null ? null : contents.getSecond();
        long length = contents == null ? -1 : contents.getFirst();

        ps.print(this);  ps.write(' ');

        try {
            if (is == null) {
                ps.print("NIL");
            } else if (mCommand.equals("BINARY.SIZE")) {
                ps.print(length >= 0 ? length : NULCheck.getLength(is));
            } else {
                boolean binary = false;
                if (mCommand.startsWith("BINARY")) {
                    NULCheck nul = NULCheck.hasNULs(is, length);
                    if (length < 0)
                        length = nul.length;
                    if (nul.content == null) {
                        // reload the original InputStream
                        is = getContent(obj).getSecond();
                    } else {
                        // use the cached copy
                        is = new ByteArrayInputStream(nul.content);
                    }
                    binary = nul.hasNULs;
                }

                ps.print(binary ? "~{" : "{");  ps.print(length);  ps.write('}');
                if (os != null) {
                    os.write(ImapHandler.LINE_SEPARATOR_BYTES);
                    long written = ByteUtil.copy(is, false, os, false);
                    assert(written == length);
                }
            }
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    Pair<Long, InputStream> getContent(Object obj) throws IOException, BinaryDecodingException, ServiceException {
        Pair<Long, InputStream> contents;
        if (obj instanceof MimeMessage) {
            contents = getContent((MimeMessage) obj);
        } else if (obj instanceof MailItem) {
            if (!isEntireMessage())
                throw ServiceException.FAILURE("called writeMessage on non-toplevel part", null);
            contents = ImapMessage.getContent((MailItem) obj);
        } else {
            throw ServiceException.FAILURE("called write() with unexpected argument: " + (obj == null ? "null" : obj.getClass().getSimpleName()), null);
        }

        if (mOctetStart >= 0 && contents != null) {
            // if there is a "partial" octet start/length constraint on this part specifier, apply it here
            InputStream is = contents.getSecond();
            long statedLength = contents.getFirst();
            long realLength = Math.max(0, Math.min(statedLength < 0 ? Integer.MAX_VALUE : statedLength, mOctetEnd) - mOctetStart);
            try {
                int start = mOctetStart;
                // the JavaMail implementations of the content-transfer decoders don't do skip() correctly
                if (is instanceof com.sun.mail.util.BASE64DecoderStream || is instanceof com.sun.mail.util.QPDecoderStream) {
                    ByteUtil.skip(is, start);
                    start = 0;
                }
                is = ByteUtil.SegmentInputStream.create(is, start, start + realLength);
            } catch (IOException ioe) {
                ByteUtil.closeStream(is);
                throw ioe;
            }
            contents = new Pair<Long, InputStream>(statedLength < 0 ? -1 : realLength, is);
        }
        return contents;
    }

    private static class NULCheck {
        long length;
        byte[] content;
        boolean hasNULs;

        static long getLength(InputStream is) throws BinaryDecodingException {
            return scan(is, -1, false).length;
        }

        static NULCheck hasNULs(InputStream is, long length) throws BinaryDecodingException {
            return scan(is, length, true);
        }

        private static NULCheck scan(InputStream is, long length, boolean checkNULs) throws BinaryDecodingException {
            // we want to (a) avoid rereading the message from disk, but (b) avoid reading a 30GB blob to memory
            //   so we're going to read messages up to a certain length into memory and do the rest by going back to disk
            NULCheck nul = new NULCheck();
            long totalSize = 0;
            ByteArrayOutputStream baos = null;
            if (checkNULs && length <= MAX_PINNED_BINARY_CONTENT)
                baos = new ByteArrayOutputStream(length < 0 ? MAX_PINNED_BINARY_CONTENT / 2 : (int) length);

            byte[] buffer = new byte[8192];
            int bytesRead;
            try {
                while ((bytesRead = is.read(buffer)) >= 0) {
                    // check for NUL bytes
                    if (checkNULs && !nul.hasNULs) {
                        for (int i = 0; i < bytesRead; i++) {
                            if (buffer[i] == '\0') {
                                nul.hasNULs = true;  break;
                            }
                        }
                    }
                    // and copy to memory buffer if we're doing that...
                    if (length < 0) {
                        totalSize += bytesRead;
                        if (totalSize > MAX_PINNED_BINARY_CONTENT && baos != null)
                            baos = null;
                    }
                    if (baos != null)
                        baos.write(buffer, 0, bytesRead);
                    else if (nul.hasNULs && length > 0)
                        break;
                }
            } catch (IOException ioe) {
                throw new BinaryDecodingException();
            } finally {
                ByteUtil.closeStream(is);
            }

            if (baos != null)
                nul.content = baos.toByteArray();
            if (length < 0)
                nul.length = totalSize;
            return nul;
        }
    }

    private Pair<Long, InputStream> getContent(MimeMessage msg) throws BinaryDecodingException {
        long length = -1;
        InputStream is = null;

        try {
            MimePart mp = Mime.getMimePart(msg, mPart);
            if (mp == null)
                return null;
            // TEXT and HEADER* modifiers operate on rfc822 messages
            if ((mModifier.equals("TEXT") || mModifier.startsWith("HEADER")) && !(mp instanceof MimeMessage)) {
                // FIXME: hackaround for JavaMail's failure to handle multipart/digest properly
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
                            is = ((MimeBodyPart) mp).getInputStream();
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    } else {
                        is = ((MimeBodyPart) mp).getRawInputStream();  length = Math.max(0, mp.getSize());
                    }
                } else if (mp instanceof MimeMessage) {
                    if (mCommand.startsWith("BINARY")) {
                        try {
                            is = ((MimeMessage) mp).getInputStream();
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    } else {
                        is = ((MimeMessage) mp).getRawInputStream();  length = Math.max(0, mp.getSize());
                    }
                } else {
                    ZimbraLog.imap.debug("getting content of part; not MimeBodyPart: " + this);
                    return ImapMessage.EMPTY_CONTENT;
                }
            } else if (mModifier.startsWith("HEADER")) {
                MimeMessage mm = (MimeMessage) mp;
                Enumeration<?> headers;
                if (mModifier.equals("HEADER"))              headers = mm.getAllHeaderLines();
                else if (mModifier.equals("HEADER.FIELDS"))  headers = mm.getMatchingHeaderLines(getHeaders());
                else                                         headers = mm.getNonMatchingHeaderLines(getHeaders());
                StringBuilder result = new StringBuilder();
                while (headers.hasMoreElements())
                    result.append(headers.nextElement()).append(ImapHandler.LINE_SEPARATOR);

                byte[] content = result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
                is = new ByteArrayInputStream(content);  length = content.length;
            } else if (mModifier.equals("MIME")) {
                Enumeration<?> mime = mp.getAllHeaderLines();
                StringBuilder result = new StringBuilder();
                while (mime.hasMoreElements())
                    result.append(mime.nextElement()).append(ImapHandler.LINE_SEPARATOR);

                byte[] content = result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
                is = new ByteArrayInputStream(content);  length = content.length;
            } else if (mModifier.equals("TEXT")) {
                is = ((MimeMessage) mp).getRawInputStream();  length = Math.max(0, mp.getSize());
            } else {
                return null;
            }

            return new Pair<Long, InputStream>(length, is);
        } catch (IOException e) {
            ByteUtil.closeStream(is);
            return null;
        } catch (MessagingException e) {
            ByteUtil.closeStream(is);
            return null;
        }
    }
}
