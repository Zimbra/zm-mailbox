/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.zmime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.ZimbraLog;

public class ZMimeMessage extends MimeMessage implements ZMimePart {
    static final boolean ZPARSER = LC.javamail_zparser.booleanValue();

    public static boolean usingZimbraParser() {
        return ZPARSER;
    }

    protected long size;
    protected int lines;

    public ZMimeMessage(Session session) {
        super(session);
        if (ZPARSER) {
            this.headers = new ZInternetHeaders().setParent(this);
            this.size = -1;
            this.lines = -1;
        }
    }

    public ZMimeMessage(Session session, InputStream is) throws MessagingException {
        super(session, is);
    }

    public ZMimeMessage(MimeMessage source) throws MessagingException {
        super(source instanceof ZMimeMessage ? ((ZMimeMessage) source).getSession() : Session.getDefaultInstance(new Properties()));

        this.size = -1;
        this.lines = -1;

        boolean copied = false;
        if (ZPARSER) {
            try {
                ZContentType ctype = new ZContentType(source.getContentType());

                this.flags = source.getFlags();
                if (source instanceof ZMimeMessage) {
                    ZMimeMessage zsrc = (ZMimeMessage) source;
                    this.session = zsrc.session;
                    if (zsrc.headers instanceof ZInternetHeaders) {
                        this.headers = new ZInternetHeaders((ZInternetHeaders) zsrc.headers).setParent(this);
                    } else {
                        this.headers = ZInternetHeaders.copyHeaders(this);
                    }
                    this.content = zsrc.content;
                    this.contentStream = zsrc.contentStream;
                    this.saved = zsrc.saved;
                    this.size = zsrc.size;
                    this.lines = zsrc.lines;
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

        if (!copied) {
            // fall back to making an in-memory copy and re-parsing it
            flags = source.getFlags();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(source.getSize(), 1024));
            try {
                source.writeTo(bos);
                parse(new SharedByteArrayInputStream(bos.toByteArray()));
            } catch (IOException ex) {
                // should never happen, but just in case...
                throw new MessagingException("IOException while copying message", ex);
            }
        }

        saved = true;
    }

    static ZMimeMessage newMessage(Session session, ZMimePart container) {
        ZMimeMessage mm = new ZMimeMessage(session);
        mm.modified = false;

        if (container instanceof ZMimeMessage) {
            ((ZMimeMessage) container).cacheContent(mm);
        } else if (container instanceof ZMimeBodyPart) {
            ((ZMimeBodyPart) container).cacheContent(mm);
        }

        return mm;
    }

    @Override
    protected void parse(InputStream is) throws MessagingException {
        if (ZPARSER) {
            this.headers = new ZInternetHeaders().setParent(this);
            try {
                new ZMimeParser(this, session, is).parse();
            } catch (IOException ioex) {
                throw new MessagingException("IOException", ioex);
            }
            this.modified = false;
        } else {
            super.parse(is);
        }
    }

    @Override
    public Charset defaultCharset() {
        return session == null ? null : CharsetUtil.toCharset(session.getProperty("mail.mime.charset"));
    }

    public void cacheContent(Object o) {
        dh = new DataHandler(o, null);
        boolean placeholder = content == null && contentStream == null;
        if (placeholder) {
            content = ZMimeBodyPart.CONTENT_PLACEHOLDER;
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

    public Session getSession() {
        return session;
    }

    public ZMimeMessage setSession(Session s) {
        this.session = s;
        return this;
    }

    public ZMimeMessage setProperty(String key, String value) {
        if (session == null && value != null) {
            this.session = Session.getInstance(new Properties());
        }
        Properties props = session == null ? null : session.getProperties();
        if (props != null) {
            if (value != null) {
                props.setProperty(key, value);
            } else {
                props.remove(key);
            }
        }
        return this;
    }

    @Override
    public void endPart(SharedInputStream sis, long partSize, int lineCount) {
        this.contentStream = (InputStream) sis;
        this.size = partSize;
        this.lines = lineCount;
    }

    @Override
    public int getSize() throws MessagingException {
        return !ZPARSER || modified || size < 0 ? super.getSize() : (int) size;
    }

    @Override
    public int getLineCount() throws MessagingException {
        return !ZPARSER || modified || lines < 0 ? super.getLineCount() : lines;
    }

    @Override
    public synchronized void setDataHandler(DataHandler dh) throws MessagingException {
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
    public Address[] getFrom() throws MessagingException {
        if (ZPARSER) {
            Address[] a = getAddressHeader("From");
            return a == null ? getAddressHeader("Sender") : a;
        } else {
            return super.getFrom();
        }
    }

    @Override
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        if (ZPARSER && type != RecipientType.NEWSGROUPS) {
            return getAddressHeader(getHeaderName(type));
        } else {
            return super.getRecipients(type);
        }
    }

    private String getHeaderName(Message.RecipientType type) throws MessagingException {
        if (type == Message.RecipientType.TO) {
            return "To";
        } else if (type == Message.RecipientType.CC) {
            return "Cc";
        } else if (type == Message.RecipientType.BCC) {
            return "Bcc";
        } else if (type == MimeMessage.RecipientType.NEWSGROUPS) {
            return "Newsgroups";
        } else {
            throw new MessagingException("Invalid Recipient Type");
        }
    }

    @Override
    public Address[] getReplyTo() throws MessagingException {
        if (ZPARSER) {
            Address[] a = getAddressHeader("Reply-To");
            return a == null || a.length == 0 ? getFrom() : a;
        } else {
            return super.getReplyTo();
        }
    }

    @Override
    public Address getSender() throws MessagingException {
        if (ZPARSER) {
            Address[] a = getAddressHeader("Sender");
            return a == null || a.length == 0 ? null : a[0];
        } else {
            return super.getSender();
        }
    }

    private Address[] getAddressHeader(String name) throws MessagingException {
        String s = getHeader(name, ",");
        try {
            return s == null ? null : InternetAddress.parseHeader(MimeUtility.unfold(s), false);
        } catch (RuntimeException re) {
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("Problem parsing Internet Address Header [%s:%s] %s", name, s, re.getMessage());
            }
            return null;
        }
    }

    @Override
    public String getSubject() throws MessagingException {
        if (ZPARSER) {
            String rawvalue = getHeader("Subject", null);
            return rawvalue == null ? null : ZInternetHeader.decode(rawvalue);
        } else {
            return super.getSubject();
        }
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

    @Override
    public String getEncoding() {
        try {
            return ZMimeBodyPart.sanitizeEncoding(super.getEncoding());
        } catch (MessagingException e) {
            return "binary";
        }
    }

    @Override
    public String getFileName() throws MessagingException {
        return !ZPARSER ? super.getFileName() : ZMimeBodyPart.getFileName(this);
    }

    @Override
    protected void updateHeaders() throws MessagingException {
        if (ZPARSER) {
            ZMimeBodyPart.recalculateTransferEncoding(this);
        }
        super.updateHeaders();
    }

    // FIXME: add getMessageStream() to return entire message stream
}
