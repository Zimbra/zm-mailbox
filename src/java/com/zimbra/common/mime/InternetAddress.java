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

    @Override public String toString() {
        if (mDisplay != null)
            return MimeHeader.escape(mDisplay, mCharset, true) + " <" + mEmail + '>';
        if (mEmail != null)
            return mEmail;
        return "";
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


    private static void testParser(String[] test) {
        String raw = test[0], display = test[1], email = test[2], description = test[3];
        InternetAddress iaddr = new InternetAddress(raw);

        boolean fail = false;
        if (display == null ^ iaddr.mDisplay == null || (display != null && !display.equals(iaddr.mDisplay)))
            fail = true;
        else if (email == null ^ iaddr.mEmail == null || (email != null && !email.equals(iaddr.mEmail)))
            fail = true;

        if (fail) {
            System.out.println("failed test: " + description);
            System.out.println("  raw:      {" + raw + '}');
            System.out.println("  expected: |" + display + "|, <" + email + '>');
            System.out.println("  actual:   |" + iaddr.mDisplay + "|, <" + iaddr.mEmail + '>');
        }
    }

    public static void main(String[] args) {
        String[][] tests = new String[][] {
            { "Bob the Builder <bob@example.com>", "Bob the Builder", "bob@example.com", "standard address" },
            { "bob@example.com", null, "bob@example.com", "no display name" },
            { "Bob the Builder", "Bob the Builder", null, "no addr-spec" },
            { "Bob the Builder (Bob the Builder)", "Bob the Builder", null, "ignoring comment with no addr-spec" },
            { "<bob@example.com>", null, "bob@example.com", "no display name, but addr-spec in brackets" },
            { "  <bob@example.com>  ", null, "bob@example.com", "addr-spec in brackets with leading/trailing whitespace" },
            { "  < b o b @ e x a m p l e . c o m >  ", null, "bob@example.com", "addr-spec in brackets with whitespace everywhere" },
            { "Bob\t the\tBuilder <bob@example.com>", "Bob the Builder", "bob@example.com", "normalizing/compacting whitespace" },
            { "Bob the (Big) Builder <bob@example.com>", "Bob the Builder", "bob@example.com", "ignoring comments" },
            { "Bob the Buil(Big)der <bob@example.com>", "Bob the Builder", "bob@example.com", "ignoring in-word comments" },
            { "Bob the Builder <bob(Big)@(Bob)example.com>", "Bob the Builder", "bob@example.com", "ignoring comments in address" },
            { "  Bob the Builder   <\"bob\"@\"example.com\">", "Bob the Builder", "bob@example.com", "stripping leading/trailing whitespace and useless quotes in address" },
            { "  Bob the Builder   <\"b ob\"@\"example.com\">", "Bob the Builder", "\"b ob\"@example.com", "stripping leading/trailing whitespace and proper quoting in address" },
            { "  Bob the Builder   <b ob@\"example.com\">", "Bob the Builder", "bob@example.com", "stripping whitespace and useless quotes in address" },
            { "Bob the Builder <bob@[127.0.0.1]>", "Bob the Builder", "bob@[127.0.0.1]", "domain-literal in address" },
            { "Bob the Builder <bob@  [ 127.0 .0. 1 ] >", "Bob the Builder", "bob@[127.0.0.1]", "spaces in domain-literal in address" },
            { "Bob the Builder <bob@  [ 127.0 \\.0. 1 ] >", "Bob the Builder", "bob@[127.0.0.1]", "quoted-pair in domain-literal in address" },
            { "bob@  [ 127.0 \\.0. 1 ] ", null, "bob@[127.0.0.1]", "spaces and quoted-pair in domain-literal in address with no display-name" },
            { "\"Bob, the Builder\" <bob@example.com>", "Bob, the Builder", "bob@example.com", "quotes around commas" },
            { "bob@example.com (Bob the Builder)  ", "Bob the Builder", "bob@example.com", "display part in comment" },
            { "(Bob the Builder) <bob@example.com>", "Bob the Builder", "bob@example.com", "comments before the address" },
            { "<bob@example.com> (Bob the Builder)", "Bob the Builder", "bob@example.com", "comments after the address" },
            { " (Bob the Builder)  bob@example.com", "Bob the Builder", "bob@example.com", "comments and spaces before a non-bracketed address" },
            { "bob@example.com (Bob( the )Builder)  ", "Bob (the) Builder", "bob@example.com", "display part in nested comment" },
            { "(Hambone) bob@example.com (Bob the Builder)  ", "Bob the Builder", "bob@example.com", "ignoring all but last comment" },
            { " ( Bob   the\tBuilder ) <bob@example.com", "Bob the Builder", "bob@example.com", "trailing spaces in comments and a missing end-bracket" },
            { "\"Bob the\" Builder <bob(Bob)@example.com> (Bobbles)", "Bob the Builder", "bob@example.com", "joining quoted strings with normal text and dropping extra comments" },
            { "\"Bob\" the \"Builder\" <bob@example.com>", "Bob the Builder", "bob@example.com", "joining quoted strings with normal text" },
            { " \"\"    \"Bob the Builder\" <bob@example.com>", " Bob the Builder", "bob@example.com", "blank quoted strings" },
            { "_Bob_, the Build\u00ear == <bob@example.com>", "_Bob_, the Build\u00ear ==", "bob@example.com", "bare non-ASCII character" },
            { "\"bob\"@example.com (Bob the Builder)  ", "Bob the Builder", "bob@example.com", "stripping quotes from local-part of addr-spec" },
            { "\"b ob\"@example.com (Bob the Builder)  ", "Bob the Builder", "\"b ob\"@example.com", "not stripping quotes from local-part of addr-spec" },
            { "b ob@example.com (Bob the Builder)  ", "Bob the Builder", "bob@example.com", "eliding whitespace in local-part of unquoted addr-spec" },
            { "Bob the Builder < bob@example.com >", "Bob the Builder", "bob@example.com", "spaces around the address" },
            { "=?us-ascii?Q?Bob_the=20Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "basic 2047 encoding" },
            { "bob@example.com (=?us-ascii?Q?Bob_the=20Builder?=)", "Bob the Builder", "bob@example.com", "basic 2047 encoding in comments" },
            { "=?x-unknown?Q?Bob_the=20Builder?= <bob@example.com>", "=?x-unknown?Q?Bob_the=20Builder?=", "bob@example.com", "unknown encoded-word charset" },
            { "bob@example.com (=?x-unknown?Q?Bob_the=20Builder?=)", "=?x-unknown?Q?Bob_the=20Builder?=", "bob@example.com", "unknown encoded-word charset in comments" },
            { "=?us-ascii?x?Bob_the=20Builder?= <bob@example.com>", "=?us-ascii?x?Bob_the=20Builder?=", "bob@example.com", "invalid encoded-word encoding" },
            { "bob@example.com (=?us-ascii?x?Bob_the=20Builder?=)", "=?us-ascii?x?Bob_the=20Builder?=", "bob@example.com", "invalid encoded-word encoding in comments" },
            { "=?us-ascii?Q?Bob?= the =?us-ascii?Q?Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining 2047 encoded-words with straight text" },
            { "bob@example.com (=?us-ascii?Q?Bob?= the =?us-ascii?Q?Builder?=)", "Bob the Builder", "bob@example.com", "joining 2047 encoded-words with straight text in comments" },
            { "=?us-ascii?Q?Bob_th?= =?us-ascii?Q?e_Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words" },
            { "bob@example.com (=?us-ascii?Q?Bob_th?= =?us-ascii?Q?e_Builder?=)", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words in comments" },
            { "=?us-ascii?Q?Bob_th?= (Bob) =?us-ascii?Q?e_Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words split by a comment" },
            { "=?us-ascii?Q?Bob_th?= (=?us-ascii?Q?Bob=) =?us-ascii?Q?e_Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words split by a comment containing an encoded-word" },
            { "=?us-ascii?q?Bob_?=\t=?us-ascii?Q?the_Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words with an encoded trailing space" },
            { "=?us-ascii?Q?Bob_th?==?us-ascii?Q?e_Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words with no space in between" },
            { "=?us-ascii?Q?Bob_th?=(Bob)=?us-ascii?Q?e_Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "joining two 2047 encoded-words with just a comment in between" },
            { "Bo=?us-ascii?Q?b_the=20Buil?=der <bob@example.com>", "Bob the Builder", "bob@example.com", "2047 encoding inside of a word" },
            { "bob@example.com (Bo=?us-ascii?Q?b_the=20Buil?=der)", "Bob the Builder", "bob@example.com", "2047 encoding inside of a word in a comment" },
            { " =?us-ascii?q??=    \"Bob the Builder\" <bob@example.com>", " Bob the Builder", "bob@example.com", "joining blank encoded-word and quoted-string" },
            { " =?x-unknown?q??=    \"Bob the Builder\" <bob@example.com>", " Bob the Builder", "bob@example.com", "joining blank encoded-word with unknown charset and quoted-string" },
            { "=?us-ascii?Q?Bob the Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "spaces inside encoded-word" },
            { "bob@example.com (=?us-ascii?Q?Bob the Builder?=)", "Bob the Builder", "bob@example.com", "spaces inside encoded-word in comments" },
            { "=?us-ascii?Q?Bob_the__Builder?= <bob@example.com>", "Bob the  Builder", "bob@example.com", "encoded double spaces inside encoded-word" },
            { "=?us-ascii?Q?Bob the  Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "non-encoded double spaces inside encoded-word" },
            { "=?us-ascii?Q?Bob the ?= Builder <bob@example.com>", "Bob the  Builder", "bob@example.com", "spaces at end of encoded-word" },
            { "=?us-ascii?Q?Bob the <bob@example.com>", "=?us-ascii?Q?Bob the", "bob@example.com", "open-brace in unterminated encoded-word" },
            { "=?us-ascii*en?Q?Bob_the=20Builder?= <bob@example.com>", "Bob the Builder", "bob@example.com", "RFC 2231 language in encoded-word" },
        };

        for (String[] test : tests)
            testParser(test);
    }
}
