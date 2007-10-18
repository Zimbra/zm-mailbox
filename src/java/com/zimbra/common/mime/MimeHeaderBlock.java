/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MimeHeaderBlock implements Iterable<MimeHeaderBlock.MimeHeader> {
    private final List<MimeHeader> mHeaders;

    public MimeHeaderBlock(boolean isMessage) {
        mHeaders = new ArrayList<MimeHeader>(isMessage ? 20 : 5);
    }

    public MimeHeaderBlock(MimeHeaderBlock headers) {
        mHeaders = new ArrayList<MimeHeader>(headers.mHeaders);
    }

    /** Returns the value of the first header matching the given
     *  <tt>name</tt>. */
    public String getHeader(String name, String defaultCharset) {
        for (MimeHeader hdr : mHeaders) {
            if (hdr.getName().equalsIgnoreCase(name))
                return hdr.getValue(defaultCharset);
        }
        return null;
    }

    /** Returns the raw (byte array) value of the first header matching
     *  the given <tt>name</tt>. */
    public byte[] getRawHeader(String name) {
        for (MimeHeader hdr : mHeaders) {
            if (hdr.getName().equalsIgnoreCase(name))
                return hdr.getRawHeader();
        }
        return null;
    }

    public Iterator<MimeHeader> iterator() {
        return mHeaders.iterator();
    }

    String validateFieldName(String name) {
        // FIXME: need a sanity-check that more closely parallels the 2822 ABNF
        if (name == null)
            return null;
        name = name.trim();
        if (name.equals(""))
            return null;
        return name;
    }

    public void setHeader(String name, MimeHeader header) {
        if ((name = validateFieldName(name)) == null)
            return;
        for (Iterator<MimeHeader> it = mHeaders.iterator(); it.hasNext(); ) {
            if (it.next().getName().equalsIgnoreCase(name))
                it.remove();
        }
        addHeader(name, header);
    }

    public void addHeader(String name, MimeHeader header) {
        if ((name = validateFieldName(name)) == null)
            return;
        if (header != null)
            mHeaders.add(header);
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

    @Override public String toString()  { return new String(toByteArray()); }

    public MimeHeaderBlock parse(MimePart.PeekAheadInputStream pais) throws IOException {
        StringBuilder name = new StringBuilder(25);
        ByteArrayOutputStream content = new ByteArrayOutputStream(80);
        int c;
        do {
            name.setLength(0);  content.reset();
    
            // read the field name
            for (c = pais.read(); c != -1; c = pais.read()) {
                content.write(c);
                if (c == ':' || c == '\n' || c == '\r') {
                    if (c == '\r' && pais.peek() == '\n')
                        content.write(pais.read());
                    break;
                }
                name.append((char) c);
            }
    
            // FIXME: need to check to see if a MIME boundary appears in the middle of the headers!
    
            if (c != ':') {
                // check for the CRLF CRLF that terminates the headers
                if (name.length() == 0)
                    break;
                // no colon, so abort now rather than reading more data
                continue;
            }
            int valuestart = content.size();  boolean colon = true;
    
            // read the field value, including extra lines from folding
            for (c = pais.read(); c != -1; c = pais.read()) {
                content.write(c);
                if (c == ' ' && colon) {
                    // if there's a space after the colon, the value starts after the space
                    valuestart++;
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && pais.peek() == '\n') 
                        content.write(pais.read());
                    if (pais.peek() != ' ' && pais.peek() != '\t')
                        break;
                }
                colon = false;
            }
    
            // if the name was valid, save the header to the hash
            String key = name.toString().trim();
            if (!key.equals(""))
                mHeaders.add(new MimeHeader(key, content.toByteArray(), valuestart));
        } while (c != -1);

        return this;
    }


    static class MimeHeader {
        private final String mName;
        private final byte[] mContent;
        private final int mValueStart;

        /** Constructor for pre-analyzed header line read from message source.
         * @param name    Header field name.
         * @param content Complete raw header line, with folding and trailing CRLF
         *                and 2047-encoding intact.
         * @param start   The position within <code>content</code> where the header
         *                field value begins (after the ":"/": "). */
        MimeHeader(String name, byte[] content, int start) {
            mName = name;  mContent = content;  mValueStart = start;
        }

        /** Constructor for new header lines.  Header will be serialized as
         *  <tt>{name}: {value}CRLF</tt>.  <i>Note: No folding or 2047-encoding
         *  is done at present.</i> */
        MimeHeader(String name, String value) {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            byte[] bname = name.getBytes(), bvalue = value.getBytes();
            content.write(bname, 0, bname.length);  content.write(':');  content.write(' ');
            int start = content.size();
            content.write(bvalue, 0, bvalue.length);  content.write('\r');  content.write('\n');

            mName = name;  mContent = content.toByteArray();  mValueStart = start;
        }

        MimeHeader(String name, MimeCompoundHeader mch) {
            mName = name;  mContent = mch.toString(name).getBytes();  mValueStart = mName.length() + 2;
        }

        String getName()       { return mName; }
        byte[] getRawHeader()  { return mContent; }
        String getValue()      { return unfold(new String(mContent, mValueStart, mContent.length - mValueStart)); }
        String getValue(String charset) {
            if (charset == null || charset.equals(""))
                return getValue();
            try {
                return unfold(new String(mContent, mValueStart, mContent.length - mValueStart, charset));
            } catch (UnsupportedEncodingException e) {
                return getValue();
            }
        }
        private String unfold(final String folded) {
            int length = folded.length();
            StringBuilder unfolded = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                char c = folded.charAt(i);
                if (c != '\r' && c != '\n')
                    unfolded.append(c);
            }
            return unfolded.toString();
        }

        @Override public String toString()  { return new String(mContent); }
    }
}