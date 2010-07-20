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
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.BufferedIndexOutput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.SimpleFSDirectory;

import com.zimbra.cs.stats.ZimbraPerf;

/**
 * TODO: Is this class still used?
 */
public class Z21FSDirectory extends SimpleFSDirectory {

    private AtomicLong mBytesWritten = new AtomicLong();
    private AtomicLong mBytesRead = new AtomicLong();

    /**
     * TODO: Need to check compatibility.
     */
    public Z21FSDirectory(File path) throws IOException {
        super(path);
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

    class FSIndexOutput extends BufferedIndexOutput {
        RandomAccessFile file = null;

        // remember if the file is open, so that we don't try to close it
        // more than once
        private boolean isOpen;

        public FSIndexOutput(File path) throws IOException {
            file = new RandomAccessFile(path, "rw");
            isOpen = true;
        }

        @Override
        public void flushBuffer(byte[] b, int offset, int size)
            throws IOException {
        }

        public void flushBuffer(byte[] b, int size) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(size);
            mBytesWritten.addAndGet(size);
            file.write(b, 0, size);
        }

        @Override
        public void close() throws IOException {
            // only close the file if it has not been closed yet
            if (isOpen) {
                super.close();
                file.close();
                isOpen = false;
            }
        }

        /**
         * Random-access
         */
        @Override
        public void seek(long pos) throws IOException {
            super.seek(pos);
            file.seek(pos);
        }

        @Override
        public long length() throws IOException {
            return file.length();
        }
    }

    /**
     * @return a stream reading an existing file.
     */
    @Override
    public IndexInput openInput(String name) throws IOException {
        return new FSIndexInput(new File(getFile(), name));
    }

    class FSIndexInput extends BufferedIndexInput {

        private class Descriptor extends RandomAccessFile {
            // remember if the file is open, so that we don't try to close it
            // more than once
            private boolean isOpen;
            long position;
            final long length;

            public Descriptor(File file, String mode) throws IOException {
                super(file, mode);
                isOpen=true;
                length=length();
            }

            @Override
            public void close() throws IOException {
                if (isOpen) {
                    isOpen=false;
                    super.close();
                }
            }

            @Override
            protected void finalize() throws Throwable {
                try {
                    close();
                } finally {
                    super.finalize();
                }
            }
        }

        private final Descriptor file;
        boolean isClone;

        public FSIndexInput(File path) throws IOException {
            file = new Descriptor(path, "r");
        }

        /**
         * index input
         */
        @Override
        protected void readInternal(byte[] b, int offset, int len)
            throws IOException {

            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
            mBytesRead.addAndGet(len);
            synchronized (file) {
                long position = getFilePointer();
                if (position != file.position) {
                    file.seek(position);
                    file.position = position;
                }
                int total = 0;
                do {
                    int i = file.read(b, offset+total, len-total);
                    if (i == -1) {
                        throw new IOException("read past EOF");
                    }
                    file.position += i;
                    total += i;
                } while (total < len);
            }
        }

        /**
         * Only close the file if this is not a clone.
         */
        @Override
        public void close() throws IOException {
            if (!isClone) {
                file.close();
            }
        }

        @Override
        protected void seekInternal(long position) {
        }

        @Override
        public long length() {
            return file.length;
        }

        @Override
        public Object clone() {
            FSIndexInput clone = (FSIndexInput)super.clone();
            clone.isClone = true;
            return clone;
        }

        /**
         * Method used for testing.
         *
         * @return true if the underlying file descriptor is valid.
         */
        boolean isFDValid() throws IOException {
            return file.getFD().valid();
        }
    }

}
