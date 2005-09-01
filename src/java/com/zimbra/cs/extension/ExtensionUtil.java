package com.zimbra.cs.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.util.ZimbraLog;

public class ExtensionUtil {

	private static List sClassLoaders = new ArrayList();
	
	public static synchronized void loadAll() {
		File extDir = new File(LC.zimbra_extensions_directory.value());
		if (extDir == null) {
			ZimbraLog.extension.info(LC.zimbra_extensions_directory.key() + " is null, no extensions loaded");
			return;
		}

		File[] extDirs = extDir.listFiles();
		for (int i = 0; i < extDirs.length; i++) {
			if (!extDirs[i].isDirectory()) {
				ZimbraLog.extension.warn("ignored non-directory in extensions directory: " + extDirs[i]);
				continue;
			}
			
			ZimbraExtensionClassLoader zcl = new ZimbraExtensionClassLoader(extDirs[i], ExtensionUtil.class.getClassLoader());
			if (!zcl.hasExtensions()) {
				ZimbraLog.extension.warn("no " + ZimbraExtensionClassLoader.ZIMBRA_EXTENSION_CLASS + " found, ignored: " + extDirs[i]);
				continue;
			}
			
			sClassLoaders.add(zcl);
		}
	}

	private static List sInitializedExtensions = new ArrayList();
	
	public static synchronized void initAll() {
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
						ZimbraLog.extension.info("Initialized extension: " + name + "@" + zcl);
						sInitializedExtensions.add(ext);
					} catch (Throwable t) { 
						ZimbraLog.extension.warn("exception in " + name + ".init()", t);
					}
				} catch (InstantiationException e) {
					ZimbraLog.extension.warn("exception occurred initializing extension " + name, e);
				} catch (IllegalAccessException e) {
					ZimbraLog.extension.warn("exception occurred initializing extension " + name, e);
				} catch (ClassNotFoundException e) {
					ZimbraLog.extension.warn("exception occurred initializing extension " + name, e);
				}
				
			}
		}
	}

	public static synchronized void destroyAll() {
		for (ListIterator iter = sInitializedExtensions.listIterator(sInitializedExtensions.size());
			iter.hasPrevious();)
		{
			ZimbraExtension ext = (ZimbraExtension)iter.previous();
			try {
				ext.destroy();
				ZimbraLog.extension.info("Initialized extension: " + ext.getClass().getName() + "@" + ext.getClass().getClassLoader());
				iter.remove();
			} catch (Throwable t) {
				ZimbraLog.extension.warn("exception in " + ext.getClass().getName() + ".destroy()", t);
			}
		}
	}
}
