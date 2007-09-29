/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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
package com.zimbra.znative;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IO {

    private static native void link0(byte[] oldpath, byte[] newpath)
        throws IOException;
	
    private static native int linkCount0(byte[] path)
        throws IOException;

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
}
