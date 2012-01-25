/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

    private final DataSource ds;
    private String ctype;
    private String name;

    public DataSourceWrapper(DataSource dataSource) {
        if (dataSource == null) {
            throw new NullPointerException("dataSource cannot be null");
        }
        this.ds = dataSource;
    }

    public DataSourceWrapper setContentType(String contentType) {
        this.ctype = contentType;
        return this;
    }

    public DataSourceWrapper setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getContentType() {
        String ct = ctype != null ? ctype : ds.getContentType();
        return new ContentType(ct).cleanup().toString();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return ds.getInputStream();
    }

    @Override
    public String getName() {
        return name != null ? name : ds.getName();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return ds.getOutputStream();
    }
}
