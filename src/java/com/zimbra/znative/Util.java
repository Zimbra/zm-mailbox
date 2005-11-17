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

import java.io.PrintWriter;
import java.io.StringWriter;

public class Util {

    private static final boolean mHaveNativeCode;
    
    private static boolean loadLibrary() {
        if (mHaveNativeCode) {
            return mHaveNativeCode;
        }
 
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            /* We do not have the shared library for windows yet. */
            return false;
        } else {
            try {
                if (osName.equalsIgnoreCase("Mac OS X")) {
                    /* TODO: is this really required?  I think not.
                     * loadLibrary should just work on the mac...
                     */ 
                    System.load("/opt/zimbra/lib/libzimbra-native.jnilib");
                } else {
                    System.loadLibrary("zimbra-native");
                }
                return true;
            } catch (UnsatisfiedLinkError ule) {
                /* On non-Windows, we fail if the shared library is
                 * not present for two reasons: (a) it lets porters
                 * know that this is something they have to deal with
                 * and (b) if tomcat is started as root, and the
                 * shared library did not load for some reason, drop
                 * privileges would not work. */
                halt("Failed to loadLibrary(zimbra-native)", ule);
            }
            return false;
        }
    }
    
    static {
        mHaveNativeCode = loadLibrary();
    }
    
    public static boolean haveNativeCode() {
        return mHaveNativeCode;
    }

    /**
     * Logs the given message and shuts down the server.  This method
     * is for use during the early life of the server, where Log4j has
     * not been initialized and/or we are unable to call Zimbra.halt.
     * There is no native code involved here.
     *
     * @param message the message to log before shutting down
     */
    public static void halt(String message) {
        try {
            System.err.println(message);
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }

    
    /**
     * Logs the given message and shuts down the server.  This method
     * is for use during the early life of the server, where Log4j has
     * not been initialized and/or we are unable to call Zimbra.halt.
     * There is no native code involved here.
     * 
     * @param message the message to log before shutting down
     * @param t the exception that was thrown
     */
    public static void halt(String message, Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println(message);
            t.printStackTrace(pw);
            System.err.println(sw.toString());
        } finally {
            Runtime.getRuntime().halt(1);
        }
    }
}
