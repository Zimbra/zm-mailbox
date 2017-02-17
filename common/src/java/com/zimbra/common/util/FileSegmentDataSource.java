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

package com.zimbra.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.common.zmime.ZSharedFileInputStream;

/**
 * <tt>DataSource</tt> implementation that returns a stream to a section of a file.
 */
public class FileSegmentDataSource implements DataSource {

    private final File file;
    private final long offset;
    private final long length;
    private String contentType;

    public FileSegmentDataSource(File file, long offset, long length) {
        this.file = file;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public String getContentType() {
        if (contentType == null) {
            return "application/octet-stream";
        }
        return contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ZSharedFileInputStream(file, offset, offset + length);
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("not supported");
    }
}
