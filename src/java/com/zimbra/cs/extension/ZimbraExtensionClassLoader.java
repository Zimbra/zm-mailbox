/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.zimbra.common.util.ZimbraLog;

/**
 * Loads an extension.
 */
public class ZimbraExtensionClassLoader extends URLClassLoader {

    public static final String ZIMBRA_EXTENSION_CLASS = "Zimbra-Extension-Class";

    private List<String> mExtensionClassNames = new ArrayList<String>();

    /**
     * Load classes from all jar files or directories in the directory
     * 'dir'.
     */
    public ZimbraExtensionClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        findExtensions();
    }

    /**
     * For testing.
     *
     * @param url extension class path directory
     * @param clazz extension class name
     * @param parent parent class loader
     */
    ZimbraExtensionClassLoader(URL url, String clazz) {
        super(new URL[]{url},
                ZimbraExtensionClassLoader.class.getClassLoader());
        mExtensionClassNames.add(clazz);
    }

    private void findExtensions() {
        URL[] urls = getURLs();
        for (int i = 0; i < urls.length; i++) {
            File entry = new File(urls[i].getFile());
            String className = getExtensionClassInManifest(entry);
            if (className != null) {
                mExtensionClassNames.add(className);
            }
        }
    }

    public boolean hasExtensions() {
        return mExtensionClassNames.size() > 0;
    }

    public List<String> getExtensionClassNames() {
        return mExtensionClassNames;
    }

    /*
     * URLClassLoader does not provide access to manifest of each
     * jar files.  I don't want to do this with resources,
     * so we have to walk all the jar files or directories and
     * read manifest ourselves.
     */
    private String getExtensionClassInManifest(File file) {
        Manifest man = null;
        if (file.isDirectory()) {
            File manifestFile = new File(file, JarFile.MANIFEST_NAME);
            if (manifestFile.exists()) {
                // don't need BufferedInputStream because Manifest uses it's
                // own buffering stream around the InputStream we provide.
                try {
                    man = new Manifest(new FileInputStream(manifestFile));
                } catch (IOException ioe) {
                    if (ZimbraLog.extensions.isDebugEnabled()) {
                        ZimbraLog.extensions.debug("exception looking for manifest in directory: " + file, ioe);
                    }
                }
                if (man == null) {
                    if (ZimbraLog.extensions.isDebugEnabled()) {
                        ZimbraLog.extensions.debug("no manifest for directory: " + file);
                    }
                    return null;
                }
            }
        } else if (file.isFile()) {
            JarFile jarFile;
            try {
                jarFile = new JarFile(file);
                man = jarFile.getManifest();
            } catch (IOException ioe) {
                if (ZimbraLog.extensions.isDebugEnabled()) {
                    ZimbraLog.extensions.debug("exception looking for manifest in jar file: " + file, ioe);
                }
            }
            if (man == null) {
                if (ZimbraLog.extensions.isDebugEnabled()) {
                    ZimbraLog.extensions.debug("no manifest for jar file: " + file);
                }
                return null;
            }
        } else {
            ZimbraLog.extensions.warn("entry in extension load path is not file or directory: " + file);
            return null;
        }

        Attributes attrs = man.getMainAttributes();
        if (ZimbraLog.extensions.isDebugEnabled()) {
            for (Object key : attrs.keySet()) {
                Attributes.Name name = (Attributes.Name) key;
                ZimbraLog.extensions.debug("Manifest attribute=" + name + " value=" + attrs.getValue(name));
            }
        }
        String classname = (String) attrs.getValue(ZIMBRA_EXTENSION_CLASS);
        if (classname == null) {
            if (ZimbraLog.extensions.isDebugEnabled()) {
                ZimbraLog.extensions.debug("no extension class found in manifest of: " + file);
            }
        } else {
            ZimbraLog.extensions.info("extension " + classname + " found in " + file);
        }
        return classname;
    }

}
