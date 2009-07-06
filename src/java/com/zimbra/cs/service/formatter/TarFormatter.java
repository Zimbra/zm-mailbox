/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
import java.io.OutputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarInputStream;
import com.zimbra.common.util.tar.TarOutputStream;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;

public class TarFormatter extends ArchiveFormatter {
    public class TarArchiveInputStream implements ArchiveInputStream {
        public class TarArchiveInputEntry implements ArchiveInputEntry {
            private TarEntry entry;

            public TarArchiveInputEntry(TarInputStream is) throws IOException {
                entry = is.getNextEntry();
            }
            public long getModTime() { return entry.getModTime().getTime(); }
            public String getName() { return entry.getName(); }
            public long getSize() { return entry.getSize(); }
            public int getType() { return entry.getMajorDeviceId(); }
            public boolean isUnread() { return (entry.getMode() & 0200) == 0; }
        }
        
        private TarInputStream is;
        
        public TarArchiveInputStream(InputStream is, String cset) {
            this.is = new TarInputStream(is, cset);
        }
        
        public void close() throws IOException { is.close(); }
        public InputStream getInputStream() { return is; }
        public ArchiveInputEntry getNextEntry() throws IOException {
            TarArchiveInputEntry taie = new TarArchiveInputEntry(is);
            
            return taie.entry == null ? null : taie;
        }
        public int read(byte[] buf, int offset, int len) throws IOException {
            return is.read(buf, offset, len);
        }
    }
    
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
            
            public void setUnread() { entry.setMode(entry.getMode() & ~0200); }
            public void setSize(long size) { entry.setSize(size); }
        }
        
        private TarOutputStream os;
        
        public TarArchiveOutputStream(OutputStream os, String cset) throws
            IOException {
            this.os = new TarOutputStream(os, cset);
            this.os.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        }
        public void close() throws IOException { os.close(); }
        public void closeEntry() throws IOException { os.closeEntry(); }
        public int getRecordSize() { return os.getRecordSize(); }
        public ArchiveOutputEntry newOutputEntry(String path, String name,
            int type, long date) {
            return new TarArchiveOutputEntry(path, name, type, date);
        }
        public void putNextEntry(ArchiveOutputEntry entry) throws IOException {
            os.putNextEntry(((TarArchiveOutputEntry)entry).entry);
        }
        public void write(byte[] buf) throws IOException { os.write(buf); }
        public void write(byte[] buf, int offset, int len) throws IOException {
            os.write(buf, offset, len);
        }
    }

    @Override public String[] getDefaultMimeTypes() {
        return new String[] { "application/x-tar" };
    }

    @Override public String getType() { return "tar"; }
    
    protected ArchiveInputStream getInputStream(Context context,
        String charset) throws IOException, ServiceException, UserServletException {

        return new TarArchiveInputStream(context.getRequestInputStream(-1),
            charset);
    }

    protected ArchiveOutputStream getOutputStream(Context context, String
        charset) throws IOException {
        return new TarArchiveOutputStream(context.resp.getOutputStream(), charset);
    }
}
