/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.store.BlobInputStream;


class RedoableOpData {

    private byte[] mData;
    private File mFile;
    private long mFileOffset;
    private InputStream mInputStream;
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
        mInputStream = in;
        mLength = length;
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
            if (mInputStream != null) {
                mData = ByteUtil.getContent(mInputStream, 1024);
            }
        }
        assert(mData != null);
        return mData;
    }
    
    InputStream getInputStream()
    throws IOException {
        if (mInputStream != null) {
            return mInputStream;
        }
        if (mData != null) {
            return new ByteArrayInputStream(mData);
        }
        if (mFile != null) {
            return new BlobInputStream(mFile, mFileOffset, mFileOffset + mLength);
        }
        assert(false);
        return null;
    }
    
    boolean hasDataInMemory() {
        return (mData != null);
    }
}
