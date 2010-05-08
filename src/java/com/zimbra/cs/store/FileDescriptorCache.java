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
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.localconfig.DebugConfig;

/**
 * Caches file descriptors to blobs in the mail store.  If the blob is compressed,
 * uses {@link UncompressedFileCache} to access the uncompressed data.  Cache entries
 * that reference uncompressed blobs keep the file descriptor open until {@link #remove}
 * is called or the cache entry is aged out.
 */
public class FileDescriptorCache
{
    private static final Log sLog = LogFactory.getLog(FileDescriptorCache.class);

    // Create the file cache with default LinkedHashMap values, but sorted by last access time.
    private LinkedHashMap<String, SharedFile> mCache = new LinkedHashMap<String, SharedFile>(16, 0.75f, true);
    private int mMaxSize = 1000;
    private UncompressedFileCache<String> mUncompressedFileCache;
    private Counter mHitRate = new Counter();

    public FileDescriptorCache(UncompressedFileCache<String> uncompressedCache) {
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
        int uncompressedMaxFiles = server.getMailUncompressedCacheMaxFiles();
        long uncompressedMaxBytes = server.getMailUncompressedCacheMaxBytes();
        int fileDescriptorCacheSize = server.getMailFileDescriptorCacheSize();
    
        sLog.info("Loading settings: %s=%d, %s=%d, %s=%d.",
                Provisioning.A_zimbraMailUncompressedCacheMaxFiles, uncompressedMaxFiles,
                Provisioning.A_zimbraMailUncompressedCacheMaxBytes, uncompressedMaxBytes,
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
                close(file, path);
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
            if (sharedFile != null) {
                sLog.debug("Found existing file descriptor for %s, rawSize=%d.", path, rawSize);
                sharedFile.aboutToRead();
                mHitRate.increment(100);
                return sharedFile;
            }
        }

        // Open a new file descriptor.
        mHitRate.increment(0);
        File file = new File(path);
        
        if (file.length() != rawSize && FileUtil.isGzipped(file)) {
            sLog.debug("Adding file descriptor cache entry for %s from the uncompressed file cache.", path);
            sharedFile = mUncompressedFileCache.get(path, file, !DebugConfig.disableMessageStoreFsync);
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
            sharedFile.aboutToRead();
        }
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
            close(file, path);
        } else {
            sLog.debug("Attempted to remove %s but could not find it in the cache.", path);
        }
    }

    /**
     * Waits for all threads to finish reading from the given <tt>SharedFile</tt>
     * and closes it.
     * @throws IOException if the operation times out waiting for readers to finish
     */
    private void close(SharedFile file, String path)
    throws IOException {
        if (file != null) {
            sLog.debug("Closing file descriptor for %s, %s", path, file);
            
            // Loop until other threads are done reading.
            for (int i = 1; i <= 20; i++) {
                int numReaders = file.getNumReaders();
                if (numReaders == 0) {
                    file.close();

                    synchronized (this) {
                        if (!mCache.containsKey(path)) {
                            mUncompressedFileCache.remove(path);
                        } else {
                            sLog.debug("Not removing %s from the uncompressed cache.  Another thread reopened it.");
                        }
                    }

                    return;
                } else {
                    sLog.debug("numReaders=%d.  Sleeping.", numReaders);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
            }
            throw new IOException("FileDescriptorCache.close() timed out waiting for " + file);

        }
    }
    
    public synchronized int getSize() {
        return mCache.size();
    }
    
    public double getHitRate() {
        return mHitRate.getAverage();
    }
    
    private synchronized void pruneIfNecessary() {
        if (mCache.size() <= mMaxSize)
            return;

        Iterator<Map.Entry<String, SharedFile>> iEntries = mCache.entrySet().iterator();
        while (iEntries.hasNext() && mCache.size() > mMaxSize) {
            Map.Entry<String, SharedFile> mapEntry = iEntries.next();
            String path = mapEntry.getKey();
            SharedFile file = mapEntry.getValue();
            if (file.getNumReaders() == 0) {
                iEntries.remove();
                try {
                    close(file, path);
                } catch (IOException e) {
                    ZimbraLog.store.warn("Unable to close file descriptor for " + path, e);
                }
            } else {
                sLog.debug("Not pruning %s because another thread is reading from it.", path);
            }
        }
    }
}
