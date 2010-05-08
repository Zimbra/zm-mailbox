/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import junit.framework.TestCase;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.FileDescriptorCache;

/**
 * This is a server-side test because it calls <tt>FileDescriptorCache</tt> directly.
 */
public class TestFileDescriptorCache
extends TestCase {

    private static final String NAME_PREFIX = TestFileDescriptorCache.class.getSimpleName();
    
    private String mOriginalCacheSize = null;
    private Set<File> mFiles = new HashSet<File>();
    private File mUncompressedDir;
    
    public void setUp()
    throws Exception {
        mOriginalCacheSize = TestUtil.getServerAttr(Provisioning.A_zimbraMailFileDescriptorCacheSize);
        mUncompressedDir = new File(LC.zimbra_tmp_directory.value() + File.separator + "uncompressed");
    }
    
    public void testUncompressedCache()
    throws Exception {
        File file1 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        File file2 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        File file3 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        File file4 = File.createTempFile(NAME_PREFIX, ".tmp.gz");
        
        mFiles.add(file1);
        mFiles.add(file2);
        mFiles.add(file3);
        mFiles.add(file4);
        
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
        
        assertFalse(exists(digest1));
        assertFalse(exists(digest3));
        assertFalse(exists(digest4));
        
        TestUtil.setServerAttr(Provisioning.A_zimbraMailFileDescriptorCacheSize, "2");
        FileDescriptorCache fdc = BlobInputStream.getFileDescriptorCache();
        fdc.shutdown();
        assertEquals(0, fdc.getSize());
        byte[] buf = new byte[10];
        
        fdc.read(file1.getPath(), content1.length(), 0, buf, 0, buf.length);
        assertTrue(exists(digest1));
        assertFalse(exists(digest3));
        assertFalse(exists(digest4));
        assertEquals(1, fdc.getSize());

        fdc.read(file2.getPath(), content2.length(), 0, buf, 0, buf.length);
        assertTrue(exists(digest1));
        assertFalse(exists(digest3));
        assertFalse(exists(digest4));
        assertEquals(2, fdc.getSize());
        
        fdc.read(file3.getPath(), content3.length(), 0, buf, 0, buf.length);
        assertTrue(exists(digest1));
        assertTrue(exists(digest3));
        assertFalse(exists(digest4));
        assertEquals(2, fdc.getSize());
        
        fdc.read(file4.getPath(), content4.length(), 0, buf, 0, buf.length);
        assertFalse(exists(digest1));
        assertTrue(exists(digest3));
        assertTrue(exists(digest4));
        assertEquals(2, fdc.getSize());
    }
    
    private boolean exists(String digest) {
        return (new File(mUncompressedDir, digest)).exists();
    }
    
    private void write(File file, String content)
    throws IOException {
        OutputStream out = new GZIPOutputStream(new FileOutputStream(file));
        out.write(content.getBytes());
        out.close();
    }
    
    public void tearDown()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailFileDescriptorCacheSize, mOriginalCacheSize);

        FileDescriptorCache fdc = BlobInputStream.getFileDescriptorCache();
        for (File file : mFiles) {
            fdc.remove(file.getPath());
            file.delete();
        }
    }
}
