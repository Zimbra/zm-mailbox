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

import java.io.IOException;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePartDataSource;

public class ZMimeMultipart extends MimeMultipart {
    private static final boolean ZPARSER = ZMimeMessage.ZPARSER;

    String implicitBoundary;
    private boolean complete = true;

    public ZMimeMultipart() {
        super();
    }

    public ZMimeMultipart(String subtype) {
        super(subtype);
    }

    public ZMimeMultipart(DataSource ds) throws MessagingException {
        super(ds);
    }

    @SuppressWarnings("unchecked")
    ZMimeMultipart(MimeMultipart source, ZContentType ctype, ZMimePart container) throws MessagingException {
        super();
        assert ZPARSER : "should not clone multipart when our parser is not active";

        this.contentType = ctype.toString();
        this.complete = false;
        this.parent = container;

        String defaultType = ctype.getSubType().equals("digest") ? ZContentType.MESSAGE_RFC822 : ZContentType.TEXT_PLAIN;

        setPreamble(source.getPreamble());
        for (int i = 0, count = source.getCount(); i < count; i++) {
            MimeBodyPart part = (MimeBodyPart) source.getBodyPart(i);
            parts.add(new ZMimeBodyPart(part, new ZContentType(part.getContentType(), defaultType), this));
        }
        this.complete = source.isComplete();
    }

    static ZMimeMultipart newMultipart(ZContentType ctype, ZMimePart container, boolean markParsed) {
        ZMimeMultipart multi;
        try {
            multi = markParsed ? new ZMimeMultipart() : new ZMimeMultipart(new MimePartDataSource(container));
        } catch (MessagingException me) {
            // can never happen, since MimePartDataSource is not a MultipartDataSource
            return null;
        }
        multi.contentType = ZInternetHeader.unfold(ctype.toString());
        multi.complete = false;

        multi.setParent(container);
        if (container instanceof ZMimeMessage) {
            ((ZMimeMessage) container).cacheContent(multi);
        } else if (container instanceof ZMimeBodyPart) {
            ((ZMimeBodyPart) container).cacheContent(multi);
        }
        return multi;
    }

    @Override
    protected synchronized void parse() throws MessagingException {
        if (ZPARSER) {
            if (parsed)
                return;

            try {
                ZMimeParser.parseMultipart(this, ds.getInputStream());
            } catch (IOException e) {
                throw new MessagingException("No inputstream from datasource", e);
            }
            parsed = true;
        } else {
            super.parse();
        }
    }

    @SuppressWarnings("unchecked")
    ZMimeMultipart addBodyPart(ZMimeBodyPart mp) {
        parts.add(mp);
        return this;
    }

    @Override
    public synchronized boolean isComplete() throws MessagingException {
        return !ZPARSER ? super.isComplete() : complete;
    }

    void markComplete() {
        complete = true;
    }

    @Override
    public synchronized void setPreamble(String preamble) {
        try {
            super.setPreamble(preamble);
        } catch (MessagingException e) {
            // superclass method does not actually throw this exception
        }
    }

    String getBoundary() {
        return implicitBoundary != null ? implicitBoundary : new ZContentType(contentType).getParameter("boundary");
    }
}
