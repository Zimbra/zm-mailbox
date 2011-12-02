/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 VMware, Inc.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import com.zimbra.common.util.ByteUtil;

public enum ZTransferEncoding {
    QUOTED_PRINTABLE("quoted-printable"), BASE64("base64"), SEVEN_BIT("7bit"), EIGHT_BIT("8bit"), BINARY("binary");

    private String mValue;
    private ZTransferEncoding(String value)  { mValue = value; }

    @Override public String toString()  { return mValue; }

    static ZTransferEncoding forString(String cte) {
        if (cte == null)                     return BINARY;
        cte = cte.toLowerCase().trim();
        if (cte.equals("7bit"))              return SEVEN_BIT;
        if (cte.equals("base64"))            return BASE64;
        if (cte.equals("quoted-printable"))  return QUOTED_PRINTABLE;
        if (cte.equals("8bit"))              return EIGHT_BIT;
        return BINARY;
    }

    ZTransferEncoding normalize() {
        return (this == QUOTED_PRINTABLE || this == BASE64 ? this : BINARY);
    }


    static class Base64EncoderStream extends TransferEncodingStream {
        static final byte[] BASE64_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();

        private int column;

        private final int buf[] = new int[4];
        private boolean fold = true;

        Base64EncoderStream(InputStream is) {
            super(is);
        }

        void disableFolding()  { fold = false; }

        @Override public int read() throws IOException {
            if (column < CHUNK_SIZE || !fold) {
                int index = column % 4;
                if (index == 0) {
                    int c1 = nextByte(), c2 = nextByte(), c3 = nextByte();
                    if (c1 == -1) {
                        if (!fold || column == 0)
                            return -1;
                        column = CHUNK_SIZE;  return read();
                    }
                    int accumulator = (c1 << 16) | ((c2 == -1 ? 0 : c2) << 8) | (c3 == -1 ? 0 : c3);
                    buf[0] = BASE64_TABLE[(accumulator >> 18) & 0x3F];
                    buf[1] = BASE64_TABLE[(accumulator >> 12) & 0x3F];
                    buf[2] = c2 == -1 ? (byte) '=' : BASE64_TABLE[(accumulator >> 6) & 0x3F];
                    buf[3] = c3 == -1 ? (byte) '=' : BASE64_TABLE[accumulator & 0x3F];
                }
                column++;
                return buf[index];
            } else if (column == CHUNK_SIZE) {
                column++;  return '\r';
            } else {
                column = 0;  return '\n';
            }
        }
    }

    static class Base64DecoderStream extends TransferEncodingStream {
        private static final int[] BASE64_DECODE = new int[128];
            static {
                for (int i = 0; i < 128; i++)
                    BASE64_DECODE[i] = -1;
                for (int i = 0; i < Base64EncoderStream.BASE64_TABLE.length; i++)
                    BASE64_DECODE[Base64EncoderStream.BASE64_TABLE[i]] = i;
            }

        private boolean closed;
        private int position = 3, valid = 3;
        private final int buf[] = new int[3];

        Base64DecoderStream(InputStream is) {
            super(is);
        }

        @Override public int read() throws IOException {
            if (position >= valid) {
                if (closed)
                    return -1;

                int accumulator = 0, bytes = 0, c, decoded;
                do {
                    if ((c = nextByte()) >= 0 && c < 128 && (decoded = BASE64_DECODE[c]) != -1) {
                        accumulator = (accumulator << 6) | decoded;  bytes++;
                    }
                } while (c != -1 && c != '=' && bytes < 4);

                if (bytes == 0) {
                    closed = true;  valid = 0;  return -1;
                } else if (bytes < 4) {
                    closed = true;  valid = (bytes == 3 ? 2 : 1);
                    while (bytes++ < 4) {
                        accumulator <<= 6;
                    }
                }

                buf[0] = (accumulator >> 16) & 0xFF;
                buf[1] = (accumulator >> 8) & 0xFF;
                buf[2] = accumulator & 0xFF;
                position = 0;
            }
            return buf[position++];
        }
    }

    static class QuotedPrintableDecoderStream extends TransferEncodingStream {
        private static final int[] QP_DECODE = new int[128];
        static {
            for (int i = 0; i < 128; i++) {
                QP_DECODE[i] = -1;
            }
            for (int i = 0; i < QuotedPrintableEncoderStream.QP_TABLE.length; i++) {
                QP_DECODE[QuotedPrintableEncoderStream.QP_TABLE[i]] = i;
                QP_DECODE[Character.toLowerCase(QuotedPrintableEncoderStream.QP_TABLE[i])] = i;
            }
        }

        private boolean trim = true;
        private int wsp = 0;
        private final byte[] wspbuf = new byte[1000];

        QuotedPrintableDecoderStream(InputStream is) {
            super(is, 1000);
        }

        QuotedPrintableDecoderStream disableTrimming() {
            trim = false;
            return this;
        }

