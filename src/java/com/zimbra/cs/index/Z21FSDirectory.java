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
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.store.BufferedIndexInput;
import org.apache.lucene.store.BufferedIndexOutput;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import com.zimbra.cs.stats.ZimbraPerf;

/**
 * 
 */
public class Z21FSDirectory extends FSDirectory {

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

    class FSIndexOutput extends BufferedIndexOutput {
        RandomAccessFile file = null;

        // remember if the file is open, so that we don't try to close it
        // more than once
        private boolean isOpen;

        public FSIndexOutput(File path) throws IOException {
            file = new RandomAccessFile(path, "rw");
            isOpen = true;
        }

        public void flushBuffer(byte[] b, int offset, int size) throws IOException {
        }

        /** output methods: */
        public void flushBuffer(byte[] b, int size) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(size);
            mBytesWritten.addAndGet(size);
            file.write(b, 0, size);
        }
        public void close() throws IOException {
            // only close the file if it has not been closed yet
            if (isOpen) {
                super.close();
                file.close();
                isOpen = false;
            }
        }

        /** Random-access methods */
        public void seek(long pos) throws IOException {
            super.seek(pos);
            file.seek(pos);
        }
        public long length() throws IOException {
            return file.length();
        }
    }

    /** Returns a stream reading an existing file. */
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

            public void close() throws IOException {
                if (isOpen) {
                    isOpen=false;
                    super.close();
                }
            }

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

        /** IndexInput methods */
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
                    if (i == -1)
                        throw new IOException("read past EOF");
                    file.position += i;
                    total += i;
                } while (total < len);
            }
        }

        public void close() throws IOException {
            // only close the file if this is not a clone
            if (!isClone) file.close();
        }

        protected void seekInternal(long position) {
        }

        public long length() {
            return file.length;
        }

        public Object clone() {
            FSIndexInput clone = (FSIndexInput)super.clone();
            clone.isClone = true;
            return clone;
        }

        /** Method used for testing. Returns true if the underlying
         *  file descriptor is valid.
         */
        boolean isFDValid() throws IOException {
            return file.getFD().valid();
        }
    }

}
