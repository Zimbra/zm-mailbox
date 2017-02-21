/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
