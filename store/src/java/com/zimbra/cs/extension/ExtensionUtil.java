/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.extension;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.Factory;
import com.zimbra.cs.redolog.op.RedoableOp;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtensionUtil {

    private static List<ZimbraExtensionClassLoader> sClassLoaders = new ArrayList<ZimbraExtensionClassLoader>();
    private static ClassLoader sExtParentClassLoader;
    private static Map<String, ZimbraExtension> sInitializedExtensions = new LinkedHashMap<String, ZimbraExtension>();


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

    static {
        File extCommonDir = new File(LC.zimbra_extension_common_directory.value());
        URL[] extCommonURLs = dirListToURLs(extCommonDir);
        if (extCommonURLs == null) {
            // No ext-common libraries are present.
            sExtParentClassLoader = ExtensionUtil.class.getClassLoader();
        } else {
            sExtParentClassLoader = new URLClassLoader(extCommonURLs, ExtensionUtil.class.getClassLoader());
        }
        loadAll();
    }

    protected static synchronized void addClassLoader(ZimbraExtensionClassLoader zcl) {
        sClassLoaders.add(zcl);
    }

    private static synchronized void loadAll() {
        if (LC.zimbra_extension_directory.value() == null) {
            ZimbraLog.extensions.info(LC.zimbra_extension_directory.key() +
                    " is null, no extensions loaded");
            return;
        }
        File extDir = new File(LC.zimbra_extension_directory.value());
        ZimbraLog.extensions.info("Loading extensions from " + extDir.getPath());

        File[] extDirs = extDir.listFiles();
        if (extDirs == null) {
            return;
        }
        for (File dir : extDirs) {
            if (!dir.isDirectory()) {
                ZimbraLog.extensions.warn("ignored non-directory in extensions directory: " + dir);
                continue;
            }

            ZimbraExtensionClassLoader zcl = new ZimbraExtensionClassLoader(
                    dirListToURLs(dir), sExtParentClassLoader);
            if (!zcl.hasExtensions()) {
                ZimbraLog.extensions.warn("no " + ZimbraExtensionClassLoader.ZIMBRA_EXTENSION_CLASS + " found, ignored: " + dir);
                continue;
            }

            sClassLoaders.add(zcl);
        }
    }

    public static interface ExtensionMatcher {
        public boolean matches(ZimbraExtension ext);
    }

    /** @param matcher - Used to filter which extensions to initialize.  Can be null */
    public static synchronized void initAllMatching(ExtensionMatcher matcher) {
        ZimbraLog.extensions.info("Initializing extensions");
        List<ZimbraExtensionClassLoader> sClassLoadersToRemove = new ArrayList<ZimbraExtensionClassLoader>();
        for (ZimbraExtensionClassLoader zcl : sClassLoaders) {
            for (String name : zcl.getExtensionClassNames()) {
                try {
                    Class<?> clazz = zcl.loadClass(name);
                    ZimbraExtension ext = (ZimbraExtension) clazz.newInstance();
                    if (matcher != null && !matcher.matches(ext)) {
                        continue;
                    }
                    try {
                        ext.init();
                        RedoableOp.registerClassLoader(ext.getClass().getClassLoader());
                        String extName = ext.getName();
                        ZimbraLog.extensions.info("Initialized extension %s: %s@%s", extName, name, zcl);
                        sInitializedExtensions.put(extName, ext);
                    } catch (ExtensionException|ServiceException e) {
                        ZimbraLog.extensions.info("Disabled '%s' %s", ext.getName(), e.getMessage());
                        ext.destroy();
                        RedoableOp.deregisterClassLoader(ext.getClass().getClassLoader());
                        sClassLoadersToRemove.add(zcl);
                    } catch (Exception e) {
                        ZimbraLog.extensions.warn("exception in %s.init()", name, e);
                        RedoableOp.deregisterClassLoader(ext.getClass().getClassLoader());
                    }
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    ZimbraLog.extensions.warn("exception occurred initializing extension %s", name, e);
                }
            }
        }

        for (ZimbraExtensionClassLoader zcl : sClassLoadersToRemove) {
            sClassLoaders.remove(zcl);
        }
    }

    public static synchronized void initAll() {
        initAllMatching((ExtensionMatcher) null);
    }

    public static synchronized void init(String className) {
        boolean found = false;
        for (ZimbraExtensionClassLoader zcl : sClassLoaders) {
            try {
                if (zcl.getExtensionClassNames().contains(className)) {
                    Class<?> clazz = zcl.loadClass(className);
                    ZimbraExtension ext = (ZimbraExtension) clazz.newInstance();
                    try {
                        ext.init();
                        ZimbraLog.extensions.info("Initialized extension %s: %s@%s", ext.getName(), className, zcl);
                        sInitializedExtensions.put(ext.getName(), ext);
                        found = true;
                        break;
                    } catch (ExtensionException e) {
                        ZimbraLog.extensions.info("Disabled '%s' %s", ext.getName(), e.getMessage());
                        ext.destroy();
                    } catch (Exception e) {
                        ZimbraLog.extensions.warn("exception in %s.init()", className, e);
                        RedoableOp.deregisterClassLoader(
                                ext.getClass().getClassLoader());
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                ZimbraLog.extensions.warn("exception occurred initializing extension %s",className, e);
            }
        }
        if (!found) {
            ZimbraLog.extensions.warn("unable to locate extension class %s, not found", className);
        }
    }

    public static void initEphemeralBackendExtension(String backendName) throws ServiceException {
        Level savedExten = ZimbraLog.extensions.getLevel();
        try {
            if (!ZimbraLog.ephemeral.isDebugEnabled()) {
                // cut down on noise unless enabled debug
                ZimbraLog.extensions.setLevel(Level.error);
            }
            ExtensionUtil.initAllMatching(new EphemeralStore.EphemeralStoreMatcher(backendName));
        } finally {
            ZimbraLog.extensions.setLevel(savedExten);
        }
        Factory factory = EphemeralStore.getFactory(backendName, true);

        if (factory == null) {
            ZimbraLog.ephemeral.error(
                    "no extension class name found for backend '%s'",
                    backendName);
            return; // keep Eclipse happy
        }
        ZimbraLog.ephemeral.info("Using ephemeral backend %s (%s)", backendName,
                factory.getClass().getName());
    }


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
        for (String extName : extNames) {
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

    public static synchronized void destroy(String extName) {
        ZimbraExtension ext = getExtension(extName);
        try {
            RedoableOp.deregisterClassLoader(ext.getClass().getClassLoader());
            ext.destroy();
            ZimbraLog.extensions.info("Destroyed extension " + extName + ": " + ext.getClass().getName() + "@" + ext.getClass().getClassLoader());
            sInitializedExtensions.remove(extName);
        } catch (Exception e) {
            ZimbraLog.extensions.warn("exception in " + ext.getClass().getName() + ".destroy()", e);
        }
    }

    public static synchronized Class<?> loadClass(String extensionName, String className) throws ClassNotFoundException {
        if (extensionName == null) {
            return Class.forName(className);
        }
        ZimbraExtension ext = sInitializedExtensions.get(extensionName);
        if (ext == null) {
            throw new ClassNotFoundException("extension " + extensionName + " not found");
        }
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
        return sInitializedExtensions.get(name);
    }
}
