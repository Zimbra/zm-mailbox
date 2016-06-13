/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
