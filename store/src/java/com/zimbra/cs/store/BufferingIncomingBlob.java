/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.zimbra.common.service.ServiceException;

/**
 * IncomingBlob implementation which buffers locally using BlobBuilder
 *
 */
public class BufferingIncomingBlob extends IncomingBlob
{
    protected final String id;
    protected BlobBuilder blobBuilder;
    private Object ctx;
    private long expectedSize;
    private boolean expectedSizeSet;
    protected long lastAccessTime;

    protected BufferingIncomingBlob(String id, BlobBuilder blobBuilder, Object ctx)
    {
        this.id = id;
        this.blobBuilder = blobBuilder;
        this.ctx = ctx;

        lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public OutputStream getAppendingOutputStream() throws IOException
    {
        lastAccessTime = System.currentTimeMillis();
        return new BlobBuilderOutputStream(blobBuilder);
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        assert false : "Not yet implemented";
        return null;
        //return new SizeLimitedInputStream(blobBuilder.getBlob().getInputStream(), getCurrentSize());
    }

    @Override
    public Object getContext()
    {
        return ctx;
    }

    @Override
    public void setContext(Object value)
    {
        ctx = value;
    }

    @Override
    public long getCurrentSize() throws IOException
    {
        return blobBuilder.getTotalBytes();
    }

    @Override
    public boolean hasExpectedSize()
    {
        return expectedSizeSet;
    }

    @Override
    public long getExpectedSize()
    {
        assert expectedSizeSet : "Expected size not set";
        return expectedSize;
    }

    @Override
    public void setExpectedSize(long value)
    {
        assert !expectedSizeSet : "Expected size already set: " + expectedSize;
        expectedSize = value;
        expectedSizeSet = true;
    }

    @Override
    public long getLastAccessTime()
    {
        return lastAccessTime;
    }

    @Override
    public Blob getBlob() throws IOException, ServiceException {
        return blobBuilder.finish();
    }

    @Override
    public void cancel() {
        blobBuilder.dispose();
    }
}
