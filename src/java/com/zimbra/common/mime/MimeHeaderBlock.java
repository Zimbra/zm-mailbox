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
    private final List<MimeHeader> headers;
    private MimePart parent;
    private boolean ordered = true;

    public MimeHeaderBlock(boolean isMessage) {
        this(isMessage, null);
    }

    public MimeHeaderBlock(boolean isMessage, MimePart parent) {
        this.headers = new ArrayList<MimeHeader>(isMessage ? 20 : 5);
        this.parent = parent;
    }

    public MimeHeaderBlock(MimeHeader... headers) {
        this.headers = new ArrayList<MimeHeader>(Math.max(headers.length, 5));
        for (MimeHeader header : headers) {
            appendHeader(header);
        }
    }

    public MimeHeaderBlock(MimeHeaderBlock hblock) {
        this.headers = new ArrayList<MimeHeader>(hblock.headers);
    }

    public MimeHeaderBlock(MimeHeaderBlock hblock, String[] omitHeaders) {
        this.headers = new ArrayList<MimeHeader>(hblock.headers.size());
        for (MimeHeader header : hblock) {
            boolean present = false;
            for (String name : omitHeaders) {
                if (header.getName().equalsIgnoreCase(name)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                appendHeader(header);
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
        this.headers = parser.getHeaders().headers;
    }


    MimeHeaderBlock setParent(MimePart parent) {
        this.parent = parent;
        return this;
    }


    public boolean isEmpty() {
        return headers == null || headers.isEmpty();
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
        for (int i = headers.size() - 1; i >= 0; i--) {
            MimeHeader header = headers.get(i);
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
            return isEmpty() ? null : new ArrayList<MimeHeader>(headers);
        }

        List<MimeHeader> matches = new ArrayList<MimeHeader>(2); 
        for (MimeHeader header : headers) {
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
        private final Iterator<MimeHeader> iterator;
        private MimeHeader lastReturned;

        HeaderIterator(Iterable<MimeHeader> headers) {
            iterator = headers.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public MimeHeader next() {
            return lastReturned = iterator.next();
        }

        @Override
        public void remove() {
            announce(lastReturned.getName(), null);
            iterator.remove();
            markDirty();
        }
    }

    @Override
    public Iterator<MimeHeader> iterator() {
        return new HeaderIterator(headers);
    }


    void announce(String hdrName, MimeHeader header) {
        if (parent != null && hdrName.toLowerCase().equals("content-type")) {
            MimePart container = parent.getParent();
            String defaultType = container instanceof MimeMultipart ? ((MimeMultipart) container).getDefaultChildContentType() : "text/plain";
            parent.updateContentType(new ContentType(header, defaultType));
        }
    }

    void markDirty() {
        if (parent != null) {
            parent.markDirty(MimePart.Dirty.HEADERS);
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

    /** Creates a new {@code MimeHeader} and adds it to the header block,
     *  replacing any other headers in the block with the same name.  The new
     *  header is inserted in the location of the first replaced matching
     *  header.  If no matching header exists in the block, the logic from
     *  {@link #addHeader(String, String)} applies.  If the {@code value} is
     *  {@code null}, any matching existing headers are removed but no new
     *  header is added.
     * @param name   The new header's name.
     * @param value  The new header's value.  If non-ASCII, RFC 2047 encoding
     *               will be applied to the value when generating the resulting
     *               header line.
     * @see #addHeader(String, String) */
    public MimeHeaderBlock setHeader(String name, String value) {
        return setHeader(name, value == null ? null : new MimeHeader(name, value));
    }

    /** Creates a new {@code MimeHeader} and adds it to the header block,
     *  replacing any other headers in the block with the same name.  The new
     *  header is inserted in the location of the first replaced matching
     *  header.  If no matching header exists in the block, the logic from
     *  {@link #addHeader(String, byte[])} applies.  If the {@code bvalue} is
     *  {@code null}, any matching existing headers are removed but no new
     *  header is added.
     * @param name    The new header's name.
     * @param bvalue  The new header's raw value.  The added header is formed
     *                by concatenating {@code name}, "{@code : }", and {@code
     *                bvalue} into a single {@code byte[]}. 
     * @see #addHeader(String, byte[]) */
    public MimeHeaderBlock setHeader(String name, byte[] bvalue) {
        return setHeader(name, bvalue == null ? null : new MimeHeader(name, bvalue));
    }

    /** Adds a copy of the header to the header block, replacing any other
     *  headers in the block with the same name.  The header is inserted in
     *  the location of the first replaced matching header.  If no matching
     *  header exists in the block, the logic from {@link #addHeader(MimeHeader)}
     *  applies.  If the {@code header} is {@code null}, any matching existing
     *  headers are removed and no new header is added.
     * @see #addHeader(MimeHeader) */
    @SuppressWarnings("null")
    public MimeHeaderBlock setHeader(String name, MimeHeader header) {
        String key = validateFieldName(name);
        if (header != null && !header.getName().equals(name))
            throw new IllegalArgumentException("name does not match header.getName()");
        if (key == null)
            return this;

        boolean dirty = false, replaced = header == null;
        for (int index = 0; index < headers.size(); index++) {
            if (headers.get(index).getName().equalsIgnoreCase(key)) {
                if (!dirty) {
                    announce(name, header);
                    dirty = true;
                }
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

        if (dirty) {
            markDirty();
        } else {
            // make sure the new value actually does get added
            addHeader(header);
        }
        return this;
    }

    /** Creates a new {@code MimeHeader} and adds it to the header block, using
     *  our preferred ordering if possible.
     *  <ul><li>If the header is tagged "unique" and there is already a header
     *  with the same name in the block, this call is converted into a call to
     *  {@link #setHeader(String, MimeHeader)} and the existing header(s) will
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
     * @param name   The new header's name.
     * @param value  The new header's value.  If non-ASCII, RFC 2047 encoding
     *               will be applied to the value when generating the resulting
     *               header line.
     * @see #addHeader(MimeHeader)
     * @see MimeHeader.HeaderInfo */
    public MimeHeaderBlock addHeader(String name, String value) {
        if (value != null) {
            addHeader(new MimeHeader(name, value));
        }
        return this;
    }

    /** Creates a new {@code MimeHeader} and adds it to the header block, using
     *  our preferred ordering if possible.
     *  <ul><li>If the header is tagged "unique" and there is already a header
     *  with the same name in the block, this call is converted into a call to
     *  {@link #setHeader(String, MimeHeader)} and the existing header(s) will
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
     * @param name    The new header's name.
     * @param bvalue  The new header's raw value.  The added header is formed
     *                by concatenating {@code name}, "{@code : }", and {@code
     *                bvalue} into a single {@code byte[]}. 
     * @see #addHeader(MimeHeader)
     * @see MimeHeader.HeaderInfo */
    public MimeHeaderBlock addHeader(String name, byte[] bvalue) {
        if (bvalue != null) {
            addHeader(new MimeHeader(name, bvalue));
        }
        return this;
    }

    /** Adds a copy of the header to the header block, using our preferred
     *  ordering if possible.
     *  <ul><li>If the header is tagged "unique" and there is already a header
     *  with the same name in the block, this call is converted into a call to
     *  {@link #setHeader(String, MimeHeader)} and the existing header(s) will
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
     * @see MimeHeader.HeaderInfo */
    public MimeHeaderBlock addHeader(MimeHeader header) {
        if (header == null || validateFieldName(header.getName()) == null)
            return this;

        final MimeHeader.HeaderInfo hinfo = header.hinfo;
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
                if (headers.get(index).hinfo.position >= hinfo.position)
                    break;
            }
        } else if (ordered) {
            // if the headers are in our special ordering, try to maintain that ordering
            int slot = -1;
            for (index = headers.size(); index > 0; index--) {
                MimeHeader existing = headers.get(index - 1);
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
                if (headers.get(index - 1).getName().equalsIgnoreCase(header.getName()))
                    break;
            }
            if (index == 0) {
                index = headers.size();
            }
        }
        announce(header.getName(), header);
        headers.add(index, header.clone());
        markDirty();
        return this;
    }

    /** Creates a new {@code MimeHeader} and adds it to the end of the header
     *  block.  No other logic or sanity-checking is applied.  Tracks whether
     *  the resulting header block is in our preferred order; if so, subsequent
     *  calls to {@link #addHeader(String, String)} will maintain that ordering.
     * @param name   The new header's name.
     * @param value  The new header's value.  If non-ASCII, RFC 2047 encoding
     *               will be applied to the value when generating the resulting
     *               header line.
     * @see MimeHeader.HeaderInfo */
    public MimeHeaderBlock appendHeader(String name, String value) {
        if (value != null) {
            appendHeader(new MimeHeader(name, value));
        }
        return this;
    }

    /** Creates a new {@code MimeHeader} and adds it to the end of the header
     *  block.  No other logic or sanity-checking is applied.  Tracks whether
     *  the resulting header block is in our preferred order; if so, subsequent
     *  calls to {@link #addHeader(String, byte[])} will maintain that ordering.
     * @param name    The new header's name.
     * @param bvalue  The new header's raw value.  The added header is formed
     *                by concatenating {@code name}, "{@code : }", and {@code
     *                bvalue} into a single {@code byte[]}. 
     * @see MimeHeader.HeaderInfo */
    public MimeHeaderBlock appendHeader(String name, byte[] bvalue) {
        if (bvalue != null) {
            appendHeader(new MimeHeader(name, bvalue));
        }
        return this;
    }

    /** Appends a copy of the header to the end of the header block.  No other
     *  logic or sanity-checking is applied.  Tracks whether the resulting
     *  header block is in our preferred order; if so, subsequent calls to
     *  {@link #addHeader(MimeHeader)} will maintain that ordering.
     * @see MimeHeader.HeaderInfo */
    public MimeHeaderBlock appendHeader(MimeHeader header) {
        if (header == null || validateFieldName(header.getName()) == null)
            return this;

        if (ordered && !isEmpty() && header.hinfo.position < headers.get(headers.size() - 1).hinfo.position) {
            this.ordered = false;
        }
        announce(header.getName(), header);
        headers.add(header.clone());
        markDirty();
        return this;
    }

    /** Appends a copy of all headers in the given header block to the end of
     *  this block.  Ordering is preserved.  No other logic or sanity-checking
     *  is applied.
     * @see #appendHeader(MimeHeader) */
    public MimeHeaderBlock appendAll(MimeHeaderBlock hblock) {
        for (MimeHeader header : hblock) {
            appendHeader(header);
        }
        return this;
    }

    MimeHeaderBlock removeHeader(MimeHeader header) {
        for (Iterator<MimeHeader> it = headers.iterator(); it.hasNext(); ) {
            if (it.next() == header) {
                it.remove();
                markDirty();
                break;
            }
        }
        return this;
    }


    /** Returns the length of this {@code MimeHeaderBlock} when serialized as
     *  a {@code byte[]}.  This count includes the extra {@code CRLF} that
     *  terminates the block.
     * @see #toByteArray() */
    public int getLength() {
        int length = 0;
        if (headers != null) {
            for (MimeHeader header : headers) {
                length += header.getRawHeader().length;
            }
        }
        // include the trailing "\r\n" terminating the block
        return length + 2;
    }

    public byte[] toByteArray() {
        byte[] block = new byte[getLength()];
        int offset = 0;
        if (headers != null) {
            for (MimeHeader header : headers) {
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
        return new String(toByteArray());
    }
}
