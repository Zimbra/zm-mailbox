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

package com.zimbra.common.localconfig;

import com.zimbra.common.util.ZimbraLog;

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
        if (sUseZimbraLog) {
            ZimbraLog.misc.warn(message, e);
        } else {
            System.err.println("Warning: " + message);
            if (e != null) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        }
       
    }

    public static void error(String message) {
        error(message, null);
    }

    public static void error(String message, Exception e) {
        if (sQuietMode) {
            return;
        }
        if (sUseZimbraLog) {
            ZimbraLog.misc.warn(message, e);
        } else {
            System.err.println("Error: " + message);
            if (e != null) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        }
    }
}
