/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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

    public void delete(File file, FileCopierCallback cb, Object cbarg) {
        file.delete();
    }
}
