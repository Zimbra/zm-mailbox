/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.znative.IO;

class SerialFileCopier implements FileCopier {

    private static final int MAX_COPY_BUFSIZE = 1024 * 1024;  // 1MB

    private boolean mUseNIO;
    private int mCopyBufSizeOIO;
    private boolean mIgnoreMissingSource;

    SerialFileCopier(boolean useNIO, int copyBufSizeOIO) {
        ZimbraLog.io.debug(
                "Creating SerialFileCopier: " +
                "useNIO = " + useNIO +
                ", copyBufSizeOIO = " + copyBufSizeOIO);

        mUseNIO = useNIO;
        mCopyBufSizeOIO = copyBufSizeOIO > 0
            ? copyBufSizeOIO : FileCopierOptions.DEFAULT_OIO_COPY_BUFFER_SIZE;
        if (mCopyBufSizeOIO > MAX_COPY_BUFSIZE) {
            ZimbraLog.io.warn(
                    "OIO copy buffer size " + mCopyBufSizeOIO +
                    " is too big; limiting to " + MAX_COPY_BUFSIZE);
            mCopyBufSizeOIO = MAX_COPY_BUFSIZE;
        }
    }

    public boolean isAsync() {
        return false;
    }

    public void start() {
        ZimbraLog.io.info("SerialFileCopier is starting");
        // do nothing
    }

    public void shutdown() {
        ZimbraLog.io.info("SerialFileCopier is shut down");
        // do nothing
    }

    public synchronized void setIgnoreMissingSource(boolean ignore) {
        mIgnoreMissingSource = ignore;
    }

    private synchronized boolean ignoreMissingSource() {
        return mIgnoreMissingSource;
    }

    public void copy(File src, File dest,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileUtil.ensureDirExists(dest.getParentFile());
        try {
            if (mUseNIO) {
                FileUtil.copy(src, dest);
            } else {
                byte[] buf = new byte[mCopyBufSizeOIO];
                FileUtil.copyOIO(src, dest, buf);
            }
        } catch (FileNotFoundException e) {
            if (!ignoreMissingSource())
                throw e;
        }
    }

    public void copyReadOnly(File src, File dest,
                             FileCopierCallback cb, Object cbarg)
    throws IOException {
        copy(src, dest, cb, cbarg);
        if (dest.exists())
            dest.setReadOnly();
    }

    public void link(File real, File link,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileUtil.ensureDirExists(link.getParentFile());
        try {
            IO.link(real.getAbsolutePath(), link.getAbsolutePath());
        } catch (FileNotFoundException e) {
            if (!ignoreMissingSource())
                throw e;
        }
    }

    public void move(File oldPath, File newPath,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileUtil.ensureDirExists(newPath.getParentFile());
        oldPath.renameTo(newPath);
    }

    public void delete(File file,
                       FileCopierCallback cb, Object cbarg)
    throws IOException {
        file.delete();
    }
}
