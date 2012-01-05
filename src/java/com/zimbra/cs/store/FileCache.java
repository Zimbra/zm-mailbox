/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CalculatorStream;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

public class FileCache<K> {

    public static final Log log = LogFactory.getLog(FileCache.class);

    private final File cacheDir;
    private File dataDir;
    private File tmpDir;
    private final int maxFiles;
    private final long maxBytes;
    private final long minLifetime;

    private final Map<K, String> keyToDigest = Maps.newHashMap();
    private final Multimap<String, K> digestToKeys = HashMultimap.create();
    private final LinkedHashMap<String, Item> digestToItem = new LinkedHashMap<String, Item>(16, 0.75f, true);
    private long numBytes = 0;

    public static class Item {
        public final File file;
        public final long length;
        public final String digest;
        private long accessTime;

        Item(File file, String digest) {
            assert(file.exists());
            this.file = file;
            this.digest = digest;
            this.length = file.length();
            updateAccessTime();
        }

        private synchronized void updateAccessTime() {
            accessTime = System.currentTimeMillis();
        }

        private synchronized long getAccessTime() {
            return accessTime;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("file", file)
                .add("length", length)
                .add("digest", digest)
                .add("accessTime", new Date(getAccessTime())).toString();
        }
    }

    public static class Builder<K2> {
        private final File cacheDir;
        private int maxFiles = Integer.MAX_VALUE;
        private long maxBytes = Long.MAX_VALUE;

        // Default value is -1 instead of 0.  When set to 0, unit tests fail intermittently
        // if prune() runs at the exact same timestamp as when the item is added.
        private long minLifetime = -1;

        public Builder(File cacheDir) {
            this.cacheDir = cacheDir;
        }

        public Builder<K2> maxFiles(int maxFiles) {
            this.maxFiles = maxFiles;
            return this;
        }

        public Builder<K2> maxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
            return this;
        }

        public Builder<K2> minLifetime(long minLifetime) {
            this.minLifetime = minLifetime;
            return this;
        }

        public FileCache<K2> build() throws IOException {
            return new FileCache<K2>(cacheDir, maxFiles, maxBytes, minLifetime).startup();
        }
    }

    protected FileCache(File cacheDir, Integer maxFiles, Long maxBytes, Long minLifetime) {
        this.cacheDir = cacheDir;
        this.maxFiles = (maxFiles == null ? Integer.MAX_VALUE : maxFiles);
        this.maxBytes = (maxBytes == null ? Long.MAX_VALUE : maxBytes);

        // Default is -1 instead of 0, to avoid intermittent unit test failures
        // when several operations happen within the same millisecond.
        this.minLifetime = (minLifetime == null ? -1 : minLifetime);
    }

    /**
     * Initializes the cache and deletes any existing files.  Call this method before
     * using the cache.
     */
    public synchronized FileCache<K> startup()
    throws IOException {
        ZimbraLog.store.info("Starting up FileCache at %s.  maxFiles=%d, maxBytes=%d.", cacheDir, maxFiles, maxBytes);

        dataDir = new File(cacheDir, "data");
        FileUtil.deleteDir(dataDir);
        FileUtil.ensureDirExists(dataDir);
        tmpDir = new File(cacheDir, "tmp");
        FileUtil.deleteDir(tmpDir);
        FileUtil.ensureDirExists(tmpDir);

        return this;
    }

    synchronized int getNumFiles() {
        return digestToItem.size();
    }

    synchronized long getNumBytes() {
        return numBytes;
    }

    synchronized int getNumKeys() {
        return keyToDigest.size();
    }

    public synchronized boolean contains(K key) {
        return (get(key) != null);
    }

    synchronized boolean containsDigest(String digest) {
        return digestToItem.containsKey(digest);
    }

    public synchronized Item get(K key) {
        String digest = keyToDigest.get(key);
        if (digest == null) {
            log.debug("No cache entry for key %s.", key);
            return null;
        }

        Item entry = digestToItem.get(digest);
        log.debug("Looked up key %s: digest=%s, entry=%s", key, digest, entry);
        if (entry != null) {
            entry.updateAccessTime();
            return entry;
        }
        return null;
    }

    /**
     * Puts content for the given key into the cache.  The caller is responsible for closing
     * the stream.
     */
    public Item put(K key, InputStream content) throws IOException {
        if (dataDir == null) {
            throw new IOException("Please call startup() before using the cache.");
        }
        CalculatorStream calc = new CalculatorStream(content);
        Item file;

        // Write content to a temp file.
        File temp = File.createTempFile(FileCache.class.getSimpleName(), ".tmp", tmpDir);
        OutputStream out = null;
        String digest;

        try {
            out = new FileOutputStream(temp);
            ByteUtil.copy(calc, false, out, true);
            digest = calc.getDigest();
        } catch (IOException e) {
            temp.delete();
            throw e;
        }

        synchronized (this) {
            file = digestToItem.get(digest);
            if (file != null) {
                // File is already in the cache.  Delete the temp file.
                if (!temp.delete()) {
                    log.warn("Unable to delete %s.", temp);
                }
            } else {
                // File is not in the cache.  Move to the cached location.
                File dataFile = new File(dataDir, digest);
                FileUtil.rename(temp, dataFile);
                file = new Item(dataFile, digest);
                digestToItem.put(digest, file);
                numBytes += calc.getSize();
            }
            keyToDigest.put(key, digest);
            digestToKeys.put(digest, key);
        }

        prune();
        return file;
    }

    /**
     * Removes the given key from the cache.  If no other keys reference the file,
     * deletes the file from disk.
     * @return {@code} true if the file was deleted
     */
    public synchronized boolean remove(K key) {
        String digest = keyToDigest.remove(key);
        log.debug("Removing %s, digest=%s", key, digest);

        if (digest != null) {
            Collection<K> keys = digestToKeys.get(digest);
            keys.remove(key);

            if (keys.isEmpty()) {
                Item entry = digestToItem.remove(digest);
                numBytes -= entry.length;
                log.debug("Deleting unreferenced file %s.", entry.file);
                if (entry.file.delete()) {
                    return true;
                } else {
                    log.warn("Unable to delete %s.", entry.file.getAbsolutePath());
                }
            } else {
                log.debug("Not deleting file for %s.  It is referenced by %s.", digest, keys);
            }
        }

        return false;
    }

    private synchronized void prune() {
        Iterator<Map.Entry<String, Item>> i = digestToItem.entrySet().iterator();
        long now = System.currentTimeMillis();

        while (i.hasNext() && (digestToItem.size() > maxFiles || numBytes > maxBytes)) {
            Map.Entry<String, Item> mapEntry = i.next();
            String digest = mapEntry.getKey();
            Item item = mapEntry.getValue();

            if (now - item.getAccessTime() <= minLifetime) {
                log.debug("Not removing %s because it has not expired.", item);
                continue;
            }
            if (!okToRemove(item)) {
                log.debug("Not removing %s because okToRemove() returned false.", item);
                continue;
            }

            long size = item.length;
            if (item.file.delete()) {
                i.remove();
                numBytes -= size;
                for (K key : digestToKeys.get(digest)) {
                    keyToDigest.remove(key);
                }
                digestToKeys.removeAll(digest);
                log.debug("Removed digest %s.  Cache contains %d files, %d bytes.",
                    digest, digestToItem.size(), numBytes);
            } else {
                log.warn("Could not delete file for %s.", item);
            }
        }
    }

    protected boolean okToRemove(Item item) {
        return true;
    }
}
