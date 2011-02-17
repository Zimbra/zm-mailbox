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
package com.zimbra.common.util;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link BlobMetaData}.
 *
 * @author ysasaki
 */
public class BlobMetaDataTest {

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
}
