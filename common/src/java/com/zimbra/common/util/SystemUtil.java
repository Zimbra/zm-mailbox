/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SystemUtil {

    public static final boolean ON_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");


    public static String getStackTrace() {
        return getStackTrace(new Throwable());
    }
    
    public static String getStackTrace(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
    
    /**
     * Returns the innermost exception wrapped by
     * <tt>t</tt>.  The innermost exception is found by iterating
     * the exceptions returned by {@link Throwable#getCause()}.
     * 
     * @return the innermost exception, or <tt>null</tt> if <tt>t</tt>
     * is <tt>null</tt>.
     */
    public static Throwable getInnermostException(Throwable t) {
        if (t == null) {
            return null;
        }
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
    
    /**
     * Returns the first non-null value in the given list.
     */
    public static <E> E coalesce(E ... values) {
        if (values != null) {
            for (E value : values) {
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }
    
    public static String getProductVersion() {
        return SystemUtil.class.getPackage().getImplementationVersion();
    }
}
