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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MimeHeaderBlock implements Iterable<MimeHeader> {
    private final List<MimeHeader> mHeaders;
    private MimePart mParent;

    public MimeHeaderBlock(boolean isMessage) {
        this(isMessage, null);
    }

    public MimeHeaderBlock(boolean isMessage, MimePart parent) {
        mHeaders = new ArrayList<MimeHeader>(isMessage ? 20 : 5);
        mParent = parent;
    }

    public MimeHeaderBlock(MimeHeader... headers) {
        mHeaders = new ArrayList<MimeHeader>(Math.max(headers.length, 5));
        for (MimeHeader header : headers) {
            addHeader(header);
        }
    }

    public MimeHeaderBlock(MimeHeaderBlock headers) {
        mHeaders = new ArrayList<MimeHeader>(headers.mHeaders);
    }

    public MimeHeaderBlock(MimeHeaderBlock headers, String[] omitHeaders) {
        mHeaders = new ArrayList<MimeHeader>(headers.mHeaders.size());
        for (MimeHeader header : headers) {
            boolean present = false;
            for (String name : omitHeaders) {
                if (header.getName().equalsIgnoreCase(name)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                addHeader(header);
            }
        }
    }

    public MimeHeaderBlock(InputStream is) throws IOException {
        MimeParser.HeaderParser parser = new MimeParser.HeaderParser();
        for (int b = is.read(); b != -1; b = is.read()) {
            if (!parser.handleByte((byte) b)) {
                break;
            }
        }
        mHeaders = parser.getHeaders().mHeaders;
    }


    MimeHeaderBlock setParent(MimePart parent) {
        mParent = parent;
        return this;
    }


    /** Returns the decoded value of the last header matching the given
     *  {@code name}, or {@code null} if none match.  If {@code name} is
     *  {@code null}, returns the value of the last header in the block. */
    public String getValue(String name) {
        return getValue(name, null);
    }

    /** Returns the decoded value of the last header matching the given {@code
     *  name}, or {@code null} if none match.  Header content not encoded with
     *  RFC 2047 encoded-words or RFC 2231 encoding is decoded using the
     *  specified default charset.  If {@code name} is {@code null}, returns
     *  the value of the last header in the block. */
    public String getValue(String name, String defaultCharset) {
        MimeHeader header = get(name);
        return header == null ? null : header.getValue(defaultCharset);
    }

    /** Returns the value of the last header matching the given {@code name},
     *  or {@code null} if none match.  No decoding is performed other than
     *  removing the trailing CRLF.  Header content not encoded with RFC 2047
     *  encoded-words or RFC 2231 encoding is decoded using the specified
     *  default charset.  If {@code name} is {@code null}, returns the value
     *  of the last header in the block. */
    public String getEncodedValue(String name, String defaultCharset) {
        MimeHeader header = get(name);
        return header == null ? null : header.getEncodedValue(defaultCharset);
    }

    /** Returns the raw (byte array) value of the last header matching the
     *  given {@code name}, or {@code null} if none match.  If {@code name}
     *  is {@code null}, returns the last header in the block. */
    public byte[] getRawHeader(String name) {
        MimeHeader header = get(name);
        return header == null ? null : header.getRawHeader();
    }

    /** Returns the last header matching the given {@code name}, or {@code
     *  null} if none match.  If {@code name} is {@code null}, returns the
     *  last header in the block. */
    MimeHeader get(String name) {
        for (int i = mHeaders.size() - 1; i >= 0; i--) {
            MimeHeader header = mHeaders.get(i);
            if (name == null || header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    /** Returns all headers matching the given {@code name}, in order of their
     *  appearance in the header block.  If none match, returns {@code null}. */
    public List<MimeHeader> getAll(String name) {
        if (name == null) {
            return isEmpty() ? null : new ArrayList<MimeHeader>(mHeaders);
        }

        List<MimeHeader> matches = new ArrayList<MimeHeader>(2); 
        for (MimeHeader header : mHeaders) {
            if (header.getName().equalsIgnoreCase(name)) {
                matches.add(header.clone());
            }
        }
        return matches.isEmpty() ? null : matches;
    }

    /** Returns whether this header block contains any headers with the given
     *  {@code name}. */
    boolean containsHeader(String name) {
        return get(name) != null;
    }

    private class HeaderIterator implements Iterator<MimeHeader> {
        private final Iterator<MimeHeader> mIterator;
        private MimeHeader mLastReturned;

        HeaderIterator(Iterable<MimeHeader> headers) {
            mIterator = headers.iterator();
        }

        @Override public boolean hasNext() {
            return mIterator.hasNext();
        }

        @Override public MimeHeader next() {
            return mLastReturned = mIterator.next();
        }

        @Override public void remove() {
            announce(mLastReturned.getName(), null);
            mIterator.remove();
            markDirty();
        }
    }

    @Override public Iterator<MimeHeader> iterator() {
        return new HeaderIterator(mHeaders);
    }


    void announce(String hdrName, MimeHeader header) {
        if (mParent != null && hdrName.toLowerCase().equals("content-type")) {
            MimePart container = mParent.getParent();
            String defaultType = container instanceof MimeMultipart ? ((MimeMultipart) container).getDefaultChildContentType() : "text/plain";
            mParent.updateContentType(new ContentType(header, defaultType));
        }
    }

    void markDirty() {
        if (mParent != null) {
            mParent.markDirty(MimePart.Dirty.HEADERS);
        }
    }

    private String validateFieldName(String name) {
        // FIXME: need a sanity-check that more closely parallels the RFC 5322 ABNF
        if (name == null) {
            return name;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public MimeHeaderBlock setHeader(String name, String value) {
        return setHeader(name, value == null ? null : new MimeHeader(name, value));
    }

    public MimeHeaderBlock setHeader(String name, byte[] bvalue) {
        return setHeader(name, bvalue == null ? null : new MimeHeader(name, bvalue));
    }

    @SuppressWarnings("null")
    public MimeHeaderBlock setHeader(String name, MimeHeader header) {
        String key = validateFieldName(name);
        if (header != null && !header.getName().equals(name)) {
            throw new IllegalArgumentException("name does not match header.getName()");
        } else if (key == null) {
            return this;
        }

        boolean dirty = false, replaced = header == null;
        for (int index = 0; index < mHeaders.size(); index++) {
            if (mHeaders.get(index).getName().equalsIgnoreCase(key)) {
                if (!dirty) {
                    announce(name, header);
                    dirty = true;
                }
                if (!replaced) {
                    // replace the first old instance of the header
                    mHeaders.set(index, header.clone());
                    replaced = true;
                } else {
                    // all other old instances are cleared
                    mHeaders.remove(index--);
                }
            }
        }

        if (dirty) {
            markDirty();
        } else {
            // make sure the new value actually does get added
            addHeader(header);
        }
        return this;
    }

    public MimeHeaderBlock addHeader(String name, String value) {
        if (value != null) {
            addHeader(new MimeHeader(name, value));
        }
        return this;
    }

    public MimeHeaderBlock addHeader(String name, byte[] bvalue) {
        if (bvalue != null) {
            addHeader(new MimeHeader(name, bvalue));
        }
        return this;
    }

    public MimeHeaderBlock addHeader(MimeHeader header) {
        if (header != null && validateFieldName(header.getName()) != null) {
            announce(header.getName(), header);
            mHeaders.add(header.clone());
            markDirty();
        }
        return this;
    }

    public MimeHeaderBlock addAll(MimeHeaderBlock headers) {
        for (MimeHeader header : headers) {
            addHeader(header);
        }
        return this;
    }

    MimeHeaderBlock removeHeader(MimeHeader header) {
        for (Iterator<MimeHeader> it = mHeaders.iterator(); it.hasNext(); ) {
            if (it.next() == header) {
                it.remove();
                markDirty();
                break;
            }
        }
        return this;
    }



    public boolean isEmpty() {
        return mHeaders == null || mHeaders.isEmpty();
    }

    public int getLength() {
        int length = 0;
        if (mHeaders != null) {
            for (MimeHeader header : mHeaders) {
                length += header.getRawHeader().length;
            }
        }
        // include the trailing "\r\n" terminating the block
        return length + 2;
    }

    public byte[] toByteArray() {
        byte[] block = new byte[getLength()];
        int offset = 0;
        if (mHeaders != null) {
            for (MimeHeader header : mHeaders) {
                byte[] line = header.getRawHeader();
                System.arraycopy(line, 0, block, offset, line.length);
                offset += line.length;
            }
        }
        // include the trailing "\r\n" terminating the block
        block[offset++] = '\r';  block[offset++] = '\n';
        return block;
    }

    @Override public String toString() {
        return new String(toByteArray());
    }
}
