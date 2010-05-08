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
    
    private File mCacheDir;
    
    /** Maps the key to the cache to the uncompressed file digest. */
    private Map<K, String> mKeyToDigest;
    
    /** Reverse map of the digest to all the keys that reference it. */
    private Map<String, Set<K>> mDigestToKeys;
    
    /** All the files in the cache, indexed by digest. */
    private LinkedHashMap<String, File> mDigestToFile;
    private long mNumBytes = 0;
    
    public UncompressedFileCache(String path) {
        if (path == null) {
            throw new NullPointerException("Path cannot be null.");
        }
        mCacheDir = new File(path);
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
        
        // Create caches with default LinkedHashMap values, but sorted by last access time.
        mKeyToDigest = new HashMap<K, String>();
        mDigestToKeys = new HashMap<String, Set<K>>();
        mDigestToFile = new LinkedHashMap<String, File>(16, 0.75f, true);

        for (File file : mCacheDir.listFiles()) {
            sLog.debug("Deleting %s.", file.getPath());
            if (!file.delete())
                ZimbraLog.store.warn("unable to delete " + file.getPath() + " from uncompressed file cache");
        }
        return this;
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
            sLog.debug("Digest for %s is %s", key, digest);
            if (digest != null) {
                uncompressedFile = mDigestToFile.get(digest);
                if (uncompressedFile != null) {
                    sLog.debug("Found existing uncompressed file.  Returning new SharedFile.");
                    return new SharedFile(uncompressedFile);
                } else {
                    sLog.debug("No existing uncompressed file.");
                }
            }
        }

        // Uncompress the file outside of the synchronized block.
        UncompressedFile temp = uncompressToTempFile(compressedFile, sync);
        SharedFile shared = null;
        
        synchronized (this) {
            uncompressedFile = mDigestToFile.get(temp.digest);
            
            if (uncompressedFile != null) {
                // Another thread uncompressed the same file at the same time.
                sLog.debug("Found existing uncompressed file.  Deleting %s.", temp.file);
                mapKeyToDigest(key, temp.digest);
                FileUtil.delete(temp.file);
                shared = new SharedFile(uncompressedFile);
            } else {
                uncompressedFile = new File(mCacheDir, temp.digest);
                sLog.debug("Renaming %s to %s.", temp.file, uncompressedFile);
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
    
    /**
     * Creates a record of a new uncompressed file in the cache data structures.
     */
    private synchronized void put(K key, String digest, File file) {
        long fileSize = file.length();
        mapKeyToDigest(key, digest);
        mDigestToFile.put(digest, file);
        mNumBytes += fileSize;
        sLog.debug("Added file: key=%s, size=%d, path=%s.  Cache size=%d, numBytes=%d.",
            key, fileSize, file.getPath(), mDigestToFile.size(), mNumBytes);
    }

    private synchronized void mapKeyToDigest(K key, String digest) {
        mKeyToDigest.put(key, digest);
        Set<K> keys = mDigestToKeys.get(digest);
        if (keys == null) {
            keys = new HashSet<K>();
            mDigestToKeys.put(digest, keys);
        }
        keys.add(key);
    }
    
    public synchronized void remove(K key) {
        String digest = mKeyToDigest.remove(key);
        sLog.debug("Removing %s, digest=%s", key, digest);
        
        if (digest != null) {
            Set<K> keys = mDigestToKeys.get(digest);
            if (keys != null) {
                keys.remove(key);
            }
            
            if (keys == null || keys.isEmpty()) {
                File file = mDigestToFile.remove(digest);
                sLog.debug("Deleting unreferenced file %s.", file);
                if (file != null) {
                    try {
                        FileUtil.delete(file);
                    } catch (Exception e) { // IOException and SecurityException
                        ZimbraLog.store.warn("Unable to remove a file from the uncompressed cache.", e);
                    }
                }
            } else {
                sLog.debug("Not deleting %s.  It is referenced by %s.", digest, keys);
            }
        }
    }
}
