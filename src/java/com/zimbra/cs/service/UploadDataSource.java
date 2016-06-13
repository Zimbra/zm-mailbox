/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Dec 9, 2004
 */
package com.zimbra.cs.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.common.mime.ContentType;
import com.zimbra.cs.service.FileUploadServlet.Upload;

/**
 * @author dkarp
 */
public class UploadDataSource implements DataSource {

    private Upload mUpload;
    private ContentType mContentType;

    public UploadDataSource(Upload up) {
        mUpload = up;
    }

    public void setContentType(ContentType ctype) {
        mContentType = ctype;
    }

    @Override public String getContentType() {
        if (mContentType == null) {
            return new ContentType(mUpload.getContentType()).cleanup().toString();
        } else {
            return mContentType.cleanup().toString();
        }
    }

    @Override public InputStream getInputStream() throws IOException {
        return mUpload.getInputStream();
    }

    @Override public String getName() {
        return mUpload.getName();
    }

    @Override public OutputStream getOutputStream() {
        return null;
    }
}
