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
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import com.zimbra.common.localconfig.LC;

public class ZMimeMessage extends MimeMessage implements ZMimePart {
    static final boolean ZPARSER = LC.javamail_zparser.booleanValue();

    public static boolean usingZimbraParser() {
        return ZPARSER;
    }

    protected long size = -1;
    protected int lines = -1;

    public ZMimeMessage(Session session) {
        super(session);
    }

    public ZMimeMessage(Session session, InputStream is) throws MessagingException {
        super(session);
        if (ZPARSER) {
            try {
                new ZMimeParser(this, session, is).parse();
            } catch (IOException ioex) {
                throw new MessagingException("IOException", ioex);
            }
        } else {
            super.parse(is);
        }
        modified = false;
        saved = true;
    }

    public ZMimeMessage(MimeMessage source) throws MessagingException {
        // get the other message's session if we can, since it's not formally exposed
        super(source instanceof ZMimeMessage ? ((ZMimeMessage) source).getSession() : Session.getInstance(new Properties()));

        InputStream is = new SharedByteArrayInputStream(asByteArray(source));
        if (ZPARSER) {
            try {
                // FIXME: alternative clone() method to avoid serializing to byte[]
                new ZMimeParser(this, session, (SharedInputStream) is).parse();
            } catch (IOException ioex) {
                throw new MessagingException("IOException", ioex);
            }
        } else {
            modified = false;
            super.parse(is);
        }
    }

    private static byte[] asByteArray(MimeMessage mm) throws MessagingException {
        try {
            int size = mm.getSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(size > 0 ? size : 1024);
            mm.writeTo(baos);
            return baos.toByteArray();
        } catch (IOException ioe) {
            // should never happen, but just in case...
            throw new MessagingException("IOException while copying message", ioe);
        }
    }

    static ZMimeMessage newMessage(Session session, ZMimePart container) {
        ZMimeMessage mm = new ZMimeMessage(session);
        mm.headers = new ZMimeBodyPart.ZInternetHeaders();
        mm.modified = false;

        if (container instanceof ZMimeMessage) {
            ((ZMimeMessage) container).cacheContent(mm);
        } else if (container instanceof ZMimeBodyPart) {
            ((ZMimeBodyPart) container).cacheContent(mm);
        }

        return mm;
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
    public void addHeaderLine(String line) {
        headers.addHeaderLine(line);
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
        return s == null ? null : InternetAddress.parseHeader(MimeUtility.unfold(s), false);
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
            return s == null ? null : new ZContentDisposition(s).getDisposition();
        } else {
            return super.getDisposition();
        }
    }

    @Override
    public String getEncoding() throws MessagingException {
        return ZMimeBodyPart.sanitizeEncoding(super.getEncoding());
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
