/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

public class Process {
    private static native int getuid0();
    
    public static int getuid() {
        if (Util.haveNativeCode()) {
            return getuid0();
        } else {
            return -1;
        }
    }

    private static native int geteuid0();

    public static int geteuid() {
        if (Util.haveNativeCode()) {
            return geteuid0();
        } else {
            return -1;
        }
    }
    
    private static native int getgid0();
    
    public static int getgid() {
        if (Util.haveNativeCode()) {
            return getgid0();
        } else {
            return -1;
        }
    }
    
    private static native int getegid0();
    
    public static int getegid() {
        if (Util.haveNativeCode()) {
            return getegid0();
        } else {
            return -1;
        }
    }

    private static native void setPrivileges0(byte[] username, int uid, int gid);

    public static void setPrivileges(String username, int uid, int gid)
        throws OperationFailedException
    {
        if (Util.haveNativeCode()) {
            setPrivileges0(username.getBytes(), uid, gid);
        }
    }
}
