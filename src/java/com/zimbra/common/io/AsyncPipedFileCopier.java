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

package com.zimbra.common.io;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.znative.IO;

class AsyncPipedFileCopier extends AbstractAsyncFileCopier implements FileCopier {

    private static final int MEGABYTE = 1024 * 1024;
    private static final int MAX_COPY_BUFSIZE = MEGABYTE;
    private static final int MAX_TOTAL_PIPE_BUFSIZE = 100 * MEGABYTE;
    private static final int MAX_PIPES = 50;
    private static final int MAX_TOTAL_READER_THREADS = 50;
    private static final int MAX_TOTAL_WRITER_THREADS = 50;

    private PipedCopier[] mPipes;
    private boolean mUseNIO;
    private int mCopyBufSizeOIO;

    AsyncPipedFileCopier(
            boolean useNIO, int copyBufSizeOIO,
            int queueCapacity, int numPipes,
            int numReadersPerPipe, int numWritersPerPipe,
            int pipeBufSize) {
        super(queueCapacity);

        ZimbraLog.io.debug(
                "Creating AsyncPipedFileCopier: " +
                "useNIO = " + useNIO +
                ", copyBufSizeOIO = " + copyBufSizeOIO +
                ", queueCapacity = " + queueCapacity +
                ", numPipes = " + numPipes +
                ", numReadersPerPipe = " + numReadersPerPipe +
                ", numWritersPerPipe = " + numWritersPerPipe +
                ", pipeBufSize = " + pipeBufSize);

        mUseNIO = useNIO;

        mCopyBufSizeOIO = copyBufSizeOIO > 0 ? copyBufSizeOIO : FileCopierOptions.DEFAULT_OIO_COPY_BUFFER_SIZE;
        if (mCopyBufSizeOIO > MAX_COPY_BUFSIZE) {
            ZimbraLog.io.warn(
                    "OIO copy buffer size " + mCopyBufSizeOIO +
                    " is too big; limiting to " + MAX_COPY_BUFSIZE);
            mCopyBufSizeOIO = MAX_COPY_BUFSIZE;
        }

        numPipes = numPipes > 0
            ? numPipes : FileCopierOptions.DEFAULT_CONCURRENCY;
        if (numPipes > MAX_PIPES) {
            ZimbraLog.io.warn(
                    numPipes + " pipes are too many; limiting to " +
                    MAX_PIPES);
            numPipes = MAX_PIPES;
        }
        mPipes = new PipedCopier[numPipes];

        int maxReadersPerPipe = MAX_TOTAL_READER_THREADS / numPipes;
        if (numReadersPerPipe > maxReadersPerPipe) {
            ZimbraLog.io.warn(
                    numReadersPerPipe + " readers/pipe are too many; limiting to " +
                    maxReadersPerPipe);
            numReadersPerPipe = maxReadersPerPipe;
        }
        int maxWritersPerPipe = MAX_TOTAL_WRITER_THREADS / numPipes;
        if (numWritersPerPipe > maxWritersPerPipe) {
            ZimbraLog.io.warn(
                    numWritersPerPipe + " readers/pipe are too many; limiting to " +
                    maxWritersPerPipe);
            numWritersPerPipe = maxWritersPerPipe;
        }
        int maxPipeBufSize = ((MAX_TOTAL_PIPE_BUFSIZE / numPipes) >> 12) << 12;
        if (pipeBufSize > maxPipeBufSize) {
            ZimbraLog.io.warn(
                    "Pipe buffer size " + pipeBufSize +
                    " is too big; limiting to " + maxPipeBufSize);
            pipeBufSize = maxPipeBufSize;
        }

        for (int i = 0; i < mPipes.length; i++) {
            mPipes[i] = new PipedCopier(i, numReadersPerPipe, numWritersPerPipe, pipeBufSize);
        }
    }

    public void start() {
        ZimbraLog.io.info("AsyncPipedFileCopier is starting");
        for (PipedCopier pipe : mPipes)
            pipe.start();
    }

    public void shutdown() throws IOException {
        // Put enough QUIT commands in the queue to reach all reader threads
        // of all pipes.
        for (PipedCopier pipe : mPipes) {
            int readers = pipe.getNumReaders();
            for (int i = 0; i < readers; i++) {
                try {
                    queuePut(FileTask.QUIT);
                } catch (InterruptedException e) {
                    throw new IOException("InterruptedException: " + e.getMessage());
                }
            }
        }

        for (PipedCopier pipe : mPipes)
            pipe.shutdown();
        ZimbraLog.io.info("AsyncPipedFileCopier is shut down");
    }

