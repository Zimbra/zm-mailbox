/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.zimbra.common.util.ByteUtil;

/**
 * Represents Zimlet distribution file.
 * 
 * @author jylee
 *
 */
public class ZimletFile implements Comparable {

	private File mBase;
	private InputStream mBaseStream;
	private byte[] mCopy;
	
	public int compareTo(Object obj) {
		if (obj instanceof ZimletFile) {
			ZimletFile f = (ZimletFile) obj;
			return getZimletName().compareTo(f.getZimletName());
		}
		return 0;
	}
	
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
			InputStream is = mContainer.getInputStream(mEntry);
			byte[] ret = ByteUtil.getContent(is, (int)mEntry.getSize());
			is.close();
			return ret;
		}
	}
	
	public static class ZimletDirEntry extends ZimletEntry {
		private File mFile;
		
		public ZimletDirEntry(File f) {
			super(f.getName());
			mFile = f;
		}
		public byte[] getContents() throws IOException {
			InputStream is = new FileInputStream(mFile);
			byte[] ret = ByteUtil.getContent(is, (int)mFile.length());
			is.close();
			return ret;
		}
	}
	
	public static class ZimletRawEntry extends ZimletEntry {
		private byte[] mData;
		
		public ZimletRawEntry(InputStream is, String name, int size) throws IOException {
			super(name);
			mData = new byte[size];
    		int num;
    		int offset, len;
    		offset = 0;
    		len = size;
    		while (len > 0 && (num = is.read(mData, offset, len)) != -1) {
    			offset += num;
    			len -= num;
    		}
	    }
		public byte[] getContents() throws IOException {
			return mData;
		}
	}
	
	private static final String XML_SUFFIX = ".xml";
	private static final String ZIP_SUFFIX = ".zip";
	private static final String CONFIG_TMPL = "config_template.xml";

	private String mDescFile;
	private Map<String,ZimletEntry> mEntries;

	private ZimletDescription mDesc;
	private String            mDescString;
	private ZimletConfig      mConfig;
	private String            mConfigString;
	
	private String mZimletName;
	
	public ZimletFile(String zimlet) throws IOException, ZimletException {
		mBase = new File(findZimlet(zimlet));
		initialize();
	}

	public ZimletFile(File parent, String zimlet) throws IOException, ZimletException {
		mBase = new File(parent, zimlet);
		initialize();
	}
	
	public ZimletFile(String name, InputStream is) throws IOException, ZimletException {
		mBaseStream = is;
		initialize(name);
	}

	private void initialize() throws IOException, ZimletException {
		String name = mBase.getName().toLowerCase();
		int index = name.lastIndexOf(File.separatorChar);
		if (index > 0) {
			name = name.substring(index + 1);
		}
		initialize(name);
	}
	
	private void initialize(String name) throws IOException, ZimletException {
		if (name.endsWith(ZIP_SUFFIX)) {
			name = name.substring(0, name.length() - 4);
		}
		mDescFile = name + XML_SUFFIX;
		
		mEntries = new HashMap<String,ZimletEntry>();

		if (mBaseStream != null) {
			mCopy = ByteUtil.getContent(mBaseStream, 0);
			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(mCopy));
			ZipEntry entry = zis.getNextEntry();
			while (entry != null) {
				mEntries.put(entry.getName().toLowerCase(), new ZimletRawEntry(zis, entry.getName(), (int)entry.getSize()));
				zis.closeEntry();
				entry = zis.getNextEntry();
			}
			zis.close();
		} else if (mBase.isDirectory()) {
			File[] files = mBase.listFiles();
			assert(files != null);
			for (int i = 0; i < files.length; i++) {
				mEntries.put(files[i].getName().toLowerCase(), new ZimletDirEntry(files[i]));
			}
		} else {
			ZipFile zip = new ZipFile(mBase);
			Enumeration entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				mEntries.put(entry.getName().toLowerCase(), new ZimletZipEntry(zip, entry));
			}
		}
		
		initZimletDescription();
		mZimletName = mDesc.getName();
	}
	
	private void initZimletDescription() throws IOException, ZimletException {
		if (mDesc == null) {
			ZimletEntry entry = (ZimletEntry)mEntries.get(mDescFile);
			if (entry == null) {
				throw new FileNotFoundException("zimlet description not found: " + mDescFile);
			}
			mDescString = new String(entry.getContents());
			mDesc = new ZimletDescription(mDescString);
		}
	}
	
	/**
	 * 
	 * @return The original XML zimlet description.
	 * @throws IOException
	 * @throws ZimletException
	 */
	public String getZimletDescString() throws IOException, ZimletException {
		initZimletDescription();
		return mDescString;
	}
	
	/**
	 * 
	 * @return The zimlet description for this instance.
	 * @throws IOException
	 * @throws ZimletException
	 */
	public ZimletDescription getZimletDescription() throws IOException, ZimletException {
		initZimletDescription();
		return mDesc;
	}
	
	public boolean hasZimletConfig() {
		return mEntries.containsKey(CONFIG_TMPL);
	}
	
	private void initZimletConfig() throws IOException, ZimletException {
		if (mConfig == null) {
			ZimletEntry entry = (ZimletEntry)mEntries.get(CONFIG_TMPL);
			if (entry == null) {
				throw new FileNotFoundException("zimlet config not found: " + CONFIG_TMPL);
			}
			mConfigString = new String(entry.getContents());
			mConfig = new ZimletConfig(mConfigString);
		}
	}
	
	/**
	 * 
	 * @return The original XML config template.
	 * @throws IOException
	 * @throws ZimletException
	 */
	public String getZimletConfigString() throws IOException, ZimletException {
		initZimletConfig();
		return mConfigString;
	}

	/**
	 * 
	 * @return The configuration section for this instance.
	 * @throws IOException
	 * @throws ZimletException
	 */
	public ZimletConfig getZimletConfig() throws IOException, ZimletException {
		initZimletConfig();
		return mConfig;
	}

	public Map getAllEntries() {
		return mEntries;
	}
	
	public String getZimletName() {
		return mZimletName;
	}
	
	public String getName() {
		return getZimletName() + ".zip";
	}
	
	public URL toURL() throws MalformedURLException {
		return mBase.toURL();
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
				if (files[i].equals(zimletTargetName)) {
					return zimlet + File.separator + files[i];
				}
			}
		}
		
		return zimlet;
	}
	
	public byte[] toByteArray() {
		return mCopy;
	}
}
