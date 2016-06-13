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
import java.io.InputStream;

import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarInputStream;
import com.zimbra.cs.service.formatter.ArchiveFormatter.ArchiveInputEntry;
import com.zimbra.cs.service.formatter.ArchiveFormatter.ArchiveInputStream;

public class TarArchiveInputStream implements ArchiveInputStream {
    public class TarArchiveInputEntry implements ArchiveInputEntry {
        private TarEntry entry;

        public TarArchiveInputEntry(TarInputStream is) throws IOException {
            entry = is.getNextEntry();
        }
        @Override
        public long getModTime() { return entry.getModTime().getTime(); }
        @Override
        public String getName() { return entry.getName(); }
        @Override
        public long getSize() { return entry.getSize(); }
        @Override
        public int getType() { return entry.getMajorDeviceId(); }
        @Override
        public boolean isUnread() { return (entry.getMode() & 0200) == 0; }
    }

    private TarInputStream is;

    public TarArchiveInputStream(InputStream is, String cset) {
        this.is = new TarInputStream(is, cset);
    }

    @Override
    public void close() throws IOException { is.close(); }
    @Override
    public InputStream getInputStream() { return is; }
    @Override
    public ArchiveInputEntry getNextEntry() throws IOException {
        TarArchiveInputEntry taie = new TarArchiveInputEntry(is);

        return taie.entry == null ? null : taie;
    }
    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        return is.read(buf, offset, len);
    }
}
