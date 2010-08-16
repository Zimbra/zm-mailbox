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

import java.util.ArrayList;
import java.util.List;

public class InternetAddress {
    private String mDisplay;
    private String mEmail;
    private String mCharset;

    public InternetAddress() {
        mCharset = "utf-8";
    }

    public InternetAddress(String display, String email) {
        mDisplay = display;  mEmail = email;  mCharset = "utf-8";
    }

    public InternetAddress(String content) {
        this(content.getBytes());
    }

    public InternetAddress(byte[] content) {
        this(content, 0, content.length, null);
    }

    public InternetAddress(byte[] content, String charset) {
        this(content, 0, content.length, charset);
    }

    public InternetAddress(byte[] content, int start, int length, String charset) {
        if (charset != null && !charset.trim().equals(""))
            mCharset = charset;
        parse(content, start, length);
        if (mCharset == null)
            mCharset = "utf-8";
    }


    public String getAddress() {
        return mEmail;
    }

    public String getPersonal() {
        return mDisplay;
    }

    public String getCharset() {
        return mCharset;
    }

    public InternetAddress setAddress(String address) {
        mEmail = address;
        return this;
    }

    public InternetAddress setPersonal(String personal) {
        mDisplay = personal;
        return this;
    }

    public InternetAddress setCharset(String charset) {
        mCharset = charset == null || charset.trim().equals("") ? "utf-8" : charset.trim();
        return this;
    }

    @Override
    public String toString() {
        if (mDisplay != null) {
            return MimeHeader.escape(mDisplay, mCharset, true) +
                (mEmail != null ? (" <" + mEmail + '>') : "");
        }
        if (mEmail != null) {
            return mEmail;
        }
        return "";
    }

    /**
     * Returns a properly formatted address (RFC 822 syntax) of Unicode
     * characters.
     *
     * @return Unicode address string
     */
    public String toUnicodeString() {
        if (mDisplay != null) {
            return MimeHeader.quote(mDisplay) +
                (mEmail != null ? (" <" + mEmail + '>') : "");
        }
        if (mEmail != null){
            return mEmail;
        }
        return "";
    }

    /**
     * Parse the given comma separated sequence of addresses into a list of
     * {@link InternetAddress} objects. Addresses must follow RFC822 syntax.
     *
     * @param raw comma separated address strings
     * @return list of {@link InternetAddress}
     */
    public static List<InternetAddress> parse(String raw) {
        byte[] array = raw.getBytes();
        return parseHeader(array, 0, array.length);
    }

    static List<InternetAddress> parseHeader(final byte[] content,
            final int start, final int length) {
        // FIXME: will split the header incorrectly if there's a ',' in the
        // middle of a domain literal ("@[...]")
        boolean quoted = false, escaped = false, group = false, empty = true;
        int pos = start, astart = pos, end = start + length, clevel = 0;
        List<InternetAddress> iaddrs = new ArrayList<InternetAddress>();

        while (pos < end) {
            byte c = content[pos++];
            if (c == '\r' || c == '\n') {
                // ignore folding, even where it's not actually permitted
                escaped = false;
            } else if (quoted) {
                quoted = !escaped && c == '"';
                escaped = !escaped && c == '\\';
            } else if (c == '(' || clevel > 0) {
                // handle comments outside of quoted strings, even where they're
                // not actually permitted
                if (!escaped && (c == '(' || c == ')')) {
                    clevel += c == '(' ? 1 : -1;
                }
                escaped = !escaped && c == '\\';
            } else if (c == '"') {
                quoted = true;
                empty = false;
            } else if (c == ',' || (c == ';' && group)) {
                // this concludes the address portion of our program
                if (!empty) {
                    iaddrs.add(new InternetAddress(content,
                            astart, pos - astart - 1, null));
                }
                group = c == ';';
                empty = true;
                astart = pos;
            } else if (c == ':' && !group) {
                // ignore the group name that we've just passed
                group = empty = true;
                astart = pos;
            } else if (c != ' ' && c != '\t' && empty) {
                empty = false;
            }
        }
        // don't forget the last address in the list
        if (!empty) {
            iaddrs.add(new InternetAddress(content, astart, pos - astart, null));
        }
        return iaddrs;
    }

    private void parse(byte[] content, int start, int length) {
        parse(content, start, length, false);
    }

