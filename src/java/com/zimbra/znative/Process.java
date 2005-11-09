/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
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
