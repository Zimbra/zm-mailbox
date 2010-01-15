/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author jylee
 *
 */
public class ZimletClassLoader extends URLClassLoader {
	
	private List mZimletClassNames = new ArrayList();

	/**
	 * Load Zimlet server extension class.
	 */
	public ZimletClassLoader(File rootDir, String className, ClassLoader parent)
	throws MalformedURLException {
		super(fileToURL(rootDir, className), parent);
		mZimletClassNames.add(className);
	}

	public List getExtensionClassNames() {
		return mZimletClassNames;
	}
	
	private static URL[] fileToURL(File dir, String file)
	throws MalformedURLException {
		URL url = new File(dir, file).toURL();
		return new URL[] { url };
	}
}
