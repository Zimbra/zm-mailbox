/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BlobInputStreamTest {
    @Before
    public void startUp() {
        BlobInputStream.setFileDescriptorCache(new FileDescriptorCache(null));
    }

    @After
    public void tearDown() {
        BlobInputStream.setFileDescriptorCache(null);
    }

    private static final byte[] CONTENT = "0123456789".getBytes();

    private File createTempFile() throws IOException {
        File file = File.createTempFile(BlobInputStreamTest.class.getSimpleName(), ".msg");
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(CONTENT);
        fos.close();

        return file;
    }

    @Test
    public void deleteHalfway() throws Exception {
        // set up file and build base stream
        File file = createTempFile();
        BlobInputStream bis = new BlobInputStream(file, file.length());

        // make sure you can read from it (this also puts it in the FD cache)
        Assert.assertEquals("can read 10 bytes", CONTENT.length, bis.read(new byte[100]));

        // get a full-length substream
        InputStream copy = bis.newStream(0, CONTENT.length);
        Assert.assertNotNull("can create substream before delete", copy);
        Assert.assertEquals("can read 10 bytes from full substream", CONTENT.length, copy.read(new byte[100]));

        // rely on the FD cache to keep it readable through a file delete
        file.delete();
        Assert.assertFalse("file is gone", file.exists());
        InputStream substream = bis.newStream(2, 8);
        Assert.assertNotNull("can create substream after delete", substream);
        Assert.assertEquals("can read 6 bytes from substream after deelete", 6, substream.read(new byte[100]));


        // set up file again
        file = createTempFile();
        bis = new BlobInputStream(file, file.length());

        // new file is not in FD cache, so it shouldn't be readable after a file delete
        file.delete();
        Assert.assertFalse("file is gone", file.exists());
        Assert.assertNull("can't create substream after delete", bis.newStream(0, CONTENT.length));
    }
}
