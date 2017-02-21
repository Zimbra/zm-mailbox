/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;

public class ZipFormatter extends ArchiveFormatter {
    public class ZipArchiveInputStream implements ArchiveInputStream {
        public class ZipArchiveInputEntry implements ArchiveInputEntry {
            private ZipEntry entry;

            public ZipArchiveInputEntry(ZipInputStream is) throws IOException {
                entry = is.getNextEntry();
            }
            public long getModTime() { return entry.getTime(); }
            public String getName() { return entry.getName(); }
            public long getSize() { return entry.getSize(); }
            public int getType() { return 0; }
            public boolean isUnread() {
                return entry.getComment() != null &&
                    entry.getComment().endsWith("-unread");
            }
        }
        
        private ZipInputStream is;
        
        public ZipArchiveInputStream(InputStream is, String cset) {
            this.is = new ZipInputStream(is, Charset.forName(cset));
        }
        
        public void close() throws IOException { is.close(); }
        public InputStream getInputStream() { return is; }
        public ArchiveInputEntry getNextEntry() throws IOException {
            ZipArchiveInputEntry zaie = new ZipArchiveInputEntry(is);
            
            return zaie.entry == null ? null : zaie;
        }
        public int read(byte[] buf, int offset, int len) throws IOException {
            return is.read(buf, offset, len);
        }
    }
    
    public class ZipArchiveOutputStream implements ArchiveOutputStream {
        public class ZipArchiveOutputEntry implements ArchiveOutputEntry {
            private com.zimbra.common.util.zip.ZipEntry entry;

            public ZipArchiveOutputEntry(String path, String name, int type, long
                date) {
                entry = new com.zimbra.common.util.zip.ZipEntry(path);
                entry.setComment(name);
                entry.setTime(date);
                entry.setUnixMode(0660);
            }
            public void setUnread() {
                entry.setUnixMode(0640);
                entry.setComment(entry.getComment() + "-unread");
            }
            public void setSize(long size) { entry.setSize(size); }
        }
        
        private ZipOutputStream os;
        
        public ZipArchiveOutputStream(OutputStream os, String cset, int lvl)
            throws IOException {
            this.os = new ZipOutputStream(os);
            this.os.setEncoding(cset);
            if (lvl >= 0 && lvl <= 9)
                this.os.setLevel(lvl);
        }
        public void close() throws IOException { os.close(); }
        public void closeEntry() throws IOException { os.closeEntry(); }
        public OutputStream getOutputStream() { return os; }
        public int getRecordSize() { return 2048; }
        public ArchiveOutputEntry newOutputEntry(String path, String name,
            int type, long date) {
            return new ZipArchiveOutputEntry(path, name, type, date);
        }
        public void putNextEntry(ArchiveOutputEntry entry) throws IOException {
            os.putNextEntry(((ZipArchiveOutputEntry)entry).entry);
        }
        public void write(byte[] buf) throws IOException { os.write(buf); }
        public void write(byte[] buf, int offset, int len) throws IOException {
            os.write(buf, offset, len);
        }
    }

    @Override public String[] getDefaultMimeTypes() {
        return new String[] { "application/zip", "application/x-zip-compressed" };
    }

    @Override 
    public FormatType getType() { 
        return FormatType.ZIP; 
    }

    @Override protected boolean getDefaultMeta() { return false; }
    
    protected ArchiveInputStream getInputStream(UserServletContext context,
        String charset) throws IOException, ServiceException, UserServletException {
        return new ZipArchiveInputStream(context.getRequestInputStream(-1),
            charset);
    }

    protected ArchiveOutputStream getOutputStream(UserServletContext context, String
        charset) throws IOException {
        OutputStream os = context.resp.getOutputStream();
        String zlv = context.params.get(UserServlet.QP_ZLV);
        int lvl = -1;
        
        if (zlv != null && zlv.length() > 0) {
            try {
                lvl = Integer.parseInt(zlv);
            } catch (NumberFormatException x) {}
        }
        return new ZipArchiveOutputStream(os, charset, lvl);
    }
}
