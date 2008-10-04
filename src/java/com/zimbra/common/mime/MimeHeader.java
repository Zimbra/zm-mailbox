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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;

public class MimeHeader {
    protected final String mName;
    protected byte[] mContent;
    protected int mValueStart;
    protected String mCharset = "utf-8";

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
     *  <tt>{name}: {value}CRLF</tt>.  <i>Note: No folding is done at
     *  present.</i> */
    MimeHeader(String name, String value) {
        this(name, value, null);
    }
    
    MimeHeader(String name, String value, String charset) {
        mName = name;
        if (charset != null && !charset.equals(""))
            mCharset = charset;
        updateContent(escape(value, mCharset, false).getBytes());
    }

    MimeHeader(String name, byte[] bvalue) {
        mName = name;
        updateContent(bvalue);
    }

    MimeHeader updateContent(byte[] bvalue) {
        byte[] bname = mName.getBytes();
        int nlen = bname.length, vlen = bvalue == null ? 0 : bvalue.length;
        int csize = nlen + vlen + 4;

        byte[] content = new byte[csize];
        System.arraycopy(bname, 0, content, 0, nlen);
        content[nlen] = ':';  content[nlen + 1]= ' ';
        if (bvalue != null)
            System.arraycopy(bvalue, 0, content, nlen + 2, vlen);
        content[csize - 2] = '\r';  content[csize - 1] = '\n';

        mContent = content;  mValueStart = nlen + 2;
        return this;
    }

    MimeHeader(String name, MimeCompoundHeader mch) {
        mName = name;
        mContent = (mch.toString(name) + "\r\n").getBytes();
        mValueStart = mName.length() + 2;
    }

    String getName() {
        return mName;
    }

    byte[] getRawHeader() {
        reserialize();
        return mContent;
    }

    String getValue() {
        reserialize();
        return unfold(new String(mContent, mValueStart, mContent.length - mValueStart));
    }

    String getValue(String charset) {
        if (charset == null || charset.equals(""))
            return getValue();

        reserialize();
        try {
            return unfold(new String(mContent, mValueStart, mContent.length - mValueStart, charset));
        } catch (UnsupportedEncodingException e) {
            return getValue();
        }
    }

