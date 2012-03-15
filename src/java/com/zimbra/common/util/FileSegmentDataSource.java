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
