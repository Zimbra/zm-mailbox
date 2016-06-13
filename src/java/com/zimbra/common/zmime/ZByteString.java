/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.zmime;

import java.nio.charset.Charset;
import java.util.List;

import com.zimbra.common.zmime.ZMimeUtility.ByteBuilder;

public class ZByteString {

    private byte[] bytes;
    private int offset;
    private int length;
    private Charset charset;

    public ZByteString(byte[] bytes, int offset, int length, Charset charset) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
        this.charset = charset;
    }

    public ZByteString(ByteBuilder builder) {
        this.bytes = builder.toByteArray();
        this.offset = 0;
        this.length = this.bytes.length;
        this.charset = builder.getCharset();
    }

    public boolean canMerge (ZByteString byteString) {
        return this.charset.equals(byteString.charset);
    }

    public static String makeString(List<ZByteString> byteStrings) {
        StringBuilder builder = new StringBuilder();
        ZByteString lastByteString = null;
        for (ZByteString bStr : byteStrings) {
            if (lastByteString != null) {
                if (lastByteString.canMerge(bStr)) {
                    lastByteString = lastByteString.merge(bStr);
                }
                else {
                    builder.append(lastByteString.toString());
                    lastByteString = bStr;
                }
            }
            else {
                lastByteString = bStr;
            }
        }
        if (lastByteString != null) {
            builder.append(lastByteString.toString());
        }
        return builder.toString();
    }

    public ZByteString merge (ZByteString byteString) throws IllegalArgumentException {
        if (this.canMerge(byteString)) {
            int newLength = this.length + byteString.length;
            byte[] newBytes = new byte[newLength];
            System.arraycopy(this.bytes, this.offset, newBytes, 0, this.length);
            System.arraycopy(byteString.bytes, byteString.offset, newBytes, this.length, byteString.length);
            return new ZByteString(newBytes, 0, newLength, this.charset);
        } else {
            throw new IllegalArgumentException("merged charsets must match");
        }
    }

    @Override
    public String toString () {
        return new String(this.bytes, this.offset, this.length, this.charset);
    }
}

