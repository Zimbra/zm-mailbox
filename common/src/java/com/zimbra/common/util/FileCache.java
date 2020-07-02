/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.zimbra.common.util.BEncoding.BEncodingException;

/**
 * A thread-safe file cache.  Files are stored in the filesystem.  The cache
 * can be reloaded from the filesystem content.  Multiple keys may reference
 * the same file.  When this happens, only one copy of the file content is kept.
 *
 * A {@code FileCache} may have a limit on the number of files or the total
 * number of bytes.  When either limit is exceeded, the least recently accessed
 * files are removed from the cache.  Files are also kept when the minimum
 * lifetime is set and has not been exceeded, or when the cache has
 * a {@link RemoveCallback} and {@link RemoveCallback#okToRemove(Item)} returns
 * false.
 *
 * Callers also have the option of storing extra file properties.  File
 * properties are associated with the file, as opposed to each key.
 *
 * @param <K> the key type that is used to look up files in the cache
 */
public class FileCache<K> {

    public static final Log log = LogFactory.getLog(FileCache.class);

    private final File cacheDir;
    private File dataDir;
    private File tmpDir;
    private File propDir;
    private final int maxFiles;
    private final long maxBytes;
    private final long minLifetime;
    private final RemoveCallback removeCallback;
    private final KeyParser<K> keyParser;

    private final Map<K, String> keyToDigest = Maps.newHashMap();
    private final Multimap<String, K> digestToKeys = HashMultimap.create();
    private final LinkedHashMap<String, Item> digestToItem = new LinkedHashMap<String, Item>(16, 0.75f, true);
    private long numBytes = 0;

    private static final String PROP_KEYS = "FileCache.keys";
    private static final ImmutableSet<String> INTERNAL_PROP_NAMES = ImmutableSet.of(PROP_KEYS);
    private boolean persistent;

    public static class Item {
        public final File file;
        public final long length;
        public final String digest;
        private long accessTime;
        public final ImmutableMap<String, String> properties;

        Item(File file, String digest, Map<String, String> properties) {
            assert(file.exists());
            this.file = file;
            this.digest = digest;
            this.length = file.length();
            if (properties == null) {
                properties = Collections.emptyMap();
            }
            this.properties = ImmutableMap.copyOf(properties);
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
            return MoreObjects.toStringHelper(this)
                .add("file", file)
                .add("length", length)
                .add("digest", digest)
                .add("accessTime", new Date(getAccessTime())).toString();
        }
    }

    public static class Builder<K2> {
        private final File cacheDir;
        private final KeyParser<K2> keyParser;
        private int maxFiles = Integer.MAX_VALUE;
        private long maxBytes = Long.MAX_VALUE;
        private RemoveCallback removeCallback;
        private boolean persistent;

        // Default value is -1 instead of 0.  When set to 0, unit tests fail intermittently
        // if prune() runs at the exact same timestamp as when the item is added.
        private long minLifetime = -1;

        public static Builder<String> createWithStringKey(File cacheDir, boolean persistent) {
            return new Builder<String>(cacheDir, STRING_KEY_PARSER, persistent);
        }

        public static Builder<Integer> createWithIntegerKey(File cacheDir, boolean persistent) {
            return new Builder<Integer>(cacheDir, INTEGER_KEY_PARSER, persistent);
        }

        public Builder(File cacheDir, KeyParser<K2> keyParser, boolean persistent) {
            this.cacheDir = cacheDir;
            this.keyParser = keyParser;
            this.persistent = persistent;
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

        public Builder<K2> removeCallback(RemoveCallback callback) {
            this.removeCallback = callback;
            return this;
        }

        public FileCache<K2> build() throws IOException {
            return new FileCache<K2>(cacheDir, maxFiles, maxBytes, minLifetime, keyParser, removeCallback, persistent).startup();
        }
    }

    /**
     * Generates a key value from its {@code toString()} representation.
     */
    public interface KeyParser<K3> {
        public K3 parse(String keyString);
    }

    private static final KeyParser<String> STRING_KEY_PARSER = new KeyParser<String>() {
        @Override
        public String parse(String keyString) {
            return keyString;
        }
    };

    private static final KeyParser<Integer> INTEGER_KEY_PARSER = new KeyParser<Integer>() {
        @Override
        public Integer parse(String keyString) {
            if (keyString == null) {
                return null;
            }
            return Integer.parseInt(keyString);
        }
    };

