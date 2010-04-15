/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.util.CalculatorStream;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

/**
 * Caches uncompressed versions of compressed files on disk.  Uses the digest
 * of the file internally to dedupe multiple copies of the same data.  The cache size
 * can be limited by the number of files or the total size.
 *
 * @param <K> the type of key used to look up files in the cache 
 */
public class UncompressedFileCache<K> {

    private static final Log sLog = LogFactory.getLog(UncompressedFileCache.class);
    
    private long mMaxBytes = 100 * 1024 * 1024; // 100MB default
    private int mMaxFiles = 10 * 1024; // 10k files default
    private File mCacheDir;
    private Set<Listener> mListeners = new HashSet<Listener>();
    
    /** Maps the key to the cache to the uncompressed file digest. */
    private LinkedHashMap<K, String> mKeyToDigest;
    
    /** All the files in the cache, indexed by digest. */
    private Map<String, File> mDigestToFile;
    private long mNumBytes = 0;
    
    public UncompressedFileCache(String path) {
        if (path == null) {
            throw new NullPointerException("Path cannot be null.");
        }
        mCacheDir = new File(path);
    }
    
    public interface Listener<K> {
        /**
         * Notifies listeners that a file is going away, to allow them
         * to close any open file descriptors.
         */
        public void willPurge(K key);
    }

    /**
     * Sets the limit for the total size of all files in the cache.
     * @param maxBytes the limit, or <tt>null</tt> for no limit
     */
    public synchronized UncompressedFileCache<K> setMaxBytes(Long maxBytes) {
        if (maxBytes != null) {
            mMaxBytes = maxBytes;
        } else {
            mMaxBytes = Long.MAX_VALUE;
        }
        pruneIfNecessary();
        return this;
    }
    
    /**
     * Sets the limit for the total number of files in the cache.
     * @param maxFiles the limit, or <tt>null</tt> for no limit
     */
    public synchronized UncompressedFileCache<K> setMaxFiles(Integer maxFiles) {
        if (maxFiles != null) {
            mMaxFiles = maxFiles;
        } else {
            mMaxFiles = Integer.MAX_VALUE;
        }
        pruneIfNecessary();
        return this;
    }

    /**
     * Initializes the cache and deletes any existing files.  Call this method before
     * using the cache.
     */
    public synchronized UncompressedFileCache<K> startup()
    throws IOException {
        if (!mCacheDir.exists())
            throw new IOException("uncompressed file cache folder does not exist: " + mCacheDir);
        if (!mCacheDir.isDirectory())
            throw new IOException("uncompressed file cache folder is not a directory: " + mCacheDir);
        
        // Create the file cache with default LinkedHashMap values, but sorted by last access time.
        mKeyToDigest = new LinkedHashMap<K, String>(16, 0.75f, true);
        mDigestToFile = new HashMap<String, File>();
        
        // Clear out the cache on disk.
        for (File file : mCacheDir.listFiles()) {
            sLog.debug("Deleting %s.", file.getPath());
            if (!file.delete())
                ZimbraLog.store.warn("unable to delete " + file.getPath() + " from uncompressed file cache");
        }

        return this;
    }
    
    public synchronized void addListener(Listener l) {
        mListeners.add(l);
    }

    private class UncompressedFile {
        String digest;
        File file;
        UncompressedFile()  { }
    }
    
