/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.mime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.zimbra.common.util.ByteUtil;

public class MimeBodyPart extends MimePart {

    private ContentTransferEncoding mEncoding, mTargetEncoding;

    public MimeBodyPart(ContentType ctype) {
        super(ctype != null ? ctype : new ContentType("text/plain"));
        mEncoding = mTargetEncoding = ContentTransferEncoding.BINARY;
    }

    MimeBodyPart(ContentType ctype, MimePart parent, long start, long body, MimeHeaderBlock headers) {
        super(ctype, parent, start, body, headers);
        mEncoding = mTargetEncoding = ContentTransferEncoding.forString(getMimeHeader("Content-Transfer-Encoding"));
    }


    @Override void removeChild(MimePart mp)  {}


    @Override void checkContentType(ContentType ctype) {
        if (ctype != null && (ctype.getPrimaryType().equals("multipart") || ctype.getValue().equals(ContentType.MESSAGE_RFC822)))
            throw new UnsupportedOperationException("cannot change a message to text");
    }

    @Override public void setContentType(ContentType ctype) {
        if (ctype == null)
            ctype = new ContentType(ContentType.TEXT_PLAIN);
        checkContentType(ctype);
        super.setContentType(ctype);
    }

    public ContentTransferEncoding getTransferEncoding() {
        return mTargetEncoding;
    }

    public void setTransferEncoding(ContentTransferEncoding cte) {
        setMimeHeader("Content-Transfer-Encoding", cte == null ? null : cte.toString());
        mTargetEncoding = cte == null ? ContentTransferEncoding.BINARY : cte;
    }


    @Override public InputStream getRawContentStream() throws IOException {
        InputStream stream = super.getRawContentStream();
        if (mEncoding.normalize() != mTargetEncoding.normalize()) {
            // decode the raw version if necessary
            if (mEncoding == ContentTransferEncoding.BASE64)
                stream = new ContentTransferEncoding.Base64DecoderStream(stream);
            else if (mEncoding == ContentTransferEncoding.QUOTED_PRINTABLE)
                stream = new ContentTransferEncoding.QuotedPrintableDecoderStream(stream);
            // encode to the target encoding if necessary
            if (mTargetEncoding == ContentTransferEncoding.BASE64)
                stream = new ContentTransferEncoding.Base64EncoderStream(stream);
            else if (mTargetEncoding == ContentTransferEncoding.QUOTED_PRINTABLE)
                stream = new ContentTransferEncoding.QuotedPrintableEncoderStream(stream, getContentType());
        }
        return stream;
    }

    @Override public byte[] getRawContent() throws IOException {
        if (mEncoding.normalize() == mTargetEncoding.normalize())
            return super.getRawContent();
        else
            return ByteUtil.getContent(getRawContentStream(), -1);
    }

    @Override public InputStream getContentStream() throws IOException {
        InputStream raw = super.getContentStream();
        if (mEncoding == ContentTransferEncoding.BASE64)
            return new ContentTransferEncoding.Base64DecoderStream(raw);
        else if (mEncoding == ContentTransferEncoding.QUOTED_PRINTABLE)
            return new ContentTransferEncoding.QuotedPrintableDecoderStream(raw);
        else
            return raw;
    }

    @Override public byte[] getContent() throws IOException {
        // certain encodings mean that the decoded content is the same as the raw content
        if (mEncoding.normalize() == ContentTransferEncoding.BINARY)
            return super.getRawContent();
        else
            return ByteUtil.getContent(getContentStream(), (int) (getSize() * (mEncoding == ContentTransferEncoding.BASE64 ? 0.75 : 1.0)));
    }

    public Reader getTextReader() throws IOException {
        InputStream is = getContentStream();

        String charset = getContentType().getParameter("charset");
        if (charset != null) {
            try {
                return new InputStreamReader(is, HeaderUtils.normalizeCharset(charset));
            } catch (UnsupportedEncodingException e) { }
        }

        // if we're here, either there was no explicit charset or it was invalid, so try the default charset
        String defaultCharset = getDefaultCharset();
        if (defaultCharset != null && !defaultCharset.trim().equals("")) {
            try {
                return new InputStreamReader(is, HeaderUtils.normalizeCharset(defaultCharset));
            } catch (UnsupportedEncodingException e) { }
        }

        // if we're here, the default charset was also either unspecified or unavailable, so go with the JVM's default charset
        return new InputStreamReader(is);
    }

    public String getText() throws IOException {
        StringBuilder buffer = new StringBuilder();
        Reader reader = getTextReader();
        try {
            char[] cbuff = new char[8192];
            int num;
            while ((num = reader.read(cbuff, 0, cbuff.length)) != -1)
                buffer.append(cbuff, 0, num);
        } finally {
            reader.close();
        }
        return buffer.toString();
    }

    /** Changes the <tt>Content-Type</tt> of the part to <tt>text/plain</tt>
     *  and sets the part's content to the given text using the default
     *  charset.
     * @throws UnsupportedEncodingException */
    public MimeBodyPart setText(String text) throws UnsupportedEncodingException {
        return setText(text, null, null, null);
    }

