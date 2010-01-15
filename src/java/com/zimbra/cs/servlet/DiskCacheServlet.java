/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servlet;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.collections.LRUMap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.admin.FlushCache;

/**
 * Base class for servlets that cache resources to disk.
 */

public abstract class DiskCacheServlet extends ZimbraServlet {

	//
	// Constants
	//

	protected static final String P_CACHE_SIZE = "resource-cache-size";
	protected static final String P_CACHE_DIR = "resource-cache-dir";

	protected static final int DEFAULT_CACHE_SIZE = 1000;

	protected static final String EXT_COMPRESSED = ".gz";

	//
	// Data
	//

	private int cacheSize;
	private String cacheDirName;
	private File cacheDir;

	private Map<String,File> cache;

	//
	// Constructors
	//

	protected DiskCacheServlet(String cacheDirName) {
		this.cacheDirName = cacheDirName;
	}

	//
	// Servlet methods
	//

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		createCache();
		createCacheDir();
	}

	//
	// Servlet methods
	//

	public void service(ServletRequest req, ServletResponse resp)
	throws IOException, ServletException {
		Boolean flushCache = (Boolean)req.getAttribute(FlushCache.FLUSH_CACHE);
		if (flushCache != null && flushCache.booleanValue()) {
			if (ZimbraLog.misc.isDebugEnabled()) {
				ZimbraLog.misc.debug("flushing "+getClass().getName()+" cache");
			}
			boolean deleteFiles = true; // TODO: Should we skip the delete?
			clearCache(deleteFiles);
			return;
		}
		super.service(req, resp);
	}

	//
	// Protected methods
	//

	// properties

	protected int getCacheSize() {
		return this.cacheSize;
	}

	/**
	 * Returns the name of the cache dir. This is <em>not</em> the full
	 * path of the cache dir; it is name of the dir where resources for
	 * this servlet should be stored. The cache dir is the sub-directory
	 * of the base temp directory.
	 */
	protected String getCacheDirName() {
		return this.cacheDirName;
	}

	/**
	 * Returns the directory where cached resources are stored.
	 */
	protected File getCacheDir() {
		return this.cacheDir;
	}

	// cache management

	protected synchronized void putCacheFile(String cacheId, File file) {
		this.cache.put(cacheId, file);
	}

	protected synchronized File getCacheFile(String cacheId) {
		return this.cache.get(cacheId);
	}

	protected synchronized void clearCache(boolean deleteFiles) {
		if (deleteFiles) {
			for (File file : this.cache.values()) {
				file.delete();
				// attempt to delete compressed version of file
				File gzfile = new File(file.getParentFile(), file.getName()+EXT_COMPRESSED);
				if (gzfile.exists()) {
					gzfile.delete();
				}
			}
		}
		this.cache.clear();
	}

	protected void processRemovedFile(String cacheId, File file) {
		file.delete();
	}

	// factory

	protected void createCache() {
		this.cacheSize = DEFAULT_CACHE_SIZE;
		String value = getServletConfig().getInitParameter(P_CACHE_SIZE);
		if (value != null) {
			try {
				this.cacheSize = Integer.parseInt(value.trim());
			}
			catch (NumberFormatException e) {
				// ignore -- just use default
			}
		}
		this.cache = new LRUMap(this.cacheSize) {
			protected void processRemovedLRU(Object key, Object value) {
				String cacheId = (String)key;
				File file = (File)value;
				DiskCacheServlet.this.processRemovedFile(cacheId, file);
			}
		};
	}

	protected synchronized void createCacheDir() {
		if (this.cacheDir == null) {
			String subDirName = "latest";
			if (this.cacheDirName != null) {
				subDirName = this.cacheDirName + '/' + subDirName;
			}
			subDirName = subDirName.replace('/', File.separatorChar);

			this.cacheDir = new File(getTempDir(), subDirName);
			if (this.cacheDir.exists()) {
				Date date = new Date(this.cacheDir.lastModified());
				String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(date);
				File parentDir = this.cacheDir.getParentFile();
				File backupDir = new File(parentDir, timestamp);
				this.cacheDir.renameTo(backupDir);
				this.cacheDir = new File(getTempDir(), subDirName);
			}
			this.cacheDir.mkdirs();

			cleanupOldCacheDirs();
		}
	}

	protected void cleanupOldCacheDirs() {
		Thread thread = new Thread() {
			public void run() {
				File cacheDir = DiskCacheServlet.this.getCacheDir().getParentFile();
				File[] files = cacheDir.listFiles(new CacheDirFilter());
				for (File file : files) {
					if (file.isDirectory()) {
						delete(file);
					}
				}
			}
			public void delete(File dir) {
				File[] files = dir.listFiles();
				for (File file : files) {
					if (file.isDirectory()) {
						delete(file);
					}
					else {
						file.delete();
					}
				}
				dir.delete();
			}
		};
		thread.start();
	}

	// utility

	protected File getTempDir() {
		File tempDir = null;

		String cacheDirname = getServletConfig().getInitParameter(P_CACHE_DIR);
		if (cacheDirname != null) {
			// careful with separatorChar on Win - backslashes in second arg can confuse replaceAll()
			String webappDirname = getServletContext().getRealPath("/").replace(File.separatorChar, '/');
			cacheDirname = cacheDirname.replaceAll("\\$\\{webapp\\}", webappDirname).replace('/', File.separatorChar).trim();
			tempDir = new File(cacheDirname);
		}
		else {
			try {
				File checkFile = File.createTempFile("diskcache-",".check");
				tempDir = checkFile.getParentFile();
				checkFile.delete();
			}
			catch (IOException e) {
				return null;
			}
		}

		tempDir.mkdirs();
		return tempDir;
	}


	// file utility functions

	// TODO: Should these be moved to some ZimbraCommon utility class?

	/**
	 * Compress a file and store the content in a new file in the same
	 * location as the original file, appending ".gz" extension.
	 */
	protected void compress(File src) throws IOException {
		File dest = new File(src.getParentFile(), src.getName()+EXT_COMPRESSED);
		GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(dest));
		copy(src, out);
		out.finish();
		out.close();
	}

	/** Copy file to HTTP response. */
	protected void copy(File src, HttpServletResponse resp,
						boolean compress) throws IOException {
		if (compress) {
			try {
				resp.setHeader("Content-Encoding", "gzip");
			}
			catch (IllegalStateException e) {
				compress = false;
			}
		}
		try {
            OutputStream out = resp.getOutputStream();
			if (compress) {
				File gzSrc = new File(src.getParentFile(), src.getName()+EXT_COMPRESSED);
				if (gzSrc.exists()) {
					src = gzSrc;
					compress = false;
				}
				else {
					out = new GZIPOutputStream(out);
				}
			}
			copy(src, out);
			if (compress) {
				((GZIPOutputStream)out).finish();
			}
		}
        catch (IllegalStateException e) {
            PrintWriter out = resp.getWriter();
			copy(src, out);
		}
	}

	/** Copy string to HTTP response. */
	protected void copy(String src, HttpServletResponse resp,
						boolean compress) throws IOException {
		if (compress) {
			try {
				resp.setHeader("Content-Encoding", "gzip");
			}
			catch (IllegalStateException e) {
				compress = false;
			}
		}
		try {
            OutputStream out = resp.getOutputStream();
			if (compress) {
				out = new GZIPOutputStream(out);
			}
			out.write(src.getBytes("UTF-8"));
			if (compress) {
				((GZIPOutputStream)out).finish();
			}
		}
        catch (IllegalStateException e) {
            PrintWriter out = resp.getWriter();
			out.write(src);
		}
	}

	/** Copy bytes of file to output stream without any conversion. */
	protected void copy(File src, OutputStream dest) throws IOException {
		byte[] buffer = new byte[4096];
		int count;
		InputStream in = new FileInputStream(src);
		while ((count = in.read(buffer)) != -1) {
			dest.write(buffer, 0, count);
		}
		dest.flush();
		in.close();
	}

	/** Copy characters from file to writer. */
	protected void copy(File src, Writer dest) throws IOException {
		char[] buffer = new char[4096];
		int count;
		Reader in = new InputStreamReader(new FileInputStream(src), "UTF-8");
		while ((count = in.read(buffer)) != -1) {
			dest.write(buffer, 0, count);
		}
		dest.flush();
		in.close();
	}

	/** Copy a string to file in specified encoding. */
	protected void copy(String src, File dest) throws IOException {
		OutputStream out = new FileOutputStream(dest);
		copy(src, out);
		out.close();
	}

	/** Copy a string to output stream in specified encoding. */
	protected void copy(String src, OutputStream dest) throws IOException {
		Writer out = new OutputStreamWriter(dest, "UTF-8");
		out.write(src);
		out.flush();
	}

	/** Copy a string to writer. */
	protected void copy(String src, Writer dest) throws IOException {
		dest.write(src);
		dest.flush();
	}

	//
	// Classes
	//

	static class CacheDirFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return !name.equals("latest"); 
		}
	}

} // class DiskCacheServlet
