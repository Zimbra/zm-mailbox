/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.octosync;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link BinaryPatchReader}.
 *
 * @author grzes
 */
public final class BinaryPatchReaderTest
{
    @Test
    public void oneDataRecord() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();
        patchBuilder.addData("hello");

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        PatchReader patchReader = new BinaryPatchReader(is);

        Assert.assertTrue(patchReader.hasMoreRecordInfos());
        Assert.assertEquals(patchReader.getNextRecordInfo().type, PatchReader.RecordType.DATA);
        Assert.assertEquals(patchReader.getNextRecordInfo().length, 5);

        DataInputStream dis = new DataInputStream(patchReader.popData());
        byte[] buf = new byte[5];
        dis.readFully(buf);

        Assert.assertEquals(dis.read(), -1);
        Assert.assertEquals(new String(buf), "hello");
        Assert.assertFalse(patchReader.hasMoreRecordInfos());
    }

    @Test
    public void oneDataOneRef() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();
        patchBuilder.addData("hello");

        {
            PatchRef pref = new PatchRef();
            pref.fileId = 1;
            pref.fileVersion = 2;
            pref.offset = 123;
            pref.length = 456;
            pref.hashKey = patchBuilder.calcSHA256("foobar");
            patchBuilder.addRef(pref);
        }

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        PatchReader patchReader = new BinaryPatchReader(is);

        Assert.assertTrue(patchReader.hasMoreRecordInfos());
        Assert.assertEquals(patchReader.getNextRecordInfo().type, PatchReader.RecordType.DATA);

        patchReader.popData().skip(5);

        Assert.assertTrue(patchReader.hasMoreRecordInfos());
        Assert.assertEquals(patchReader.getNextRecordInfo().type, PatchReader.RecordType.REF);

        PatchRef pref = patchReader.popRef();

        Assert.assertEquals(pref.fileId, 1);
        Assert.assertEquals(pref.fileVersion, 2);
        Assert.assertEquals(pref.offset, 123);
        Assert.assertEquals(pref.length, 456);
        Assert.assertArrayEquals(pref.hashKey, patchBuilder.calcSHA256("foobar"));

        Assert.assertFalse(patchReader.hasMoreRecordInfos());
    }

    @Test(expected=BadPatchFormatException.class)
    public void negativeGarbageInPatch() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();
        patchBuilder.addData("hello");
        patchBuilder.buffer.write('X');

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        PatchReader patchReader = new BinaryPatchReader(is);

        Assert.assertTrue(patchReader.hasMoreRecordInfos());
        patchReader.popData().skip(5);
        patchReader.hasMoreRecordInfos();
    }

}
