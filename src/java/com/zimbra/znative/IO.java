/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.znative;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IO {

    private static native void link0(byte[] oldpath, byte[] newpath)
        throws IOException;
	
    private static native int linkCount0(byte[] path)
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
            return linkCount0(path.getBytes());
        } else {
            return 1;
        }
    }

    public static void setStdoutStderrTo(String path) throws IOException {
        if (!Util.haveNativeCode()) {
            return;
        }
        setStdoutStderrTo0(path.getBytes());
    }
}
