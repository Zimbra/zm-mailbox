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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class HeaderUtils {

    static String decodeWord(byte[] word) {
        int length = word.length;
        if (length <= 8 || word[0] != '=' || word[1] != '?' || word[length - 2] != '?' || word[length - 1] != '=') {
            return null;
        }

        byte b;
        int pos = 2, star = -1;
        while (pos < length && (b = word[pos]) != '?') {
            // handle RFC 2231 "*lang" in the charset portion of the encoded-word
            if (star == -1 && b == '*') {
                star = pos;
            }
            pos++;
        }
        if (pos >= length - 4 || pos == 2) {
            return null;
        }
        String charset = new String(word, 2, (star == -1 ? pos : star) - 2);

        InputStream decoder;
        byte encoding = word[++pos];
        if (word[++pos] != '?') {
            return null;
        }
        int remaining = length - pos - 3;
        if (remaining == 0) {
            return "";
        }
        if (encoding == 'Q' || encoding == 'q') {
            decoder = new QP2047Decoder(new ByteArrayInputStream(word, pos + 1, remaining));
        } else if (encoding == 'B' || encoding == 'b') {
            decoder = new ContentTransferEncoding.Base64DecoderStream(new ByteArrayInputStream(word, pos + 1, remaining));
        } else {
            return null;
        }

        try {
            byte[] dbuffer = new byte[word.length];
            int dsize = decoder.read(dbuffer);
            return new String(dbuffer, 0, dsize, normalizeCharset(charset));
        } catch (OutOfMemoryError oome) {
            throw oome;
        } catch (Error e) { // bug 40926 - catch java.lang.Error thrown by String class for invalid charset issues 
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static final String P_CHARSET_CP1252 = "windows-1252";
    private static final String P_CHARSET_EUC_CN = "euc_cn";
    private static final String P_CHARSET_GB2312 = "gb2312";
    private static final String P_CHARSET_GBK = "gbk";
    private static final String P_CHARSET_LATIN1 = "iso-8859-1";

    private static final boolean SUPPORTS_CP1252 = Charset.isSupported(P_CHARSET_CP1252);
    private static final boolean SUPPORTS_GBK = Charset.isSupported(P_CHARSET_GBK);

    static String normalizeCharset(String enc) {
        if (enc == null || enc.equals("")) {
            return enc;
        }

        String charset = enc.trim(), lccharset = charset.toLowerCase();
        if (SUPPORTS_CP1252 && lccharset.equals(P_CHARSET_LATIN1)) {
            return P_CHARSET_CP1252;
        } else if (SUPPORTS_GBK && (lccharset.equals(P_CHARSET_GB2312) || lccharset.equals(P_CHARSET_EUC_CN))) {
            return P_CHARSET_GBK;
        }
        return charset;
    }

    private static class QP2047Decoder extends ContentTransferEncoding.QuotedPrintableDecoderStream {
        QP2047Decoder(ByteArrayInputStream bais) {
            super(bais);
        }

        @Override protected int nextByte() throws IOException {
            int c = super.nextByte();
            return c == '_' ? ' ' : c;
        }
    }

    static class ByteBuilder extends ByteArrayOutputStream {
        private String mCharset;

        ByteBuilder() {
            super();
        }

        ByteBuilder(int size) {
            super(size);
        }

        ByteBuilder(String charset) {
            this();
            mCharset = charset;
        }

        ByteBuilder(int size, String charset) {
            this(size);
            mCharset = charset;
        }

        ByteBuilder pop() {
            if (count > 0) {
                count--;
            }
            return this;
        }

        ByteBuilder setCharset(String charset) {
            mCharset = charset;
            return this;
        }

        boolean isEmpty() {
            return count == 0;
        }

        public int length() {
            return count;
        }

        ByteBuilder append(ByteBuilder bb) {
            write(bb.buf, 0, bb.count);
            return this;
        }

        ByteBuilder append(byte b) {
            write(b);
            return this;
        }

        ByteBuilder append(byte[] b) {
            write(b, 0, b.length);
            return this;
        }

        ByteBuilder append(char c) {
            return append((byte) c);
        }

        ByteBuilder append(String s) {
            return append(s.getBytes());
        }

        byte byteAt(int index) {
            return buf[index];
        }

        int indexOf(byte b) {
            for (int i = 0; i < count; i++) {
                if (buf[i] == b) {
                    return i;
                }
            }
            return -1;
        }

        @Override public synchronized String toString() {
            try {
                if (mCharset != null && !mCharset.trim().equals("")) {
                    return super.toString(mCharset);
                }
            } catch (Exception e) {
            }
            return super.toString();
        }

        String appendTo(String prefix) {
            return prefix == null ? toString() : prefix + this;
        }
    }
}
