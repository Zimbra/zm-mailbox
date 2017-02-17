/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
