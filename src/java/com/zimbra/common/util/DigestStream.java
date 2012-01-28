/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
