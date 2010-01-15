/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.util.ByteArrayDataSource;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileSegmentDataSource;
import com.zimbra.common.mime.MimeConstants;

class RedoableOpData {
    private int mLength;
    private DataSource mDataSource;
    
    RedoableOpData(byte[] data) {
        mDataSource = new ByteArrayDataSource(data, MimeConstants.CT_APPLICATION_OCTET_STREAM);
        mLength = data.length;
    }
    
    RedoableOpData(File file) {
        mDataSource = new FileDataSource(file);
        mLength = (int) file.length();
    }
    
    RedoableOpData(File file, long offset, int length) {
        mDataSource = new FileSegmentDataSource(file, offset, length);
        mLength = length;
    }
    
    RedoableOpData(DataSource dataSource, int length) {
        mDataSource = dataSource;
        mLength = length;
    }
    
    int getLength() {
        return mLength;
    }
    
    byte[] getData()
    throws IOException {
        return ByteUtil.getContent(mDataSource.getInputStream(), mLength);
    }
    
    InputStream getInputStream()
    throws IOException {
        return mDataSource.getInputStream();
    }
}
