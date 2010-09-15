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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class MimeHeaderBlock implements Iterable<MimeHeader> {
    private final ArrayList<MimeHeader> mHeaders;

    public MimeHeaderBlock(boolean isMessage) {
        mHeaders = new ArrayList<MimeHeader>(isMessage ? 20 : 5);
    }

    public MimeHeaderBlock(MimeHeaderBlock headers) {
        mHeaders = new ArrayList<MimeHeader>(headers.mHeaders);
    }

    public boolean isEmpty() {
        return mHeaders == null || mHeaders.isEmpty();
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

    @Override public Iterator<MimeHeader> iterator() {
        return mHeaders.iterator();
    }

    String validateFieldName(String name) {
        // FIXME: need a sanity-check that more closely parallels the 2822 ABNF
        if (name != null) {
            name = name.trim();
            if (name.equals("")) {
                return null;
            }
        }
        return name;
    }

    public void setHeader(String name, MimeHeader header) {
        if ((name = validateFieldName(name)) != null) {
            for (Iterator<MimeHeader> it = mHeaders.iterator(); it.hasNext(); ) {
                if (it.next().getName().equalsIgnoreCase(name)) {
                    it.remove();
                }
            }
            addHeader(name, header);
        }
    }

    public void addHeader(String name, MimeHeader header) {
        if ((name = validateFieldName(name)) != null && header != null) {
            mHeaders.add(header);
        }
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        if (mHeaders != null) {
            for (MimeHeader header : mHeaders) {
                byte[] content = header.getRawHeader();
                baos.write(content, 0, content.length);
            }
        }
        baos.write('\r');  baos.write('\n');
        return baos.toByteArray();
    }

    @Override public String toString() {
        return new String(toByteArray());
    }


    public static MimeHeaderBlock parse(InputStream is) throws IOException {
        MimeParser.HeaderParser parser = new MimeParser.HeaderParser();
        for (byte b = (byte) is.read(); b != -1; b = (byte) is.read()) {
            if (!parser.handleByte(b)) {
                break;
            }
        }
        return parser.getHeaders();
    }
}