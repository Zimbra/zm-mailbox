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
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;

class BufferedPipe {

    private final int mSize;
    private int mHead;  // index of first used byte in buffer
    private int mTail;  // index of first available byte in buffer
    private boolean mIsFull;
    private final SinkChannel mSinkChannel;
    private final SourceChannel mSourceChannel;

    private byte[] mBuffer;
    private boolean mIsOpen;

    private final Object mReadLock = new Object();
    private final Object mWriteLock = new Object();

    BufferedPipe(int bufSize) {
        mSize = bufSize;
        mHead = mTail = 0;
        mIsFull = false;
        mSinkChannel = new SinkChannel();
        mSourceChannel = new SourceChannel();

        mBuffer = new byte[mSize];
        mIsOpen = true;
    }

    public int getSize() { return mSize; }

    public SinkChannel   sink()   { return mSinkChannel; }
    public SourceChannel source() { return mSourceChannel; }

    private void channelClosed() {
        boolean sinkClosed = !mSinkChannel.isOpen();
        boolean sourceClosed = !mSourceChannel.isOpen();
        synchronized (this) {
            notifyAll();
            if (sinkClosed && sourceClosed) {
                mIsOpen = false;
            }
        }
    }

    private synchronized boolean isClosed() { return !mIsOpen; }

    private synchronized int roomAfterTail() {
        if (mIsFull)
            return 0;
        else if (mHead <= mTail)
            return mSize - mTail;
        else  // mTail < mHead
            return mHead - mTail;
    }

    private synchronized int roomBeforeHead() {
        if (mIsFull)
            return 0;
        else if (mHead <= mTail)
            return mHead;
        else {
            // mTail < mHead
            // This space is considered room after tail.  Don't double count
            // it as room before head.
            return 0;
        }
    }

    /**
     * Number of available data bytes between mHead and end of buffer.
     * @return
     */
    private synchronized int headBytes() {
        if (mIsFull)
            return mSize - mHead;
        else if (mHead <= mTail)
            return mTail - mHead;
        else // mTail < mHead
            return mSize - mHead;
    }

    /**
     * Number of available data bytes between the beginning of buffer and
     * mTail.  Return value is greater than 0 only when data has wrapped
     * around the end of the buffer.
     * @return
     */
    private synchronized int tailBytes() {
        if (mIsFull || mTail < mHead)
            return mTail;
        else
            return 0;
    }

    /**
     * Advance mTail to indicate there is less room (and therefore more
     * data) in the buffer.
     * @param size mTail + size must be less than or equal to mSize
     */
    private synchronized void advanceTail(int size) {
        mTail += size;
        if (mTail == mSize)
            mTail = 0;
        if (mTail == mHead)
            mIsFull = true;
    }

    /**
     * Advance mHead to indicate there is more room (and therefore less
     * data) in the buffer.
     * @param size mHead + size must be less than or equal to mSize
     */
    private synchronized void advanceHead(int size) {
        mHead += size;
        if (mHead == mSize)
            mHead = 0;
        if (size > 0)
            mIsFull = false;
    }

    private long write(ByteBuffer[] srcs, int offset, int length) {
        synchronized (mWriteLock) {
            long written = 0;
            for (int i = offset; i < offset + length; i++) {
                if (isClosed())
                    break;

                ByteBuffer src = srcs[i];
                written += write(src);
            }
            return written;
        }
    }

    private int write(ByteBuffer src) {
        synchronized (mWriteLock) {
            synchronized (this) {
                int expected = src.remaining();
                int remaining = expected;
                int totalWritten = 0;
                int toWrite;

                while (true) {
                    if (isClosed())
                        break;

                    int written = 0;
                    int tailRoom = roomAfterTail();
                    if (tailRoom > 0) {
                        toWrite = tailRoom < remaining ? tailRoom : remaining;
                        src.get(mBuffer, mTail, toWrite);
                        written += toWrite;
                        advanceTail(toWrite);
                        remaining -= toWrite;
                    }

                    if (remaining > 0) {
                        int headRoom = roomBeforeHead();
                        if (headRoom > 0) {
                            toWrite = headRoom < remaining ? headRoom : remaining;
                            src.get(mBuffer, mTail, toWrite);
                            written += toWrite;
                            advanceTail(toWrite);
                            remaining -= toWrite;
                        }
                    }

                    // Wake up any reader that was waiting for more data.
                    if (written > 0)
                        notify();

                    totalWritten += written;
                    if (remaining == 0)
                        break;

                    // Block until more room is available.
                    if (roomBeforeHead() + roomAfterTail() == 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    }
                }

                return totalWritten;
            }
        }
    }

