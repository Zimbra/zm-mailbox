/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016, 2018 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author zimbra
 *
 */
public class BufferStreamRequestEntity extends BufferStream {

    String contentType;
    private InputStream is = null;

    public BufferStreamRequestEntity() { this(0); }

    public BufferStreamRequestEntity(long sizeHint) {
        this(sizeHint, Integer.MAX_VALUE);
    }

    public BufferStreamRequestEntity(long sizeHint, int maxBuffer) {
        this(sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public BufferStreamRequestEntity(long sizeHint, int maxBuffer, long maxSize) {
        this(null, null, sizeHint, maxBuffer, maxSize);
    }

    public BufferStreamRequestEntity(InputStream is) { this(is, 0); }

    public BufferStreamRequestEntity(InputStream is, long sizeHint) {
        this(is, sizeHint, Integer.MAX_VALUE);
    }

    public BufferStreamRequestEntity(InputStream is, long sizeHint, int maxBuffer) {
        this(is, null, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public BufferStreamRequestEntity(InputStream is, String contentType) {
        this(is, contentType, 0);
    }

    public BufferStreamRequestEntity(InputStream is, String contentType, long
        sizeHint) {
        this(is, contentType, sizeHint, Integer.MAX_VALUE);
    }

    public BufferStreamRequestEntity(InputStream is, String contentType,
        long sizeHint, int maxBuffer) {
        this(is, contentType, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public BufferStreamRequestEntity(String contentType) { this(contentType, 0); }

    public BufferStreamRequestEntity(String contentType, long sizeHint) {
        this(contentType, sizeHint, Integer.MAX_VALUE);
    }

    public BufferStreamRequestEntity(String contentType, long sizeHint, int
        maxBuffer) {
        this(null, contentType, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public BufferStreamRequestEntity(InputStream is, String contentType, long
        sizeHint, int maxBuffer, long maxSize) {
        super(sizeHint, maxBuffer, maxSize);
        this.contentType = contentType;
        this.is = is;
    }
   
    private void readData() {
        if (is != null) {
            try {
                readFrom(is);
            } catch (IOException e) {
            }
            is = null;
        }
    }
    
    public void writeRequest(final OutputStream os) throws IOException {
        readData();
        copyTo(os);
    }
}