    private static String unfold(final String folded) {
        int length = folded.length();
        StringBuilder unfolded = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = folded.charAt(i);
            if (c != '\r' && c != '\n')
                unfolded.append(c);
        }
        return unfolded.toString();
    }

    private static class Q2047Encoder extends ContentTransferEncoding.QuotedPrintableEncoderStream {
        private static final boolean[] FORCE_ENCODE = new boolean[128];
            static {
                for (int i = 0; i < FORCE_ENCODE.length; i++)
                    FORCE_ENCODE[i] = true;
                for (int c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!*+-/ ".getBytes())
                    FORCE_ENCODE[c] = false;
            }

        Q2047Encoder(byte[] content) {
            super(new ByteArrayInputStream(content), null);  disableFolding();  setForceEncode(FORCE_ENCODE);
        }

        @Override public int read() throws IOException {
            int c = super.read();  return c == ' ' ? '_' : c;
        }
    }

    private static class B2047Encoder extends ContentTransferEncoding.Base64EncoderStream {
        B2047Encoder(byte[] content) {
            super(new ByteArrayInputStream(content));  disableFolding();
        }
    }

    private static final boolean[] ATEXT_VALID = new boolean[128];
        static {
            for (int c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&'*+-/=?^_`{|}~".getBytes())
                ATEXT_VALID[c] = true;
        }

    static String escape(String value, String charset, boolean quotable) {
        boolean needsQuote = false, wsp = true;
        int needs2047 = 0, needsEscape = 0;
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c > 0x7F || c == '\0' || c == '\r' || c == '\n') {
                needs2047++;
            } else if (c == '"' || c == '\\') {
                needsQuote = true;  needsEscape++;
            } else if ((c != ' ' && !ATEXT_VALID[c]) || (c == ' ' && wsp)) {
                needsQuote = true;
            }
            wsp = c == ' ';
        }
        needsQuote |= wsp;

        if (needs2047 > 0 || (!quotable && needsQuote)) {
            // FIXME: need to limit encoded-words to 75 bytes
            StringBuilder sb = new StringBuilder(75).append("=?");

            charset = StringUtil.checkCharset(value, charset);
            byte[] content = null;
            try {
                content = value.getBytes(charset);
                sb.append(charset);
            } catch (Throwable t) {
                content = value.getBytes();
                sb.append(Charset.defaultCharset().displayName());
            }

            InputStream encoder;
            if (needs2047 > value.length() / 3) {
                sb.append("?B?");  encoder = new B2047Encoder(content);
            } else {
                sb.append("?Q?");  encoder = new Q2047Encoder(content);
            }
            try {
                sb.append(new String(ByteUtil.readInput(encoder, 0, Integer.MAX_VALUE)));
            } catch (IOException ioe) {}

            return sb.append("?=").toString();
        } else if (needsQuote && needsEscape > 0) {
            StringBuilder sb = new StringBuilder(value.length() + needsEscape + 2).append('"');
            for (int i = 0, len = value.length(); i < len; i++) {
                char c = value.charAt(i);
                if (c == '"' || c == '\\')
                    sb.append('\\');
                sb.append(c);
            }
            return sb.append('"').toString();
        } else if (needsQuote) {
            return new StringBuilder(value.length() + 2).append('"').append(value).append('"').toString();
        } else {
            return value;
        }
    }

    protected void reserialize() { }

    @Override public String toString() {
        String header = new String(mContent);
        return header.endsWith("\r\n") ? header.substring(0, header.length() - 2) : header;
    }


    public static class MimeAddressHeader extends MimeHeader {
        private ArrayList<InternetAddress> mAddresses = new ArrayList<InternetAddress>(3);

        public MimeAddressHeader(final String name, final List<InternetAddress> iaddrs) {
            super(name, null, -1);
            mAddresses.addAll(iaddrs);
        }

        public MimeAddressHeader(final String name, final String value) {
            super(name, value == null ? null : value.getBytes());
            if (mValueStart > 0)
                parse(mContent, mValueStart, mContent.length - mValueStart);
        }

        private void parse(final byte[] content, final int start, final int length) {
            boolean quoted = false, escaped = false, group = false, empty = true;
            int pos = start, astart = pos, end = start + length, clevel = 0;

            while (pos < end) {
                byte c = content[pos++];
                if (c == '\r' || c == '\n') {
                    // ignore folding, even where it's not actually permitted
                    escaped = false;
                } else if (quoted) {
                    quoted = !escaped && c == '"';
                    escaped = !escaped && c == '\\';
                } else if (c == '(' || clevel > 0) {
                    // handle comments outside of quoted strings, even where they're not actually permitted
                    if (!escaped && (c == '(' || c == ')'))
                        clevel += c == '(' ? 1 : -1;
                    escaped = !escaped && c == '\\';
                    empty = false;
                } else if (c == '"') {
                    quoted = true;
                    empty = false;
                } else if (c == ',' || (c == ';' && group)) {
                    // this concludes the address portion of our program
                    if (!empty)
                        mAddresses.add(new InternetAddress(content, astart, pos - astart));
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
            if (!empty)
                mAddresses.add(new InternetAddress(content, astart, pos - astart));
        }

        public List<InternetAddress> getAddresses() {
            return new ArrayList<InternetAddress>(mAddresses);
        }

        public MimeAddressHeader addAddress(InternetAddress iaddr) {
            mAddresses.add(iaddr);  mContent = null;  mValueStart = -1;  return this;
        }

        protected void reserialize() {
            if (mContent == null) {
                StringBuilder value = new StringBuilder();
                for (int i = 0; i < mAddresses.size(); i++)
                    value.append(i == 0 ? "" : ", ").append(mAddresses.get(i));
                // FIXME: need to fold every 75 bytes
                updateContent(value.toString().getBytes());
            }
        }
    }


    private static void dumpAddresses(MimeAddressHeader ahdr) {
        for (InternetAddress iaddr : ahdr.getAddresses())
            System.out.println("  addr: " + iaddr);
    }

    private static void dumpContent(MimeHeader header) {
        System.out.println(header + "\t=> " + header.getValue());
    }

    public static void main(String[] args) {
        dumpAddresses(new MimeAddressHeader("To", "mine:=?us-ascii?Q?Bob_?=\t=?us-ascii?Q?the_Builder?= <bob@example.com>;,=?us-ascii?Q?Bob the Builder?= <bob@example.com>"));

        dumpContent(new MimeHeader("Subject", "Pru\u00ee"));
        dumpContent(new MimeHeader("Subject", "Pru\u00ee", "iso-8859-1"));
        dumpContent(new MimeHeader("Subject", "Pru\u00ee", "iso-8859-7"));

        dumpContent(new MimeHeader("Subject", "Pru\u00ee uey liufhlasuifh lskdhf lkshfl aksjhlfi ahslkfu haskjhf lkajshf lkajshflkajhslkfj halskjhf laskjhdflaksjh ksjfh ka"));

        dumpContent(new MimeHeader("Subject", "\u00eb\u00ec\u00ed\u00ee"));
        dumpContent(new MimeHeader("Subject", "\u00eb\u00ec\u00ed\u00ee", "iso-8859-1"));
    }
}
