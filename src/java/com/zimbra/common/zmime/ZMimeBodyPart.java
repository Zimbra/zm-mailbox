/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.common.zmime;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.SharedInputStream;

import com.google.common.collect.ImmutableSet;
import com.sun.mail.util.PropUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

public class ZMimeBodyPart extends MimeBodyPart implements ZMimePart {
    private static final boolean ZPARSER = ZMimeMessage.ZPARSER;

    protected long size = -1;
    protected int lines = -1;

    public ZMimeBodyPart() {
        super();
        if (ZPARSER) {
            this.headers = new ZInternetHeaders().setParent(this);
        }
    }

    ZMimeBodyPart(MimeBodyPart source, ZContentType ctype, ZMimeMultipart container) throws MessagingException {
        boolean copied = false;
        if (ZPARSER) {
            try {
                if (source instanceof ZMimeBodyPart) {
                    ZMimeBodyPart zsrc = (ZMimeBodyPart) source;
                    if (zsrc.headers instanceof ZInternetHeaders) {
                        this.headers = new ZInternetHeaders((ZInternetHeaders) zsrc.headers).setParent(this);
                    } else {
                        this.headers = ZInternetHeaders.copyHeaders(this);
                    }
                    this.content = zsrc.content;
                    this.contentStream = zsrc.contentStream;
                    this.lines = zsrc.lines;
                    this.size = zsrc.size;
                } else {
                    this.headers = ZInternetHeaders.copyHeaders(source);
                    try {
                        InputStream is = source.getRawInputStream();
                        if (is instanceof SharedInputStream) {
                            this.contentStream = is;
                        } else {
                            ByteUtil.closeStream(is);
                        }
                    } catch (MessagingException me) {
                        // "no content"
                    }
                }

                if (ctype.getBaseType().equals("message/rfc822")) {
                    Object obj = source.getContent();
                    if (obj instanceof MimeMessage) {
                        cacheContent(new ZMimeMessage((MimeMessage) obj));
                    }
                } else if (ctype.getPrimaryType().equals("multipart")) {
                    Object obj = source.getContent();
                    if (obj instanceof MimeMultipart) {
                        cacheContent(new ZMimeMultipart((MimeMultipart) obj, ctype, this));
                    }
                } else {
                    if (content == null && contentStream == null) {
                        this.dh = source.getDataHandler();
                    }
                }
                copied = true;
            } catch (Exception e) {
                ZimbraLog.misc.warn("failed cloning " + source.getClass().getSimpleName(), e);
            }
        }

        if (!copied && content == null && contentStream == null) {
            // fall back to making an in-memory copy and re-parsing it
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(source.getSize(), 1024));
            try {
                source.writeTo(bos);
                this.content = bos.toByteArray();
            } catch (IOException ex) {
                // should never happen, but just in case...
                throw new MessagingException("IOException while copying message", ex);
            }
        }