    /**
     * Returns the uncompressed version of the given file.  If the uncompressed
     * file is not in the cache, uncompresses it and adds it to the cache.
     * 
     * @param key the key used to look up the uncompressed data
     * @param compressedFile the compressed file.  This file is read, if necessary,
     * to write the uncompressed file.
     * @param sync <tt>true</tt> to use fsync
     */
    public SharedFile get(K key, File compressedFile, boolean sync)
    throws IOException {
        File uncompressedFile = null;
        
        sLog.debug("Looking up SharedFile for key %s, path %s.", key, compressedFile.getPath());
        
        synchronized (this) {
            String digest = mKeyToDigest.get(key);
            if (digest != null) {
                uncompressedFile = mDigestToFile.get(digest);
                if (uncompressedFile != null) {
                    sLog.debug("Found existing uncompressed file.  Returning new SharedFile.");
                    return new SharedFile(uncompressedFile);
                }
            }
        }

        // Uncompress the file outside of the synchronized block.
        UncompressedFile temp = uncompressToTempFile(compressedFile, sync);
        SharedFile shared = null;
        
        synchronized (this) {
            uncompressedFile = mDigestToFile.get(temp.digest);
            
            if (uncompressedFile != null) {
                sLog.debug("Found existing uncompressed file for digest %s.  Deleting %s.", temp.digest, temp.file.getPath());
                mKeyToDigest.put(key, temp.digest);
                try {
                    FileUtil.delete(temp.file);
                } catch (Exception e) {
                }
                shared = new SharedFile(uncompressedFile);
            } else {
                uncompressedFile = new File(mCacheDir, temp.digest);
                FileUtil.rename(temp.file, uncompressedFile);
                shared = new SharedFile(uncompressedFile); // Opens the file implicitly.
                put(key, temp.digest, uncompressedFile);
            }
        }
        
        return shared;
    }
    
    private UncompressedFile uncompressToTempFile(File compressedFile, boolean sync)
    throws IOException {
        // Write the uncompressed file and calculate the digest.
        CalculatorStream calc = new CalculatorStream(new GZIPInputStream(new FileInputStream(compressedFile)));
        File tempFile = File.createTempFile(UncompressedFileCache.class.getSimpleName(), null);
        FileUtil.uncompress(calc, tempFile,  sync);
        String digest = calc.getDigest();
        sLog.debug("Uncompressed %s to %s, digest=%s.", compressedFile.getPath(), tempFile.getPath(), digest);
        
        UncompressedFile result = new UncompressedFile();
        result.file = tempFile;
        result.digest = digest;
        return result;
    }
    
    private synchronized void put(K key, String digest, File file) {
        long fileSize = file.length();
        sLog.debug("Adding file to the uncompressed cache: key=%s, size=%d, path=%s.",
            key, fileSize, file.getPath());
        mKeyToDigest.put(key, digest);
        mDigestToFile.put(digest, file);
        mNumBytes += fileSize;
        pruneIfNecessary();
    }

    synchronized void remove(K key) {
        String digest = mKeyToDigest.remove(key);
        if (digest != null)
            mDigestToFile.remove(digest);
    }

    /**
     * Removes the least recently accessed files from the cache and deletes them
     * from disk so that the cache size doesn't exceed {@link #mMaxFiles} and
     * {@link #mMaxBytes}.
     */
    private synchronized void pruneIfNecessary() {
        if (mKeyToDigest == null || (mNumBytes < mMaxBytes && mDigestToFile.size() < mMaxFiles)) {
            return;
        }
        
        Iterator<Map.Entry<K, String>> iEntries = mKeyToDigest.entrySet().iterator();
        
        while (iEntries.hasNext()) {
            // Get key.
            Map.Entry<K, String> entry = iEntries.next();
            K key = entry.getKey();
            String digest = entry.getValue();
            
            // Notify listeners.
            for (Listener<K> listener : mListeners) {
                listener.willPurge(key);
            }

            // Remove key and file.
            iEntries.remove();
            File file = mDigestToFile.remove(digest);
            if (file != null) {
                sLog.debug("Deleting %s: key=%s, digest=%s.", file.getPath(), key, digest);
                mNumBytes -= file.length();
                try {
                    FileUtil.delete(file);
                } catch (Exception e) {
                }
            }
            
            if (mNumBytes < mMaxBytes && mDigestToFile.size() < mMaxFiles) {
                break;
            }
        }
    }
}
