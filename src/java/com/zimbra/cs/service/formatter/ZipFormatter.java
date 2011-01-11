/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            this.is = new ZipInputStream(is);
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
