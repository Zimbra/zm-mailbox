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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.map.ListOrderedMap;

import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.util.ZimbraLog;

public class ExtensionUtil {

	private static List sClassLoaders = new ArrayList();
	
	public static synchronized void loadAll() {
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
			
			ZimbraExtensionClassLoader zcl = new ZimbraExtensionClassLoader(extDirs[i], ExtensionUtil.class.getClassLoader());
			if (!zcl.hasExtensions()) {
				ZimbraLog.extensions.warn("no " + ZimbraExtensionClassLoader.ZIMBRA_EXTENSION_CLASS + " found, ignored: " + extDirs[i]);
				continue;
			}
			
			sClassLoaders.add(zcl);
		}
	}

	private static ListOrderedMap sInitializedExtensions = new ListOrderedMap();
	
	public static synchronized void initAll() {
		ZimbraLog.extensions.info("Initializing extensions");
		for (Iterator clIter = sClassLoaders.iterator(); clIter.hasNext();) {
			ZimbraExtensionClassLoader zcl = (ZimbraExtensionClassLoader)clIter.next();
			List classes = zcl.getExtensionClassNames();
			for (Iterator nameIter = classes.iterator(); nameIter.hasNext();) {
				String name = (String)nameIter.next();
				Class clz;
				try {
					clz = zcl.loadClass(name);
					ZimbraExtension ext = (ZimbraExtension)clz.newInstance();
					try {
						ext.init();
                        String extName = ext.getName();
						ZimbraLog.extensions.info("Initialized extension " + extName + ": " + name + "@" + zcl);
						sInitializedExtensions.put(extName, ext);
					} catch (Throwable t) { 
						ZimbraLog.extensions.warn("exception in " + name + ".init()", t);
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

	public static synchronized void destroyAll() {
		ZimbraLog.extensions.info("Destroying extensions");
        List extNames = sInitializedExtensions.asList();
		for (ListIterator iter = extNames.listIterator(extNames.size());
			iter.hasPrevious();)
		{
            String extName = (String) iter.previous();
			ZimbraExtension ext = (ZimbraExtension) getExtension(extName);
			try {
				ext.destroy();
				ZimbraLog.extensions.info("Destroyed extension " + extName + ": " + ext.getClass().getName() + "@" + ext.getClass().getClassLoader());
			} catch (Throwable t) {
				ZimbraLog.extensions.warn("exception in " + ext.getClass().getName() + ".destroy()", t);
			}
		}
        sInitializedExtensions.clear();
	}
    
    public static synchronized Class loadClass(String extensionName, String className) throws ClassNotFoundException {
        if (extensionName == null)
            return Class.forName(className);
        ZimbraExtension ext = (ZimbraExtension) sInitializedExtensions.get(extensionName);
        if (ext == null)
            throw new ClassNotFoundException("extension " + extensionName + " not found");
        ClassLoader loader = ext.getClass().getClassLoader();
        return loader.loadClass(className);
    }
    
    public static synchronized ZimbraExtension getExtension(String name) {
        return (ZimbraExtension) sInitializedExtensions.get(name);
    }
}
