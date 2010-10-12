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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import com.zimbra.common.util.ByteUtil;

public class MimeBodyPart extends MimePart {

    private ContentTransferEncoding mEncoding, mTargetEncoding;

    public MimeBodyPart(ContentType ctype) {
        super(ctype != null ? ctype : new ContentType(ContentType.TEXT_PLAIN));
        mEncoding = mTargetEncoding = ContentTransferEncoding.BINARY;
    }

    MimeBodyPart(ContentType ctype, MimePart parent, long start, long body, MimeHeaderBlock headers) {
        super(ctype, parent, start, body, headers);
        mEncoding = mTargetEncoding = ContentTransferEncoding.forString(getMimeHeader("Content-Transfer-Encoding"));
    }


    @Override void removeChild(MimePart mp)  {}


    @Override ContentType checkContentType(ContentType ctype) {
        if (ctype != null && (ctype.getPrimaryType().equals("multipart") || ctype.getContentType().equals(ContentType.MESSAGE_RFC822))) {
            throw new UnsupportedOperationException("cannot change a message to text");
        }
        return ctype;
    }

    @Override public MimeBodyPart setContentType(ContentType ctype) {
        super.setContentType(checkContentType(ctype == null ? new ContentType(ContentType.TEXT_PLAIN) : ctype));
        return this;
    }

    public ContentTransferEncoding getTransferEncoding() {
        return mTargetEncoding;
    }

    public MimeBodyPart setTransferEncoding(ContentTransferEncoding cte) {
        ContentTransferEncoding newEncoding = cte == null ? ContentTransferEncoding.BINARY : cte;
        if (newEncoding.normalize() != mTargetEncoding.normalize()) {
            markDirty(Dirty.CTE);
        }
        setMimeHeader("Content-Transfer-Encoding", cte == null ? null : cte.toString());
        mTargetEncoding = newEncoding;
        return this;
    }


    @Override public long getSize() throws IOException {
        long size = super.getSize();
        if (size == -1) {
            size = recordSize(ByteUtil.countBytes(getRawContentStream()));
        }
        return size;
    }

    @Override public InputStream getRawContentStream() throws IOException {
        InputStream stream = super.getRawContentStream();
        if (mEncoding.normalize() != mTargetEncoding.normalize()) {
            // decode the raw version if necessary
            if (mEncoding == ContentTransferEncoding.BASE64) {
                stream = new ContentTransferEncoding.Base64DecoderStream(stream);
            } else if (mEncoding == ContentTransferEncoding.QUOTED_PRINTABLE) {
                stream = new ContentTransferEncoding.QuotedPrintableDecoderStream(stream);
            }
            // encode to the target encoding if necessary
            if (mTargetEncoding == ContentTransferEncoding.BASE64) {
                stream = new ContentTransferEncoding.Base64EncoderStream(stream);
            } else if (mTargetEncoding == ContentTransferEncoding.QUOTED_PRINTABLE) {
                stream = new ContentTransferEncoding.QuotedPrintableEncoderStream(stream, getContentType());
            }
        }
        return stream;
    }

    @Override public byte[] getRawContent() throws IOException {
        if (mEncoding.normalize() == mTargetEncoding.normalize()) {
            return super.getRawContent();
        } else {
            return ByteUtil.getContent(getRawContentStream(), -1);
        }
    }

    @Override public InputStream getContentStream() throws IOException {
        InputStream raw = super.getContentStream();
        if (mEncoding == ContentTransferEncoding.BASE64) {
            return new ContentTransferEncoding.Base64DecoderStream(raw);
        } else if (mEncoding == ContentTransferEncoding.QUOTED_PRINTABLE) {
            return new ContentTransferEncoding.QuotedPrintableDecoderStream(raw);
        } else {
            return raw;
        }
    }

    @Override public byte[] getContent() throws IOException {
        // certain encodings mean that the decoded content is the same as the raw content
        if (mEncoding.normalize() == ContentTransferEncoding.BINARY) {
            return super.getRawContent();
        } else {
            return ByteUtil.getContent(getContentStream(), (int) (getSize() * (mEncoding == ContentTransferEncoding.BASE64 ? 0.75 : 1.0)));
        }
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
        if (defaultCharset != null && !defaultCharset.trim().isEmpty()) {
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
            while ((num = reader.read(cbuff, 0, cbuff.length)) != -1) {
                buffer.append(cbuff, 0, num);
            }
        } finally {
            reader.close();
        }
        return buffer.toString();
    }

    /** Changes the <tt>Content-Type</tt> of the part to <tt>text/plain</tt>
     *  and sets the part's content to the given text using the default
     *  charset. */
    public MimeBodyPart setText(String text) throws IOException {
        return setText(text, null, null, null);
    }

    public MimeBodyPart setText(String text, String charset, String subtype, ContentTransferEncoding cte) throws IOException {
        // default the subtype and charset appropriately
        ContentType ctype = getContentType();
        ctype.setContentType("text/" + (subtype == null || subtype.trim().isEmpty() ? ctype.getSubType() : subtype));

        String cset = charset;
        if (cset == null || cset.trim().isEmpty()) {
            cset = ctype.getParameter("charset");
        }
        if (cset == null || cset.trim().isEmpty()) {
            cset = getDefaultCharset();
        }
        if (cset == null || cset.trim().isEmpty()) {
            cset = "utf-8";
        }
        ctype.setParameter("charset", cset);

        setContent((text == null ? "" : text).getBytes(cset), cte);
        return setContentType(ctype);
    }

    public MimeBodyPart setContent(byte[] content) throws IOException {
        return setContent(content, null);
    }

    public MimeBodyPart setContent(byte[] content, ContentTransferEncoding cte) throws IOException {
        return setContent(content == null ? null : new PartSource(content), cte);
    }

    public MimeBodyPart setContent(File file) throws IOException {
        return setContent(file, null);
    }

    public MimeBodyPart setContent(File file, ContentTransferEncoding cte) throws IOException {
        return setContent(file == null || !file.exists() ? null : new PartSource(file), cte);
    }

    private MimeBodyPart setContent(PartSource psource, ContentTransferEncoding cte) throws IOException {
        super.setContent(psource);
        mEncoding = ContentTransferEncoding.BINARY;
        mTargetEncoding = cte == null ? pickEncoding() : cte;
        return this;
    }

    private ContentTransferEncoding pickEncoding() throws IOException {
        int encodeable = 0, toolong = 0, length = 0;

        InputStream is = getRawContentStream();
        if (is != null) {
            try {
                is = is instanceof ByteArrayInputStream || is instanceof BufferedInputStream ? is : new BufferedInputStream(is);
                for (int octet = is.read(), column = 0; octet != -1; octet = is.read()) {
                    if (octet >= 0x7F || (octet < 0x20 && octet != '\t' && octet != '\r' && octet != '\n')) {
                        encodeable++;
                    }
                    if (octet == '\n') {
                        if (column > 998) {
                            toolong++;
                        }
                        column = 0;
                    } else {
                        column++;
                    }
                    length++;
                }
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        if (encodeable == 0 && toolong == 0) {
            return ContentTransferEncoding.SEVEN_BIT;
        } else if (encodeable < length / 4) {
            return ContentTransferEncoding.QUOTED_PRINTABLE;
        } else {
            return ContentTransferEncoding.BASE64;
        }
    }
}
