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

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Lucene {@link FSDirectory} wrapper to count I/O bytes.
 * <p>
 * This forwards all its method calls to the underlying {@link FSDirectory}.
 *
 * @see FSDirectory
 * @author ysasaki
 */
public final class LuceneDirectory extends Directory {
    private final FSDirectory directory;
    private final AtomicLong rcount = new AtomicLong();
    private final AtomicLong wcount = new AtomicLong();

    private LuceneDirectory(FSDirectory dir) {
        directory = dir;
    }

    /**
     * Creates a new {@link LuceneDirectory} with {@code NativeFSLockFactory}.
     *
     * @see #open(File, LockFactory)
     */
    public static LuceneDirectory open(File path) throws IOException {
        return new LuceneDirectory(FSDirectory.open(path));
    }

    /**
     * Creates a new {@link LuceneDirectory}.
     * <p>
     * Lucene will try to pick the best {@link FSDirectory} implementation given
     * the current environment. Currently this returns {@code NIOFSDirectory} on
     * non-Windows JREs and {@code SimpleFSDirectory} on Windows.
     *
     * @param path directory path
     * @param lockFactory lock factory
     * @return wrapped {@link FSDirectory}
     * @throws IOException failed to access the directory
     */
    public static LuceneDirectory open(File path, LockFactory lockFactory) throws IOException {
        return new LuceneDirectory(FSDirectory.open(path, lockFactory));
    }

    long getBytesRead() {
        return rcount.get();
    }

    void resetBytesRead() {
        rcount.set(0);
    }

    long getBytesWritten() {
        return wcount.get();
    }

    void resetBytesWritten() {
        wcount.set(0);
    }

    public File getFile() {
        return directory.getFile();
    }

    @Override
    public String[] listAll() throws IOException {
        return directory.listAll();
    }

    @Override
    public boolean fileExists(String name) throws IOException {
        return directory.fileExists(name);
    }

    @Override
    public long fileModified(String name) throws IOException {
        return directory.fileModified(name);
    }

    @Override
    public void touchFile(String name) throws IOException {
        directory.touchFile(name);
    }

    @Override
    public void deleteFile(String name) throws IOException {
        directory.deleteFile(name);
    }

    @Override
    public long fileLength(String name) throws IOException {
        return directory.fileLength(name);
    }


    @Override
    public IndexOutput createOutput(String name) throws IOException {
        return new LuceneIndexOutput(directory.createOutput(name));
    }

    @Override
    public void sync(String name) throws IOException {
        directory.sync(name);
    }

    @Override
    public IndexInput openInput(String name) throws IOException {
        return new LuceneIndexInput(directory.openInput(name));
    }

    @Override
    public IndexInput openInput(String name, int bufferSize) throws IOException {
        return new LuceneIndexInput(directory.openInput(name, bufferSize));
    }

    @Override
    public Lock makeLock(String name) {
        return directory.makeLock(name);
    }

    @Override
    public void clearLock(String name) throws IOException {
        directory.clearLock(name);
    }

    @Override
    public void close() throws IOException {
        directory.close();
    }

    @Override
    public void setLockFactory(LockFactory lockFactory) {
        directory.setLockFactory(lockFactory);
    }

    @Override
    public LockFactory getLockFactory() {
        return directory.getLockFactory();
    }

    @Override
    public String getLockID() {
        return directory.getLockID();
    }

    @Override
    public String toString() {
        return directory.toString();
    }

    private final class LuceneIndexInput extends IndexInput {
        private final IndexInput input;

        LuceneIndexInput(IndexInput in) {
            input = in;
        }

        @Override
        public byte readByte() throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(1);
            rcount.addAndGet(1);
            return input.readByte();
        }

        @Override
        public void readBytes(byte[] b, int offset, int len) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
            rcount.addAndGet(len);
            input.readBytes(b, offset, len);
        }

        @Override
        public void readBytes(byte[] b, int offset, int len, boolean useBuffer)
            throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
            rcount.addAndGet(len);
            input.readBytes(b, offset, len, useBuffer);
        }

        @Override
        public void setModifiedUTF8StringsMode() {
            input.setModifiedUTF8StringsMode();
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        @Override
        public long getFilePointer() {
            return input.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            input.seek(pos);
        }

        @Override
        public long length() {
            return input.length();
        }

        @Override
        public Object clone() {
            return new LuceneIndexInput((IndexInput) input.clone());
        }
    }

    private final class LuceneIndexOutput extends IndexOutput {
        private final IndexOutput output;

        LuceneIndexOutput(IndexOutput out) {
            output = out;
        }

        @Override
        public void writeByte(byte b) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(1);
            wcount.addAndGet(1);
            output.writeByte(b);
        }

        @Override
        public void writeBytes(byte[] b, int len) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(len);
            wcount.addAndGet(len);
            output.writeBytes(b, len);
        }

        @Override
        public void writeBytes(byte[] b, int offset, int len) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(len);
            wcount.addAndGet(len);
            output.writeBytes(b, offset, len);
        }

        @Override
        public void flush() throws IOException {
            output.flush();
        }

        @Override
        public void close() throws IOException {
            output.close();
        }

        @Override
        public long getFilePointer() {
            return output.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            output.seek(pos);
        }

        @Override
        public long length() throws IOException {
            return output.length();
        }

        @Override
        public void setLength(long len) throws IOException {
            output.setLength(len);
        }
    }

}
