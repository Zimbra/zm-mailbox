/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.OutputStream;

import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarOutputStream;
import com.zimbra.cs.service.formatter.ArchiveFormatter.ArchiveOutputEntry;
import com.zimbra.cs.service.formatter.ArchiveFormatter.ArchiveOutputStream;

public class TarArchiveOutputStream implements ArchiveOutputStream {
    public class TarArchiveOutputEntry implements ArchiveOutputEntry {
        private TarEntry entry;

        public TarArchiveOutputEntry(String path, String name, int type, long
            date) {
            entry = new TarEntry(path);
            entry.setGroupName(name);
            entry.setMajorDeviceId(type);
            entry.setModTime(date);
        }

        @Override
        public void setUnread() { entry.setMode(entry.getMode() & ~0200); }
        @Override
        public void setSize(long size) { entry.setSize(size); }
    }

    private TarOutputStream os;

    public TarArchiveOutputStream(OutputStream os, String cset) throws
        IOException {
        this.os = new TarOutputStream(os, cset);
        this.os.setLongFileMode(TarOutputStream.LONGFILE_GNU);
    }
    @Override
    public void close() throws IOException { os.close(); }
    @Override
    public void closeEntry() throws IOException { os.closeEntry(); }
    @Override
    public OutputStream getOutputStream() { return os; }
    @Override
    public int getRecordSize() { return os.getRecordSize(); }
    @Override
    public ArchiveOutputEntry newOutputEntry(String path, String name,
        int type, long date) {
        return new TarArchiveOutputEntry(path, name, type, date);
    }
    @Override
    public void putNextEntry(ArchiveOutputEntry entry) throws IOException {
        os.putNextEntry(((TarArchiveOutputEntry)entry).entry);
    }
    @Override
    public void write(byte[] buf) throws IOException { os.write(buf); }
    @Override
    public void write(byte[] buf, int offset, int len) throws IOException {
        os.write(buf, offset, len);
    }
}
