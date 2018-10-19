/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.zmime;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimePart;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.zmime.ZMimeUtility.ByteBuilder;

public class ZInternetHeaders extends InternetHeaders {
    private static final boolean ZPARSER = ZMimeMessage.ZPARSER;
    protected List headers;

    private ZMimePart parent;
    private boolean ordered = true;

    public ZInternetHeaders() {
        this.headers = new ArrayList<ZInternetHeader>(5);
    }

    @SuppressWarnings("unchecked")
    public ZInternetHeaders(ZInternetHeaders hblock) {
        this.headers = new ArrayList<ZInternetHeader>(hblock.headers);
    }

    static ZInternetHeaders copyHeaders(MimePart part) throws MessagingException {
        ZInternetHeaders zhdrs = new ZInternetHeaders();
        for (@SuppressWarnings("unchecked") Enumeration<String> en = part.getAllHeaderLines(); en.hasMoreElements(); ) {
            zhdrs.addHeaderLine(en.nextElement());
        }
        return zhdrs;
    }

    ZInternetHeaders setParent(ZMimePart parent) {
        this.parent = parent;
        return this;
    }


    public boolean isEmpty() {
        return headers == null || headers.isEmpty();
    }

    /** Returns whether this header block contains any headers with the given
     *  {@code name}. */
    @SuppressWarnings("unchecked")
    boolean containsHeader(String name) {
        for (ZInternetHeader header : (List<ZInternetHeader>) headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private Charset defaultCharset() {
        return parent == null ? null : parent.defaultCharset();
    }

    @Override
    public void load(InputStream is) throws MessagingException {
        super.load(is);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String[] getHeader(String name) {
        if (ZPARSER) {
            Charset charset = defaultCharset();
            List<String> matches = new ArrayList<String>(5);
            for (ZInternetHeader header : (List<ZInternetHeader>) headers) {
                if (name.equalsIgnoreCase(header.getName())) {
                    matches.add(header.getEncodedValue(charset));
                }
            }
            return matches.isEmpty() ? null : matches.toArray(new String[matches.size()]);
        } else {
            return super.getHeader(name);
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (ZPARSER) {
            setHeader(name, value == null ? null : new ZInternetHeader(name, value));
        } else {
            super.setHeader(name, value);
        }
    }

    /** Adds a copy of the header to the header block, replacing any other
     *  headers in the block with the same name.  The header is inserted in
     *  the location of the first replaced matching header.  If no matching
     *  header exists in the block, the logic from {@link #addHeader(ZInternetHeader)}
     *  applies.  If the {@code header} is {@code null}, any matching existing
     *  headers are removed and no new header is added.
     * @see #addHeader(ZInternetHeader) */
    @SuppressWarnings({ "null", "unchecked" })
    public ZInternetHeaders setHeader(String name, ZInternetHeader header) {
        if (header != null && !header.getName().equals(name)) {
            throw new IllegalArgumentException("name does not match header.getName()");
        }

        boolean dirty = false, replaced = header == null;
        for (int index = 0; index < headers.size(); index++) {
            if (((ZInternetHeader) headers.get(index)).getName().equalsIgnoreCase(name)) {
                dirty = true;
                if (!replaced) {
                    // replace the first old instance of the header
                    headers.set(index, header.clone());
                    replaced = true;
                } else {
                    // all other old instances are cleared
                    headers.remove(index--);
                }
            }
        }

        if (!dirty) {
            // make sure the new value actually does get added
            addHeader(header);
        }
        return this;
    }

    private static final Set<String> RESENT_HEADERS = ImmutableSet.of(
            "resent-date", "resent-from", "resent-sender", "resent-to", "resent-cc", "resent-bcc", "resent-message-id"
    );

    @SuppressWarnings("unchecked")
    @Override
    public void addHeader(String name, String value) {
        if (ZPARSER) {
            if (value != null) {
                addHeader(new ZInternetHeader(name, value));
            }
        } else {
            if (name != null && RESENT_HEADERS.contains(name.toLowerCase())) {
                headers.add(0, new InternetHeader(name, value));
            } else {
                super.addHeader(name, value);
            }
        }
    }

    /** Adds a copy of the header to the header block, using our preferred
     *  ordering if possible.
     *  <ul><li>If the header is tagged "unique" and there is already a header
     *  with the same name in the block, this call is converted into a call to
     *  {@link #setHeader(String, ZInternetHeader)} and the existing header(s) will
     *  be replaced with a copy of the new header.
     *  <li>If the header is tagged "first" (i.e. the {@code Resent-*}
     *  headers), then it will be prepended at the very beginning of the block.
     *  <li>If the header is tagged "prepend" (i.e. {@code Return-Path} and
     *  {@code Received}), it will be added *before* any other such headers.
     *  <li>If none of the above cases apply and the header block is in our
     *  preferred ordering, the header is inserted in its appropriate position,
     *  after any other headers with the same priority.
     *  <li>If none of the above apply, the header will be inserted after the
     *  last occurrence of a header with the same name.
     *  <li>If none of the above apply, the header will be inserted at the end
     *  of the block.</ul>
     * @see ZInternetHeader.HeaderInfo */
    @SuppressWarnings("unchecked")
    public ZInternetHeaders addHeader(ZInternetHeader header) {
        if (header == null) {
            return this;
        }

        final ZInternetHeader.HeaderInfo hinfo = header.hinfo;
        if (hinfo.unique && containsHeader(header.name)) {
            // trying to add a second copy of a header of which you're only allowed one: BAD
            return setHeader(header.name, header);
        }

        int index;
        if (hinfo.first) {
            // a few headers, like Resent-*, automatically get prepended even though they don't sort first
            index = 0;
        } else if (hinfo.prepend) {
            // the first set of headers has to be *prepended* in the appropriate slot
            for (index = 0; index < headers.size(); index++) {
                if (((ZInternetHeader) headers.get(index)).hinfo.position >= hinfo.position)
                    break;
            }
        } else if (ordered) {
            // if the headers are in our special ordering, try to maintain that ordering
            int slot = -1;
            for (index = headers.size(); index > 0; index--) {
                ZInternetHeader existing = (ZInternetHeader) headers.get(index - 1);
                if (existing.hinfo.position > hinfo.position)
                    continue;
                if (existing.hinfo.position < hinfo.position) {
                    index = Math.max(index, slot);
                    break;
                }
                if (existing.getName().equalsIgnoreCase(header.getName()))
                    break;
                slot = Math.max(index, slot);
            }
        } else {
            // put it after the last matching header, or at the end of the block if none match
            for (index = headers.size(); index > 0; index--) {
                if (((ZInternetHeader) headers.get(index - 1)).getName().equalsIgnoreCase(header.getName()))
                    break;
            }
            if (index == 0) {
                index = headers.size();
            }
        }
        headers.add(index, header);
        return this;
    }

    /** Appends a copy of the header to the end of the header block.  No other
     *  logic or sanity-checking is applied.  Tracks whether the resulting
     *  header block is in our preferred order; if so, subsequent calls to
     *  {@link #addHeader(ZInternetHeader)} will maintain that ordering.
     * @see ZInternetHeader.HeaderInfo */
    @SuppressWarnings("unchecked")
    public void appendHeader(ZInternetHeader header) {
        if (header == null)
            return;

        if (ordered && !isEmpty() && header.hinfo.position < ((ZInternetHeader) headers.get(headers.size() - 1)).hinfo.position) {
            this.ordered = false;
        }
        headers.add(header);
        return;
    }

    @Override
    public void addHeaderLine(String line) {
        if (ZPARSER) {
            if (!line.endsWith("\r\n")) {
                line += "\r\n";
            }
            byte[] content = line.getBytes(ZInternetHeader.decodingCharset(defaultCharset()));
            if (!line.isEmpty() && Character.isWhitespace(line.charAt(0)) && !headers.isEmpty()) {
                ZInternetHeader last = (ZInternetHeader) headers.remove(headers.size() - 1);
                content = new ByteBuilder(last.content).append(content).toByteArray();
            }
            appendHeader(new ZInternetHeader(content));
        } else {
            super.addHeaderLine(line);
        }
    }

    @Override
    public void removeHeader(String name) {
        if (ZPARSER) {
            if (name == null)
                return;

            for (@SuppressWarnings("unchecked") Iterator<ZInternetHeader> it = headers.iterator(); it.hasNext(); ) {
                if (it.next().getName().equalsIgnoreCase(name)) {
                    it.remove();
                }
            }
        } else {
            super.removeHeader(name);
        }
    }

    public static class IteratorEnumeration<T> implements Enumeration<T> {
        private final Iterator<? extends T> iterator;

        public IteratorEnumeration(Iterator<? extends T> iterator) {
            this.iterator = iterator;
        }

        public IteratorEnumeration(Iterable<? extends T> iterable) {
            this.iterator = iterable.iterator();
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }
    }

    private static final String[] NO_HEADERS = new String[0];

    @SuppressWarnings("unchecked")
    private Enumeration<Header> enumerateHeaders(boolean match, String[] names) {
        if (names == null) {
            names = NO_HEADERS;
        }
        Charset charset = defaultCharset();
        List<InternetHeader> jmheaders = new ArrayList<InternetHeader>();
        for (ZInternetHeader header : (List<ZInternetHeader>) headers) {
            int i = 0;
            for ( ; i < names.length; i++) {
                if (header.getName().equalsIgnoreCase(names[i])) {
                    break;
                }
            }
            if (match == (i != names.length)) {
                jmheaders.add(new InternetHeader(header.getName(), header.getEncodedValue(charset)));
            }
        }
        return new IteratorEnumeration<Header>(jmheaders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Header> getAllHeaders() {
        if (ZPARSER) {
            return enumerateHeaders(false, NO_HEADERS);
        } else {
            return super.getAllHeaders();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Header> getMatchingHeaders(String[] names) {
        if (ZPARSER) {
            return enumerateHeaders(true, names);
        } else {
            return super.getMatchingHeaders(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Header> getNonMatchingHeaders(String[] names) {
        if (ZPARSER) {
            return enumerateHeaders(false, names);
        } else {
            return super.getNonMatchingHeaders(names);
        }
    }

    @SuppressWarnings("unchecked")
    private Enumeration<String> enumerateHeaderLines(boolean match, String[] names) {
        if (names == null) {
            names = NO_HEADERS;
        }
        Charset charset = ZInternetHeader.decodingCharset(defaultCharset());
        List<String> jmheaders = new ArrayList<String>();
        for (ZInternetHeader header : (List<ZInternetHeader>) headers) {
            int i = 0;
            for ( ; i < names.length; i++) {
                if (header.getName().equalsIgnoreCase(names[i])) {
                    break;
                }
            }
            if (match == (i != names.length)) {
                // need to strip the CRLF from the raw content
                byte[] rawHeader = header.getRawHeader();
                jmheaders.add(new String(rawHeader, 0, rawHeader.length - 2, charset));
            }
        }
        return new IteratorEnumeration<String>(jmheaders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getAllHeaderLines() {
        if (ZPARSER) {
            return enumerateHeaderLines(false, NO_HEADERS);
        } else {
            return super.getAllHeaderLines();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getMatchingHeaderLines(String[] names) {
        if (ZPARSER) {
            return enumerateHeaderLines(true, names);
        } else {
            return super.getMatchingHeaderLines(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getNonMatchingHeaderLines(String[] names) {
        if (ZPARSER) {
            return enumerateHeaderLines(false, names);
        } else {
            return super.getNonMatchingHeaderLines(names);
        }
    }

    /** Returns the length of this {@code ZInternetHeaders} when serialized as
     *  a {@code byte[]}.  This count includes the extra {@code CRLF} that
     *  terminates the block.
     * @see #toByteArray() */
    @SuppressWarnings("unchecked")
    public int getLength() {
        int length = 0;
        if (headers != null) {
            for (ZInternetHeader header : (List<ZInternetHeader>) headers) {
                length += header.getRawHeader().length;
            }
        }
        // include the trailing "\r\n" terminating the block
        return length + 2;
    }

    @SuppressWarnings("unchecked")
    public byte[] toByteArray() {
        byte[] block = new byte[getLength()];
        int offset = 0;
        if (headers != null) {
            for (ZInternetHeader header : (List<ZInternetHeader>) headers) {
                byte[] line = header.getRawHeader();
                System.arraycopy(line, 0, block, offset, line.length);
                offset += line.length;
            }
        }
        // include the trailing "\r\n" terminating the block
        block[offset++] = '\r';  block[offset++] = '\n';
        return block;
    }

    @Override
    public String toString() {
        return new String(toByteArray(), ZInternetHeader.decodingCharset(defaultCharset()));
    }
}
