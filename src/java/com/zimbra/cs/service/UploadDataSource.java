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

	/**
	 * 
	 */
	public UploadDataSource(Upload up) {
		mUpload = up;
	}

    public void setContentType(ContentType ctype) {
        mContentType = ctype;
    }

	public String getContentType() {
        if (mContentType == null)
            return new ContentType(mUpload.getContentType()).toString();
        else
            return mContentType.toString();
	}

	public InputStream getInputStream() throws IOException {
		return mUpload.getInputStream(); 
	}

	public String getName() {
		return mUpload.getName();
	}

	public OutputStream getOutputStream() {
		return null;
	}

}
