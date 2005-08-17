package com.zimbra.cs.localconfig;

import com.zimbra.cs.util.LiquidLog;

/*
 * Log4j is expensive for command line invocation
 */
public class Logging {

    private static boolean sQuietMode = false;
    
    public static void setQuietMode(boolean value) {
        sQuietMode = value;
    }

    private static boolean sUseLiquidLog = true;

    public static void setUseLiquidLog(boolean value) {
        sUseLiquidLog = value;
    }

    public static void warn(String message) {
        warn(message, null);
    }

    public static void warn(String message, Exception e) {
        if (sQuietMode) {
            return;
        }
        if (sUseLiquidLog) {
            LiquidLog.misc.warn(message, e);
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
        if (sUseLiquidLog) {
            LiquidLog.misc.warn(message, e);
        } else {
            System.err.println("Error: " + message);
            if (e != null) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        }
    }
}
