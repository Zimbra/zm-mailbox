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

package com.zimbra.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileCache;
import com.zimbra.common.util.FileUtil;

public class FileCacheTest {

    File tmpDir;

    @Before
    public void setUp() {
        tmpDir = Files.createTempDir();
    }

    /**
     * Tests getting an item from the cache.
     */
    @Test
    public void get() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        FileCache.Item item = cache.get(1);
        assertArrayEquals(data, ByteUtil.getContent(item.file));

        File dataDir = new File(tmpDir, "data");
        assertEquals(dataDir, item.file.getParentFile());
    }

    /**
     * Tests getting an item that doesn't exist.
     */
    @Test
    public void keyDoesNotExist() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        assertNull(cache.get(1));
    }

    /**
     * Tests {@link FileCache#contains}.
     */
    @Test
    public void contains() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        assertTrue(cache.contains(1));
        assertFalse(cache.contains(2));
    }

    /**
     * Verifies that files with the same digest are deduped.
     */
    @Test
    public void dedupe() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        cache.put(2, new ByteArrayInputStream(data));
        assertEquals(1, cache.getNumFiles());
        assertEquals(2, cache.getNumKeys());
        assertEquals(100, cache.getNumBytes());
    }

    /**
     * Tests removing keys and files from the cache.
     */
    @Test
    public void remove() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        cache.put(2, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(3, new ByteArrayInputStream(data));

        FileCache.Item item1 = cache.get(1);
        FileCache.Item item2 = cache.get(2);
        FileCache.Item item3 = cache.get(3);
        assertEquals(item1.digest, item2.digest);
        assertFalse(item1.digest.equals(item3.digest));

        assertFalse(cache.remove(1));
        assertFalse(cache.contains(1));
        assertEquals(2, cache.getNumFiles());
        assertEquals(200, cache.getNumBytes());

        assertTrue(item1.file.exists());
        assertTrue(item2.file.exists());
        assertTrue(item3.file.exists());

        assertTrue(cache.remove(2));
        assertFalse(cache.contains(2));
        assertEquals(1, cache.getNumFiles());
        assertEquals(100, cache.getNumBytes());

        assertFalse(item1.file.exists());
        assertFalse(item2.file.exists());
        assertTrue(item3.file.exists());
    }

    /**
     * Tests pruning the cache when the maximum number of files has been exceeded.
     */
    @Test
    public void maxFiles() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).maxFiles(2).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        cache.put(2, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(3, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(4, new ByteArrayInputStream(data));

        assertEquals(2, cache.getNumFiles());
        assertEquals(200, cache.getNumBytes());
        assertFalse(cache.contains(1));
        assertFalse(cache.contains(2));
        assertTrue(cache.contains(3));
        assertTrue(cache.contains(4));
    }

    /**
     * Verifies that the least recently accessed item is pruned.
     */
    @Test
    public void accessOrder() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).maxFiles(2).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(2, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.get(1);
        cache.put(3, new ByteArrayInputStream(data));
        data = getRandomBytes(100);

        assertTrue(cache.contains(1));
        assertFalse(cache.contains(2));
        assertTrue(cache.contains(3));
    }

    @Test
    public void maxBytes() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).maxBytes(299).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(2, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(3, new ByteArrayInputStream(data));

        assertEquals(2, cache.getNumFiles());
        assertEquals(200, cache.getNumBytes());
        assertFalse(cache.contains(1));
        assertTrue(cache.contains(2));
        assertTrue(cache.contains(3));
    }

    /**
     * Never prunes files that are smaller or equal to the given size.
     */
    private class KeepSmallFiles implements FileCache.RemoveCallback {
        private final long size;

        KeepSmallFiles(long size) {
            this.size = size;
        }

        @Override
        public boolean okToRemove(FileCache.Item item) {
            return item.file.length() > size;
        }
    }

    /**
     * Tests overriding the behavior of {@link FileCache#okToRemove}.
     */
    @Test
    public void okToRemove() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir)
            .maxFiles(2)
            .removeCallback(new KeepSmallFiles(99)).build();
        byte[] data = getRandomBytes(99);
        cache.put(1, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(2, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(3, new ByteArrayInputStream(data));
        data = getRandomBytes(100);

        assertTrue(cache.contains(1));
        assertFalse(cache.contains(2));
        assertTrue(cache.contains(3));
    }

    /**
     * Verifies that files are not purged if their lifetime is lower than the minimum.
     */
    @Test
    public void minLifetime() throws IOException, InterruptedException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).maxFiles(2).minLifetime(200).build();
        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.put(2, new ByteArrayInputStream(data));
        data = getRandomBytes(100);
        cache.get(1);
        cache.get(2);
        cache.put(3, new ByteArrayInputStream(data));

        assertTrue(cache.contains(1));
        assertTrue(cache.contains(2));
        assertTrue(cache.contains(3));

        Thread.sleep(250);

        data = getRandomBytes(100);
        cache.put(4, new ByteArrayInputStream(data));
        assertFalse(cache.contains(1));
        assertFalse(cache.contains(2));
        assertTrue(cache.contains(3));
        assertTrue(cache.contains(4));
    }

    /**
     * Verifies that cached files are cleaned up after the item is aged out.
     */
    @Test
    public void cleanUpFiles() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).maxFiles(1).build();
        File dataDir = new File(tmpDir, "data");
        File propDir = new File(tmpDir, "properties");

        byte[] data = getRandomBytes(100);
        cache.put(1, new ByteArrayInputStream(data));
        FileCache.Item item1 = cache.get(1);
        File dataFile = new File(dataDir, item1.digest);
        File propFile = new File(propDir, item1.digest + ".properties");
        assertTrue(dataFile.isFile());
        assertTrue(propFile.isFile());

        data = getRandomBytes(100);
        cache.put(2, new ByteArrayInputStream(data));
        assertFalse(dataFile.exists());
        assertFalse(propFile.exists());
    }

    /**
     * Verifies reloading cache content.
     */
    @Test
    public void reload() throws IOException {
        FileCache<Integer> cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        byte[] data = getRandomBytes(100);

        // Store 2 entries for the same content.
        Map<String, String> props = ImmutableMap.of("Bill", "Evans", "Winton", "Kelly");
        cache.put(1, new ByteArrayInputStream(data), props);
        cache.put(2, new ByteArrayInputStream(data), props);

        FileCache.Item item = cache.get(1);
        assertEquals(2, item.properties.size());
        assertEquals("Evans", item.properties.get("Bill"));
        assertEquals("Kelly", item.properties.get("Winton"));

        assertEquals(item, cache.get(2));

        // Reload cache and compare content.
        cache = FileCache.Builder.createWithIntegerKey(tmpDir).build();
        assertEquals(1, cache.getNumFiles());
        assertEquals(100, cache.getNumBytes());

        item = cache.get(1);
        assertEquals(2, item.properties.size());
        assertEquals("Evans", item.properties.get("Bill"));
        assertEquals("Kelly", item.properties.get("Winton"));

        assertEquals(item, cache.get(2));
    }

    private static byte[] getRandomBytes(int size) {
        byte[] data = new byte[size];
        Random r = new Random();
        r.nextBytes(data);
        return data;
    }

    @After
    public void tearDown() throws IOException {
        FileUtil.deleteDir(tmpDir);
    }
}
