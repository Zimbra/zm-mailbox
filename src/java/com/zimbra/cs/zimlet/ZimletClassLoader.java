/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
