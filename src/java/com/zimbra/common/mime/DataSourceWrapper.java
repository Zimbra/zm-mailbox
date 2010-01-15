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

package com.zimbra.common.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * Wraps another <tt>DataSource</tt> and allows the caller to
 * override the content type and name.
 */
public class DataSourceWrapper implements DataSource {

    private DataSource mDataSource;
    private String mContentType;
    private String mName;
    
    public DataSourceWrapper(DataSource dataSource) {
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        mDataSource = dataSource;
    }
    
    public DataSourceWrapper setContentType(String contentType) {
        mContentType = contentType;
        return this;
    }
    
    public DataSourceWrapper setName(String name) {
        mName = name;
        return this;
    }
    
    public String getContentType() {
        if (mContentType != null) {
            return mContentType;
        } else {
            return mDataSource.getContentType();
        }
    }

    public InputStream getInputStream() throws IOException {
        return mDataSource.getInputStream();
    }

    public String getName() {
        if (mName != null) {
            return mName;
        } else {
            return mDataSource.getName();
        }
    }

    public OutputStream getOutputStream() throws IOException {
        return mDataSource.getOutputStream();
    }
}
