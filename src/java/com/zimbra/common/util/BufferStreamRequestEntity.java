/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.RequestEntity;

public class BufferStreamRequestEntity extends BufferStream implements
    RequestEntity {
    String contentType = null;
    private InputStream is = null;

    public BufferStreamRequestEntity() { this(null, 0); }

    public BufferStreamRequestEntity(long sizeHint, int maxBuffer) {
        this(null, sizeHint, maxBuffer);
    }

    public BufferStreamRequestEntity(long sizeHint, int maxBuffer, long maxSize) {
        this(null, sizeHint, maxBuffer, maxSize);
    }

    public BufferStreamRequestEntity(InputStream is) { this(is, 0); }

    public BufferStreamRequestEntity(InputStream is, long sizeHint) {
        this(is, sizeHint, Integer.MAX_VALUE);
    }

    public BufferStreamRequestEntity(InputStream is, long sizeHint, int maxBuffer) {
        this(is, sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public BufferStreamRequestEntity(InputStream is, long sizeHint, int
        maxBuffer, long maxSize) {
        super(sizeHint, maxBuffer, maxSize);
        this.is = is;
    }

    public long getContentLength() { return getSize(); }

    public String getContentType() { return contentType; }

    public boolean isRepeatable() { return true; }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void writeRequest(final OutputStream out) throws IOException {
        if (is != null) {
            copy(is);
            is = null;
        }
        
        Pair<byte[], Integer> rawBuf = getRawBuffer();
        File file = getFile();
        
        if (rawBuf != null)
            out.write(rawBuf.getFirst(), 0, rawBuf.getSecond());
        if (file != null) {
            byte buf[] = new byte[32 * 1024];
            FileInputStream fis = new FileInputStream(file);
            int in;

            try {
                while ((in = fis.read(buf)) != -1)
                    out.write(buf, 0, in);
            } finally {
                fis.close();
            }
        }
    }
}
