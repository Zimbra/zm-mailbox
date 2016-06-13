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

//        // No size truncation on ParsedDocument of <2GB file
//        FakeBlob blob2GBMinus = new FakeBlob(new File("/dev/null"), size2GBMinus);
//        ParsedDocument pd2GBMinus = new ParsedDocument(blob2GBMinus, "blob-2gb-minus.txt", "text/plain", System.currentTimeMillis(), "creator", "desc", false);
//        Assert.assertEquals(size2GBMinus, pd2GBMinus.getSize());
//
//        // No size truncation on ParsedDocument of 2GB file
//        FakeBlob blob2GB = new FakeBlob(new File("/dev/null"), size2GB);
//        ParsedDocument pd2GB = new ParsedDocument(blob2GB, "blob-2gb.txt", "text/plain", System.currentTimeMillis(), "creator", "desc", false);
//        Assert.assertEquals(size2GB, pd2GB.getSize());
    }
}
