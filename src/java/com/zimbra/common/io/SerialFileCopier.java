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

class SerialFileCopier implements FileCopier {

    private static final int MAX_COPY_BUFSIZE = 1024 * 1024;  // 1MB

    private boolean mUseNIO;
    private int mCopyBufSizeOIO;

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

    public void copy(File src, File dest,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileUtil.ensureDirExists(dest.getParentFile());
        if (mUseNIO) {
            FileUtil.copy(src, dest);
        } else {
            byte[] buf = new byte[mCopyBufSizeOIO];
            FileUtil.copyOIO(src, dest, buf);
        }
    }

    public void copyReadOnly(File src, File dest,
                             FileCopierCallback cb, Object cbarg)
    throws IOException {
        copy(src, dest, cb, cbarg);
        dest.setReadOnly();
    }

    public void link(File real, File link,
                     FileCopierCallback cb, Object cbarg)
    throws IOException {
        FileUtil.ensureDirExists(link.getParentFile());
        IO.link(real.getAbsolutePath(), link.getAbsolutePath());
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
