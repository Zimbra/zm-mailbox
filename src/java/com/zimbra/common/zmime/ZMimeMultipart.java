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
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.LineOutputStream;

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

    static ZMimeMultipart newMultipart(ZContentType ctype, ZMimePart container) {
        ZMimeMultipart multi = new ZMimeMultipart();
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

    void setDataSource(DataSource ds) {
        this.ds = ds;
        setPreamble(null);
        parts.clear();
        this.parsed = false;
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

    // JavaMail makes it impossible to set MimeMultipart.allowEmpty if you don't use their parser, and that
    // means an empty multipart always throws an exception on MimeMultipart.writeTo regardless of what
    // "mail.mime.multipart.allowempty" is set to.  So we have to copy the whole method from the superclass
    // just so we can strip that conditional out.
    @Override
    public synchronized void writeTo(OutputStream os) throws IOException, MessagingException {
        if (ZPARSER) {
            parse();

            String boundary = "--" + getBoundary();
            LineOutputStream los = new LineOutputStream(os);

            // if there's a preamble, write it out
            String preamble = getPreamble();
            if (preamble != null) {
                byte[] pb = ASCIIUtility.getBytes(preamble);
                los.write(pb);
                // make sure it ends with a newline
                if (pb.length > 0 && !(pb[pb.length-1] == '\r' || pb[pb.length-1] == '\n')) {
                    los.writeln();
                }
                // XXX - could force a blank line before start boundary
            }

            if (parts.size() == 0) {
                // write out a single empty body part
                los.writeln(boundary); // put out boundary
                los.writeln(); // put out empty line
            } else {
                for (int i = 0; i < parts.size(); i++) {
                    los.writeln(boundary); // put out boundary
                    ((MimeBodyPart) parts.elementAt(i)).writeTo(os);
                    los.writeln(); // put out empty line
                }
            }

            // put out last boundary
            los.writeln(boundary + "--");
        } else {
            super.writeTo(os);
        }
    }
}
