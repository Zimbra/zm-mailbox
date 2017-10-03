/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Unit test for {@link LuceneDirectory}.
 *
 * @author ysasaki
 */
public final class LuceneDirectoryTest {
    /* TODO: bring this back when LuceneDirectory is fixed
    private static File tmpDir;

    @BeforeClass
    public static void init() throws Exception {
        tmpDir = new File("build/test/" + LuceneDirectoryTest.class.getSimpleName());
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
        // make sure index perf counters are enabled.
        LC.zimbra_index_disable_perf_counters.setDefault(false);
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
        Assert.assertEquals(1, ZimbraPerf.COUNTER_IDX_BYTES_READ.getCount() - count);
        Assert.assertEquals(5, ZimbraPerf.COUNTER_IDX_BYTES_READ.getTotal() - total);
    }

    @Test
    public void write() throws IOException {
        long count = ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getCount();
        long total = ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getTotal();
        LuceneDirectory dir = LuceneDirectory.open(new File("/tmp"));
        IndexOutput out = dir.createOutput("write");
        out.writeBytes(new byte[] { 0, 1, 2 }, 3);
        out.close();

        Assert.assertEquals(1, ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getCount() - count);
        Assert.assertEquals(3, ZimbraPerf.COUNTER_IDX_BYTES_WRITTEN.getTotal() - total);
    } */

}
