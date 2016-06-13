/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.security.MessageDigest;

public class DigestStream extends BufferStream {
    private MessageDigest messageDigest;

    public DigestStream() { this(0); }

    public DigestStream(long sizeHint) { this(sizeHint, Integer.MAX_VALUE); }

    public DigestStream(long sizeHint, int maxBuffer) {
        this(sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public DigestStream(long sizeHint, int maxBuffer, long maxSize) {
        super(sizeHint, maxBuffer, maxSize);
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize " +
                DigestStream.class.getSimpleName(), e);
        }
    }
    
    public String getDigest() {
        return ByteUtil.encodeFSSafeBase64(messageDigest.digest());
    }

    public void write(int data) {
        super.write(data);
        messageDigest.update((byte)data);
    }
    
    public void write(byte data[], int off, int len) {
        super.write(data, off, len);
        messageDigest.update(data, off, len);
    }
}
