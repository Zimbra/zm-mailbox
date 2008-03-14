/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import com.zimbra.cs.stats.ZimbraPerf;

/**
 * 
 */
public class Z23FSDirectory extends FSDirectory {

    AtomicLong mBytesWritten = new AtomicLong();

    long getBytesWritten() { return mBytesWritten.get(); }
    void resetBytesWritten() { mBytesWritten.set(0); }

    AtomicLong mBytesRead = new AtomicLong();

    long getBytesRead() { return mBytesRead.get(); }
    void resetBytesRead() { mBytesRead.set(0); }

    /** Creates a new, empty file in the directory with the given name.
    Returns a stream writing this file. */
    public IndexOutput createOutput(String name) throws IOException {

        File file = new File(getFile(), name);
        if (file.exists() && !file.delete())          // delete existing, if any
            throw new IOException("Cannot overwrite: " + file);

        return new FSIndexOutput(file);
    }

    // Inherit javadoc
    public IndexInput openInput(String name) throws IOException {
        return new FSIndexInput(new File(getFile(), name));
    }

    // Inherit javadoc
    public IndexInput openInput(String name, int bufferSize) throws IOException {
        return new FSIndexInput(new File(getFile(), name), bufferSize);
    }

    protected class FSIndexOutput extends FSDirectory.FSIndexOutput {
        public FSIndexOutput(File path) throws IOException {
            super(path);
        }

        /** output methods: */
        public void flushBuffer(byte[] b, int offset, int size) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(size);
            mBytesWritten.addAndGet(size);
            super.flushBuffer(b, offset, size);
        }
    }

    protected class FSIndexInput extends FSDirectory.FSIndexInput {
        public FSIndexInput(File path) throws IOException {
            super(path);
        }

        public FSIndexInput(File path, int bufferSize) throws IOException {
            super(path, bufferSize);
        }

        protected void readInternal(byte[] b, int offset, int len)
        throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
            mBytesRead.addAndGet(len);
            super.readInternal(b, offset, len);
        }
    }
}
