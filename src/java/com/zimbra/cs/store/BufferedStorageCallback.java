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
package com.zimbra.cs.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;

public class BufferedStorageCallback extends StorageCallback {
    private byte array[] = null;
    private ByteArrayOutputStream byteStream;
    private DigestOutputStream digestStream;
    private long maxSize;
    private MessageDigest messageDigest;
    private long size = 0;

    public BufferedStorageCallback(long sizeHint) throws ServiceException {
        this(sizeHint, getDiskStreamingThreshold());
    }
    
    public BufferedStorageCallback(long sizeHint, int maxSize) {
        this.maxSize = maxSize;
        if (sizeHint > maxSize)
            sizeHint = maxSize;
        if (sizeHint > 0)
            byteStream = new ByteArrayOutputStream((int)sizeHint);
        else if (maxSize > 0)
            byteStream = new ByteArrayOutputStream();
        else
            byteStream = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize " +
                BufferedStorageCallback.class.getSimpleName(), e);
        }
        digestStream  = new DigestOutputStream(byteStream, messageDigest);
    }
    
    @Override
    public void wrote(Blob blob, byte[] data, int offset, int len) throws
        IOException {
        if (data.length - offset < len)
            len = data.length - offset;
        size += len;
        if (size > maxSize)
            byteStream = null;
        else if (byteStream != null)
            byteStream.write(data, offset, len);
        digestStream.write(data, offset, len);
    }

    public byte[] getData() {
        if (array == null && byteStream != null)
            array = byteStream.toByteArray();
        return array;
    }
    
    public String getDigest() {
        return ByteUtil.encodeFSSafeBase64(messageDigest.digest());
    }
    
    public long getSize() { return size; }
}
