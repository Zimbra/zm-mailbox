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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.zimbra.common.util.ZimbraLog;

abstract class AbstractAsyncFileCopier implements FileCopier {

    private static final int MAX_QUEUE_SIZE = 100000;

    private BlockingQueue<FileTask> mQueue;
    private boolean mIgnoreMissingSource;

    protected AbstractAsyncFileCopier(int queueCapacity) {
        int qsize = queueCapacity > 0
            ? queueCapacity
            : FileCopierOptions.DEFAULT_ASYNC_QUEUE_CAPACITY;
        if (qsize > MAX_QUEUE_SIZE) {
            ZimbraLog.io.warn(
                    "Async queue size " + qsize +
                    " is too big; limiting to " + MAX_QUEUE_SIZE);
            qsize = MAX_QUEUE_SIZE;
        }
        mQueue = new LinkedBlockingQueue<FileTask>(qsize);
    }

    protected void queuePut(FileTask task) throws InterruptedException {
        mQueue.put(task);
    }

    protected FileTask queueTake() throws InterruptedException {
        return mQueue.take();
    }

    private void enqueue(FileTask task,
                         FileCopierCallback cb, Object cbarg)
    throws IOException {
        if (cb != null) {
            boolean okay = cb.fileCopierCallbackBegin(cbarg);
            if (!okay)
                throw new IOException("Operation rejected by callback");
        }
        try {
            mQueue.put(task);
        } catch (InterruptedException e) {
            if (cb != null)
                cb.fileCopierCallbackEnd(cbarg, e);
        }
    }

    public boolean isAsync() {
        return true;
    }

    public synchronized void setIgnoreMissingSource(boolean ignore) {
        mIgnoreMissingSource = ignore;
    }

    protected synchronized boolean ignoreMissingSource() {
        return mIgnoreMissingSource;
    }

    public void copy(File src, File dest,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileTask task = FileTask.copyTask(src, dest, cb, cbarg);
        enqueue(task, cb, cbarg);
    }

    public void copyReadOnly(File src, File dest,
                             FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileTask task = FileTask.copyReadOnlyTask(src, dest, cb, cbarg);
        enqueue(task, cb, cbarg);
    }

    public void link(File file, File link,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileTask task = FileTask.linkTask(file, link, cb, cbarg);
        enqueue(task, cb, cbarg);
    }

    public void move(File oldPath, File newPath,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileTask task = FileTask.moveTask(oldPath, newPath, cb, cbarg);
        enqueue(task, cb, cbarg);
    }

    public void delete(File file,
                       FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileTask task = FileTask.deleteTask(file, cb, cbarg);
        enqueue(task, cb, cbarg);
    }

    protected static class FileTask {

        public static enum Op { COPY, COPYRO, LINK, MOVE, DELETE, QUIT };

        public static FileTask QUIT = new FileTask(Op.QUIT, null, null, null, null);

        private Op mOp;
        private File mSrc;
        private File mDest;
        private FileCopierCallback mCb;
        private Object mCbArg;

        static FileTask copyTask(
                File src, File dest,
                FileCopierCallback cb, Object cbArg) {
            return new FileTask(Op.COPY, src, dest, cb, cbArg);
        }

        static FileTask copyReadOnlyTask(
                File src, File dest,
                FileCopierCallback cb, Object cbArg) {
            return new FileTask(Op.COPYRO, src, dest, cb, cbArg);
        }

        static FileTask linkTask(
                File real, File link,
                FileCopierCallback cb, Object cbArg) {
            return new FileTask(Op.LINK, real, link, cb, cbArg);
        }

        static FileTask moveTask(
                File oldFile, File newFile,
                FileCopierCallback cb, Object cbArg) {
            return new FileTask(Op.MOVE, oldFile, newFile, cb, cbArg);
        }

        static FileTask deleteTask(
                File file,
                FileCopierCallback cb, Object cbArg) {
            return new FileTask(Op.DELETE, file, null, cb, cbArg);
        }

        private FileTask(Op op, File src, File dest,
                         FileCopierCallback cb, Object cbarg) {
            mOp = op;
            mSrc = src;
            mDest = dest;
            mCb = cb;
            mCbArg = cbarg;
        }

        public Op getOp()     { return mOp; }
        public File getSrc()  { return mSrc; }
        public File getDest() { return mDest; }
        public FileCopierCallback getCallback() { return mCb; }
        public Object getCallbackArg() { return mCbArg; }

        public boolean isQuit() {
            return Op.QUIT.equals(mOp);
        }
    }
}
