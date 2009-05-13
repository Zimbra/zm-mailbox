/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.common.util;

/**
 * Inherit from this logging helper class and implement the getInstanceInfo()
 * method so that it returns identifying information about your class instance
 * (ie Username, settings, whatever) -- that information is automatically
 * appended to the log output from this class.
 * 
 * Optionally, if you override the formatObject() function then the printf-style
 * log APIs can be made to format objects in special ways: since the formatting
 * happens AFTER the "isXXXEnabled" check, this is potentially a useful performance
 * win for nontrivial formatting.
 */
public abstract class ClassLogger {

    /**
     * @return Identifying information about this instance.  This string
     * is prepended to logging output
     */
    protected abstract String getInstanceInfo();

    /**
     * For the printf-style log APIs, this function is called once for every Object
     * paremeter -- it can be used to format objects in a special way based on their
     * type or something else
     * 
     * @param o
     * @return
     */
    protected Object formatObject(Object o) {
        if (o instanceof Throwable) {
            return SystemUtil.getStackTrace((Throwable) o);
        } else {
            return o;
        }
    }

    protected ClassLogger(Log log) {
        mLog = log;
    }

    protected Log mLog;

    protected Object[] printArgs(Object[] objs) {
        if (objs == null)
            return null;
        else {
            for (int i = 0; i < objs.length; i++) {
                objs[i] = formatObject(objs[i]);
            }
            return objs;
        }
    }

    protected void debug(String format, Object... objects) {
        if (mLog.isDebugEnabled()) {
            mLog.debug(getInstanceInfo() + " - " + format, printArgs(objects));
        }
    }
    protected void info(String format, Object... objects) {
        if (mLog.isInfoEnabled()) {
            mLog.info(getInstanceInfo() + " - " + format, printArgs(objects));
        }
    }
    protected void warn(String format, Object... objects) {
        if (mLog.isWarnEnabled()) {
            mLog.warn(getInstanceInfo() + " - " + format, printArgs(objects));
        }
    }
    protected void error(String format, Object... objects) {
        if (mLog.isErrorEnabled()) {
            mLog.error(getInstanceInfo() + " - " + format, printArgs(objects));
        }
    }
    protected void debug(String str, Throwable t) {
        if (mLog.isDebugEnabled())
            mLog.debug(getInstanceInfo() + " - " + str, t);
    }
    protected void info(String str, Throwable t) {
        if (mLog.isInfoEnabled())
            mLog.info(getInstanceInfo() + " - " + str, t);
    }
    protected void warn(String str, Throwable t) {
        if (mLog.isWarnEnabled())
            mLog.warn(getInstanceInfo() + " - " + str, t);
    }
}
