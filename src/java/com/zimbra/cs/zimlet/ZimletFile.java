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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.zimbra.cs.util.ByteUtil;

/**
 * Represents Zimlet distribution file.
 * 
 * @author jylee
 *
 */
public class ZimletFile extends ZipFile {

	private static final String XML_SUFFIX = ".xml";
	private static final String ZIP_SUFFIX = ".zip";
	private static final String CONFIG_TMPL = "config_template.xml";

	private String mZimletName;
	private String mDescContent;
	private String mConfigContent;
	
	private List   mEntries;
	
	public ZimletFile(String zimlet) throws IOException {
		super(getFile(zimlet));
		initialize();
	}

	private void initialize() throws IOException {
		String name = getName().toLowerCase();
		int index = name.lastIndexOf(File.separatorChar);
		if (index > 0) {
			name = name.substring(index + 1);
		}
		if (name.endsWith(ZIP_SUFFIX)) {
			name = name.substring(0, name.length() - 4);
		}
		mZimletName = name;
		String descFile = name + XML_SUFFIX;
		
		mEntries = new ArrayList();
		Enumeration entries = entries();
		boolean zimletDescriptionFound = false;
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			String entryName = entry.getName().toLowerCase();
			if (entryName.equals(descFile)) {
				zimletDescriptionFound = true;
				mDescContent = new String(getEntryContent(entry));
			} else if (entryName.equals(CONFIG_TMPL)) {
				mConfigContent = new String(getEntryContent(entry));
			}
			mEntries.add(entryName);
		}
		
		if (!zimletDescriptionFound) {
			throw new FileNotFoundException("Zimlet description " + descFile + 
											" not found in " + getName());
		}
	}
	
	public String getZimletDescription() {
		return mDescContent;
	}
	
	public String getZimletConfig() {
		return mConfigContent;
	}
	
	public String[] getAllEntryNames() {
		return (String[])mEntries.toArray(new String[0]);
	}
	
	public byte[] getEntryContent(String name) throws IOException {
		return getEntryContent(getEntry(name));
	}

	public byte[] getEntryContent(ZipEntry entry) throws IOException {
		if (entry == null) {
			throw new FileNotFoundException(entry.getName() + " not found in the Zimlet distribution " + mZimletName);
		}
		return ByteUtil.getContent(getInputStream(entry), (int)entry.getSize());
	}
	
	private static File getFile(String zimlet) throws FileNotFoundException {
		File zimletFile = new File(zimlet);
		
		if (!zimletFile.exists()) {
			zimletFile = new File(zimlet + ZIP_SUFFIX);
			if (!zimletFile.exists()) {
				throw new FileNotFoundException("Zimlet not found: " + zimlet);
			}
		}
		
		if (zimletFile.isDirectory()) {
			String[] files = zimletFile.list();
			String zimletTargetName = zimletFile.getName() + ZIP_SUFFIX;
			int i;
			for (i = 0; i < files.length; i++) {
				if (files[i] == zimletTargetName) {
					return new File(zimletFile, files[i]);
				}
			}
			throw new FileNotFoundException("Zimlet not found in the directory: " + zimlet);
		}
		
		return zimletFile;
	}
}
