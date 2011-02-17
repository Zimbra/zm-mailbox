/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.mime.MimeAddressHeader;
import com.zimbra.common.util.CharsetUtil;

public class InternetAddress implements Cloneable {
    private String display;
    private String email;
    private Charset charset;

    public InternetAddress() {
        this.charset = CharsetUtil.UTF_8;
    }

    public InternetAddress(String display, String email) {
        this.display = display;
        this.email = email;
        this.charset = CharsetUtil.UTF_8;
    }

    public InternetAddress(InternetAddress iaddr) {
        this.display = iaddr.display;
        this.email = iaddr.email;
        this.charset = iaddr.charset;
    }

    public InternetAddress(String content) {
        this.charset = CharsetUtil.UTF_8;
        byte[] bvalue = content.getBytes(this.charset);
        parse(bvalue, 0, bvalue.length);
    }

    public InternetAddress(byte[] content) {
        this(content, 0, content.length, null);
    }

    public InternetAddress(byte[] content, String charset) {
        this(content, 0, content.length, CharsetUtil.toCharset(charset));
    }

    InternetAddress(byte[] content, int start, int length, Charset charset) {
        if (charset != null) {
            this.charset = charset;
        }
        parse(content, start, length);
        if (this.charset == null) {
            this.charset = CharsetUtil.UTF_8;
        }
    }

    @Override
    protected InternetAddress clone() {
        return new InternetAddress(this);
    }


    public String getAddress() {
        return email;
    }

    public String getPersonal() {
        return display;
    }

    public Charset getCharset() {
        return charset;
    }

    public InternetAddress setAddress(String address) {
        this.email = address;
        return this;
    }

    public InternetAddress setPersonal(String personal) {
        this.display = personal;
        return this;
    }

    public InternetAddress setCharset(String charset) {
        this.charset = charset == null || charset.trim().equals("") ? CharsetUtil.UTF_8 : CharsetUtil.toCharset(charset.trim());
        return this;
    }

    public InternetAddress setCharset(Charset charset) {
        this.charset = charset == null ? CharsetUtil.UTF_8 : charset;
        return this;
    }

    /** Returns the address, properly MIME encoded for use in a message. */
    @Override
    public String toString() {
        if (display != null) {
            return MimeHeader.escape(display, charset, true) + (email != null ? (" <" + email + '>') : "");
        } else if (email != null) {
            return email;
        } else {
            return "";
        }
    }