    public interface RemoveCallback {
        public boolean okToRemove(Item item);
    }

    private FileCache(File cacheDir, Integer maxFiles, Long maxBytes, Long minLifetime, KeyParser<K> keyParser, RemoveCallback callback, boolean persistent) {
        if (cacheDir == null) {
            throw new IllegalStateException("cacheDir cannot be null");
        }
        if (keyParser == null) {
            throw new IllegalStateException("keyParser cannot be null");
        }
        this.cacheDir = cacheDir;
        this.maxFiles = (maxFiles == null ? Integer.MAX_VALUE : maxFiles);
        this.maxBytes = (maxBytes == null ? Long.MAX_VALUE : maxBytes);
        this.keyParser = keyParser;
        this.removeCallback = callback;

        // Default is -1 instead of 0, to avoid intermittent unit test failures
        // when several operations happen within the same millisecond.
        this.minLifetime = (minLifetime == null ? -1 : minLifetime);
        this.persistent = persistent;
    }

    /**
     * Initializes the cache and deletes any existing files.  Call this method before
     * using the cache.
     */
    private synchronized FileCache<K> startup()
    throws IOException {
        ZimbraLog.store.info("Starting up FileCache at %s.  maxFiles=%d, maxBytes=%d.", cacheDir, maxFiles, maxBytes);

        // Create directories if necessary.
        dataDir = new File(cacheDir, "data");
        propDir = new File(cacheDir, "properties");
        tmpDir = new File(cacheDir, "tmp");
        FileUtil.ensureDirExists(dataDir);
        FileUtil.ensureDirExists(propDir);
        FileUtil.deleteDirContents(tmpDir);
        FileUtil.ensureDirExists(tmpDir);
        if (!persistent) {
            FileUtil.deleteDirContents(dataDir);
            FileUtil.deleteDirContents(propDir);
            return this;
        }

        // Load existing files.
        List<File> dataFiles = Lists.newArrayList(listFiles(dataDir));
        FileUtil.sortFilesByModifiedTime(dataFiles);
        Set<File> propFiles = listFiles(propDir);

        for (File dataFile : dataFiles) {
            String digest = dataFile.getName();
            File propFile = new File(propDir, digest + ".properties");
            Properties props = null;
            Set<K> keys = Sets.newHashSet();

            // Load properties.
            if (propFiles.contains(propFile)) {
                props = new Properties();
                InputStream in = null;
                try {
                    log.debug("Loading properties from %s", propFile);
                    in = new FileInputStream(propFile);
                    props.load(in);

                    // Parse keys.
                    String keysString = props.getProperty(PROP_KEYS, "");
                    List<String> keyList = BEncoding.decode(keysString);
                    for (String keyString : keyList) {
                        keys.add(keyParser.parse(keyString));
                    }
                } catch (IOException e) {
                    log.warn("Unable to load %s", propFile, e);
                } catch (BEncodingException e) {
                    log.warn("Unable to load %s", propFile, e);
                } finally {
                    ByteUtil.closeStream(in);
                }
            }

            if (keys.isEmpty()) {
                log.warn("Unable to load properties, keys=%s.  Deleting %s", keys, dataFile);
                deleteWithWarning(dataFile);
                continue;
            }

            // Create Item and update caches.
            Map<String, String> userProps = Maps.newHashMap();

            // Can't use stringPropertyNames() because older Android phones don't support JDK 6.
            @SuppressWarnings("unchecked")
            Enumeration<String> names = (Enumeration<String>) props.propertyNames();

            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (!INTERNAL_PROP_NAMES.contains(name)) {
                    userProps.put(name, props.getProperty(name));
                }
            }

            for (K key : keys) {
                keyToDigest.put(key, digest);
                digestToKeys.put(digest, key);
            }
            Item item = new Item(dataFile, digest, userProps);
            digestToItem.put(digest, item);
            numBytes += item.length;
            propFiles.remove(propFile);
        }
        return this;
    }

    private static boolean deleteWithWarning(File file) {
        if (file.delete()) {
            return true;
        }
        log.warn("Unable to delete %s.", file.getAbsolutePath());
        return false;
    }

