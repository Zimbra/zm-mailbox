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
