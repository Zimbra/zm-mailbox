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
package com.zimbra.cs.mime;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.store.Blob;

public class ParsedDocumentTest {

    private static class FakeBlob extends Blob {
        public FakeBlob(File file, long size) {
            super(file, size, "fake-digest");
        }
    }

    @Test
    public void test2GBFileSize() throws Exception {
        long size2GBMinus = 0x7fffffffL;
        long size2GB = 0x80000000L;

        // No truncation when converting <2GB long to int
        int int2GBMinus = (int) size2GBMinus;
        Assert.assertEquals(size2GBMinus, int2GBMinus);
        Assert.assertEquals("2147483647", Integer.toString(int2GBMinus));

        // Truncation when converting 2GB long to int (simulates error responsible for HS-5126)
        int int2GB = (int) size2GB;
        long negative2GB = -2147483648;
        Assert.assertEquals(negative2GB, int2GB);
        Assert.assertEquals("-2147483648", Integer.toString(int2GB));

        // No size truncation on ParsedDocument of <2GB file
        FakeBlob blob2GBMinus = new FakeBlob(new File("/dev/null"), size2GBMinus);
        ParsedDocument pd2GBMinus = new ParsedDocument(blob2GBMinus, "blob-2gb-minus.txt", "text/plain", System.currentTimeMillis(), "creator", "desc", false);
        Assert.assertEquals(size2GBMinus, pd2GBMinus.getSize());

        // No size truncation on ParsedDocument of 2GB file
        FakeBlob blob2GB = new FakeBlob(new File("/dev/null"), size2GB);
        ParsedDocument pd2GB = new ParsedDocument(blob2GB, "blob-2gb.txt", "text/plain", System.currentTimeMillis(), "creator", "desc", false);
        Assert.assertEquals(size2GB, pd2GB.getSize());
    }
}
