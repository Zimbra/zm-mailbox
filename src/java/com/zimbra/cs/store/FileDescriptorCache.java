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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileCache;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * Caches file descriptors to blobs in the mail store.  If the blob is compressed,
 * uses a {@link FileCache} to access the uncompressed data.  Cache entries
 * that reference uncompressed blobs keep the file descriptor open until {@link #remove}
 * is called or the cache entry is aged out.
 */
public class FileDescriptorCache
{
    private static final Log sLog = LogFactory.getLog(FileDescriptorCache.class);

    // Create the file cache with default LinkedHashMap values, but sorted by last access time.
    private LinkedHashMap<String, SharedFile> mCache = new LinkedHashMap<String, SharedFile>(16, 0.75f, true);
    // Create a concurrent list for the SharedFies for which the mapping has been removed but is still in use by some threads.
    private List<SharedFileInfo> mInactiveCache = Collections.synchronizedList(new ArrayList<SharedFileInfo>());
    private int mMaxSize = 1000;
    private FileCache<String> mUncompressedFileCache;
    private Counter mHitRate = new Counter();
    
    private class SharedFileInfo {
        public String path;
        public SharedFile file;
        
        public SharedFileInfo(String path, SharedFile file) {
            this.path = path;
            this.file = file;
        }
    }

    public FileDescriptorCache(FileCache<String> uncompressedCache) {
        mUncompressedFileCache = uncompressedCache;
    }

    public synchronized FileDescriptorCache setMaxSize(int maxSize) {
        if (maxSize < 0)
            throw new IllegalArgumentException("maxSize value of " + maxSize + " is invalid (must be at least 0)");

        mMaxSize = maxSize;
        mHitRate.reset(); // Recalculate hit rate based on the new size.
        pruneIfNecessary();
        return this;
    }

    public FileDescriptorCache loadSettings() throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        int fileDescriptorCacheSize = server.getMailFileDescriptorCacheSize();

        sLog.info("Loading settings: %s=%d.",
            Provisioning.A_zimbraMailFileDescriptorCacheSize, fileDescriptorCacheSize);

        setMaxSize(fileDescriptorCacheSize);

        return this;
    }

    /**
     * Closes all file descriptors, clears the cache, and removes any files from
     * the uncompressed cache.
     */
    public synchronized void shutdown() {
        Iterator<Map.Entry<String, SharedFile>> iEntries = mCache.entrySet().iterator();
        while (iEntries.hasNext()) {
            Map.Entry<String, SharedFile> entry = iEntries.next();
            String path = entry.getKey();
            SharedFile file = entry.getValue();
            iEntries.remove();
            try {
                boolean success = close(file, path);
                if (!success)
                    sLog.warn("Unable to close %s. File is in use.", file);
            } catch (IOException e) {
                sLog.warn("Unable to close %s", file, e);
            }
        }
    }

    /**
     * Reads from the specified file.
     */
    public int read(String path, long rawSize, long fileOffset, byte[] buf, int bufferOffset, int len)
    throws IOException {
        sLog.debug("Reading %s.  rawSize=%d, fileOffset=%d, bufferOffset=%d, len=%d.", path, rawSize, fileOffset, bufferOffset, len);
        SharedFile file = null;
        int numRead;

        try {
            file = getSharedFile(path, rawSize);
            numRead = file.read(fileOffset, buf, bufferOffset, len);
        } finally {
            if (file != null) {
               file.doneReading();
            }
        }

        return numRead;
    }

    /**
     * Returns the existing cache entry or creates a new one.  Implicitly
     * increments the number of readers for the <tt>SharedFile</tt>.
     */
    private SharedFile getSharedFile(String path, long rawSize)
    throws IOException {
        SharedFile sharedFile = null;

        synchronized (this) {
            sharedFile = mCache.get(path);
        }
        if (sharedFile != null) {
            sLog.debug("Found existing file descriptor for %s, rawSize=%d.", path, rawSize);
            sharedFile.aboutToRead();
            mHitRate.increment(100);
            return sharedFile;
        }

        // Open a new file descriptor.
        mHitRate.increment(0);
        File file = new File(path);

        if (file.length() != rawSize && FileUtil.isGzipped(file)) {
            sLog.debug("Adding file descriptor cache entry for %s from the uncompressed file cache.", path);
            FileCache.Item uncompressed = mUncompressedFileCache.get(path);
            if (uncompressed == null) {
                InputStream in = null;
                try {
                    in = new GZIPInputStream(new FileInputStream(file));
                    mUncompressedFileCache.put(path, in);
                } finally {
                    ByteUtil.closeStream(in);
                }
                uncompressed = mUncompressedFileCache.get(path);
                if (uncompressed == null) {
                    // Should not happen, since the uncompressed file is guaranteed to
                    // be in the cache for at least a minute.
                    throw new IOException("Unable to get uncompressed file for " + path);
                }
                sharedFile = new SharedFile(uncompressed.file);
            }
        } else {
            sLog.debug("Opening new file descriptor for %s.", path);
            sharedFile = new SharedFile(file);
        }

        synchronized (this) {
            if (mCache.containsKey(path)) {
                sLog.debug("Another thread just opened the same file.  Closing our copy and returning the other one.");
                sharedFile.close();
                sharedFile = mCache.get(path);
            } else {
                sLog.debug("Caching file descriptor: path=%s, sharedFile=%s", path, sharedFile);
                mCache.put(path, sharedFile);
            }
        }
        sharedFile.aboutToRead();
        pruneIfNecessary();

        return sharedFile;
    }

    /**
     * Closes the file descriptor and removes it from the cache.  Does nothing if the file
     * descriptor is not in the cache.
     */
    public void remove(String path)
    throws IOException {
        SharedFile file = null;

        synchronized (this) {
            file = mCache.remove(path);
        }

        if (file != null) {
            boolean success = close(file, path);
            if (!success)
                mInactiveCache.add(new SharedFileInfo(path, file));
        } else {
            sLog.debug("Attempted to remove %s but could not find it in the cache.", path);
        }
        
        // Close if there are any SharedFiles in the inactive cache.
        quietCloseInactiveCache();
    }

    /**
     * Close the file if it is not in use.
     * @return true if the file is closed, false otherwise.
     * @throws IOException if there is an error closing the file.
     */
    private boolean close(SharedFile file, String path)
    throws IOException {
        if (file != null) {
            sLog.debug("Closing file descriptor for %s, %s", path, file);

            if (file.getNumReaders() == 0) {
                file.close();
                synchronized (this) {
                    if (!mCache.containsKey(path)) {
                        mUncompressedFileCache.remove(path);
                    } else {
                        sLog.debug("Not removing %s from the uncompressed cache.  Another thread reopened it.");
                    }
                }
                return true;
            }
            return false;
        }
        return true;
    }
    
    private void quietCloseInactiveCache() {
        synchronized (mInactiveCache) {
            Iterator<SharedFileInfo> iter = mInactiveCache.iterator();
            while (iter.hasNext()) {
                SharedFileInfo info = iter.next();
                try {
                    boolean success = close(info.file, info.path);
                    if (success)
                        iter.remove();
                } catch (IOException e) {
                    ZimbraLog.store.warn("Unable to close file descriptor for " + info.path, e);
                    iter.remove();
                }
            }
        }
    }

    public synchronized int getSize() {
        return mCache.size();
    }

    public double getHitRate() {
        return mHitRate.getAverage();
    }

    private void pruneIfNecessary() {
        if (getSize() <= mMaxSize)
            return;

        List<Map.Entry<String, SharedFile>> removeList = new ArrayList<Map.Entry<String, SharedFile>>();
        
        synchronized (this) {
            Iterator<Map.Entry<String, SharedFile>> iEntries = mCache.entrySet().iterator();
            while (iEntries.hasNext() && mCache.size() > mMaxSize) {
                Map.Entry<String, SharedFile> mapEntry = iEntries.next();
                iEntries.remove();
                removeList.add(mapEntry);
            }
        }
        for (Map.Entry<String, SharedFile> mapEntry : removeList) {
            String path = mapEntry.getKey();
            SharedFile file = mapEntry.getValue();
            try {
                boolean success = close(file, path);
                if (!success)
                    mInactiveCache.add(new SharedFileInfo(path, file));
            } catch (IOException e) {
                ZimbraLog.store.warn("Unable to close file descriptor for " + path, e);
            }
        }

        // Close if there are any SharedFiles in the inactive cache.
        quietCloseInactiveCache();
    }
}
