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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MimeHeaderBlock implements Iterable<MimeHeader> {
    private final ArrayList<MimeHeader> mHeaders;

    public MimeHeaderBlock(boolean isMessage) {
        mHeaders = new ArrayList<MimeHeader>(isMessage ? 20 : 5);
    }

    public MimeHeaderBlock(MimeHeaderBlock headers) {
        mHeaders = new ArrayList<MimeHeader>(headers.mHeaders);
    }

    /** Returns the value of the first header matching the given
     *  <tt>name</tt>. */
    public String getHeader(String name) {
        return getHeader(null);
    }

    /** Returns the value of the last header matching the given <tt>name</tt>.
     *  Header content not encoded with RFC 2047 encoded-words or RFC 2231
     *  encoding is decoded using the specified default charset. */
    public String getHeader(String name, String defaultCharset) {
        for (int i = mHeaders.size() - 1; i >= 0; i--) {
            MimeHeader hdr = mHeaders.get(i);
            if (hdr.getName().equalsIgnoreCase(name))
                return hdr.getValue(defaultCharset);
        }
        return null;
    }

    /** Returns the raw (byte array) value of the last header matching
     *  the given <tt>name</tt>. */
    public byte[] getRawHeader(String name) {
        for (int i = mHeaders.size() - 1; i >= 0; i--) {
            MimeHeader hdr = mHeaders.get(i);
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

    public MimeHeaderBlock parse(InputStream is) throws IOException {
        return parse(new MimePart.PeekAheadInputStream(is), null);
    }

    MimeHeaderBlock parse(MimePart.PeekAheadInputStream pais, List<String> boundaries) throws IOException {
        pais.clearBoundary();

        StringBuilder name = new StringBuilder(25);
        ByteArrayOutputStream content = new ByteArrayOutputStream(80);
        int c;
        do {
            long linestart = pais.getPosition();
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

            boolean dashdash = name.length() > 2 && name.charAt(0) == '-' && name.charAt(1) == '-';

            if (c != ':') {
                // check for the CRLF CRLF that terminates the headers
                if (name.length() == 0)
                    break;
                // check for incorrectly-located boundary delimiter
                if (dashdash && MimeBodyPart.checkBoundary(content.toByteArray(), 2, pais, boundaries, linestart))
                    return this;
                // no colon, so abort now rather than reading more data
                continue;
            }
            int valuestart = content.size();  boolean colon = true;

            // read the field value, including extra lines from folding
            boolean folded = false;
            for (c = pais.read(); c != -1; c = pais.read()) {
                content.write(c);
                if (c == ' ' && colon) {
                    // if there's a space after the colon, the value starts after the space
                    valuestart++;
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && pais.peek() == '\n') 
                        content.write(pais.read());
                    // unless the first char on the next line is whitespace (i.e. folding), this header is complete
                    if (pais.peek() != ' ' && pais.peek() != '\t')
                        break;
                    // check for incorrectly-located boundary delimiter
                    if (dashdash && !folded && MimeBodyPart.checkBoundary(content.toByteArray(), 2, pais, boundaries, linestart))
                        return this;
                    folded = true;
                }
                colon = false;
            }

            // check for incorrectly-located boundary delimiter
            if (dashdash && !folded && MimeBodyPart.checkBoundary(content.toByteArray(), 2, pais, boundaries, linestart))
                return this;

            // if the name was valid, save the header to the hash
            String key = name.toString().trim();
            if (!key.equals(""))
                mHeaders.add(new MimeHeader(key, content.toByteArray(), valuestart));

            // FIXME: note that the first multipart/* Content-Type header needs to add a new boundary to the <code>boundaries</code> list
        } while (c != -1);

        return this;
    }
}