    private class PipedCopier {

        private static final int DEFAULT_NUM_THREADS = 1;
        private static final int DEFAULT_CALLBACK_MAP_CAPACITY = 1000;

        private static final String CHARSET_UTF8 = "UTF-8";
        private final byte[] CMD_COPY = "COPY".getBytes();
        private final byte[] CMD_EXIT = "EXIT".getBytes();

        private static final int MAX_STRING_LEN = 1024;
        private static final int INT_BYTES = Integer.SIZE / 8;
        private static final int LONG_BYTES = Long.SIZE / 8;

        private BufferedPipe mPipe;
        private ReaderThread[] mReaders;
        private WriterThread[] mWriters;
        private CallbackMap mCallbackMap;

        private PipedCopier(int pipeNum, int numReaders, int numWriters, int bufsize) {
            mPipe = new BufferedPipe(bufsize > 0 ? bufsize : FileCopierOptions.DEFAULT_PIPE_BUFFER_SIZE);
            if (numReaders <= 0)
                numReaders = DEFAULT_NUM_THREADS;
            if (numWriters <= 0)
                numWriters = DEFAULT_NUM_THREADS;
            mReaders = new ReaderThread[numReaders];
            mWriters = new WriterThread[numWriters];
            for (int i = 0; i < mReaders.length; i++) {
                mReaders[i] = new ReaderThread(mPipe, pipeNum, i);
            }
            for (int i = 0; i < mWriters.length; i++) {
                mWriters[i] = new WriterThread(mPipe, pipeNum, i);
            }
            mCallbackMap = new CallbackMap(DEFAULT_CALLBACK_MAP_CAPACITY);
        }

        /**
         * Returns the number of file reader threads (pipe sinks).
         * @return
         */
        public int getNumReaders() {
            return mReaders.length;
        }

        /**
         * Returns the number of file writer threads (pipe sources).
         * @return
         */
        public int getNumWriters() {
            return mWriters.length;
        }

        public void start() {
            for (WriterThread writer : mWriters)
                writer.start();
            for (ReaderThread reader : mReaders)
                reader.start();
        }

