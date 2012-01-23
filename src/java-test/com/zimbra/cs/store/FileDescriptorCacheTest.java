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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileCache;
import com.zimbra.common.util.FileUtil;

public class FileDescriptorCacheTest {

    private static final String NAME_PREFIX = FileDescriptorCacheTest.class.getSimpleName();

    private File tmpDir;
    private File uncompressedDir;
    private List<File> tempFiles = Lists.newArrayList();

    @Before
    public void startUp() {
        tmpDir = Files.createTempDir();
        uncompressedDir = Files.createTempDir();
    }

    @After
    public void tearDown() throws IOException {
        if (tmpDir != null) {
            FileUtil.deleteDir(tmpDir);
        }
        if (uncompressedDir != null) {
            FileUtil.deleteDir(uncompressedDir);
        }
        for (File file : tempFiles) {
            file.delete();
        }
    }

    @Test
    public void testUncompressedCache()
    throws Exception {
        FileCache<String> uc = FileCache.Builder.createWithStringKey(uncompressedDir).build();
        FileDescriptorCache fdc = new FileDescriptorCache(uc);
        fdc.setMaxSize(2);

        File file1 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        tempFiles.add(file1);
        File file2 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        tempFiles.add(file2);
        File file3 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        tempFiles.add(file3);
        File file4 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        tempFiles.add(file4);

        String content1 = "Tempted";
        String content2 = "Tempted";
        String content3 = "Black Coffee In Bed";
        String content4 = "Pulling Mussels";

        String digest1 = ByteUtil.getDigest(content1.getBytes());
        String digest3 = ByteUtil.getDigest(content3.getBytes());
        String digest4 = ByteUtil.getDigest(content4.getBytes());

        write(file1, content1);
        write(file2, content2);
        write(file3, content3);
        write(file4, content4);

        assertFalse(uc.containsDigest(digest1));
        assertFalse(uc.containsDigest(digest3));
        assertFalse(uc.containsDigest(digest4));

        assertEquals(0, fdc.getSize());
        byte[] buf = new byte[10];

        fdc.read(file1.getPath(), content1.length(), 0, buf, 0, buf.length);
        assertTrue(uc.containsDigest(digest1));
        assertFalse(uc.containsDigest(digest3));
        assertFalse(uc.containsDigest(digest4));
        assertEquals(1, fdc.getSize());

        fdc.read(file2.getPath(), content2.length(), 0, buf, 0, buf.length);
        assertTrue(uc.containsDigest(digest1));
        assertFalse(uc.containsDigest(digest3));
        assertFalse(uc.containsDigest(digest4));
        assertEquals(2, fdc.getSize());

        fdc.read(file3.getPath(), content3.length(), 0, buf, 0, buf.length);
        assertTrue(uc.containsDigest(digest1));
        assertTrue(uc.containsDigest(digest3));
        assertFalse(uc.containsDigest(digest4));
        assertEquals(2, fdc.getSize());

        fdc.read(file4.getPath(), content4.length(), 0, buf, 0, buf.length);
        assertFalse(uc.containsDigest(digest1));
        assertTrue(uc.containsDigest(digest3));
        assertTrue(uc.containsDigest(digest4));
        assertEquals(2, fdc.getSize());
    }

    private void write(File file, String content)
    throws IOException {
        OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
        out.write(content.getBytes());
        out.close();
    }
}