    private void parse(byte[] content, int start, int length, boolean angle) {
        HeaderUtils.ByteBuilder builder = new HeaderUtils.ByteBuilder(length, mCharset);
        String base = null, address = null, comment = null;
        boolean quoted = false, dliteral = false, escaped = false, atsign = false, slop = false, wsp = true, cwsp = true, encoded = false;
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
                    if (angle)
                        address = builder.appendTo(address);
                    else if (builder.indexOf((byte) '=') != -1)
                        base = (base == null ? "" : base) +  MimeHeader.decode(builder.toByteArray(), mCharset);
                    else
                        base = builder.appendTo(base);
                    quoted = false;  builder.reset();
                } else {
                    // continuation of a quoted string; note that quoted strings aren't recognized in comments
                    if (!dliteral || (c != ' ' && c != '\t'))
                        builder.write(c);
                    escaped = false;
                    if (dliteral && c == ']')
                        dliteral = false;
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
                if (valid)
                    pos++;
                else
                    decoded = builder.pop().toString();
                if (clevel > 0) {
                    comment = (comment == null ? "" : (valid && cencwspenc == Boolean.TRUE ? comment.substring(0, comment.length() - 1) : comment)) + decoded;
                    cwsp = false;  cencwspenc = valid ? null : Boolean.FALSE;
                } else {
                    base = (base == null ? "" : (valid && encwspenc == Boolean.TRUE ? base.substring(0, base.length() - 1) : base)) + decoded;
                    wsp = false;  encwspenc = valid ? null : Boolean.FALSE;
                }
                encoded = false;  builder.reset();
            } else if (c == '(' || clevel > 0) {
                // handle comments outside of quoted strings, even where they're not actually permitted
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
                    if (c == '(' && !cwsp)
                        builder.write(' ');
                    else if (c == ')' && cwsp)
                        builder.pop();
                    boolean isWhitespace = c == ' ' || c == '\t';
                    if (!cwsp || !isWhitespace)
                        builder.write(isWhitespace ? ' ' : c);
                    if (c == ')')
                        builder.write(' ');
                    cwsp = isWhitespace || c == ')' || c == '(';  escaped = false;
                    if (!encoded && cencwspenc != Boolean.FALSE)
                        cencwspenc = isWhitespace;
                } else {
                    // end of top-level comment
                    comment = (comment == null ? "" : comment) + (cwsp ? builder.pop() : builder).toString();
                    builder.reset();
                }
            } else if (slop) {
                // we're in the post-address state, so we can safely ignore this character
            } else if (c == '"') {
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
                if (!builder.isEmpty())
                    base = (base == null ? "" : base) + (wsp ? builder.pop() : builder).toString();
                angle = true;  wsp = true;  atsign = false;  builder.reset();
            } else if (c == '>' && angle) {
                address = builder.appendTo(address);
                slop = true;  builder.reset();
            } else {
                if (angle && !atsign && c == '@') {
                    address = builder.appendTo(address);
                    // quote the mailbox part of the address if necessary
                    if (!isValidDotAtom(address))
                        address = MimeHeader.quote(address);
                    builder.reset();
                }
                // compress multiple whitespace chars to a single space
                boolean isWhitespace = c == ' ' || c == '\t';
                if (!wsp || !isWhitespace)
                    builder.write(isWhitespace ? ' ' : c);
                wsp = angle || isWhitespace;
                // note if this may be whitespace between two encoded-words, which we have to ignore
                if (!encoded && encwspenc != Boolean.FALSE)
                    encwspenc = isWhitespace;
                atsign |= c == '@';
            }
        }

        if (!builder.isEmpty()) {
            if (clevel > 0)   comment = builder.appendTo(comment);
            else if (angle)   address = builder.appendTo(address);
            else if (quoted)  base = (base == null ? "" : base) +  MimeHeader.decode(builder.toByteArray(), mCharset);
            else              base = builder.appendTo(base);
        }

        if (!angle && wsp && base != null && base.length() > 0)
            base = base.substring(0, base.length() - 1);

        if (!angle && atsign) {
            // there are some tricky little bits to parsing addresses that weren't followed, so re-parse as an address
            parse(content, start, length, true);
        } else {
            mDisplay = base != null ? base : comment == null ? null : comment.toString();
            mEmail = address == null ? null : address.toString().trim();
        }
    }

    private static boolean isValidDotAtom(String content) {
        boolean dot = true;
        for (int i = 0, len = content.length(); i < len; i++) {
            char c = content.charAt(i);
            if (c == '.' && dot)
                return false;
            else if (c != '.' && (c < 0x00 || c > 0xFF || !MimeHeader.ATEXT_VALID[c]))
                return false;
            dot = c == '.';
        }
        return !dot;
    }

}
