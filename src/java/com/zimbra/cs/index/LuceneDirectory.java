/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.util.Collection;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
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

    private LuceneDirectory(FSDirectory dir) {
        directory = dir;
    }

    /**
     * Creates a new {@link LuceneDirectory} with {@code SingleInstanceLockFactory}.
     * <p>
     * You can switch Lucene's {@link FSDirectory} implementation by {@link LC#zimbra_index_lucene_io_impl}.
     * <ul>
     *  <li>{@code null} -Lucene will try to pick the best {@link FSDirectory} implementation given the current
     *      environment. Currently this returns {@link MMapDirectory} for most Solaris and Windows 64-bit JREs,
     *      {@link NIOFSDirectory} for other non-Windows JREs, and {@link SimpleFSDirectory} for other JREs on Windows.
     *  <li>{@code simple} - straightforward implementation using java.io.RandomAccessFile. However, it has poor
     *      concurrent performance (multiple threads will bottleneck) as it synchronizes when multiple threads read from
     *      the same file.
     *  <li>{@code nio} - uses java.nio's FileChannel's positional io when reading to avoid synchronization when reading
     *      from the same file. Unfortunately, due to a Windows-only Sun JRE bug this is a poor choice for Windows, but
     *      on all other platforms this is the preferred choice.
     *  <li>{@code mmap} - uses memory-mapped IO when reading. This is a good choice if you have plenty of virtual
     *      memory relative to your index size, eg if you are running on a 64 bit JRE, or you are running on a 32 bit
     *      JRE but your index sizes are small enough to fit into the virtual memory space. Java has currently the
     *      limitation of not being able to unmap files from user code. The files are unmapped, when GC releases the
     *      byte buffers. Due to this bug in Sun's JRE, MMapDirectory's IndexInput.close() is unable to close the
     *      underlying OS file handle. Only when GC finally collects the underlying objects, which could be quite some
     *      time later, will the file handle be closed. This will consume additional transient disk usage: on Windows,
     *      attempts to delete or overwrite the files will result in an exception; on other platforms, which typically
     *      have a "delete on last close" semantics, while such operations will succeed, the bytes are still consuming
     *      space on disk. For many applications this limitation is not a problem (e.g. if you have plenty of disk
     *      space, and you don't rely on overwriting files on Windows) but it's still an important limitation to be
     *      aware of. This class supplies a (possibly dangerous) workaround mentioned in the bug report, which may fail
     *      on non-Sun JVMs.
     * </ul>
     *
     * @param path directory path
     */
    public static LuceneDirectory open(File path) throws IOException {
        String impl = LC.zimbra_index_lucene_io_impl.value();
        FSDirectory dir;
        if ("nio".equals(impl)) {
            dir = new NIOFSDirectory(path, new SingleInstanceLockFactory());
        } else if ("mmap".equals(impl)) {
            dir = new MMapDirectory(path, new SingleInstanceLockFactory());
        } else if ("simple".equals(impl)) {
            dir = new SimpleFSDirectory(path, new SingleInstanceLockFactory());
        } else {
            dir = FSDirectory.open(path, new SingleInstanceLockFactory());
        }
        ZimbraLog.index.info("OpenLuceneIndex impl=%s,dir=%s", dir.getClass().getSimpleName(), path);
        return new LuceneDirectory(dir);
    }

    public File getDirectory() {
        return directory.getDirectory();
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
    @Deprecated
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
    public void sync(Collection<String> names) throws IOException {
        directory.sync(names);
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
    public void setLockFactory(LockFactory lockFactory) throws IOException {
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

    private static final class LuceneIndexInput extends IndexInput {
        private final IndexInput input;

        LuceneIndexInput(IndexInput in) {
            input = in;
        }

        @Override
        public byte readByte() throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(1);
            return input.readByte();
        }

        @Override
        public void readBytes(byte[] b, int offset, int len) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
            input.readBytes(b, offset, len);
        }

        @Override
        public void readBytes(byte[] b, int offset, int len, boolean useBuffer)
            throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_READ.increment(len);
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

    private static final class LuceneIndexOutput extends IndexOutput {
        private final IndexOutput output;

        LuceneIndexOutput(IndexOutput out) {
            output = out;
        }

        @Override
        public void writeByte(byte b) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(1);
            output.writeByte(b);
        }

        @Override
        public void writeBytes(byte[] b, int len) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(len);
            output.writeBytes(b, len);
        }

        @Override
        public void writeBytes(byte[] b, int offset, int len) throws IOException {
            ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.increment(len);
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
