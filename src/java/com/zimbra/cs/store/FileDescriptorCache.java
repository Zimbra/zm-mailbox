/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.localconfig.DebugConfig;

/**
 * Caches file descriptors to blobs in the mail store.  If the blob is compressed,
 * uses {@link UncompressedFileCache} to access the uncompressed data.  Cache entries
 * that reference uncompressed blobs keep the file descriptor open until {@link #close}
 * is called or the cache entry is aged out.
 */
public class FileDescriptorCache
{
    // Create the file cache with default LinkedHashMap values, but sorted by last access time.
    private LinkedHashMap<String, SharedFile> mCache = new LinkedHashMap<String, SharedFile>(16, 0.75f, true);
    private int mMaxSize = 1000;
    private UncompressedFileCache<String> mUncompressedFileCache;

    public FileDescriptorCache(UncompressedFileCache<String> uncompressedCache) {
        mUncompressedFileCache = uncompressedCache;
    }
    
    public synchronized FileDescriptorCache setMaxSize(int maxSize) {
        if (maxSize < 0) {
            String msg = String.format("maxSize value of %d is invalid.  Must be at least 0.", maxSize);
            throw new IllegalArgumentException(msg);
        }
        mMaxSize = maxSize;
        pruneIfNecessary();
        return this;
    }
    
    UncompressedFileCache<String> getUncompressedFileCache() {
        return mUncompressedFileCache;
    }

    /**
     * Reads one byte from the specified file.
     */
    public int read(String path, long fileOffset)
    throws IOException {
        SharedFile file = getSharedFile(path);
        int retVal = file.read(fileOffset);
        closeIfPruned(path, file);
        return retVal;
    }
    
    /**
     * Reads from the specified file.
     */
    public int read(String path, long fileOffset, byte[] buf, int bufferOffset, int len)
    throws IOException {
        SharedFile file = getSharedFile(path);
        int numRead = file.read(fileOffset, buf, bufferOffset, len);
        closeIfPruned(path, file);
        return numRead;
    }
    
    private synchronized void closeIfPruned(String path, SharedFile file)
    throws IOException {
        if (!mCache.containsKey(path)) {
            // Another thread pruned this file from the cache.
            file.close();
        }
    }
    
    /**
     * Returns the existing cache entry or creates a new one.
     */
    private SharedFile getSharedFile(String path)
    throws IOException {
        SharedFile sharedFile = null;
        synchronized (this) {
            sharedFile = mCache.get(path);
        }
        
        if (sharedFile == null) {
            File file = new File(path);
            if (FileUtil.isGzipped(file)) {
                ZimbraLog.store.debug("Adding file descriptor cache entry for %s from the uncompressed file cache.", path);
                sharedFile = mUncompressedFileCache.get(path, new File(path), !DebugConfig.disableMessageStoreFsync);
            } else {
                ZimbraLog.store.debug("Opening new file descriptor for %s.", path);
                sharedFile = new SharedFile(file);
            }
            synchronized (this) {
                mCache.put(path, sharedFile);
            }
            pruneIfNecessary();
        }
        
        return sharedFile;
    }
    
    public long getLength(String path)
    throws IOException {
        SharedFile file = getSharedFile(path);
        long length = file.getLength();
        closeIfPruned(path, file);
        return length;
    }
    
    /**
     * Closes the file descriptor to the given file.  Does nothing if the file
     * descriptor is not in the cache.
     */
    public synchronized void close(String path) {
        SharedFile file = mCache.remove(path); 
        
        if (file != null) {
            ZimbraLog.store.debug("Closing file descriptor for %s.", path);
            try {
                file.close();
            } catch (IOException e) {
                ZimbraLog.store.warn("Unable to close file descriptor for %s.", path, e);
            }
        }
    }
    
    private synchronized void pruneIfNecessary() {
        if (mCache.size() <= mMaxSize) {
            return;
        }
        Iterator<Map.Entry<String, SharedFile>> iEntries = mCache.entrySet().iterator();
        while (iEntries.hasNext() && mCache.size() > mMaxSize) {
            Map.Entry<String, SharedFile> mapEntry = iEntries.next();
            String path = mapEntry.getKey();
            SharedFile file = mapEntry.getValue();
            iEntries.remove();
            
            try {
                ZimbraLog.store.debug("Closing file descriptor for %s.", path);
                file.close();
            } catch (IOException e) {
                ZimbraLog.store.warn("Unable to close file descriptor for %s.", path, e);
            }
        }
    }
}