    /**
     * Write len bytes in src from the specified offset.  This method blocks
     * until all bytes are written or the pipe is closed.
     * @param src
     * @param offset
     * @param len
     * @return actual number of bytes written
     * @throws IOException
     */
    private int write(byte[] src, int offset, int len) {
        synchronized (mWriteLock) {
            synchronized (this) {
                int remaining = len;
                int totalWritten = 0;
                int toWrite;

                while (true) {
                    if (isClosed())
                        break;

                    int written = 0;
                    int tailRoom = roomAfterTail();
                    if (tailRoom > 0) {
                        toWrite = tailRoom < remaining ? tailRoom : remaining;
                        System.arraycopy(src, offset, mBuffer, mTail, toWrite);
                        offset += toWrite;
                        written += toWrite;
                        advanceTail(toWrite);
                        remaining -= toWrite;
                    }

                    if (remaining > 0) {
                        int headRoom = roomBeforeHead();
                        if (headRoom > 0) {
                            toWrite = headRoom < remaining ? headRoom : remaining;
                            System.arraycopy(src, offset, mBuffer, mTail, toWrite);
                            offset += toWrite;
                            written += toWrite;
                            advanceTail(toWrite);
                            remaining -= toWrite;
                        }
                    }

                    // Wake up any reader that was waiting for more data.
                    if (written > 0)
                        notify();

                    totalWritten += written;
                    if (remaining == 0)
                        break;

                    // Block until more room is available.
                    if (roomBeforeHead() + roomAfterTail() == 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    }
                }

                return totalWritten;
            }
        }
    }

    private long read(ByteBuffer[] dsts, int offset, int length) {
        synchronized (mReadLock) {
            long bytesRead = 0;
            for (int i = offset; i < offset + length; i++) {
                if (isClosed())
                    break;

                ByteBuffer dest = dsts[i];
                bytesRead += read(dest);
            }
            return bytesRead;
        }
    }

    private int read(ByteBuffer dst) {
        synchronized (mReadLock) {
            synchronized (this) {
                int expected = dst.remaining();
                int remaining = expected;
                int totalBytesRead = 0;
                int toRead;

                while (true) {
                    if (isClosed())
                        break;

                    int bytesRead = 0;  // bytes read in this iteration

                    int bytesPart1 = headBytes();
                    if (bytesPart1 > 0) {
                        toRead = bytesPart1 < remaining ? bytesPart1 : remaining;
                        dst.put(mBuffer, mHead, toRead);
                        bytesRead += toRead;
                        advanceHead(toRead);
                        remaining -= toRead;
                    }

                    if (remaining > 0) {
                        int bytesPart2 = tailBytes();
                        if (bytesPart2 > 0) {
                            toRead = bytesPart2 < remaining ? bytesPart2 : remaining;
                            dst.put(mBuffer, mHead, toRead);
                            bytesRead += toRead;
                            advanceHead(toRead);
                            remaining -= toRead;
                        }
                    }

                    // Wake up any writer that was waiting for room to
                    // free up in the buffer.
                    if (bytesRead > 0)
                        notify();

                    totalBytesRead += bytesRead;
                    if (remaining == 0)
                        break;

                    // Block until more data is available.
                    if (headBytes() + tailBytes() == 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    }
                }
                return totalBytesRead;
            }
        }
    }

