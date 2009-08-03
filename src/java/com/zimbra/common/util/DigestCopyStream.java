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

import java.io.IOException;
import java.security.MessageDigest;

public class DigestCopyStream extends CopyStream {
    private MessageDigest messageDigest;

    public DigestCopyStream() { this(0); }

    public DigestCopyStream(long sizeHint) { this(sizeHint, BUFFER_SIZE); }

    public DigestCopyStream(long sizeHint, int maxBuffer) {
        this(sizeHint, maxBuffer, Long.MAX_VALUE);
    }

    public DigestCopyStream(long sizeHint, int maxBuffer, long maxSize) {
        super(sizeHint, maxBuffer, maxSize);
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize " +
                DigestCopyStream.class.getSimpleName(), e);
        }
    }
    
    public String getDigest() {
        return ByteUtil.encodeFSSafeBase64(messageDigest.digest());
    }

    public void write(int data) throws IOException {
        super.write(data);
        messageDigest.update((byte)data);
    }
    
    public void write(byte data[], int off, int len) throws IOException {
        super.write(data, off, len);
        messageDigest.update(data, off, len);
    }
}