        this.parent = container;
    }

    static ZMimeBodyPart newBodyPart(ZMimeMultipart multi) {
        ZMimeBodyPart mbp = new ZMimeBodyPart();
        if (multi != null) {
            mbp.parent = multi.addBodyPart(mbp);
        }
        return mbp;
    }

    @Override
    public Charset defaultCharset() {
        // walk structure up to MimeMessage or die tryin'
        MimeBodyPart part = this;
        while (part != null) {
            MimeMultipart multi = (MimeMultipart) part.getParent();
            MimePart container = multi == null ? null : (MimePart) multi.getParent();
            if (container instanceof MimeMessage) {
                return container instanceof ZMimeMessage ? ((ZMimeMessage) container).defaultCharset() : null;
            } else {
                part = (MimeBodyPart) container;
            }
        }
        return null;
    }

    static final byte[] CONTENT_PLACEHOLDER = new byte[0];

    public void cacheContent(Object o) {
        dh = new DataHandler(o, null);
        boolean placeholder = content == null && contentStream == null;
        if (placeholder) {
            content = CONTENT_PLACEHOLDER;
        }
        try {
            super.getContent();
        } catch (Exception e) {
            // should never happen
        } finally {
            dh = null;
            if (placeholder) {
                content = null;
            }
        }
    }

    @Override
    public void endPart(SharedInputStream sis, long partSize, int lineCount) {
        this.contentStream = (InputStream) sis;
        this.size = partSize;
        this.lines = lineCount;
    }

    @Override
    public int getSize() throws MessagingException {
        return !ZPARSER || size < 0 ? super.getSize() : (int) size;
    }

    @Override
    public int getLineCount() throws MessagingException {
        return !ZPARSER || lines < 0 ? super.getLineCount() : lines;
    }

    @Override
    public void setDataHandler(DataHandler dh) throws MessagingException {
        size = lines = -1;
        super.setDataHandler(dh);
    }

    @Override
    public void appendHeader(ZInternetHeader header) {
        if (ZPARSER) {
            ((ZInternetHeaders) headers).appendHeader(header);
        } else {
            headers.addHeaderLine(header.toString().trim());
        }
    }

    @Override
    public String getContentType() throws MessagingException {
        String s = getHeader("Content-Type", null);
        if (s == null) {
            if (parent instanceof MimeMultipart && new ZContentType(parent.getContentType()).getSubType().equals("digest")) {
                s = "message/rfc822";
            } else {
                s = "text/plain";
            }
        }
        return s;
    }

    @Override
    public String getDisposition() throws MessagingException {
        if (ZPARSER) {
            String s = getHeader("Content-Disposition", null);
            return s == null || s.isEmpty() ? null : new ZContentDisposition(s).getDisposition();
        } else {
            return super.getDisposition();
        }
    }

    static final Set<String> RAW_ENCODINGS = ImmutableSet.of("7bit", "8bit", "binary");

    private static final Set<String> SUPPORTED_ENCODINGS = ImmutableSet.of("7bit", "8bit", "binary", "base64", "quoted-printable", "uuencode", "x-uuencode", "x-uue");

    @Override
    public String getEncoding() {
        try {
            return sanitizeEncoding(super.getEncoding());
        } catch (MessagingException e) {
            return "binary";
        }
    }

    static String sanitizeEncoding(String enc) {
        if (enc != null) {
            enc = enc.toLowerCase();
        }
        return enc != null && !SUPPORTED_ENCODINGS.contains(enc) ? "binary" : enc;
    }

    @Override
    public String getFileName() throws MessagingException {
        return !ZPARSER ? super.getFileName() : getFileName(this);
    }

    private static boolean decodeFileName = PropUtil.getBooleanSystemProperty("mail.mime.decodefilename", false);

    static String getFileName(ZMimePart part) throws MessagingException {
        String filename = null;

        String s = part.getHeader("Content-Disposition", null);
        if (s != null) {
            // Parse the header ..
            filename = new ZContentDisposition(s).getParameter("filename");
        }

        if (filename == null) {
            // Still no filename ? Try the "name" ContentType parameter
            s = part.getHeader("Content-Type", null);
            if (s != null) {
                filename = new ZContentType(s).getParameter("name");
            }
        }

        if (!decodeFileName && filename != null) {
            // our classes support RFC 2231 so they do the decoding themselves
            //   if that's not what the caller wanted, we go back and re-encode the filename (*sigh*)
            filename = ZInternetHeader.escape(filename, null, false);
        }
        return filename;
    }

    @Override
    protected void updateHeaders() throws MessagingException {
        if (ZPARSER) {
            ZMimeBodyPart.recalculateTransferEncoding(this);
        }
        super.updateHeaders();
    }

    static void recalculateTransferEncoding(ZMimePart part) throws MessagingException {
        // don't overwrite an explicitly-set transfer encoding
        if (part.getHeader("Content-Transfer-Encoding") != null)
            return;

        // don't set transfer encoding on messages or multiparts
        DataHandler dh = part.getDataHandler();
        ZContentType ctype = new ZContentType(dh.getContentType());
        if (ctype.getBaseType().equals("message/rfc822") || ctype.getPrimaryType().equals("multipart"))
            return;

        boolean isText = ctype.getPrimaryType().equals("text");
        String disp = part.getDisposition();
        boolean isAttachment = disp != null && disp.equals(ATTACHMENT);

        String encoding = "base64";
        if (dh.getName() == null) {
            try {
                EncodingOutputStream eos = new EncodingOutputStream();
                dh.writeTo(eos);
                encoding = eos.tester.getEncoding(isAttachment, isText);
            } catch (Exception e) {
            }
        } else {
            EncodingInputStream eis = null;
            try {
                eis = new EncodingInputStream(dh.getDataSource().getInputStream());
                ByteUtil.drain(eis, false);
                encoding = eis.tester.getEncoding(isAttachment, isText);
            } catch (IOException e) {
            } finally {
                ByteUtil.closeStream(eis);
            }
        }

        part.setHeader("Content-Transfer-Encoding", encoding);
    }

    static class EncodingOutputStream extends OutputStream {
        final EncodingTester tester = new EncodingTester();

        @Override
        public void write(int b) throws IOException {
            tester.testByte((byte) (b & 0xFF));
        }
    }

    static class EncodingInputStream extends FilterInputStream {
        final EncodingTester tester = new EncodingTester();

        protected EncodingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                tester.testByte((byte) (b & 0xFF));
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                for (int pos = off, max = off + read; pos < max; pos++) {
                    tester.testByte(b[pos]);
                }
            }
            return read;
        }
    }

    static class EncodingTester {
        /** Maximum number of octets in a line before we force a content transfer
         *  encoding.  Many MTAs will wrap lines over 998 octets. */
        private static final int MAX_LINE_OCTETS = 900;

        private boolean sevenbit = true;
        private int qpencodeable = 0, toolong = 0, length = 0, column = 0, badEOL = 0;
        private byte last = '\0';

        void testByte(byte octet) {
            if (octet >= 0x7F || (octet < 0x20 && octet != '\t' && octet != '\r' && octet != '\n')) {
                // this octet must be encoded if we choose quoted-printable (RFC2045 6.7)
                qpencodeable++;
                // all of these octets except for non-NUL control chars rule out "7bit" (RFC2045 2.7)
                if (sevenbit && (octet <= 0x00 || octet >= 0x7F)) {
                    sevenbit = false;
                }
            }

            if (octet == '\n') {
                if (column > MAX_LINE_OCTETS) {
                    toolong++;
                }
                if (last != '\r') {
                    badEOL++;
                }
                column = 0;
            } else {
                if (last == '\r') {
                    badEOL++;
                }
                column++;
            }

            length++;
            last = octet;
        }

        String getEncoding(boolean isAttachment, boolean isText) {
            if (column > MAX_LINE_OCTETS) {
                toolong++;
            }

            if (badEOL > 0 && (isAttachment || !isText)) {
                return "base64";
            } else if (sevenbit && toolong == 0) {
                return "7bit";
            } else if (qpencodeable < length / 4) {
                return "quoted-printable";
            } else {
                return "base64";
            }
        }
    }
}
