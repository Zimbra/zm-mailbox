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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.op.RedoableOp;

public class ExtensionUtil {

    private static List<ZimbraExtensionClassLoader> sClassLoaders = new ArrayList<ZimbraExtensionClassLoader>();

    public static URL[] dirListToURLs(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        List<URL> urls = new ArrayList<URL>(files.length);
        for (int i = 0; i < files.length; i++) {
            try {
                URI uri = files[i].toURI();
                URL url = uri.toURL();
                urls.add(url);
                if (ZimbraLog.extensions.isDebugEnabled()) {
                    ZimbraLog.extensions.debug("adding url: " + url);
                }
            } catch (MalformedURLException mue) {
                ZimbraLog.extensions.warn("ExtensionsUtil: exception creating url for " + files[i], mue);
            }
        }
        return urls.toArray(new URL[0]);
    }

    private static ClassLoader sExtParentClassLoader;

    static {
        File extCommonDir = new File(LC.zimbra_extensions_common_directory.value());
        URL[] extCommonURLs = dirListToURLs(extCommonDir);
        if (extCommonURLs == null) {
            // No ext-common libraries are present.
            sExtParentClassLoader = ExtensionUtil.class.getClassLoader();
        } else {
            sExtParentClassLoader = new URLClassLoader(extCommonURLs, ExtensionUtil.class.getClassLoader());    
        }
        loadAll();
    }

    private static synchronized void loadAll() {
        File extDir = new File(LC.zimbra_extensions_directory.value());
        if (extDir == null) {
            ZimbraLog.extensions.info(LC.zimbra_extensions_directory.key() + " is null, no extensions loaded");
            return;
        }
        ZimbraLog.extensions.info("Loading extensions from " + extDir.getPath());

        File[] extDirs = extDir.listFiles();
        if (extDirs == null) {
            return;
        }
        for (int i = 0; i < extDirs.length; i++) {
            if (!extDirs[i].isDirectory()) {
                ZimbraLog.extensions.warn("ignored non-directory in extensions directory: " + extDirs[i]);
                continue;
            }

            ZimbraExtensionClassLoader zcl = new ZimbraExtensionClassLoader(dirListToURLs(extDirs[i]), sExtParentClassLoader);
            if (!zcl.hasExtensions()) {
                ZimbraLog.extensions.warn("no " + ZimbraExtensionClassLoader.ZIMBRA_EXTENSION_CLASS + " found, ignored: " + extDirs[i]);
                continue;
            }

            sClassLoaders.add(zcl);
        }
    }

    private static Map<String, ZimbraExtension> sInitializedExtensions = new LinkedHashMap<String, ZimbraExtension>();

    @SuppressWarnings("unchecked")
    public static synchronized void initAll() {
        ZimbraLog.extensions.info("Initializing extensions");
        for (ZimbraExtensionClassLoader zcl : sClassLoaders) {
            for (String name : zcl.getExtensionClassNames()) {
                Class<ZimbraExtension> clz;
                try {
                    clz = (Class<ZimbraExtension>) zcl.loadClass(name);
                    ZimbraExtension ext = clz.newInstance();
                    try {
                        ext.init();
                        RedoableOp.registerClassLoader(ext.getClass().getClassLoader());
                        String extName = ext.getName();
                        ZimbraLog.extensions.info("Initialized extension " + extName + ": " + name + "@" + zcl);
                        sInitializedExtensions.put(extName, ext);
                    } catch (Exception e) { 
                        ZimbraLog.extensions.warn("exception in " + name + ".init()", e);
                    }
                } catch (InstantiationException e) {
                    ZimbraLog.extensions.warn("exception occurred initializing extension " + name, e);
                } catch (IllegalAccessException e) {
                    ZimbraLog.extensions.warn("exception occurred initializing extension " + name, e);
                } catch (ClassNotFoundException e) {
                    ZimbraLog.extensions.warn("exception occurred initializing extension " + name, e);
                }

            }
        }
    }
    
    /**
     * Called 
     */
    public static synchronized void postInitAll() {
        ZimbraLog.extensions.info("Post-Initializing extensions");
        
        for (Object o : sInitializedExtensions.values()) {
            if (o instanceof ZimbraExtensionPostInit) {
                ((ZimbraExtensionPostInit)o).postInit();
            }
        }
    }
    

    public static synchronized void destroyAll() {
        ZimbraLog.extensions.info("Destroying extensions");
        List<String> extNames = new ArrayList<String>(sInitializedExtensions.keySet());
        for (ListIterator<String> iter = extNames.listIterator(extNames.size());
        iter.hasPrevious();)
        {
            String extName = iter.previous();
            ZimbraExtension ext = getExtension(extName);
            try {
                RedoableOp.deregisterClassLoader(ext.getClass().getClassLoader());
                ext.destroy();
                ZimbraLog.extensions.info("Destroyed extension " + extName + ": " + ext.getClass().getName() + "@" + ext.getClass().getClassLoader());
            } catch (Exception e) {
                ZimbraLog.extensions.warn("exception in " + ext.getClass().getName() + ".destroy()", e);
            }
        }
        sInitializedExtensions.clear();
    }

    public static synchronized Class<?> loadClass(String extensionName, String className) throws ClassNotFoundException {
        if (extensionName == null)
            return Class.forName(className);
        ZimbraExtension ext = sInitializedExtensions.get(extensionName);
        if (ext == null)
            throw new ClassNotFoundException("extension " + extensionName + " not found");
        ClassLoader loader = ext.getClass().getClassLoader();
        return loader.loadClass(className);
    }

    /**
     * look for the specified class on our class path then across all extension class loaders and return first match.
     *
     * @param name class name to load
     * @return class
     * @throws ClassNotFoundException if class is not found
     */
    public static synchronized Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return ExtensionUtil.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            // ignore and look through extensions
        }
        for (ZimbraExtensionClassLoader zcl : sClassLoaders) {
            try {
                return zcl.loadClass(name);
            } catch (ClassNotFoundException e) {
                // ignore and keep looking
            }
        }
        throw new ClassNotFoundException(name);
    }


    public static synchronized ZimbraExtension getExtension(String name) {
        return (ZimbraExtension) sInitializedExtensions.get(name);
    }
}
