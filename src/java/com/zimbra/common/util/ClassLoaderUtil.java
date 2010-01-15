/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
