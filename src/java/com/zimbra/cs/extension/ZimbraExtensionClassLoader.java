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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
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
	
	private List mExtensionClassNames = new ArrayList();

	/**
	 * Load classes from all jar files or directories in the directory
	 * 'dir'.
	 */
	public ZimbraExtensionClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		findExtensions();
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
	
	public List getExtensionClassNames() {
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
			for (Iterator iter = attrs.keySet().iterator(); iter.hasNext();) {
				Attributes.Name name = (Attributes.Name)iter.next();
				ZimbraLog.extensions.debug("Manifest attribute=" + name + " value=" + attrs.getValue(name));
			}
		}
		String classname = (String)attrs.getValue(ZIMBRA_EXTENSION_CLASS);
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
