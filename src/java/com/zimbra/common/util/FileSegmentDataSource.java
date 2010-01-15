/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.common.util.ByteUtil.SegmentInputStream;

/**
 * <tt>DataSource</tt> implementation that returns a stream to a section of a file.
 */
public class FileSegmentDataSource implements DataSource {

    private File mFile;
    private long mOffset;
    private long mLength;
    private String mContentType;

    public FileSegmentDataSource(File file, long offset, long length) {
        mFile = file;
        mOffset = offset;
        mLength = length;
    }
    
    public String getContentType() {
        if (mContentType == null) {
            return "application/octet-stream";
        }
        return mContentType;
    }

    public InputStream getInputStream() throws IOException {
        InputStream in = new FileInputStream(mFile);
        if (mOffset > 0 || mLength != mFile.length()) {
            in = SegmentInputStream.create(in, mOffset, mOffset + mLength);
        }
        return in;
    }

    public String getName() {
        return mFile.getName();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("not supported");
    }
}
