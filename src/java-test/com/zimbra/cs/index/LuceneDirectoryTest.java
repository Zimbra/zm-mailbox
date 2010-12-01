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

package com.zimbra.cs.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Unit test for {@link LuceneDirectory}.
 *
 * @author ysasaki
 */
public class LuceneDirectoryTest {
    private static File tmpDir;

    @BeforeClass
    public static void init() throws Exception {
        tmpDir = new File("build/test/" +
                LuceneDirectoryTest.class.getSimpleName());
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
    }

    @Test
    public void read() throws IOException {
        FileOutputStream out = new FileOutputStream(new File(tmpDir, "read"));
        out.write(new byte[] { 0, 1, 2, 3, 4 });
        out.close();

        long count = ZimbraPerf.COUNTER_IDX_BYTES_READ.getCount();
        long total = ZimbraPerf.COUNTER_IDX_BYTES_READ.getTotal();
        LuceneDirectory dir = LuceneDirectory.open(tmpDir);
        IndexInput in = dir.openInput("read");
        in.readBytes(new byte[5], 0, 5);
        in.close();
        Assert.assertEquals(5, dir.getBytesRead());
        Assert.assertEquals(1,
                ZimbraPerf.COUNTER_IDX_BYTES_READ.getCount() - count);
        Assert.assertEquals(5,
                ZimbraPerf.COUNTER_IDX_BYTES_READ.getTotal() - total);
    }

    @Test
    public void write() throws IOException {
        long count = ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getCount();
        long total = ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getTotal();
        LuceneDirectory dir = LuceneDirectory.open(new File("/tmp"));
        IndexOutput out = dir.createOutput("write");
        out.writeBytes(new byte[] { 0, 1, 2 }, 3);
        out.close();

        Assert.assertEquals(3, dir.getBytesWritten());
        Assert.assertEquals(1,
                ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getCount() - count);
        Assert.assertEquals(3,
                ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getTotal() - total);
    }

}