        public void shutdown() {
            for (ReaderThread reader : mReaders) {
                try {
                    reader.join();
                } catch (InterruptedException e) {}
            }

            BufferedPipe.SinkChannel sinkChannel = mPipe.sink();
            ByteBuffer buf = ByteBuffer.allocate(INT_BYTES + CMD_EXIT.length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(CMD_EXIT.length);
            buf.put(CMD_EXIT);
            buf.flip();
            buf.mark();
            for (int i = 0; i < mWriters.length; i++) {
                buf.reset();
                sinkChannel.write(buf);
            }
            sinkChannel.close();

            for (WriterThread writer : mWriters) {
                try {
                    writer.join();
                } catch (InterruptedException e) {}
            }

            mPipe.source().close();
        }

        /**
         * Thread that reads from file and copies data into pipe.
         */
        private class ReaderThread extends Thread {

            private BufferedPipe.SinkChannel mChannel;
            private ByteBuffer mByteBuffer;
            private byte[] mCopyBuffer;

            public ReaderThread(BufferedPipe pipe, int pipeNum, int threadNum) {
                setName("AsyncPipedFileCopierReader-" + pipeNum + "-" + threadNum);
                mChannel = pipe.sink();
                // The buffer is only ever used to send COPY and EXIT commands.
                // COPY is longer than EXIT.
                mByteBuffer = ByteBuffer.allocate(
                        INT_BYTES +       // length of command string, in bytes
                        MAX_STRING_LEN +  // command string
                        LONG_BYTES +      // callback ID
                        1 +               // read-only flag
                        INT_BYTES +       // length of destination path string, in bytes
                        MAX_STRING_LEN +  // destination string
                        LONG_BYTES        // file data length in bytes
                        );
                mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                mCopyBuffer = new byte[mCopyBufSizeOIO];
            }

            public void run() {
                boolean done = false;
                while (!done) {
                    FileTask task = null;
                    try {
                        task = queueTake();
                    } catch (InterruptedException e) {
                        break;
                    }
                    boolean deferCallback = false;
                    Throwable err = null;
                    try {
                        switch (task.getOp()) {
                        case COPY:
                            deferCallback = true;
                            copy(task, false);
                            break;
                        case COPYRO:
                            deferCallback = true;
                            copy(task, true);
                            break;
                        case LINK:
                            link(task.getSrc(), task.getDest());
                            break;
                        case MOVE:
                            move(task.getSrc(), task.getDest());
                            break;
                        case DELETE:
                            delete(task.getSrc());
                            break;
                        case QUIT:
                            done = true;
                            break;
                        }
                    } catch (Throwable t) {
                        err = t;
                    } finally {
                        if (err != null || !deferCallback) {
                            FileCopierCallback cb = task.getCallback();
                            if (cb != null)
                                cb.fileCopierCallbackEnd(task.getCallbackArg(), err);
                        }
                    }
                }
            }

            private void copy(FileTask task, boolean readOnly) {
                File src = task.getSrc();
                File dest = task.getDest();
                FileCopierCallback cb = task.getCallback();
                Object cbarg = task.getCallbackArg();

                long callbackId = -1;
                try {
                    byte destPath[] = dest.getAbsolutePath().getBytes(CHARSET_UTF8);
                    mByteBuffer.clear();
                    // command
                    mByteBuffer.putInt(CMD_COPY.length);
                    mByteBuffer.put(CMD_COPY);
                    // callback ID
                    if (cb != null)
                        callbackId = mCallbackMap.put(cb, cbarg);
                    mByteBuffer.putLong(callbackId);
                    // read-only flag
                    mByteBuffer.put((byte) (readOnly ? 1 : 0));
                    // destination path
                    mByteBuffer.putInt(destPath.length);
                    mByteBuffer.put(destPath);
                    // file length
                    mByteBuffer.putLong(src.length());
                    mByteBuffer.flip();

                    FileInputStream fin = null;
                    try {
                        fin = new FileInputStream(src);
                        long expected = src.length();
                        long written;
                        Object writeLock = mChannel.getLock();
                        synchronized (writeLock) {
                            mChannel.write(mByteBuffer);
                            if (mUseNIO)
                                written = fin.getChannel().transferTo(0, expected, mChannel);
                            else
                                written = copyFromInputStream(fin, expected);
                        }

                        if (written != expected) {
                            throw new IOException(
                                    "copy(" + src + ", " + dest +
                                    "): incomplete transfer expected=" +
                                    expected + " written=" + written); 
                        }
                    } finally {
                        ByteUtil.closeStream(fin);
                    }
                } catch (Throwable t) {
                    if (callbackId != -1)
                        mCallbackMap.remove(callbackId);
                    if (cb != null)
                        cb.fileCopierCallbackEnd(cbarg, t);
                }
            }

            private long copyFromInputStream(InputStream is, long expected)
            throws IOException {
                int bytesRead;
                long bytesWritten = 0;
                while (bytesWritten < expected && (bytesRead = is.read(mCopyBuffer)) != -1)
                    bytesWritten += mChannel.write(mCopyBuffer, 0, bytesRead);
                return bytesWritten;
            }

            private void link(File file, File link) throws IOException {
                FileUtil.ensureDirExists(link.getParentFile());
                IO.link(file.getAbsolutePath(), link.getAbsolutePath());
            }

            private void move(File oldPath, File newPath) throws IOException {
                FileUtil.ensureDirExists(newPath.getParentFile());
                oldPath.renameTo(newPath);
            }

            private void delete(File file) {
                file.delete();
            }
        }

        /**
         * Thread that reads data from pipe and writes into file.
         */
        private class WriterThread extends Thread {

            private BufferedPipe.SourceChannel mChannel;
            private ByteBuffer mByteBuffer;
            private byte[] mCopyBuffer;

            public WriterThread(BufferedPipe pipe, int pipeNum, int threadNum) {
                setName("AsyncPipedFileCopierWriter-" + pipeNum + "-" + threadNum);
                mChannel = pipe.source();
                mByteBuffer = ByteBuffer.allocate(MAX_STRING_LEN);
                mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                mCopyBuffer = new byte[mCopyBufSizeOIO];
            }

            public void run() {
                try {
                    Object readLock = mChannel.getLock();
                    while (true) {
                        synchronized (readLock) {
                            String cmd = getString();
                            if ("COPY".equals(cmd)) {
                                // callback ID + read-only flag
                                mByteBuffer.clear();
                                mByteBuffer.limit(LONG_BYTES + 1);
                                int bytesRead = mChannel.read(mByteBuffer);
                                if (bytesRead != LONG_BYTES + 1)
                                    throw new IOException(
                                            "Can't read " + (LONG_BYTES + 1) +
                                            " bytes of callback ID and read-only flag from PipeSource");
                                mByteBuffer.flip();
                                long callbackId = mByteBuffer.getLong();
                                FileCopierCallback cb = null;
                                Object cbarg = null;
                                if (callbackId != -1) {
                                    CallbackMap.CbObj cbobj = mCallbackMap.remove(callbackId);
                                    if (cbobj != null) {
                                        cb = cbobj.cb;
                                        cbarg = cbobj.cbarg;
                                    }
                                }

                                boolean readOnly = mByteBuffer.get() != 0;

                                Throwable err = null;
                                try {
                                    receiveFile(readOnly);
                                } catch (Throwable t) {
                                    err = t;
                                } finally {
                                    if (cb != null)
                                        cb.fileCopierCallbackEnd(cbarg, err);
                                }
                            }
                            else if ("EXIT".equals(cmd))
                                break;
                            else
                                throw new IOException("Unknown command \"" + cmd + "\" received");
                        }
                    }
                } catch (IOException ioe) {
                    System.err.println("IOException in WriterThread: " + ioe.getMessage());
                    ioe.printStackTrace(System.err);
                }
            }

            private void receiveFile(boolean readOnly) throws IOException {
                String fname = getString();
                mByteBuffer.clear();
                mByteBuffer.limit(LONG_BYTES);
                int bytesRead = mChannel.read(mByteBuffer);
                if (bytesRead != LONG_BYTES)
                    throw new IOException(
                            "Can't read " + LONG_BYTES +
                            " bytes of file length from PipeSource");
                mByteBuffer.flip();
                long len = mByteBuffer.getLong();
    
                File dest = new File(fname);
                FileUtil.ensureDirExists(dest.getParentFile());
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(dest);
                    long written;
                    if (mUseNIO)
                        written = fout.getChannel().transferFrom(mChannel, 0, len);
                    else
                        written = copyToOutputStream(fout, len);
                    if (written != len)
                        throw new IOException(
                                "receiveFile(" + dest +
                                "): incomplete transfer target=" + len +
                                " transferred=" + written);
                } finally {
                    if (fout != null) {
                        try {
                            fout.close();
                            if (readOnly)
                                dest.setReadOnly();
                        } catch (IOException ioe) {
                            System.err.println(
                                    "receiveFile(" + dest +
                                    "): ignoring exception while closing output channel: " +
                                    ioe.getMessage());
                        }
                    }
                }
            }

            private long copyToOutputStream(OutputStream os, long expected)
            throws IOException {
                int buflen = mCopyBuffer.length;
                long remaining = expected;
                int bytesRead = 0;
                int toRead = 0;
                while (remaining > 0 && bytesRead == toRead) {
                    toRead = (int) (buflen < remaining ? buflen : remaining);
                    bytesRead = mChannel.read(mCopyBuffer, 0, toRead);
                    os.write(mCopyBuffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                return expected - remaining;
            }

            private String getString() throws IOException {
                mByteBuffer.clear();
                mByteBuffer.limit(INT_BYTES);
                int bytesRead = mChannel.read(mByteBuffer);
                if (bytesRead != INT_BYTES)
                    throw new IOException(
                            "Can't read " + INT_BYTES +
                            " bytes of string length from PipeSource; read " +
                            bytesRead + " bytes instead");
                mByteBuffer.flip();
                int strLen = mByteBuffer.getInt();
                if (strLen > MAX_STRING_LEN)
                    throw new IOException("String length " + strLen + " is too large");

                mByteBuffer.clear();
                mByteBuffer.limit(strLen);
                bytesRead = mChannel.read(mByteBuffer);
                if (bytesRead != strLen)
                    throw new IOException(
                            "Can't read " + strLen +
                            " bytes of string bytes from PipeSource; read" +
                            bytesRead + " bytes instead");
                byte[] strBytes = new byte[strLen];
                mByteBuffer.flip();
                mByteBuffer.get(strBytes);
                String str = new String(strBytes, CHARSET_UTF8);
                return str;
            }
        }
    }

    private static class CallbackMap {
        public static class CbObj {
            public FileCopierCallback cb;
            public Object cbarg;
            private CbObj(FileCopierCallback cb, Object cbarg) {
                this.cb = cb;
                this.cbarg = cbarg;
            }
        }

        private Map<Long, CbObj> mMap;
        private long mNextId;

        public CallbackMap(int capacity) {
            mMap = new HashMap<Long, CbObj>(capacity);
            mNextId = 0;
        }

        public synchronized long put(FileCopierCallback cb, Object cbarg) {
            long id = mNextId++;
            mMap.put(id, new CbObj(cb, cbarg));
            return id;
        }

        public synchronized CbObj remove(long id) {
            CbObj obj = mMap.remove(id);
            return obj;
        }
    }
}
