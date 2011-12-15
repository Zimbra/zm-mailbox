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

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

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
        // FIXME: parse the multipart using our parser, not the superclass'
        super(ds);
        parse();
        complete = super.isComplete();
    }

    static ZMimeMultipart newMultipart(ZContentType ctype, ZMimePart container) {
        ZMimeMultipart multi = new ZMimeMultipart();
        multi.contentType = ctype.toString();
        multi.complete = false;

        multi.setParent(container);
        if (container instanceof ZMimeMessage) {
            ((ZMimeMessage) container).cacheContent(multi);
        } else if (container instanceof ZMimeBodyPart) {
            ((ZMimeBodyPart) container).cacheContent(multi);
        }
        return multi;
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
}
