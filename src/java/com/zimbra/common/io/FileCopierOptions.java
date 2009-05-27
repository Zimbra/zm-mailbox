/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.io;

import com.zimbra.common.service.ServiceException;

public class FileCopierOptions {

    public static enum Method {
        PIPE, PARALLEL, SERIAL;

        public static Method parseMethod(String str) throws ServiceException {
            try {
                return valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid FileCopier method " + str, e);
            }
        }
    };

    public static enum IOType {
        OIO, NIO;

        public static IOType parseIOType(String str) throws ServiceException {
            try {
                return valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid IO type " + str, e);
            }
        }
    };

    public static final int DEFAULT_OIO_COPY_BUFFER_SIZE = 16 * 1024;  // 16KB
    public static final int DEFAULT_CONCURRENCY = 8;
    public static final int DEFAULT_ASYNC_QUEUE_CAPACITY = 10000;
    public static final int DEFAULT_PIPE_BUFFER_SIZE = 1024 * 1024;    // 1MB

    private Method mMethod;
    private IOType mIOType;
    private int mOIOCopyBufferSize;
    private int mAsyncQueueCapacity;
    private int mParallelWorkers;
    private int mPipes;
    private int mPipeBufferSize;
    private int mReadersPerPipe;
    private int mWritersPerPipe;

    public FileCopierOptions() {
        mMethod = Method.PARALLEL;
        mIOType = IOType.OIO;
        mOIOCopyBufferSize = DEFAULT_OIO_COPY_BUFFER_SIZE;
        mAsyncQueueCapacity = DEFAULT_ASYNC_QUEUE_CAPACITY;
        mParallelWorkers = DEFAULT_CONCURRENCY;
        mPipes = DEFAULT_CONCURRENCY;
        mPipeBufferSize = DEFAULT_PIPE_BUFFER_SIZE;
        mReadersPerPipe = 1;
        mWritersPerPipe = 1;
    }

    public FileCopierOptions(
            Method method, IOType ioType, int oioCopyBufSize,
            int asyncQueueCapacity, int parallelWorkers,
            int pipes, int pipeBufSize, int readersPerPipe, int writersPerPipe) {
        mMethod = method;
        mIOType = ioType;
        mOIOCopyBufferSize = oioCopyBufSize;
        mAsyncQueueCapacity = asyncQueueCapacity;
        mParallelWorkers = parallelWorkers;
        mPipes = pipes;
        mPipeBufferSize = pipeBufSize;
        mReadersPerPipe = readersPerPipe;
        mWritersPerPipe = writersPerPipe;
    }