    /**
     * Read len bytes into dst at the specified offset.  This method blocks
     * until all bytes are read or the pipe is closed.
     * @param dst
     * @param offset
     * @param len
     * @return actual number of bytes read
     * @throws IOException
     */
    private int read(byte[] dst, int offset, int len) {
        synchronized (mReadLock) {
            synchronized (this) {
                int remaining = len;
                int totalBytesRead = 0;
                int toRead;

                while (true) {
                    if (isClosed())
                        break;

                    int bytesRead = 0;  // bytes read in this iteration

                    int bytesPart1 = headBytes();
                    if (bytesPart1 > 0) {
                        toRead = bytesPart1 < remaining ? bytesPart1 : remaining;
                        System.arraycopy(mBuffer, mHead, dst, offset, toRead);
                        offset += toRead;
                        bytesRead += toRead;
                        advanceHead(toRead);
                        remaining -= toRead;
                    }

                    if (remaining > 0) {
                        int bytesPart2 = tailBytes();
                        if (bytesPart2 > 0) {
                            toRead = bytesPart2 < remaining ? bytesPart2 : remaining;
                            System.arraycopy(mBuffer, mHead, dst, offset, toRead);
                            offset += toRead;
                            bytesRead += toRead;
                            advanceHead(toRead);
                            remaining -= toRead;
                        }
                    }

                    // Wake up any writer that was waiting for room to
                    // free up in the buffer.
                    if (bytesRead > 0)
                        notify();

                    totalBytesRead += bytesRead;
                    if (remaining == 0)
                        break;

                    // Block until more data is available.
                    if (headBytes() + tailBytes() == 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    }
                }
                return totalBytesRead;
            }
        }
    }

    private abstract class BaseChannel
    implements Closeable, Channel, InterruptibleChannel {
        private boolean mOpen;

        public BaseChannel() { mOpen = true; }

        public void close() {
            synchronized (this) {
                mOpen = false;
            }
            channelClosed();
        }

        public synchronized boolean isOpen() { return mOpen; }
    }

    public class SinkChannel extends BaseChannel
    implements WritableByteChannel, GatheringByteChannel {

        /**
         * Write bytes in src to this channel.  This method blocks until all
         * bytes are written or the channel is closed.  This method is
         * equivalent to write(src, 0, src.length).
         * @param src
         * @return actual number of bytes written
         * @throws IOException
         */
        public int write(byte[] src) throws IOException {
            return write(src, 0, src.length);
        }

        /**
         * Write len bytes in src from the specified offset.  This method blocks
         * until all bytes are written or the channel is closed.
         * @param src
         * @param offset
         * @param len
         * @return actual number of bytes written
         * @throws IOException
         */
        public int write(byte[] src, int offset, int len) {
            return BufferedPipe.this.write(src, offset, len);
        }

        public int write(ByteBuffer src) {
            return BufferedPipe.this.write(src);
        }

        public long write(ByteBuffer[] srcs, int offset, int length) {
            return BufferedPipe.this.write(srcs, offset, length);
        }

        public long write(ByteBuffer[] srcs) {
            return write(srcs, 0, srcs.length);
        }

        /**
         * Callers should synchronize on this lock if multiple writes must be
         * done without getting interrupted by other writer threads.
         * @return
         */
        public Object getLock()  { return mWriteLock; }
    }

    public class SourceChannel extends BaseChannel
    implements ReadableByteChannel, ScatteringByteChannel {

        /**
         * Read bytes into dst from this channel.  This method blocks until all
         * bytes are read or the channel is closed.  This method is equivalent
         * to read(dst, 0, dst.length).
         * @param dst
         * @return actual number of bytes read
         * @throws IOException
         */
        public int read(byte[] dst) {
            return read(dst, 0, dst.length);
        }

        /**
         * Read len bytes into dst at the specified offset.  This method blocks
         * until all bytes are read or the channel is closed.
         * @param dst
         * @param offset
         * @param len
         * @return actual number of bytes read
         * @throws IOException
         */
        public int read(byte[] dst, int offset, int len) {
            return BufferedPipe.this.read(dst, offset, len);
        }

        public int read(ByteBuffer dst) {
            return BufferedPipe.this.read(dst);
        }

        public long read(ByteBuffer[] dsts, int offset, int length) {
            return BufferedPipe.this.read(dsts, offset, length);
        }

        public long read(ByteBuffer[] dsts) {
            return read(dsts, 0, dsts.length);
        }

        /**
         * Callers should synchronize on this lock if multiple reads must be
         * done without getting interrupted by other reader threads.
         * @return
         */
        public Object getLock() { return mReadLock; }
    }
}
