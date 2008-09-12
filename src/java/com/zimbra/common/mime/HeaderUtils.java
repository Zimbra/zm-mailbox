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

public class HeaderUtils {
    static String decodeWord(byte[] word) {
        int length = word.length;
        if (length <= 8 || word[0] != '=' || word[1] != '?' || word[length-2] != '?' || word[length-1] != '=')
            return null;

        byte b;
        int pos = 2, star = -1;
        while (pos < length && (b = word[pos]) != '?') {
            // handle RFC 2231 "*lang" in the charset portion of the encoded-word
            if (star == -1 && b == '*')
                star = pos;
            pos++;
        }
        if (pos >= length - 4 || pos == 2)
            return null;
        String charset = new String(word, 2, (star == -1 ? pos : star) - 2);

        InputStream decoder;
        byte encoding = word[++pos];
        if (word[++pos] != '?')
            return null;
        int remaining = length - pos - 3;
        if (remaining == 0)
            return "";
        if (encoding == 'Q' || encoding == 'q')
            decoder = new QP2047Decoder(new ByteArrayInputStream(word, pos + 1, remaining));
        else if (encoding == 'B' || encoding == 'b')
            decoder = new ContentTransferEncoding.Base64DecoderStream(new ByteArrayInputStream(word, pos + 1, remaining));
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

    private static class QP2047Decoder extends ContentTransferEncoding.QuotedPrintableDecoderStream {
        QP2047Decoder(ByteArrayInputStream bais)  { super(bais); }

        @Override protected int nextByte() throws IOException {
            int c = super.nextByte();  return c == '_' ? ' ' : c;
        }
    }


    static class ByteBuilder extends ByteArrayOutputStream {
        private String mCharset;

        ByteBuilder()          { super(); }
        ByteBuilder(int size)  { super(size); }
        ByteBuilder(String charset)            { this();  mCharset = charset; }
        ByteBuilder(int size, String charset)  { this(size);  mCharset = charset; }

        ByteBuilder pop()      { if (count > 0) count--;  return this; }
        boolean isEmpty()      { return count == 0; }

        String appendTo(String prefix) {
            return prefix == null ? toString() : prefix + this;
        }

        @Override public String toString() {
            try {
                if (mCharset != null && !mCharset.trim().equals(""))
                    return super.toString(mCharset);
            } catch (Throwable t) { }
            return super.toString();
        }
    }
}
