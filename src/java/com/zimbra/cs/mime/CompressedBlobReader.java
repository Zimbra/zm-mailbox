/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StorageCallback;

/**
 * Blob storage callback that stores an uncompressed copy
 * of the blob data in memory, when the blob on disk is compressed.
 * Does not store data for blobs that are not compressed on disk. 
 */
public class CompressedBlobReader implements StorageCallback {

    private static final int MAX_INITIAL_BUFFER_SIZE = 10 * 1024 * 1024;
    private ByteArrayOutputStream mBuffer = null;
    private boolean mIsInitialized = false;
    private boolean mIsCompressed;
    private int mSizeHint;
    
    /**
     * Caches the byte array, to ensure that we only call
     * <tt>ByteArrayOutputStream.toByteArray()</tt> once.
     */
    private byte[] mData;
    
    public CompressedBlobReader(int sizeHint) {
        mSizeHint = sizeHint;
    }

    public void wrote(Blob blob, byte[] data, int numBytes)
    throws IOException {
        if (!mIsInitialized) {
            mIsCompressed = blob.isCompressed();
            if (mIsCompressed) {
                if (0 < mSizeHint && mSizeHint <= MAX_INITIAL_BUFFER_SIZE) {
                    mBuffer = new ByteArrayOutputStream(mSizeHint);
                } else {
                    // Deal with dicy size hint values. 
                    mBuffer = new ByteArrayOutputStream();
                }
            }
            mIsInitialized = true;
        }
        
        if (mBuffer != null) {
            mBuffer.write(data, 0, numBytes);
        }
    }
    
    /**
     * Returns the in-memory data collected during blob storage, or
     * <tt>null</tt> if the size limit was exceeded.
     */
    public byte[] getData() {
        if (mData == null) {
            if (mBuffer != null) {
                mData = mBuffer.toByteArray();
                mBuffer = null;
            }
        }
        return mData;
    }
}