    public MimeBodyPart setText(String text, String charset, String subtype, ContentTransferEncoding cte) throws UnsupportedEncodingException {
        // default the subtype and charset appropriately
        ContentType ctype = getContentType();
        if (subtype == null || subtype.trim().equals(""))
            subtype = ctype.getSubType();
        if (charset == null || charset.trim().equals(""))
            charset = ctype.getParameter("charset");
        if (charset == null || charset.trim().equals(""))
            charset = getDefaultCharset();
        if (charset == null || charset.trim().equals(""))
            charset = "utf-8";

        if (getParent() != null)
            getParent().markDirty(true);
        setContentType(ctype.setValue("text/" + subtype).setParameter("charset", charset));

        byte[] content = (text == null ? "" : text).getBytes(charset);
        if (cte == null) {
            // determine an appropriate Content-Transfer-Encoding if none was mandated
            int encodeable = 0, toolong = 0, column = 0;
            for (int i = 0, length = content.length; i < length; i++) {
                byte octet = content[i];
                if (octet >= 0x7F || (octet < 0x20 && octet != '\t' && octet != '\r' && octet != '\n'))
                    encodeable++;
                if (octet == '\n') {
                    if (column > 998)  toolong++;
                    column = 0;
                } else {
                    column++;
                }
            }
            if (encodeable == 0 && toolong == 0)
                cte = ContentTransferEncoding.SEVEN_BIT;
            else if (encodeable < content.length / 4)
                cte = ContentTransferEncoding.QUOTED_PRINTABLE;
            else
                cte = ContentTransferEncoding.BASE64;
        }

        setContent(content);
        mEncoding = (cte == ContentTransferEncoding.BINARY ? ContentTransferEncoding.BINARY : ContentTransferEncoding.EIGHT_BIT);
        mTargetEncoding = cte;
        return this;
    }


    @Override MimeBodyPart readContent(ParseState pstate) throws IOException {
        PeekAheadInputStream pais = pstate.getInputStream();
        List<String> boundaries = getActiveBoundaries();
        pstate.clearBoundary();

        // if there's no pending multipart, we can just consume the remainder of the input
        if (boundaries == null) {
            byte[] buffer = new byte[8192];
            while (pais.read(buffer) > -1)
                ;
            recordEndpoint(pais.getPosition());
            return this;
        }

        int boundarymax = 0;
        for (String boundary : boundaries)
            boundarymax = Math.max(boundarymax, boundary.length());

        long linestart = pais.getPosition();
        int c;
        do {
            // XXX: Boyer-Moore would be more efficient, but its backwards model doesn't mesh easily with the forwards-only InputStream model

            // we're at the start of a line
            if ((c = pais.read()) == '-' && (c = pais.read()) == '-') {
                // found a leading "--", so check to see if the rest of the line matches an active MIME boundary
                if (checkBoundary(pais, pstate, boundaries, linestart)) {
                    recordEndpoint(linestart);
                    return this;
                }
            }

            // skip to the end of the line
            do {
                if (c == '\n' || c == '\r' || c == -1) {
                    linestart = pais.getPosition() - 1;
                    if (c == '\r' && pais.peek() == '\n')
                        pais.read();
                    break;
                }
            } while ((c = pais.read()) != -1);
        } while (c != -1);

        recordEndpoint(pais.getPosition());
        return this;
    }

    static boolean checkBoundary(byte[] content, int offset, ParseState pstate, List<String> boundaries, long linestart) throws IOException {
        return checkBoundary(new ByteArrayInputStream(content, offset, content.length - offset), pstate, boundaries, linestart);
    }

    static boolean checkBoundary(InputStream is, ParseState pstate, List<String> boundaries, long linestart) throws IOException {
        // found a leading "--", so check to see if the rest of the line matches an active MIME boundary
        is.mark(1024);

        for (int i = 0; i < boundaries.size(); i++) {
            String boundary = boundaries.get(i);
            // FIXME: should be doing boundary autodetect if boundary is unset
            if (boundary == MimeMultipart.UNSET_BOUNDARY)
                continue;

            int pos = 0, bndlen = boundary.length(), c;
            // RFC 2046 5.1.1: "Boundary delimiters must not appear within the encapsulated material, and
            //                  must be no longer than 70 characters, not counting the two leading hyphens."
            if (bndlen > 512)
                continue;

            while (pos < bndlen && boundary.charAt(pos) == (c = is.read()))
                pos++;
            // if the line matched the boundary, the part's content may be done!
            if (pos == bndlen) {
                boolean isEndBoundary = (c = is.read()) == '-' && (c = is.read()) == '-';
                if (isEndBoundary)
                    c = is.read();
                // anything trailing must be whitespace
                do {
                    if (c == '\n' || c == '\r' || c == -1) {
                        if (c == '\r') {
                            is.mark(1);
                            if (is.read() != '\n')
                                is.reset();
                        }
                        pstate.recordBoundary(boundary, isEndBoundary, linestart);
                        return true;
                    } else if (!Character.isWhitespace(c)) {
                        break;
                    }
                } while ((c = is.read()) != -1);
            }

            if (i != boundaries.size() - 1)
                is.reset();
        }

        return false;
    }
}
