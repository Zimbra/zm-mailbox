/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.zimbra.common.util.ZimbraLog;

public class ClassLoaderUtil {
    
    public static ClassLoader getClassLoaderByDirectory(String directory) {
        ClassLoader classLoader = null;
        try {
            String dir = directory;
            // Append "/" at the end to tell URLClassLoader this is a
            // directory rather than a JAR file.
            if (!dir.endsWith("/"))
                dir = dir + "/";
            URL urls[] = new URL[1];
            urls[0] = new URL("file://" + dir);
            classLoader = new URLClassLoader(urls);
        } catch (MalformedURLException e) {
            ZimbraLog.system.error("unable to get ClassLoader for " + directory, e);
            return null;
        }
        
        return classLoader;
    }
}
