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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InternetAddress {
    private String mDisplay;
    private String mEmail;
    private String mCharset = "utf-8";

    public InternetAddress(String display, String email) {
        mDisplay = display;  mEmail = email;
    }

    public InternetAddress(String content) {
        this(content.getBytes());
    }

    public InternetAddress(byte[] content) {
        this(content, 0, content.length);
    }

    public InternetAddress(byte[] content, int start, int length) {
        parse(content, start, length);
    }

    public InternetAddress setCharset(String charset) {
        mCharset = charset == null || charset.trim().equals("") ? "utf-8" : charset.trim();
        return this;
    }

    private class ByteBuilder extends ByteArrayOutputStream {
        ByteBuilder()          { super(); }
        ByteBuilder(int size)  { super(size); }
        ByteBuilder pop()      { if (count > 0) count--;  return this; }
        boolean isEmpty()      { return count == 0; }
    }

    private String decodeWord(byte[] word) {
        int length = word.length;
        if (length <= 8 || word[0] != '=' || word[1] != '?' || word[length-2] != '?' || word[length-1] != '=')
            return null;

        int pos = 2;
        while (pos < length && word[pos] != '?')
            pos++;
        if (pos >= length - 4 || pos == 2)
            return null;
        String charset = new String(word, 2, pos - 2);

        InputStream decoder;
        byte encoding = word[++pos];
        if (word[++pos] != '?')
            return null;
        if (encoding == 'Q' || encoding == 'q')
            decoder = new QP2047Decoder(new ByteArrayInputStream(word, pos + 1, length - pos - 3));
        else if (encoding == 'B' || encoding == 'b')
            decoder = new ContentTransferEncoding.Base64DecoderStream(new ByteArrayInputStream(word, pos + 1, length - pos - 3));
        else
            return null;

        try {
            byte[] dbuffer = new byte[word.length];
            int dsize = decoder.read(dbuffer);
            return new String(dbuffer, 0, dsize, charset);
        } catch (Exception e) {
            return null;
        }
    }

    private void parse(byte[] content, int start, int length) {
        ByteBuilder builder = new ByteBuilder(length);
        String base = null, address = null, comment = null;
        boolean quoted = false, escaped = false, ampersand = false, angle = false, slop = false, wsp = true, cwsp = true, encoded = false;
        Boolean encwspenc = Boolean.FALSE, cencwspenc = Boolean.FALSE;
        int clevel = 0, questions = 0;

        for (int pos = start, end = start + length; pos < end; pos++) {
            byte c = content[pos];
            if (c == '\r' || c == '\n') {
                // ignore folding, even where it's not actually permitted
                escaped = false;
            } else if (quoted) {
                if (!escaped && c == '\\') {
                    escaped = true;
                } else if (!escaped && c == '"') {
                    // don't drop the quote characters when the quoted-string is the local-part of an addr-spec
                    if (angle && !ampersand)
                        builder.write(c);
                    if (angle)  address = (address == null ? "" : address) + builder.toString();
                    else        base = (base == null ? "" : base) + builder.toString();
                    quoted = false;  builder.reset();
                } else {
                    // continuation of a quoted string; note that quoted strings aren't recognized in comments
                    builder.write(c);  escaped = false;
                }
            } else if (c == '=' && (!angle || clevel > 0) && pos != end && content[pos + 1] == '?') {
                // "=?" marks the beginning of an encoded-word
                if (!builder.isEmpty()) {
                    if (clevel > 0)  comment = (comment == null ? "" : comment) + builder.toString();
                    else             base = (base == null ? "" : base) + builder.toString();
                }
                builder.reset();  builder.write('=');
                encoded = true;  questions = 0;
            } else if (c == '?' && encoded && ++questions > 3 && pos != end && content[pos + 1] == '=') {
                // "?=" may mean the end of an encoded-word, so see if it decodes correctly
                builder.write('?');  builder.write('=');
                String decoded = decodeWord(builder.toByteArray());
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
                        if (angle)  address = (address == null ? "" : address) + builder.toString();
                        else        base = (base == null ? "" : base) + builder.toString();
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
                    cwsp = angle || isWhitespace || c == ')' || c == '(';  escaped = false;
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
                    if (angle)  address = (address == null ? "" : address) + builder.toString();
                    else        base = (base == null ? "" : base) + builder.toString();
                }
                quoted = true;  wsp = false;  builder.reset();
                // don't drop the quote characters when the quoted-string is the local-part of an addr-spec
                if (angle && !ampersand)
                    builder.write(c);
            } else if (c == '<' && !angle) {
                // we've just read the leading '<' of an angle-addr, so we now read the addr-spec and the closing '>'
                if (!builder.isEmpty())
                    base = (base == null ? "" : base) + (wsp ? builder.pop() : builder).toString();
                angle = true;  wsp = true;  ampersand = false;  builder.reset();
            } else if (c == '>' && angle) {
                address = (address == null ? "" : address) + builder.toString();
                slop = true;  builder.reset();
            } else {
                boolean isWhitespace = c == ' ' || c == '\t';
                if (!wsp || !isWhitespace)
                    builder.write(isWhitespace ? ' ' : c);
                wsp = angle || isWhitespace;
                if (!encoded && encwspenc != Boolean.FALSE)
                    encwspenc = isWhitespace;
                ampersand |= c == '@';
            }
        }

        if (!builder.isEmpty()) {
            if (clevel > 0)  comment = (comment == null ? "" : comment) + builder.toString();
            else if (angle)  address = (address == null ? "" : address) + builder.toString();
            else             base = (base == null ? "" : base) + builder.toString();
        }

        if (!angle && wsp && base != null && base.length() > 0)
            base = base.substring(0, base.length() - 1);

        if (!angle && ampersand) {
            mDisplay = comment == null ? null : comment.toString();
            mEmail = base;
        } else {
            mDisplay = base != null ? base : comment == null ? null : comment.toString();
            mEmail = address == null ? null : address.toString().trim();
        }
    }

    private static class QP2047Decoder extends ContentTransferEncoding.QuotedPrintableDecoderStream {
        QP2047Decoder(ByteArrayInputStream bais)  { super(bais); }

        @Override protected int nextByte() throws IOException {
            int c = super.nextByte();  return c == '_' ? ' ' : c;
        }
    }

    @Override public String toString() {
        return mDisplay == null ? mEmail : MimeHeader.escape(mDisplay, mCharset, true) + " <" + mEmail + '>';
    }

    public static void main(String[] args) {
        // test no display name
        System.out.println(new InternetAddress("bob@example.com"));
        // test no display name, but addr-spec in brackets
        System.out.println(new InternetAddress("<bob@example.com>"));
        // test addr-spec in brackets with leading/trailing whitespace
        System.out.println(new InternetAddress("  <bob@example.com>  "));
        // test normalizing/compacting whitespace
        System.out.println(new InternetAddress("Bob\t the\tBuilder <bob@example.com>"));
        // test ignoring comments
        System.out.println(new InternetAddress("Bob the (Big) Builder <bob@example.com>"));
        // test ignoring in-word comments
        System.out.println(new InternetAddress("Bob the Buil(Big)der <bob@example.com>"));
        // test stripping leading/trailing whitespace and useless quotes in address
        System.out.println(new InternetAddress("  Bob the Builder   <\"bob\"@\"example.com\">"));
        // test quotes around commas
        System.out.println(new InternetAddress("\"Bob, the Builder\" <bob@example.com>"));
        // test display part in comment
        System.out.println(new InternetAddress("bob@example.com (Bob the Builder)  "));
        // test display part in nested comment
        System.out.println(new InternetAddress("bob@example.com (Bob( the )Builder)  "));
        // test ignoring all but last comment
        System.out.println(new InternetAddress("(Hambone) bob@example.com (Bob the Builder)  "));
        // test joining quoted strings with normal text and dropping extra comments
        System.out.println(new InternetAddress("\"Bob the\" Builder <bob(Bob)@example.com> (Bobbles)"));
        // test joining quoted strings with normal text
        System.out.println(new InternetAddress("\"Bob\" the \"Builder\" <bob@example.com>"));
        // test comments before the address
        System.out.println(new InternetAddress("(Bob the Builder) <bob@example.com>"));
        // test trailing spaces in comments and a missing end-bracket
        System.out.println(new InternetAddress(" ( Bob   the\tBuilder ) <bob@example.com"));
        // test blank quoted strings
        System.out.println(new InternetAddress(" \"\"    \"Bob the Builder\" <bob@example.com>"));
        // test comments and spaces before a non-bracketed address
        System.out.println(new InternetAddress(" (Bob the Builder)  bob@example.com"));
        System.out.println(new InternetAddress("_Bob_, the Build\u00ear == <bob@example.com>"));
        System.out.println(new InternetAddress("\"_Bob_,\tthe\" Build\u00ear == <bob@example.com>").setCharset("iso-8859-1"));
        System.out.println(new InternetAddress("_Bob_, the Build\u00ear == <bob@example.com>").setCharset("koi8-r"));
        // FIXME: test not stripping quotes from local-part of addr-spec
        System.out.println(new InternetAddress("\"bob\"@example.com (Bob the Builder)  "));
        // test spaces around the address
        System.out.println(new InternetAddress("Bob the Builder < bob@example.com >"));
        // test basic 2047 encoding
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_the=20Builder?= <bob@example.com>"));
        // test basic 2047 encoding in comments
        System.out.println(new InternetAddress("bob@example.com (=?us-ascii?Q?Bob_the=20Builder?=)"));
        // test joining 2047 encoded-words with straight text
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob?= the =?us-ascii?Q?Builder?= <bob@example.com>"));
        // test joining 2047 encoded-words with straight text in comments
        System.out.println(new InternetAddress("bob@example.com (=?us-ascii?Q?Bob?= the =?us-ascii?Q?Builder?=)"));
        // test joining two 2047 encoded-words
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_th?= =?us-ascii?Q?e_Builder?= <bob@example.com>"));
        // test joining two 2047 encoded-words in comments
        System.out.println(new InternetAddress("bob@example.com (=?us-ascii?Q?Bob_th?= =?us-ascii?Q?e_Builder?=)"));
        // test joining two 2047 encoded-words split by a comment
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_th?= (Bob) =?us-ascii?Q?e_Builder?= <bob@example.com>"));
        // test joining two 2047 encoded-words split by a comment containing an encoded-word
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_th?= (=?us-ascii?Q?Bob=) =?us-ascii?Q?e_Builder?= <bob@example.com>"));
        // test joining two 2047 encoded-words with an encoded trailing space
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_?=\t=?us-ascii?Q?the_Builder?= <bob@example.com>"));
        // test joining two 2047 encoded-words with no space in between
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_th?==?us-ascii?Q?e_Builder?= <bob@example.com>"));
        // test 2047 encoding inside of a word
        System.out.println(new InternetAddress("Bo=?us-ascii?Q?b_the=20Buil?=der <bob@example.com>"));
        // test 2047 encoding inside of a word in a comment
        System.out.println(new InternetAddress("bob@example.com (Bo=?us-ascii?Q?b_the=20Buil?=der)"));
        // test spaces inside encoded-word
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob the Builder?= <bob@example.com>"));
        // test spaces inside encoded-word in comments
        System.out.println(new InternetAddress("bob@example.com (=?us-ascii?Q?Bob the Builder?=)"));
        // test encoded double spaces inside encoded-word
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob_the__Builder?= <bob@example.com>"));
        // test non-encoded double spaces inside encoded-word
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob the  Builder?= <bob@example.com>"));
        // test spaces at end of encoded-word
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob the ?= Builder <bob@example.com>"));
        // test open-brace in encoded-word
        System.out.println(new InternetAddress("=?us-ascii?Q?Bob the <bob@example.com>"));
    }
}