    /** Returns the address, formatted as a {@link String}.  No MIME encoding
     *  is done, so this form cannot be used in a message header. */
    public String toUnicodeString() {
        if (display != null) {
            return MimeHeader.quote(display) + (email != null ? (" <" + email + '>') : "");
        } else if (email != null) {
            return email;
        } else {
            return "";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof InternetAddress)) {
            return false;
        } else {
            String addr1 = ((InternetAddress) o).getAddress(), addr2 = getAddress();
            return addr1 == addr2 || (addr1 != null && addr1.equalsIgnoreCase(addr2));
        }
    }

    @Override
    public int hashCode() {
        String addr = getAddress();
        return addr == null ? 0 : addr.toLowerCase().hashCode();
    }

    /**
     * Parse the given comma separated sequence of addresses into a list of
     * {@link InternetAddress} objects. Addresses must follow RFC822 syntax.
     *
     * @param raw comma separated address strings
     * @return list of {@link InternetAddress}
     */
    public static List<InternetAddress> parseHeader(String raw) {
        if (raw == null) {
            return null;
        }
        byte[] content = raw.getBytes(CharsetUtil.UTF_8);
        return parseHeader(content, 0, content.length, CharsetUtil.UTF_8);
    }

    static List<InternetAddress> parseHeader(MimeHeader header) {
        if (header instanceof MimeAddressHeader) {
            return ((MimeAddressHeader) header).getAddresses();
        } else {
            byte[] content = header.getRawHeader();
            return parseHeader(content, header.valueStart, content.length);
        }
    }

    static List<InternetAddress> parseHeader(final byte[] content, final int start, final int length) {
        return parseHeader(content, start, length, null);
    }

    public static List<InternetAddress> parseHeader(final byte[] content, final int start, final int length, final Charset charset) {
        // FIXME: will split the header incorrectly if there's a ',' in the middle of a domain literal ("@[...]")
        boolean quoted = false, escaped = false, empty = true;
        int pos = start, astart = pos, end = start + length, clevel = 0;
        List<InternetAddress> iaddrs = new ArrayList<InternetAddress>(5);
        Group group = null;

        while (pos < end) {
            byte c = content[pos++];
            if (c == '\r' || c == '\n') {
                // ignore folding, even where it's not actually permitted
                escaped = false;
            } else if (quoted) {
                quoted = escaped || c != '"';
                escaped = !escaped && c == '\\';
            } else if (c == '(' || clevel > 0) {
                // handle comments outside of quoted strings, even where they're not actually permitted
                if (!escaped && (c == '(' || c == ')')) {
                    clevel += c == '(' ? 1 : -1;
                }
                escaped = !escaped && c == '\\';
            } else if (c == '"') {
                quoted = true;
                empty = false;
            } else if (c == ',' || (c == ';' && group != null)) {
                // this concludes the address portion of our program
                if (!empty) {
                    if (group != null) {
                        group.addMember(new InternetAddress(content, astart, pos - astart - 1, charset));
                    } else {
                        iaddrs.add(new InternetAddress(content, astart, pos - astart - 1, charset));
                    }
                }
                if (c == ';') {
                    group = null;
                }
                empty = true;
                astart = pos;
            } else if (c == ':' && group == null) {
                if (!empty) {
                    // FIXME: using the address parser to handle decoding RFC 5322 phrases for now
                    String name = new InternetAddress(content, astart, pos - astart - 1, charset).toString();
                    iaddrs.add(group = new Group(name));
                }
                empty = true;
                astart = pos;
            } else if (c != ' ' && c != '\t' && empty) {
                empty = false;
            }
        }
        // don't forget the last address in the list
        if (!empty) {
            if (group != null) {
                group.addMember(new InternetAddress(content, astart, pos - astart, charset));
            } else {
                iaddrs.add(new InternetAddress(content, astart, pos - astart, charset));
            }
        }
        return iaddrs;
    }

    public static class Group extends InternetAddress {
        private List<InternetAddress> addresses = new ArrayList<InternetAddress>(5);

        public Group(String name) {
            super(name, null);
        }

        public Group(String name, List<InternetAddress> members) {
            super(name, null);
            if (members != null) {
                for (InternetAddress addr : members) {
                    addMember(addr);
                }
            }
        }

        @Override
        public Group setPersonal(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException();
            }
            super.setPersonal(name);
            return this;
        }

        public Group setName(String name) {
            return setPersonal(name);
        }

        public String getName() {
            return getPersonal();
        }

        @Override
        public String getAddress() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, len = addresses.size(); i < len; i++) {
                sb.append(addresses.get(i)).append(i == len - 1 ? "" : ", ");
            }
            return sb.append(";").toString();
        }

        public Group addMember(InternetAddress addr) {
            if (addr == null || addr instanceof Group) {
                throw new IllegalArgumentException();
            }
            addresses.add(addr.clone());
            return this;
        }

        public List<InternetAddress> getMembers() {
            List<InternetAddress> members = new ArrayList<InternetAddress>(addresses.size());
            for (InternetAddress addr : addresses) {
                members.add(addr.clone());
            }
            return members;
        }

        @Override
        protected Group clone() {
            return new Group(getPersonal(), addresses);
        }

        @Override
        public String toString() {
            return MimeHeader.escape(getName(), getCharset(), true) + ": " + getAddress();
        }

        @Override
        public String toUnicodeString() {
            StringBuilder sb = new StringBuilder(MimeHeader.quote(getName())).append(": ");
            for (int i = 0, len = addresses.size(); i < len; i++) {
                sb.append(i == 0 ? "" : ", ").append(addresses.get(i).toUnicodeString());
            }
            return sb.toString();
        }
    }


    private void parse(byte[] content, int start, int length) {
        parse(content, start, length, false);
    }

    private void parse(byte[] content, int start, int length, boolean angle) {
        HeaderUtils.ByteBuilder builder = new HeaderUtils.ByteBuilder(length, charset);
        String base = null, address = null, comment = null;
        boolean quoted = false, dliteral = false, escaped = false, atsign = false, route = false;
        boolean slop = false, wsp = true, cwsp = true, encoded = false;
        Boolean encwspenc = Boolean.FALSE, cencwspenc = Boolean.FALSE;
        int clevel = 0, questions = 0;

        for (int pos = start, end = start + length; pos < end; pos++) {
            byte c = content[pos];
            if (c == '\r' || c == '\n') {
                // ignore folding, even where it's not actually permitted
                escaped = false;
            } else if (quoted || dliteral) {
                if (!escaped && c == '\\') {
                    escaped = true;
                } else if (quoted && !escaped && c == '"') {
                    // make sure to check we didn't have encoded-words inside the quoted string
                    if (angle) {
                        address = builder.appendTo(address);
                    } else if (builder.indexOf((byte) '=') != -1) {
                        base = (base == null ? "" : base) + MimeHeader.decode(builder.toByteArray(), charset);
                    } else {
                        base = builder.appendTo(base);
                    }
                    quoted = false;  builder.reset();
                } else {
                    // continuation of a quoted string; note that quoted strings aren't recognized in comments
                    if (!dliteral || (c != ' ' && c != '\t')) {
                        builder.write(c);
                    }
                    escaped = false;
                    if (dliteral && c == ']') {
                        dliteral = false;
                    }
                }
            } else if (c == '=' && (!angle || clevel > 0) && pos < end - 2 && content[pos + 1] == '?' && (!encoded || content[pos + 2] != '=')) {
                // "=?" marks the beginning of an encoded-word
                if (!builder.isEmpty()) {
                    if (clevel > 0)  comment = builder.appendTo(comment);
                    else             base = builder.appendTo(base);
                }
                builder.reset();  builder.write('=');
                encoded = true;  questions = 0;
            } else if (c == '?' && encoded && ++questions > 3 && pos < end - 1 && content[pos + 1] == '=') {
                // "?=" may mean the end of an encoded-word, so see if it decodes correctly
                builder.write('?');  builder.write('=');
                String decoded = HeaderUtils.decodeWord(builder.toByteArray());
                boolean valid = decoded != null;
                if (valid) {
                    pos++;
                } else {
                    decoded = builder.pop().toString();
                }
                if (clevel > 0) {
                    comment = (comment == null ? "" : (valid && cencwspenc == Boolean.TRUE ? comment.substring(0, comment.length() - 1) : comment)) + decoded;
                    cwsp = false;  cencwspenc = valid ? null : Boolean.FALSE;
                } else {
                    base = (base == null ? "" : (valid && encwspenc == Boolean.TRUE ? base.substring(0, base.length() - 1) : base)) + decoded;
                    wsp = false;  encwspenc = valid ? null : Boolean.FALSE;
                }
                encoded = false;  builder.reset();
            } else if ((c == '(' && !encoded) || clevel > 0) {
                // handle comments outside of quoted strings/encoded words, even where they're not actually permitted
                if (!escaped && c == '\\') {
                    escaped = true;
                } else if (!escaped && c == '(' && clevel++ == 0) {
                    // start of top-level comment
                    if (!slop && !builder.isEmpty()) {
                        if (angle)  address = builder.appendTo(address);
                        else        base = builder.appendTo(base);
                    }
                    comment = null;  cwsp = true;  builder.reset();
                } else if (escaped || c != ')' || --clevel != 0) {
                    // comment body character (may be nested '(' or ')')
                    if (c == '(' && !cwsp) {
                        builder.write(' ');
                    } else if (c == ')' && cwsp) {
                        builder.pop();
                    }
                    boolean isWhitespace = c == ' ' || c == '\t';
                    if (!cwsp || !isWhitespace) {
                        builder.write(isWhitespace ? ' ' : c);
                    }
                    if (c == ')') {
                        builder.write(' ');
                    }
                    cwsp = isWhitespace || c == ')' || c == '(';  escaped = false;
                    if (!encoded && cencwspenc != Boolean.FALSE) {
                        cencwspenc = isWhitespace;
                    }
                } else {
                    // end of top-level comment
                    comment = (comment == null ? "" : comment) + (cwsp ? builder.pop() : builder).toString();
                    builder.reset();
                }
            } else if (slop) {
                // we're in the post-address state, so we can safely ignore this character
            } else if (c == '"' && !encoded) {
                if (!builder.isEmpty()) {
                    if (angle)  address = builder.appendTo(address);
                    else        base = builder.appendTo(base);
                }
                quoted = true;  wsp = false;  builder.reset();
            } else if (angle && c == '[') {
                builder.write(c);
                dliteral = true;
            } else if (c == '<' && !angle) {
                // we've just read the leading '<' of an angle-addr, so we now read the addr-spec and the closing '>'
                if (!builder.isEmpty()) {
                    base = (base == null ? "" : base) + (wsp ? builder.pop() : builder).toString();
                }
                angle = true;  wsp = true;  atsign = false;  route = false;  builder.reset();
            } else if (c == '>' && angle) {
                // clean up cruft before saving...
                while (!builder.isEmpty() && (builder.endsWith((byte) '/') || builder.endsWith((byte) '\\'))) {
                    builder.pop();
                }
                address = builder.appendTo(address);
                slop = true;  builder.reset();
            } else {
                if (c == '@') {
                    boolean startRoute = angle && builder.isEmpty() && address == null;
                    if (angle && !startRoute) {
                        address = builder.appendTo(address);
                        // quote the mailbox part of the address if necessary
                        if (!isValidDotAtom(address)) {
                            address = MimeHeader.quote(address);
                        }
                        builder.reset();
                    }
                    if (!startRoute) {
                        atsign = true;
                    } else {
                        route = true;
                    }
                } else if (c == ':' && route) {
                    address = builder.appendTo(address);
                    builder.reset();
                    route = false;
                }
                // compress multiple whitespace chars to a single space
                boolean isWhitespace = c == ' ' || c == '\t';
                if ((!wsp || !isWhitespace) && (c != '<' || !angle || !builder.isEmpty())) {
                    builder.write(isWhitespace ? ' ' : c);
                }
                wsp = angle || isWhitespace;
                // note if this may be whitespace between two encoded-words, which we have to ignore
                if (!encoded && encwspenc != Boolean.FALSE) {
                    encwspenc = isWhitespace;
                }
            }
        }

        if (!builder.isEmpty()) {
            if (clevel > 0)   comment = builder.appendTo(comment);
            else if (angle)   address = builder.appendTo(address);
            else if (quoted)  base = (base == null ? "" : base) + MimeHeader.decode(builder.toByteArray(), charset);
            else              base = builder.appendTo(base);
        }

        if (!angle && wsp && base != null && base.length() > 0) {
            base = base.substring(0, base.length() - 1);
        }

        if (!angle && atsign) {
            // there are some tricky little bits to parsing addresses that weren't followed, so re-parse as an address
            parse(content, start, length, true);
        } else {
            this.display = base != null ? base : comment == null ? null : comment.toString();
            this.email = address == null ? null : address.toString().trim();
        }
    }

    private static boolean isValidDotAtom(String content) {
        boolean dot = true;
        for (int i = 0, len = content.length(); i < len; i++) {
            char c = content.charAt(i);
            if (c == '.' && dot) {
                return false;
            } else if (c != '.' && (c < 0x00 || c >= MimeHeader.ATEXT_VALID.length || !MimeHeader.ATEXT_VALID[c])) {
                return false;
            }
            dot = c == '.';
        }
        return !dot;
    }
}
