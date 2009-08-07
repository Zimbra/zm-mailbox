/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ByteUtil.SegmentInputStream;

class RedoableOpData {

    private byte[] mData;
    private File mFile;
    private long mFileOffset;
    private int mLength;
    
    RedoableOpData(byte[] data) {
        mData = data;
        mLength = data.length;
    }
    
    RedoableOpData(File file) {
        this(file, 0, (int) file.length());
    }
    
    RedoableOpData(File file, long offset, int length) {
        mFile = file;
        mFileOffset = offset;
        mLength = length;
    }
    
    RedoableOpData(InputStream in, int length) {
        // XXX bburtin - replace this constructor with one that takes
        // a Java Activation DataSource, so we don't have to read the
        // entire stream into memory.
        try {
            mData = ByteUtil.getContent(in, length);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read content stream", e);
        }
        mLength = mData.length;
    }
    
    int getLength() {
        return mLength;
    }
    
    byte[] getData()
    throws IOException {
        if (mData == null) {
            if (mFile != null) {
                RandomAccessFile file = new RandomAccessFile(mFile, "r");
                file.seek(mFileOffset);
                mData = new byte[mLength];
                int numRead = file.read(mData);
                file.close();
                if (numRead != mLength) {
                    String msg = String.format("Attempted to read %d bytes from %s at offset %d.  Actually read %d.",
                        mLength, mFile.getPath(), mFileOffset, numRead);
                    throw new IOException(msg);
                }
            }
        }
        assert(mData != null);
        return mData;
    }
    
    InputStream getInputStream()
    throws IOException {
        if (mData != null) {
            return new ByteArrayInputStream(mData);
        }
        if (mFile != null) {
            InputStream in = new FileInputStream(mFile);
            if (mFileOffset > 0 || mLength != mFile.length()) {
                in = SegmentInputStream.create(in, mFileOffset, mFileOffset + mLength);
            }
            return in;
        }
        assert(false);
        return null;
    }
}
