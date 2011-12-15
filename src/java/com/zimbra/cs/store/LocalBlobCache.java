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
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MessageCache;

/** Temporarily caches files on disk.  The cache size can be limited
 *  by the number of files or the total size. */
public class LocalBlobCache {
    private static final Log log = LogFactory.getLog(LocalBlobCache.class);

    private long mMaxBytes = LC.http_store_local_cache_max_bytes.longValue();
    private int mMaxFiles = LC.http_store_local_cache_max_files.intValue();
    private File mCacheDir;

    /** Maps the key to the cache to the corresponding file. */
    private LinkedHashMap<String, Blob> mKeyToBlob;
    private long mNumBytes = 0;

    public LocalBlobCache(String path) {
        if (path == null)
            throw new IllegalArgumentException("path cannot be null");
        mCacheDir = new File(path);
    }

    /** Sets the limit for the total size of all files in the cache.
     * @param maxBytes the limit, or <tt>null</tt> for no limit */
    public synchronized LocalBlobCache setMaxBytes(Long maxBytes) {
        log.info("Setting max bytes to " + maxBytes);
        mMaxBytes = maxBytes == null ? Long.MAX_VALUE : maxBytes;
        pruneIfNecessary();
        return this;
    }

    /** Sets the limit for the total number of files in the cache.
     * @param maxFiles the limit, or <tt>null</tt> for no limit */
    public synchronized LocalBlobCache setMaxFiles(Integer maxFiles) {
        log.info("Setting max files to " + maxFiles);
        mMaxFiles = maxFiles == null ? Integer.MAX_VALUE : maxFiles;
        pruneIfNecessary();
        return this;
    }

    public synchronized int getNumFiles() {
        return mKeyToBlob.size();
    }

    public synchronized long getNumBytes() {
        return mNumBytes;
    }

    /** Initializes the cache and deletes any existing files.  Call this
     *  method before using the cache. */
    public synchronized void startup() throws IOException {
        log.info("Starting up cache at %s: maxFiles=%d, maxBytes=%d", mCacheDir, mMaxFiles, mMaxBytes);
        FileUtil.mkdirs(mCacheDir);
        if (!mCacheDir.exists())
            throw new IOException("local blob cache folder does not exist: " + mCacheDir);
        if (!mCacheDir.isDirectory())
            throw new IOException("local blob cache folder is not a directory: " + mCacheDir);

        // create the file cache with default LinkedHashMap values, but sorted by last access time
        mKeyToBlob = new LinkedHashMap<String, Blob>(16, 0.75f, true);

        // clear out the stale cache entries on disk
        for (File file : mCacheDir.listFiles()) {
            ZimbraLog.store.debug("deleting stale cached file " + file.getPath());
            if (!file.delete())
                ZimbraLog.store.warn("unable to delete stale cached file: " + file.getPath());
        }
    }

    public synchronized Blob get(String key) {
        return mKeyToBlob.get(key);
    }

    public synchronized Blob cache(String key, Blob blob) throws IOException {
        // Prune first, so that we don't delete the blob we're adding.
        // Large blobs won't be removed until the MimeMessage has been
        // aged out of the MessageCache, even if the size of this cache
        // has been exceeded.
        pruneIfNecessary();

        Blob found = mKeyToBlob.get(key);
        if (found != null)
            return found;

        long size = blob.getRawSize();
        // Set the filename to a hash of the key, to make sure that it's
        // filesystem-safe.
        String filename = ByteUtil.getSHA1Digest(key.getBytes(), true);
        blob.renameTo(mCacheDir + File.separator + filename);
        mKeyToBlob.put(key, blob);
        mNumBytes += size;
        log.debug("Cached %s: key=%s, %d blobs, %d bytes.", blob, key, mKeyToBlob.size(), mNumBytes);
        return blob;
    }

    /** Removes the least recently accessed files from the cache and deletes
     *  them from disk so that the cache size doesn't exceed {@link #mMaxFiles}
     *  and {@link #mMaxBytes}. */
    private synchronized void pruneIfNecessary() {
        if (mKeyToBlob == null || (mNumBytes <= mMaxBytes && mKeyToBlob.size() <= mMaxFiles))
            return;

        StoreManager sm = StoreManager.getInstance();

        Iterator<Map.Entry<String, Blob>> iEntries = mKeyToBlob.entrySet().iterator();
        while (iEntries.hasNext() && !(mNumBytes <= mMaxBytes && mKeyToBlob.size() <= mMaxFiles)) {
            Map.Entry<String, Blob> entry = iEntries.next();
            String key = entry.getKey();
            Blob blob = entry.getValue();

            String digest;
            try {
                mNumBytes -= blob.getRawSize();
                digest = blob.getDigest();
            } catch (IOException ioe) {
                log.warn("Unable to remove %s.", blob, ioe);
                continue;
            }

            if (!MessageCache.contains(digest)) {
                log.debug("Deleting %s: key=%s, %d blobs, %d bytes.", blob, key, mKeyToBlob.size(), mNumBytes);
                if (!sm.quietDelete(blob)) {
                    log.warn("Unable to delete %s.", blob);
                }
                // remove key
                iEntries.remove();
            } else {
                log.debug("Not deleting %s because it's being referenced by the message cache.", blob);
            }

        }
    }
}
