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

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;

/** Temporarily caches files on disk.  The cache size can be limited
 *  by the number of files or the total size. */
public class LocalBlobCache {
    private long mMaxBytes = 100 * 1024 * 1024;  // 100MB default
    private int mMaxFiles = 10 * 1024;           // 10k files default
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
        mMaxBytes = maxBytes == null ? Long.MAX_VALUE : maxBytes;
        pruneIfNecessary();
        return this;
    }

    /** Sets the limit for the total number of files in the cache.
     * @param maxFiles the limit, or <tt>null</tt> for no limit */
    public synchronized LocalBlobCache setMaxFiles(Integer maxFiles) {
        mMaxFiles = maxFiles == null ? Integer.MAX_VALUE : maxFiles;
        pruneIfNecessary();
        return this;
    }

    /** Initializes the cache and deletes any existing files.  Call this
     *  method before using the cache. */
    public synchronized void startup() throws IOException {
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

        return blob;
    }

    /** Removes the least recently accessed files from the cache and deletes
     *  them from disk so that the cache size doesn't exceed {@link #mMaxFiles}
     *  and {@link #mMaxBytes}. */
    private synchronized void pruneIfNecessary() {
        if (mKeyToBlob == null || (mNumBytes < mMaxBytes && mKeyToBlob.size() < mMaxFiles))
            return;

        StoreManager sm = StoreManager.getInstance();

        Iterator<Map.Entry<String, Blob>> iEntries = mKeyToBlob.entrySet().iterator();
        while (iEntries.hasNext() && !(mNumBytes < mMaxBytes && mKeyToBlob.size() < mMaxFiles)) {
            Map.Entry<String, Blob> entry = iEntries.next();
            String key = entry.getKey();
            Blob blob = entry.getValue();

            try {
                mNumBytes -= blob.getRawSize();
            } catch (IOException ioe) {
                continue;
            }

            // remove file
            if (ZimbraLog.store.isDebugEnabled())
                ZimbraLog.store.debug("deleting cached local blob: locator=" + key);

            if (!sm.quietDelete(blob))
                ZimbraLog.store.warn("unable to delete cached local blob " + blob.getPath());

            // remove key
            iEntries.remove();
        }
    }
}
