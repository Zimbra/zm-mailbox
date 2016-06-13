/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.common.mime.MimeConstants;

public class RedoableOpDataSource implements DataSource {

    private RedoableOpData mData;
    
    public RedoableOpDataSource(RedoableOpData data) {
        if (data == null) {
            throw new NullPointerException();
        }
        mData = data;
    }
    
    public String getContentType() {
        return MimeConstants.CT_APPLICATION_OCTET_STREAM;
    }

    public InputStream getInputStream() throws IOException {
        return mData.getInputStream();
    }

    public String getName() {
        return null;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("not supported");
    }
}