    /**
     * Returns the {@code Set} of files, or an empty {@code Set} if the directory is empty.
     */
    private static Set<File> listFiles(File dir) {
        Set<File> fileSet = Sets.newLinkedHashSet();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                fileSet.add(file);
            }
        }
        return fileSet;
    }

    public synchronized int getNumFiles() {
        return digestToItem.size();
    }

    public synchronized long getNumBytes() {
        return numBytes;
    }

    public synchronized int getNumKeys() {
        return keyToDigest.size();
    }

    public synchronized boolean contains(K key) {
        return (get(key) != null);
    }

    public synchronized boolean containsDigest(String digest) {
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
     * Puts content for the given key into the cache.  Closes the stream implicitly.
     */
    public Item put(K key, InputStream content) throws IOException {
        return put(key, content, null);
    }

    /**
     * Puts content for the given key into the cache.  Closes the stream implicitly.
     */
    public Item put(K key, InputStream content, Map<String, String> userProps) throws IOException {
        try {
            return putInternal(key, content, userProps);
        } finally {
            ByteUtil.closeStream(content);
        }
    }

    private Item putInternal(K key, InputStream content, Map<String, String> userProps) throws IOException {
        if (dataDir == null) {
            throw new IOException("Please call startup() before using the cache.");
        }
        CalculatorStream calc = new CalculatorStream(content);
        Item item;

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
            // If this data is already stored for the given key, return the item.
            String existingDigest = keyToDigest.get(key);
            if (existingDigest != null) {
                if (existingDigest.equals(digest)) {
                    // The same data is already mapped to this key.
                    deleteWithWarning(temp);
                    return digestToItem.get(digest);
                } else {
                    // New data for the same key.  Remove the old entry.
                    remove(key);
                }
            }

            if (persistent) {
                // Store properties.
                Properties props = makeProperties(key, userProps, digest);
                File propFile = new File(propDir, digest + ".properties");
                OutputStream propOut = null;
                try {
                    propOut = new FileOutputStream(propFile);
                    props.store(propOut, null);
                } finally {
                    ByteUtil.closeStream(propOut);
                }
            }

            item = digestToItem.get(digest);
            if (item != null) {
                // Data is already in the cache for another key.  Delete the temp file.
                deleteWithWarning(temp);
            } else {
                // Data is not in the cache.  Move to the cached location.
                File dataFile = new File(dataDir, digest);
                FileUtil.rename(temp, dataFile);
                item = new Item(dataFile, digest, userProps);
                digestToItem.put(digest, item);
                numBytes += calc.getSize();
            }
            keyToDigest.put(key, digest);
            digestToKeys.put(digest, key);
        }

        prune();
        return item;
    }

    private synchronized Properties makeProperties(K newKey, Map<String, String> userProps, String digest) {
        Properties props = new Properties();

        // Add digest and keys.  We need to convert the keys to strings, since BEncoding
        // encodes a list of strings differently than a list of integers.
        List<String> keys = Lists.newArrayList();
        for (K key : digestToKeys.get(digest)) {
            keys.add(key.toString());
        }
        keys.add(newKey.toString());
        String encoded = BEncoding.encode(keys);
        props.put(PROP_KEYS, encoded);

        // Add user properties.
        if (userProps == null) {
            userProps = Collections.emptyMap();
        }
        for (String userKey : userProps.keySet()) {
            if (INTERNAL_PROP_NAMES.contains(userKey)) {
                throw new IllegalStateException("Property '" + userKey + " is a reserved name.");
            }
            props.put(userKey, userProps.get(userKey));
        }

        return props;
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
                deleteFromDisk(entry);
                return true;
            } else {
                log.debug("Not deleting file for %s.  It is referenced by %s.", digest, keys);
            }
        }

        return false;
    }

    @VisibleForTesting
    public synchronized void removeAll() {
        for (K key : keyToDigest.keySet()) {
            remove(key);
        }
    }

    private void deleteFromDisk(Item item) {
        deleteWithWarning(item.file);
        if (persistent) {
            File propFile = new File(propDir, item.digest + ".properties");
            deleteWithWarning(propFile);
        }
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
            if (removeCallback != null && !removeCallback.okToRemove(item)) {
                log.debug("Not removing %s because okToRemove() returned false.", item);
                continue;
            }

            // Delete from filesystem.
            deleteFromDisk(item);

            // Update in-memory caches.
            i.remove();
            numBytes -= item.length;
            for (K key : digestToKeys.get(digest)) {
                keyToDigest.remove(key);
            }
            digestToKeys.removeAll(digest);
            log.debug("Removed digest %s.  Cache contains %d files, %d bytes.",
                digest, digestToItem.size(), numBytes);
        }
    }
}
