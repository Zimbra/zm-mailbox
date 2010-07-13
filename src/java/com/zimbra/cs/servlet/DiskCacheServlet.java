/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.admin.FlushCache;

/**
 * Base class for servlets that cache resources to disk.
 */
@SuppressWarnings("deprecation")
public abstract class DiskCacheServlet extends ZimbraServlet {
    private Map<String,File> cache;
    private String cacheDirName;
    private File cacheDir;
    private int cacheSize;

    protected static final String EXT_COMPRESSED = ".gz";
    protected static final String P_CACHE_DIR = "resource-cache-dir";
    protected static final String P_CACHE_SIZE = "resource-cache-size";

    protected DiskCacheServlet(String cacheDirName) {
        this.cacheDirName = cacheDirName;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        createCache();
        createCacheDir();
    }

    public void service(ServletRequest req, ServletResponse resp) throws
        IOException, ServletException {
        if (flushCache(req))
            return;
        super.service(req, resp);
    }

    protected boolean flushCache(ServletRequest req) {
        Boolean flushCache = (Boolean)req.getAttribute(FlushCache.FLUSH_CACHE);
        if (flushCache != null && flushCache.booleanValue()) {
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("flushing "+getClass().getName()+" cache");
            }
            boolean deleteFiles = true; // TODO: Should we skip the delete?
            clearCache(deleteFiles);
            return true;
        }
        return false;
    }

    // properties

    protected int getCacheSize() {
        return cacheSize;
    }

    /**
     * Returns the name of the cache dir. This is <em>not</em> the full
     * path of the cache dir; it is name of the dir where resources for
     * this servlet should be stored. The cache dir is the sub-directory
     * of the base temp directory.
     */
    protected String getCacheDirName() {
        return cacheDirName;
    }

    /**
     * Returns the directory where cached resources are stored.
     */
    protected File getCacheDir() {
        return cacheDir;
    }

    /**
     * Returns the key used to create a file from a cacheId
     */
    protected String getCacheKey(String cacheId) {
        return ByteUtil.getMD5Digest(cacheId.getBytes(), false);
    }
    
    // cache management

    protected File createCacheFile(String cacheId) throws IOException {
        return createCacheFile(cacheId, null);
    }
    
    protected File createCacheFile(String cacheId, String ext) throws IOException {
        String cacheKey = getCacheKey(cacheId);
        
        return new File(cacheDir.getAbsolutePath() + '/' + cacheKey + '-' +
            Thread.currentThread().getId() + '.' + (ext == null ? "tmp" : ext));
    }

    protected synchronized File getCacheFile(String cacheId) {
        return cache.get(getCacheKey(cacheId));
    }

    protected synchronized void putCacheFile(String cacheId, File file) {
        String cacheKey = getCacheKey(cacheId);
        File oldFile = cache.get(cacheKey);

        if (oldFile != null)
            oldFile.delete();
        cache.put(cacheKey, file);
    }

    protected synchronized void clearCache(boolean deleteFiles) {
        if (deleteFiles) {
            for (File file : cache.values()) {
                file.delete();
                // attempt to delete compressed version of file
                File gzfile = new File(file.getParentFile(), file.getName()+EXT_COMPRESSED);
                if (gzfile.exists()) {
                    gzfile.delete();
                }
            }
        }
        cache.clear();
    }

    protected void processRemovedFile(String cacheId, File file) {
        file.delete();
    }

    // factory

    @SuppressWarnings({ "serial" })
    protected void createCache() {
        cacheSize = LC.zimbra_disk_cache_servlet_size.intValue();
        String value = getServletConfig().getInitParameter(P_CACHE_SIZE);
        if (value != null) {
            try {
                cacheSize = Integer.parseInt(value.trim());
            }
            catch (NumberFormatException e) {
                // ignore -- just use default
            }
        }
        cache = new LruMap<String, File>(cacheSize) {
            protected void willRemove(String cacheId, File file) {
                processRemovedFile(cacheId, file);
            }
        };
    }

    protected synchronized void createCacheDir() {
        if (cacheDir == null) {
            String subDirName = "latest";

            if (cacheDirName != null) {
                subDirName = cacheDirName + '/' + subDirName;
            }
            subDirName = subDirName.replace('/', File.separatorChar);
            cacheDir = new File(getTempDir(), subDirName);
            if (cacheDir.exists()) {
                if (LC.zimbra_disk_cache_servlet_flush.booleanValue()) {
                    Date date = new Date(cacheDir.lastModified());
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(date);
                    File parentDir = cacheDir.getParentFile();
                    File backupDir = new File(parentDir, timestamp);
                    
                    cacheDir.renameTo(backupDir);
                    cacheDir = new File(getTempDir(), subDirName);
                } else {
                    for (String file : cacheDir.list()) {
                        int idx = file.indexOf("-");
                        
                        if (idx != -1 && !file.endsWith(EXT_COMPRESSED)) {
                            String cacheKey = file.substring(0, idx);
                            
                            cache.put(cacheKey, new File(cacheDir.getAbsolutePath() +
                                '/' + file));
                        }
                    }
                }
            }
            cacheDir.mkdirs();
            cleanupOldCacheDirs();
        }
    }

    protected void cleanupOldCacheDirs() {
        Thread thread = new Thread() {
            public void run() {
                File dir = getCacheDir().getParentFile();
                File[] files = dir.listFiles(new CacheDirFilter());
                
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
            String webappDirname = getServletContext().getRealPath("/").replace(
                File.separatorChar, '/');
            cacheDirname = cacheDirname.replaceAll("\\$\\{webapp\\}",
                webappDirname).replace('/', File.separatorChar).trim();
            tempDir = new File(cacheDirname);
        } else {
            tempDir = new File(LC.zimbra_tmp_directory.value() + "/diskcache");
        }
        tempDir.mkdirs();
        return tempDir;
    }


    // file utility functions

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
    protected void copy(File src, HttpServletResponse resp, boolean compress)
        throws IOException {
        if (compress) {
            try {
                resp.setHeader("Content-Encoding", "gzip");
            } catch (IllegalStateException e) {
                compress = false;
            }
        }
        try {
            OutputStream out = resp.getOutputStream();
            if (compress) {
                File gzSrc = new File(src.getParentFile(), src.getName() +
                    EXT_COMPRESSED);
                if (gzSrc.exists()) {
                    src = gzSrc;
                    compress = false;
                } else {
                    out = new GZIPOutputStream(out);
                }
            }
            copy(src, out);
            if (compress) {
                ((GZIPOutputStream)out).finish();
            }
        } catch (IllegalStateException e) {
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
            } catch (IllegalStateException e) {
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
        } catch (IllegalStateException e) {
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

    static class CacheDirFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return !name.equals("latest"); 
        }
    }

}
