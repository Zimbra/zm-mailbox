/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
