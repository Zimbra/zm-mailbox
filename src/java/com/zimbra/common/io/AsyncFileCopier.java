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
import java.io.IOException;

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.znative.IO;

class AsyncFileCopier extends AbstractAsyncFileCopier implements FileCopier {

    private static final int MAX_COPY_BUFSIZE = 1024 * 1024;  // 1MB
    private static final int MAX_WORKER_THREADS = 100;

    private boolean mUseNIO;
    private int mCopyBufSizeOIO;
    private WorkerThread[] mWorkers;

    AsyncFileCopier(boolean useNIO, int copyBufSizeOIO,
                    int queueCapacity, int numWorkers) {
        super(queueCapacity);

        ZimbraLog.io.debug(
                "Creating AsyncFileCopier: " +
                "useNIO = " + useNIO +
                ", copyBufSizeOIO = " + copyBufSizeOIO +
                ", queueCapacity = " + queueCapacity +
                ", numWorkers = " + numWorkers);

        mUseNIO = useNIO;

        mCopyBufSizeOIO = copyBufSizeOIO > 0
            ? copyBufSizeOIO : FileCopierOptions.DEFAULT_OIO_COPY_BUFFER_SIZE;
        if (mCopyBufSizeOIO > MAX_COPY_BUFSIZE) {
            ZimbraLog.io.warn(
                    "OIO copy buffer size " + mCopyBufSizeOIO +
                    " is too big; limiting to " + MAX_COPY_BUFSIZE);
            mCopyBufSizeOIO = MAX_COPY_BUFSIZE;
        }

        numWorkers = numWorkers > 0
            ? numWorkers : FileCopierOptions.DEFAULT_CONCURRENCY;
        if (numWorkers > MAX_WORKER_THREADS) {
            ZimbraLog.io.warn(
                    numWorkers + " worker threads are too many; limiting to " +
                    MAX_WORKER_THREADS);
            numWorkers = MAX_WORKER_THREADS;
        }
        mWorkers = new WorkerThread[numWorkers];
        for (int i = 0; i < mWorkers.length; i++) {
            mWorkers[i] = new WorkerThread(i);
        }
    }

    public void start() {
        ZimbraLog.io.info("AsyncFileCopier is starting");
        for (WorkerThread worker : mWorkers) {
            worker.start();
        }
    }

    public void shutdown() throws IOException {
        for (int i = 0; i < mWorkers.length; i++) {
            try {
                queuePut(FileTask.QUIT);
            } catch (InterruptedException e) {
                throw new IOException("InterruptedException: " + e.getMessage());
            }
        }
        for (WorkerThread worker : mWorkers) {
            try {
                worker.join();
            } catch (InterruptedException e) {}
        }
        ZimbraLog.io.info("AsyncFileCopier is shut down");
    }

    private class WorkerThread extends Thread {

        private byte[] mCopyBuffer;

        public WorkerThread(int num) {
            setName("AsyncFileCopierWorker-" + num);
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
                Throwable err = null;
                try {
                    switch (task.getOp()) {
                    case COPY:
                        copy(task.getSrc(), task.getDest());
                        break;
                    case COPYRO:
                        copyReadOnly(task.getSrc(), task.getDest());
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
                    FileCopierCallback cb = task.getCallback();
                    if (cb != null)
                        cb.fileCopierCallbackEnd(task.getCallbackArg(), err);
                }
            }
        }

        private void copy(File src, File dest) throws IOException {
            FileUtil.ensureDirExists(dest.getParentFile());
            if (mUseNIO)
                FileUtil.copy(src, dest);
            else
                FileUtil.copyOIO(src, dest, mCopyBuffer);
        }

        private void copyReadOnly(File src, File dest) throws IOException {
            copy(src, dest);
            dest.setReadOnly();
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
}
