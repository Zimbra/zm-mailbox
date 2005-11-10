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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.zimbra.cs.util.ByteUtil;

/**
 * Represents Zimlet distribution file.
 * 
 * @author jylee
 *
 */
public class ZimletFile extends File {

	public static abstract class ZimletEntry {
		protected String mName;
		
		protected ZimletEntry(String name) {
			mName = name;
		}
		public String getName() {
			return mName;
		}
		public abstract byte[] getContents() throws IOException;
	}
	
	public static class ZimletZipEntry extends ZimletEntry {
		private ZipFile  mContainer;
		private ZipEntry mEntry;
		
		public ZimletZipEntry(ZipFile f, ZipEntry e) {
			super(e.getName());
			mContainer = f;
			mEntry = e;
		}
		public byte[] getContents() throws IOException {
			return ByteUtil.getContent(mContainer.getInputStream(mEntry), (int)mEntry.getSize());
		}
	}
	
	public static class ZimletDirEntry extends ZimletEntry {
		private File mFile;
		
		public ZimletDirEntry(File f) {
			super(f.getName());
			mFile = f;
		}
		public byte[] getContents() throws IOException {
			return ByteUtil.getContent(new FileInputStream(mFile), (int)mFile.length());
		}
	}
	
	private static final String XML_SUFFIX = ".xml";
	private static final String ZIP_SUFFIX = ".zip";
	private static final String CONFIG_TMPL = "config_template.xml";

	private String mDescFile;
	private Map    mEntries;

	public ZimletFile(String zimlet) throws IOException {
		super(findZimlet(zimlet));
		initialize();
	}

	public ZimletFile(File parent, String zimlet) throws IOException {
		super(parent, zimlet);
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
		mDescFile = name + XML_SUFFIX;
		
		mEntries = new HashMap();

		if (isDirectory()) {
			File[] files = listFiles();
			for (int i = 0; i < files.length; i++) {
				mEntries.put(files[i].getName().toLowerCase(), new ZimletDirEntry(files[i]));
			}
		} else {
			ZipFile zip = new ZipFile(this);
			Enumeration entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				mEntries.put(entry.getName().toLowerCase(), new ZimletZipEntry(zip, entry));
			}
		}
	}
	
	public String getZimletDescription() throws IOException {
		ZimletEntry entry = (ZimletEntry)mEntries.get(mDescFile);
		if (entry == null) {
			return null;
		}
		return new String(entry.getContents());
	}
	
	public String getZimletConfig() throws IOException {
		ZimletEntry entry = (ZimletEntry)mEntries.get(CONFIG_TMPL);
		if (entry == null) {
			return null;
		}
		return new String(entry.getContents());
	}
	
	public Map getAllEntries() {
		return mEntries;
	}
	
	private static String findZimlet(String zimlet) throws FileNotFoundException {
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
					return zimlet + File.separator + files[i];
				}
			}
		}
		
		return zimlet;
	}
}