        @Override public int read() throws IOException {
            int c = nextByte();
            if (c == '=') {
                int p1 = nextByte(), p2 = nextByte();
                if (p1 >= 0x30 && p1 <= 0x66 && p2 >= 0x30 && p2 <= 0x66) {
                    int hi4 = QP_DECODE[p1], lo4 = QP_DECODE[p2];
                    if (hi4 != -1 && lo4 != -1)
                        return (hi4 << 4) | lo4;
                } else if (p1 == '\r' && p2 == '\n') {
                    return read();
                } else if (p1 == '\n') {
                    unread(p2);  return read();
                }
                unread(p2);  unread(p1);
            } else if ((c == ' ' || c == '\t') && trim) {
                if (wsp > 0) {
                    wsp--;
                } else {
                    // wsp+ EOL drops the wsp, wsp+ non-wsp preserves it
                    final byte[] peekahead = wspbuf;
                    int p1, p2 = -1;
                    do {
                        peekahead[wsp++] = (byte) (p1 = nextByte());
                    } while ((p1 == ' ' || p1 == '\t') && wsp <= 998);

                    if (p1 == -1 || p1 == '\n' || (p1 == '\r' && (p2 = nextByte()) == '\n')) {
                        unread(p2);  c = p1;  wsp = 0;
                    } else {
                        unread(p2);  unread(peekahead, 0, wsp--);
                    }
                }
            }
            return c;
        }
    }

    static class QuotedPrintableEncoderStream extends TransferEncodingStream {
        static final byte[] QP_TABLE = "0123456789ABCDEF".getBytes();

        private int column, valid, out1, out2;
        private boolean fold = true, text, force[];

        QuotedPrintableEncoderStream(InputStream is, ZContentType ctype) {
            super(is, 2);  setContentType(ctype);
        }

        QuotedPrintableEncoderStream setContentType(ZContentType ctype) {
            text = ctype == null || ctype.getPrimaryType().equals("text");  return this;
        }

        QuotedPrintableEncoderStream setForceEncode(boolean[] characters) {
            if (characters == null || characters.length != 128)
                throw new IllegalArgumentException("invalid force-encode array length");
            force = characters;
            return this;
        }

        QuotedPrintableEncoderStream disableFolding() {
            fold = false;
            return this;
        }

        @Override public int read() throws IOException {
            if (valid == 0) {
                if (++column >= CHUNK_SIZE && fold) {
                    out1 = '\r';  out2 = '\n';  valid = 2;  return '=';
                }
                int p1, c = nextByte();
                if (c == -1) {
                    return -1;
                } else if (text && (c == '\r' || c == '\n')) {
                    if (c == '\n' || (p1 = nextByte()) == '\n') {
                        out2 = '\n';  valid = 1;  return '\r';
                    }
                    unread(p1);
                }
                if (c == ' ' || c == '\t') {
                    unread(p1 = nextByte());
                    if (p1 != '\r' && (force == null || !force[c]))
                        return c;
                } else if (c > 0x20 && c < 0x7F && c != '=' && (force == null || !force[c])) {
                    return c;
                }
                if (fold && column >= CHUNK_SIZE - 3) {
                    unread(c);  out1 = '\r';  out2 = '\n';  valid = 2;  return '=';
                } else {
                    out1 = QP_TABLE[(c >> 4) & 0x0F];  out2 = QP_TABLE[c & 0x0F];  valid = 2;  return '=';
                }
            } else if (valid == 1) {
                valid = 0;  column = (out2 == '\n' ? 0 : column + 1);  return out2;
            } else {
                valid = 1;  column++;  return out1;
            }
        }
    }

    private static class TransferEncodingStream extends PushbackInputStream {
        static final int CHUNK_SIZE = 76;

        private static InputStream ensureBuffering(InputStream is) {
            return is instanceof ByteArrayInputStream || is instanceof BufferedInputStream ? is : new BufferedInputStream(is, 4096);
        }

        TransferEncodingStream(InputStream is) {
            super(ensureBuffering(is));
        }

        TransferEncodingStream(InputStream is, int pushBuffer) {
            super(ensureBuffering(is), pushBuffer);
        }

        int nextByte() throws IOException {
            return super.read();
        }

        boolean canUnread() {
            return pos > 0;
        }

        @Override public void unread(int b) throws IOException {
            if (b != -1) {
                super.unread(b);
            }
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (b == null || in == null)
                throw new NullPointerException();
            if (off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0)
                throw new IndexOutOfBoundsException();
            if (len == 0)
                return 0;

            int i = 0, c;
            do {
                b[off + i] = (byte) (c = read());
            } while (c != -1 && ++i < len);
            return i == 0 ? -1 : i;
        }

        @Override public long skip(long n) throws IOException {
            // can't just skip in the underlying stream, since there isn't a 1-to-1 mapping of underlying bytes and decoded bytes
            return ByteUtil.skip(this, n);
        }
    }
}