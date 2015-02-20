/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.util.RedoLogFileUtil;

public class FilesystemRedoLogFile implements RedoLogFile {

    private final File file;

    public FilesystemRedoLogFile(File file) {
        super();
        this.file = file;
    }

    @Override
    public void copyToDir(File targetDir) throws IOException {
        File dest = new File(targetDir, file.getName());
        ZimbraLog.redolog.info("Copying redo log " + file.getPath() + " to " + dest.getPath());
        FileUtil.copy(file, dest);
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public long getSeq() {
        return RedoLogFileUtil.getSeqForFile(file);
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }
}
