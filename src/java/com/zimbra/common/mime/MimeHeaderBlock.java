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

public class MimeHeaderBlock implements Iterable<MimeHeader> {
    private final ArrayList<MimeHeader> mHeaders;
    private MimePart mParent;

    public MimeHeaderBlock(boolean isMessage) {
        this(isMessage, null);
    }

    public MimeHeaderBlock(boolean isMessage, MimePart parent) {
        mHeaders = new ArrayList<MimeHeader>(isMessage ? 20 : 5);
        mParent = parent;
    }

    public MimeHeaderBlock(MimeHeaderBlock headers) {
        mHeaders = new ArrayList<MimeHeader>(headers.mHeaders);
    }


    MimeHeaderBlock setParent(MimePart parent) {
        mParent = parent;
        return this;
    }


    /** Returns the value of the last header matching the given
     *  <tt>name</tt>. */
    public String getHeader(String name) {
        return getHeader(name, null);
    }

    /** Returns the value of the last header matching the given <tt>name</tt>.
     *  Header content not encoded with RFC 2047 encoded-words or RFC 2231
     *  encoding is decoded using the specified default charset. */
    public String getHeader(String name, String defaultCharset) {
        for (int i = mHeaders.size() - 1; i >= 0; i--) {
            MimeHeader hdr = mHeaders.get(i);
            if (hdr.getName().equalsIgnoreCase(name)) {
                return hdr.getValue(defaultCharset);
            }
        }
        return null;
    }

    /** Returns the raw (byte array) value of the last header matching
     *  the given <tt>name</tt>. */
    public byte[] getRawHeader(String name) {
        for (int i = mHeaders.size() - 1; i >= 0; i--) {
            MimeHeader hdr = mHeaders.get(i);
            if (hdr.getName().equalsIgnoreCase(name)) {
                return hdr.getRawHeader();
            }
        }
        return null;
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

    public void setHeader(String name, String value) {
        setHeader(name, value == null ? null : new MimeHeader(name, value));
    }

    public void setHeader(String name, byte[] bvalue) {
        setHeader(name, bvalue == null ? null : new MimeHeader(name, bvalue));
    }

    void setHeader(String name, MimeHeader header) {
        String key = validateFieldName(name);
        if (header != null && !header.getName().equals(name)) {
            throw new IllegalArgumentException("name does not match header.getName()");
        } else if (key == null) {
            return;
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
                    mHeaders.set(index, header);
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
    }

    public void addHeader(String name, String value) {
        if (value != null) {
            addHeader(new MimeHeader(name, value));
        }
    }

    public void addHeader(String name, byte[] bvalue) {
        if (bvalue != null) {
            addHeader(new MimeHeader(name, bvalue));
        }
    }

    void addHeader(MimeHeader header) {
        if (header != null && validateFieldName(header.getName()) != null) {
            announce(header.getName(), header);
            mHeaders.add(header);
            markDirty();
        }
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
        block[offset++] = '\r';  block[offset++] = '\n';
        return block;
    }

    @Override public String toString() {
        return new String(toByteArray());
    }


    public static MimeHeaderBlock parse(InputStream is) throws IOException {
        MimeParser.HeaderParser parser = new MimeParser.HeaderParser();
        for (int b = is.read(); b != -1; b = is.read()) {
            if (!parser.handleByte((byte) b)) {
                break;
            }
        }
        return parser.getHeaders();
    }
}