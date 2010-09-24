/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