    /**
     * Create a FileCopier according to the options specified in an encoded
     * string.  Options is a colon-separated string with one or more fields.
     * There are three different methods and each method has a different
     * list of fields.
     * 
     * file-opier-options := parallel-options | pipe-options | serial-options
     * 
     * parallel-options :=
     *     "PARALLEL"
     *     [ ":" oio-or-nio
     *       [ ":" oio-copy-buffer-size
     *         [ ":" async-queue-capacity
     *           [ ":" num-worker-threads ] ] ] ]
     * 
     * pipe-options :=
     *     "PIPE"
     *     [ ":" oio-or-nio
     *       [ ":" oio-copy-buffer-size
     *         [ ":" async-queue-capacity
     *           [ ":" num-pipes
     *             [ ":" pipe-buffer-size
     *               [ ":" num-reader-threads-per-pipe
     *                 [ ":" num-writer-threads-per-pipe ] ] ] ] ] ] ]
     * 
     * serial-options :=
     *     "SERIAL"
     *     [ ":" oio-or-nio
     *       [ ":" oio-copy-buffer-size ] ]
     * 
     * oio-or-nio := "OIO" | "NIO"
     *     ; choice between OIO (stream-based "old" IO) or NIO
     *     ; Default is OIO.
     * 
     * oio-copy-buffer-size := size
     *     ; size of byte array to use in stream read/write loop
     *     ; Default is 16k.
     * 
     * async-queue-capacity := size
     *     ; size of incoming request queue for PARALLEL and PIPE copiers
     *     ; Requesting thread will block while queue is full.
     *     ; Default is 10000.
     * 
     * num-worker-threads := size
     *     ; number of worker threads in PARALLEL copier that execute queued
     *     ; file operations
     *     ; Each worker thread does both file read and write.
     *     ; Default is 8.
     * 
     * num-pipes := size
     *     ; number of pipes to use in PIPE copier
     *     ; Default is 8.
     * 
     * pipe-buffer-size := size
     *     ; size of buffer that connects readers and writers
     *     ; Default is 1m.
     * 
     * num-reader-threads-per-pipe := size
     *     ; for each pipe, number of threads that execute queued file
     *     ; operations by reading file data and writing to the pipe
     *     ; Non-copy operations (link, move, etc.) are executed directly
     *     ; by reader threads instead of being piped to writers.
     *     ; Default is 1.
     * 
     * num-writer-threads-per-pipe := size
     *     ; for each pipe, number of threads that reads data from pipe
     *     ; and copies to file
     *     ; Default is 1.
     * 
     * size := DIGIT [DIGIT*] ["k" | "K" | "m" | "M" ]
     *         ; "k/K" means *1024, "m/M" means *1024^2
     *
     * @param options
     * @return
     */
    public FileCopierOptions(String options) throws ServiceException {
        this();  // set defaults

        if (options == null || options.length() == 0)
            return;
        String[] fields = options.split(":");
        if (fields == null || fields.length == 0)
            return;

        mMethod = Method.parseMethod(fields[0]);

        if (fields.length > 1) {
            mIOType = IOType.parseIOType(fields[1]);
        }

        if (fields.length > 2)
            mOIOCopyBufferSize = parseSize(fields[2]);

        if (mMethod.equals(Method.PARALLEL) || mMethod.equals(Method.PIPE)) {
            if (fields.length > 3)
                mAsyncQueueCapacity = parseSize(fields[3]);

            if (mMethod.equals(Method.PARALLEL)) {
                if (fields.length > 4)
                    mParallelWorkers = parseSize(fields[4]);
            } else {
                if (fields.length > 4)
                    mPipes = parseSize(fields[4]);
                if (fields.length > 5)
                    mPipeBufferSize = parseSize(fields[5]);
                if (fields.length > 6)
                    mReadersPerPipe = parseSize(fields[6]);
                if (fields.length > 7)
                    mWritersPerPipe = parseSize(fields[7]);
            }
        }
    }

    public Method getMethod() { return mMethod; }
    public void setMethod(Method m) { mMethod = m; }

    public IOType getIOType() { return mIOType; }
    public void setIOType(IOType ioType) { mIOType = ioType; }

    public int getOIOCopyBufferSize() { return mOIOCopyBufferSize; }
    public void setOIOCopyBufferSize(int bytes) { mOIOCopyBufferSize = bytes; }

    public int getAsyncQueueCapacity() { return mAsyncQueueCapacity; }
    public void setAsyncQueueCapacity(int capacity) { mAsyncQueueCapacity = capacity; }

    public int getNumParallelWorkers() { return mParallelWorkers; }
    public void setNumParallelWorkers(int count) { mParallelWorkers = count; }

    public int getNumPipes() { return mPipes; }
    public void setNumPipes(int count) { mPipes = count; }

    public int getPipeBufferSize() { return mPipeBufferSize; }
    public void setPipeBufferSize(int bytes) { mPipeBufferSize = bytes; }

    public int getNumReadersPerPipe() { return mReadersPerPipe; }
    public void setNumReadersPerPipe(int count) { mReadersPerPipe = count; }

    public int getNumWritersPerPipe() { return mWritersPerPipe; }
    public void setNumWritersPerPipe(int count) { mWritersPerPipe = count; }

    public static int parseSize(String str) throws ServiceException {
        if (str == null || str.length() < 1)
            throw ServiceException.INVALID_REQUEST(
                    "Size cannot be null in parseSize", null);
        try {
            char unit = str.charAt(str.length() - 1);
            int multiplier = 1;
            unit = Character.toLowerCase(unit);
            if (unit == 'k')
                multiplier *= 1024;
            else if (unit == 'm')
                multiplier *= 1024 * 1024;
            else if (unit < '0' || unit > '9')
                throw ServiceException.INVALID_REQUEST(
                        "Invalid unit '" + unit + "'", null);
            if (multiplier != 1)
                str = str.substring(0, str.length() - 1);
            int num = Integer.parseInt(str);
            return num * multiplier;
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("Invalid size " + str, e);
        }
    }
}
