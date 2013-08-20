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
