/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.zmime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.zimbra.common.util.CharsetUtil;

public class ZMimeUtility {
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
            decoder = new ZTransferEncoding.Base64DecoderStream(new ByteArrayInputStream(word, pos + 1, remaining));
        } else {
            return null;
        }

        try {
            byte[] dbuffer = new byte[word.length];
            int dsize = decoder.read(dbuffer);
            return new String(dbuffer, 0, dsize, CharsetUtil.normalizeCharset(charset));
        } catch (OutOfMemoryError oome) {
            throw oome;
        } catch (Error e) { // bug 40926 - catch java.lang.Error thrown by String class for invalid charset issues
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static class QP2047Decoder extends ZTransferEncoding.QuotedPrintableDecoderStream {
        QP2047Decoder(ByteArrayInputStream bais) {
            super(bais);
            disableTrimming();
        }

        @Override
        protected int nextByte() throws IOException {
            int c = super.nextByte();
            return c == '_' ? ' ' : c;
        }
    }


    public static class ByteBuilder extends ByteArrayOutputStream {
        private Charset charset;

        public ByteBuilder() {
            super();
        }

        public ByteBuilder(int size) {
            super(size);
        }

        public ByteBuilder(String charset) throws UnsupportedEncodingException {
            this();
            setCharset(charset);
        }

        public ByteBuilder(int size, String charset) throws UnsupportedEncodingException {
            this(size);
            setCharset(charset);
        }

        public ByteBuilder(Charset charset) {
            this();
            setCharset(charset);
        }

        public ByteBuilder(int size, Charset charset) {
            this(size);
            setCharset(charset);
        }

        public ByteBuilder(byte[] b) {
            super((int) (b.length * 1.5 + 1));
            append(b);
        }

        public ByteBuilder setCharset(String enc) throws UnsupportedEncodingException {
            this.charset = CharsetUtil.normalizeCharset(enc);
            return this;
        }

        public ByteBuilder setCharset(Charset charset) {
            this.charset = CharsetUtil.normalizeCharset(charset);
            return this;
        }

        public ByteBuilder pop() {
            if (count > 0) {
                count--;
            }
            return this;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        public int length() {
            return count;
        }

        public ByteBuilder append(ByteBuilder bb) {
            write(bb.buf, 0, bb.count);
            return this;
        }

        public ByteBuilder append(byte b) {
            write(b);
            return this;
        }

        public ByteBuilder append(byte[] b) {
            write(b, 0, b.length);
            return this;
        }

        public ByteBuilder append(char c) {
            return append((byte) c);
        }

        public ByteBuilder append(String s) {
            return append(charset == null ? s.getBytes() : s.getBytes(charset));
        }

        public byte byteAt(int index) {
            return buf[index];
        }

        public int indexOf(byte b) {
            for (int i = 0; i < count; i++) {
                if (buf[i] == b) {
                    return i;
                }
            }
            return -1;
        }

        public boolean startsWith(byte b) {
            return count > 0 && buf[0] == b;
        }

        public boolean endsWith(byte b) {
            return count > 0 && buf[count - 1] == b;
        }

        @Override
        public synchronized String toString() {
            return charset == null ? super.toString() : new String(buf, 0, count, charset);
        }

        public String appendTo(String prefix) {
            return prefix == null ? toString() : prefix + this;
        }
    }
}
