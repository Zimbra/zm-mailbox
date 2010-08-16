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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public class MimeHeader {
    protected final String mName;
    protected byte[] mContent;
    protected int mValueStart;
    protected String mCharset = "utf-8";

    /** Constructor for pre-analyzed header line read from message source.
     * @param name    Header field name.
     * @param content Complete raw header line, <b>including</b> the field name
     *                and colon and with folding and trailing CRLF and 2047-
     *                encoding intact.
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
        if (charset != null && !charset.equals("")) {
            mCharset = charset;
        }
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
        if (bvalue != null) {
            System.arraycopy(bvalue, 0, content, nlen + 2, vlen);
        }
        content[csize - 2] = '\r';  content[csize - 1] = '\n';

        mContent = content;  mValueStart = nlen + 2;
        return this;
    }

    MimeHeader(String name, MimeCompoundHeader mch) {
        mName = name;
        mContent = (mch.toString(name) + "\r\n").getBytes();
        mValueStart = mName.length() + 2;
    }

    public String getName() {
        return mName;
    }

    public byte[] getRawHeader() {
        reserialize();
        return mContent;
    }

    public String getValue(String charset) {
        reserialize();
        int end = mContent.length, c;
        while (end > mValueStart && ((c = mContent[end-1]) == '\n' || c == '\r')) {
            end--;
        }
        return decode(mContent, mValueStart, end - mValueStart, charset);
    }

    public String getEncoded() {
        reserialize();
        return unfold(new String(mContent, mValueStart, mContent.length - mValueStart));
    }

    public String getEncoded(String charset) {
        if (charset == null || charset.equals("")) {
            return getEncoded();
        }
        reserialize();
        try {
            return unfold(new String(mContent, mValueStart, mContent.length - mValueStart, charset));
        } catch (UnsupportedEncodingException e) {
            return getEncoded();
        }
    }


    private static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

    public static String decode(final String content) {
        return decode(content.getBytes(), DEFAULT_CHARSET);
    }

    static String decode(final byte[] content, final String charset) {
        return decode(content, 0, content.length, charset);
    }

    static String decode(final byte[] content, final int start, final int length, final String charset) {
        // short-circuit if there are only ASCII characters and no "=?"
        final int end = start + length;
        boolean complicated = false;
        for (int pos = start; pos < end && !complicated; pos++) {
            byte c = content[pos];
            if (c < 0 || c > 0x7E || (c == '=' && pos < end - 1 && content[pos + 1] == '?')) {
                complicated = true;
            }
        }
        if (!complicated) {
            try {
                if (charset != null && !charset.trim().equals("")) {
                    return unfold(new String(content, start, length, charset));
                }
            } catch (Exception e) {
            }
            return unfold(new String(content, start, length));
        }

        HeaderUtils.ByteBuilder builder = new HeaderUtils.ByteBuilder(length, charset);
        String value = null;
        boolean encoded = false;
        Boolean encwspenc = Boolean.FALSE;
        int questions = 0, wsplength = 0;

        for (int pos = start; pos < end; pos++) {
            byte c = content[pos];
            if (c == '\r' || c == '\n') {
                // ignore folding
            } else if (c == '=' && pos < end - 2 && content[pos + 1] == '?' && (!encoded || content[pos + 2] != '=')) {
                // "=?" marks the beginning of an encoded-word
                if (!builder.isEmpty()) {
                    value = builder.appendTo(value);
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
                // drop all whitespace between encoded-words
                if (valid && encwspenc == Boolean.TRUE) {
                    value = value.substring(0, value.length() - wsplength);
                }
                value = value == null ? decoded : value + decoded;
                encwspenc = valid ? null : Boolean.FALSE;  wsplength = 0;
                encoded = false;  builder.reset();
            } else {
                builder.write(c);
                // track whitespace after encoded-words (enc wsp enc => remove wsp)
                boolean isWhitespace = c == ' ' || c == '\t';
                if (!encoded && encwspenc != Boolean.FALSE) {
                    encwspenc = isWhitespace;
                    wsplength = isWhitespace ? wsplength + 1 : 0;
                }
            }
        }

        if (!builder.isEmpty()) {
            value = builder.appendTo(value);
        }
        return value == null ? "" : value;
    }

    static String unfold(final String folded) {
        int length = folded.length(), i;
        for (i = 0; i < length; i++) {
            char c = folded.charAt(i);
            if (c == '\r' || c == '\n') {
                break;
            }
        }
        if (i == length) {
            return folded;
        }

        StringBuilder unfolded = new StringBuilder(length);
        if (i > 0) {
            unfolded.append(folded, 0, i);
        }
        while (++i < length) {
            char c = folded.charAt(i);
            if (c != '\r' && c != '\n') {
                unfolded.append(c);
            }
        }
        return unfolded.toString();
    }


    static class EncodedWord {
        static String encode(String value, String charset) {
            StringBuilder sb = new StringBuilder();

            // FIXME: need to limit encoded-words to 75 bytes
            charset = StringUtil.checkCharset(value, charset);
            byte[] content = null;
            try {
                content = value.getBytes(charset);
            } catch (OutOfMemoryError e) {
                try {
                    ZimbraLog.system.fatal("out of memory", e);
                } finally {
                    Runtime.getRuntime().halt(1);
                }
            } catch (Throwable t) {
                content = value.getBytes();
                charset = Charset.defaultCharset().displayName();
            }
            sb.append("=?").append(charset);

            int invalidQ = 0;
            for (int i = 0; i < content.length; i++) {
                if (content[i] < 0 || Q2047Encoder.FORCE_ENCODE[content[i]]) {
                    invalidQ++;
                }
            }

            InputStream encoder;
            if (invalidQ > content.length / 3) {
                sb.append("?B?");  encoder = new B2047Encoder(content);
            } else {
                sb.append("?Q?");  encoder = new Q2047Encoder(content);
            }

            try {
                sb.append(new String(ByteUtil.readInput(encoder, 0, Integer.MAX_VALUE)));
            } catch (IOException ioe) {
            }
            sb.append("?=");

            return sb.toString();
        }

        private static class Q2047Encoder extends ContentTransferEncoding.QuotedPrintableEncoderStream {
            static final boolean[] FORCE_ENCODE = new boolean[128];
            static {
                for (int i = 0; i < FORCE_ENCODE.length; i++) {
                    FORCE_ENCODE[i] = true;
                }
                for (int c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!*+-/ ".getBytes()) {
                        FORCE_ENCODE[c] = false;
                }
            }

            Q2047Encoder(byte[] content) {
                super(new ByteArrayInputStream(content), null);  disableFolding();  setForceEncode(FORCE_ENCODE);
            }

            @Override
            public int read() throws IOException {
                int c = super.read();  return c == ' ' ? '_' : c;
            }
        }

        private static class B2047Encoder extends ContentTransferEncoding.Base64EncoderStream {
            B2047Encoder(byte[] content) {
                super(new ByteArrayInputStream(content));  disableFolding();
            }
        }
    }

    /** Characters that can form part of an RFC 2822 atom. */
    static final boolean[] ATEXT_VALID = new boolean[128];
    static {
        for (int c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&'*+-/=?^_`{|}~".getBytes()) {
            ATEXT_VALID[c] = true;
        }
    }

    static String escape(String value, String charset, boolean phrase) {
        boolean needsQuote = false, wsp = true;
        int needs2047 = 0, needsEscape = 0, cleanTo = 0, cleanFrom = value.length();
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c > 0x7F || c == '\0' || c == '\r' || c == '\n') {
                needs2047++;  cleanFrom = len;
            } else if (!phrase) {
                // if we're not in an RFC 2822 phrase, there is no such thing as "quoting"
            } else if (c == '"' || c == '\\') {
                needsQuote = true;  needsEscape++;  cleanFrom = len;
            } else if ((c != ' ' && !ATEXT_VALID[c]) || (c == ' ' && wsp)) {
                needsQuote = true;  cleanFrom = len;
            }
            wsp = c == ' ';
            if (wsp) {
                if (!needsQuote && needs2047 == 0 && i != len - 1) {
                    cleanTo = i + 1;
                } else if (cleanFrom == len && i > cleanTo + 1) {
                    cleanFrom = i;
                }
            }
        }
        if (phrase) {
            needsQuote |= wsp;
        }
        if (wsp) {
            cleanFrom = value.length();
        }

        if (needs2047 > 0) {
            String prefix = value.substring(0, cleanTo), suffix = value.substring(cleanFrom);
            return prefix + EncodedWord.encode(value.substring(cleanTo, cleanFrom), charset) + suffix;
        } else if (needsQuote && needsEscape > 0) {
            return quote(value, needsEscape);
        } else if (needsQuote) {
            return new StringBuilder(value.length() + 2).append('"').append(value).append('"').toString();
        } else {
            return value;
        }
    }

    static String quote(String value) {
        return quote(value, 0);
    }

    private static String quote(String value, int escapeHint) {
        StringBuilder sb = new StringBuilder(value.length() + escapeHint + 2).append('"');
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.append('"').toString();
    }

    protected void reserialize() {
    }

    @Override
    public String toString() {
        String header = new String(mContent);
        return header.endsWith("\r\n") ? header.substring(0, header.length() - 2) : header;
    }


    public static class MimeAddressHeader extends MimeHeader {
        private List<InternetAddress> mAddresses;

        public MimeAddressHeader(final String name, final List<InternetAddress> iaddrs) {
            super(name, null, -1);
            mAddresses = new ArrayList<InternetAddress>(iaddrs);
        }

        public MimeAddressHeader(final String name, final String value) {
            super(name, value);
            if (mValueStart > 0) {
                mAddresses = InternetAddress.parseHeader(mContent,
                        mValueStart, mContent.length - mValueStart);
            } else {
                mAddresses = new ArrayList<InternetAddress>();
            }
        }

        public MimeAddressHeader(final String name, final byte[] bvalue) {
            super(name, bvalue);
            if (mValueStart > 0) {
                mAddresses = InternetAddress.parseHeader(mContent,
                        mValueStart, mContent.length - mValueStart);
            } else {
                mAddresses = new ArrayList<InternetAddress>();
            }
        }

        public List<InternetAddress> getAddresses() {
            return new ArrayList<InternetAddress>(mAddresses);
        }

        public MimeAddressHeader addAddress(InternetAddress iaddr) {
            mAddresses.add(iaddr);  mContent = null;  mValueStart = -1;  return this;
        }

        @Override
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
}
