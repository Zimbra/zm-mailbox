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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;


/**
 * Wrapper around Log4j that supports <code>printf</code> functionality
 * via {@link String#format}.
 * 
 * @author bburtin
 *
 */
public class Log {

    Logger mLogger;
    
    Log(Logger logger) {
        if (logger == null) {
            throw new IllegalStateException("logger cannot be null");
        }
        mLogger = logger;
    }
    
    public boolean isDebugEnabled() {
        return mLogger.isDebugEnabled();
    }
    
    public boolean isInfoEnabled() {
        return mLogger.isInfoEnabled();
    }
    
    public boolean isWarnEnabled() {
        return mLogger.isEnabledFor(Priority.WARN);
    }
    
    public boolean isErrorEnabled() {
        return mLogger.isEnabledFor(Priority.ERROR);
    }

    public boolean isFatalEnabled() {
        return mLogger.isEnabledFor(Priority.FATAL);
    }
    

    public void debug(Object o) {
        mLogger.debug(o);
    }

    public void debug(Object o, Throwable t) {
        mLogger.debug(o, t);
    }

    public void debug(String format, Object ... objects) {
        if (isDebugEnabled()) {
            mLogger.debug(String.format(format, objects));
        }
    }

    public void debug(String format, Object o, Throwable t) {
        if (isDebugEnabled()) {
            mLogger.debug(String.format(format, o), t);
        }
    }

    public void debug(String format, Object o1, Object o2, Throwable t) {
        if (isDebugEnabled()) {
            mLogger.debug(String.format(format, o1, o2), t);
        }
    }

    public void debug(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isDebugEnabled()) {
            mLogger.debug(String.format(format, o1, o2, o3), t);
        }
    }
    

    public void info(Object o) {
        mLogger.info(o);
    }

    public void info(Object o, Throwable t) {
        mLogger.info(o, t);
    }

    public void info(String format, Object ... objects) {
        if (isInfoEnabled()) {
            mLogger.info(String.format(format, objects));
        }
    }

    public void info(String format, Object o, Throwable t) {
        if (isInfoEnabled()) {
            mLogger.info(String.format(format, o), t);
        }
    }

    public void info(String format, Object o1, Object o2, Throwable t) {
        if (isInfoEnabled()) {
            mLogger.info(String.format(format, o1, o2), t);
        }
    }

    public void info(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isInfoEnabled()) {
            mLogger.info(String.format(format, o1, o2, o3), t);
        }
    }
    

    public void warn(Object o) {
        mLogger.warn(o);
    }

    public void warn(Object o, Throwable t) {
        mLogger.warn(o, t);
    }

    public void warn(String format, Object ... objects) {
        if (isWarnEnabled()) {
            mLogger.warn(String.format(format, objects));
        }
    }

    public void warn(String format, Object o, Throwable t) {
        if (isWarnEnabled()) {
            mLogger.warn(String.format(format, o), t);
        }
    }

    public void warn(String format, Object o1, Object o2, Throwable t) {
        if (isWarnEnabled()) {
            mLogger.warn(String.format(format, o1, o2), t);
        }
    }

    public void warn(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isWarnEnabled()) {
            mLogger.warn(String.format(format, o1, o2, o3), t);
        }
    }
    

    public void error(Object o) {
        mLogger.error(o);
    }

    public void error(Object o, Throwable t) {
        mLogger.error(o, t);
    }

    public void error(String format, Object ... objects) {
        if (isErrorEnabled()) {
            mLogger.error(String.format(format, objects));
        }
    }

    public void error(String format, Object o, Throwable t) {
        if (isErrorEnabled()) {
            mLogger.error(String.format(format, o), t);
        }
    }

    public void error(String format, Object o1, Object o2, Throwable t) {
        if (isErrorEnabled()) {
            mLogger.error(String.format(format, o1, o2), t);
        }
    }

    public void error(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isErrorEnabled()) {
            mLogger.error(String.format(format, o1, o2, o3), t);
        }
    }
    

    public void fatal(Object o) {
        mLogger.fatal(o);
    }

    public void fatal(Object o, Throwable t) {
        mLogger.fatal(o, t);
    }

    public void fatal(String format, Object ... objects) {
        if (isFatalEnabled()) {
            mLogger.fatal(String.format(format, objects));
        }
    }

    public void fatal(String format, Object o, Throwable t) {
        if (isFatalEnabled()) {
            mLogger.fatal(String.format(format, o), t);
        }
    }

    public void fatal(String format, Object o1, Object o2, Throwable t) {
        if (isFatalEnabled()) {
            mLogger.fatal(String.format(format, o1, o2), t);
        }
    }

    public void fatal(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isFatalEnabled()) {
            mLogger.fatal(String.format(format, o1, o2, o3), t);
        }
    }
}
