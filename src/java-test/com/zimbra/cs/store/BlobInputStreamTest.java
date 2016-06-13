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
        try (InputStream badStartIs = bis.newStream(-1, CONTENT.length)) {
            Assert.fail("Shouldn't be able to create newStream with start < 0");
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue("Blob name included in exception message", iae.getMessage().contains(file.getAbsolutePath()));
        }

        // rely on the FD cache to keep it readable through a file delete
        file.delete();
        Assert.assertFalse("file is gone", file.exists());
        InputStream substream = bis.newStream(2, 8);
        Assert.assertNotNull("can create substream after delete", substream);
        Assert.assertEquals("can read 6 bytes from substream after delete", 6, substream.read(new byte[100]));

        try (InputStream badStartIs = bis.newStream(-1, CONTENT.length)) {
            Assert.fail("Shouldn't be able to create newStream with start < 0 - especially after delete");
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue("Blob name included in exception message", iae.getMessage().contains(file.getAbsolutePath()));
        }

        // set up file again
        file = createTempFile();
        bis = new BlobInputStream(file, file.length());

        // new file is not in FD cache, so it shouldn't be readable after a file delete
        file.delete();
        Assert.assertFalse("file is gone", file.exists());
        Assert.assertNull("can't create substream after delete", bis.newStream(0, CONTENT.length));
    }
}
