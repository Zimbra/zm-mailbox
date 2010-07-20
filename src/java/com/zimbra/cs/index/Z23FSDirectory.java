/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.zimbra.cs.stats.ZimbraPerf;

public class Z23FSDirectory extends SimpleFSDirectory {

    private AtomicLong mBytesWritten = new AtomicLong();
    private AtomicLong mBytesRead = new AtomicLong();

    public Z23FSDirectory(File path) throws IOException {
        super(path);
    }

    public Z23FSDirectory(File path, LockFactory lockFactory)
        throws IOException {

        super(path, lockFactory);
    }

    long getBytesWritten() {
        return mBytesWritten.get();
    }

    void resetBytesWritten() {
        mBytesWritten.set(0);
    }

    long getBytesRead() {
        return mBytesRead.get();
    }

    void resetBytesRead() {
        mBytesRead.set(0);
    }

    /**
     * Creates a new, empty file in the directory with the given name.
     *
     * @return a stream writing this file.
     */
    @Override
    public IndexOutput createOutput(String name) throws IOException {

        File file = new File(getFile(), name);
        if (file.exists() && !file.delete()) { // delete existing, if any
            throw new IOException("Cannot overwrite: " + file);
        }

        return new FSIndexOutput(file);
    }

    @Override
    public IndexInput openInput(String name) throws IOException {
        return new FSIndexInput(new File(getFile(), name),
                BufferedIndexInput.BUFFER_SIZE, getReadChunkSize());
    }

    @Override
    public IndexInput openInput(String name, int bufferSize)
        throws IOException {

        return new FSIndexInput(new File(getFile(), name), bufferSize,
                getReadChunkSize());
    }

    protected class FSIndexOutput extends SimpleFSDirectory.SimpleFSIndexOutput {
        public FSIndexOutput(File path) throws IOException {
            super(path);
        }

        @Override
        public void flushBuffer(byte[] b, int offset, int size)
            throws IOException {

            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(size);
            mBytesWritten.addAndGet(size);
            super.flushBuffer(b, offset, size);
        }
    }

    protected class FSIndexInput extends SimpleFSDirectory.SimpleFSIndexInput {

        public FSIndexInput(File path, int bufferSize, int chunkSize)
            throws IOException {

            super(path, bufferSize, chunkSize);
        }

        @Override
        protected void readInternal(byte[] b, int offset, int len)
            throws IOException {

            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
            mBytesRead.addAndGet(len);
            super.readInternal(b, offset, len);
        }
    }
}
