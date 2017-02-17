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
package com.zimbra.common.util;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link BlobMetaData}.
 *
 * @author ysasaki
 */
public final class BlobMetaDataTest {

    @Test
    public void test() throws Exception {
        StringBuilder buf = new StringBuilder();
        BlobMetaData.encodeMetaData("name1", "value1", buf);
        BlobMetaData.encodeMetaData("name2", 1, buf);
        BlobMetaData.encodeMetaData("name3", 1L, buf);
        BlobMetaData.encodeMetaData("name4", true, buf);
        BlobMetaData.encodeMetaData("name5", false, buf);
        Map<Object, Object> map = BlobMetaData.decode(buf.toString());

        Assert.assertEquals("value1", BlobMetaData.getString(map, "name1"));
        Assert.assertEquals(1, BlobMetaData.getInt(map, "name2"));
        Assert.assertEquals(1L, BlobMetaData.getLong(map, "name3"));
        Assert.assertEquals(true, BlobMetaData.getBoolean(map, "name4"));
        Assert.assertEquals(false, BlobMetaData.getBoolean(map, "name5"));

        Assert.assertEquals("value1", BlobMetaData.getString(map, "no", "value1"));
        Assert.assertEquals(1, BlobMetaData.getInt(map, "no", 1));
        Assert.assertEquals(1L, BlobMetaData.getLong(map, "no", 1L));
        Assert.assertEquals(true, BlobMetaData.getBoolean(map, "no", true));
        Assert.assertEquals(false, BlobMetaData.getBoolean(map, "no", false));
    }

    @Test
    public void lengthCorrupted() throws Exception {
        try {
            BlobMetaData.decode("x=10");
            Assert.fail();
        } catch (BlobMetaDataEncodingException expected) {
        }
    }

    @Test
    public void valueCorrupted() throws Exception {
        try {
            BlobMetaData.decode("x=1:x");
            Assert.fail();
        } catch (BlobMetaDataEncodingException expected) {
        }
    }
}
