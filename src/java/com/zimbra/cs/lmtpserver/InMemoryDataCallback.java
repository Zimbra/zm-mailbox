/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver;

import java.io.ByteArrayOutputStream;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StorageCallback;

/**
 * Reads stored data into memory up to the specified threshold.
 */
public class InMemoryDataCallback implements StorageCallback {
    
    private int mMaxBytes;
    private ByteArrayOutputStream mBuffer;
    private byte[] mData;

    public InMemoryDataCallback(int sizeHint, int maxBytes) {
        if (sizeHint > maxBytes) {
            sizeHint = maxBytes;
        }
        if (sizeHint > 0) {
            mBuffer = new ByteArrayOutputStream(sizeHint);
        } else {
            mBuffer = new ByteArrayOutputStream();
        }
        mMaxBytes = maxBytes;
    }
    
    public void wrote(Blob blob, byte[] data, int numBytes) {
        if (mBuffer != null) {
            mBuffer.write(data, 0, numBytes);
            if (mBuffer.size() > mMaxBytes) {
                mBuffer = null;
            }
        }
    }

    /**
     * Returns the data that was stored, or <tt>null</tt>
     * if the threshold was exceeded.
     */
    public byte[] getData() {
        if (mData == null && mBuffer != null) {
            mData = mBuffer.toByteArray();
        }
        return mData;
    }
}
