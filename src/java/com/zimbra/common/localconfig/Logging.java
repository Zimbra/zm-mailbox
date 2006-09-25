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

package com.zimbra.common.localconfig;

// xxx bburtin: Termporarily disabling the dependency on ZimbraLog until we move
// logging code into ZimbraCommon
// import com.zimbra.cs.util.ZimbraLog;

/*
 * Log4j is expensive for command line invocation
 */
public class Logging {

    private static boolean sQuietMode = false;
    
    public static void setQuietMode(boolean value) {
        sQuietMode = value;
    }

    private static boolean sUseZimbraLog = true;

    public static void setUseZimbraLog(boolean value) {
        sUseZimbraLog = value;
    }

    public static void warn(String message) {
        warn(message, null);
    }

    public static void warn(String message, Exception e) {
        if (sQuietMode) {
            return;
        }
        /*
        if (sUseZimbraLog) {
            ZimbraLog.misc.warn(message, e);
        } else {*/
            System.err.println("Warning: " + message);
            if (e != null) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        // }
       
    }

    public static void error(String message) {
        error(message, null);
    }

    public static void error(String message, Exception e) {
        if (sQuietMode) {
            return;
        }
        /*
        if (sUseZimbraLog) {
            ZimbraLog.misc.warn(message, e);
        } else { */
            System.err.println("Error: " + message);
            if (e != null) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        // }
    }
}
