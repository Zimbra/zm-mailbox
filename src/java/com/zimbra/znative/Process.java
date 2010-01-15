/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
