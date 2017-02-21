/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.znative;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IO {
    
    public static class FileInfo {
        private long inodeNum;
        private long size;
        private int linkCount;
        
        public FileInfo(long inodeNum, long size, int linkCount) {
            this.inodeNum = inodeNum;
            this.size = size;
            this.linkCount = linkCount;
        }
        
        public long getInodeNum() {
            return inodeNum;
        }
        
        public long getSize() {
            return size;
        }
        
        public int getLinkCount() {
            return linkCount;
        }
    }

    private static native void link0(byte[] oldpath, byte[] newpath)
        throws IOException;
    
    private static native FileInfo fileInfo0(byte[] path)
    throws IOException;

    private static native void chmod0(byte[] path, long mode)
        throws IOException;
    
    private static native void setStdoutStderrTo0(byte[] path)
        throws IOException;

    private static native int S_IRUSR();
    private static native int S_IWUSR();
    private static native int S_IXUSR();
    private static native int S_IRGRP();
    private static native int S_IWGRP();
    private static native int S_IXGRP();
    private static native int S_IROTH();
    private static native int S_IWOTH();
    private static native int S_IXOTH();
    private static native int S_ISUID();
    private static native int S_ISGID();
    private static native int S_ISVTX();
    
    public static final int S_IRUSR = Util.haveNativeCode() ? S_IRUSR() : 0;
    public static final int S_IWUSR = Util.haveNativeCode() ? S_IWUSR() : 0;
    public static final int S_IXUSR = Util.haveNativeCode() ? S_IXUSR() : 0;
    public static final int S_IRGRP = Util.haveNativeCode() ? S_IRGRP() : 0;
    public static final int S_IWGRP = Util.haveNativeCode() ? S_IWGRP() : 0;
    public static final int S_IXGRP = Util.haveNativeCode() ? S_IXGRP() : 0;
    public static final int S_IROTH = Util.haveNativeCode() ? S_IROTH() : 0;
    public static final int S_IWOTH = Util.haveNativeCode() ? S_IWOTH() : 0;
    public static final int S_IXOTH = Util.haveNativeCode() ? S_IXOTH() : 0;
    public static final int S_ISUID = Util.haveNativeCode() ? S_ISUID() : 0;
    public static final int S_ISGID = Util.haveNativeCode() ? S_ISGID() : 0;
    public static final int S_ISVTX = Util.haveNativeCode() ? S_ISVTX() : 0;
    
    public static void chmod(String path, long mode) throws IOException {
	if (!Util.haveNativeCode()) {
	    // No-op where there is no native-code
	    return;
	}
	chmod0(path.getBytes(), mode);
    }

    /**
     * Creates a hard link "newpath" to existing "oldpath".  If
     * we do not support hard links on current platform we just copy
     * the file.
     */
    public static void link(String oldpath, String newpath)
        throws IOException 
    {
        if (Util.haveNativeCode()) {
            link0(oldpath.getBytes(), newpath.getBytes());
        } else {
            FileInputStream in = new FileInputStream(oldpath);
            FileOutputStream out = new FileOutputStream(newpath);
            byte[] buffer = new byte[16 * 1024];
            int bytes_read;
            while ((bytes_read = in.read(buffer, 0, buffer.length)) > -1) {
                out.write(buffer, 0, bytes_read);
            }
            out.close();
            in.close();
        }
    }

    public static int linkCount(String path) throws IOException {
        if (Util.haveNativeCode()) {
            // native method throws specified exception
            FileInfo info = fileInfo0(path.getBytes());
            if (info != null) {
                return info.getLinkCount();
            } else {
                return -1;
            }
        } else {
            return 1;
        }
    }
    
    public static FileInfo fileInfo(String path) throws IOException {
        if (Util.haveNativeCode()) {
            // native method throws specified exception
            return fileInfo0(path.getBytes());
        } else {
            return null;
        }
    }

    public static void setStdoutStderrTo(String path) throws IOException {
        if (!Util.haveNativeCode()) {
            return;
        }
        setStdoutStderrTo0(path.getBytes());
    }
}
