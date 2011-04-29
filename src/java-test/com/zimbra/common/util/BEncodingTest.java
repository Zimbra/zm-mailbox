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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link BEncoding}.
 *
 * @author ysasaki
 */
public class BEncodingTest {

    @Test
    public void test() throws Exception {
        List<Object> list = new ArrayList<Object>();
        list.add(new Integer(654));
        list.add("hwhergk");
        list.add(new StringBuilder("74x"));

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("testing", new Long(5));
        map.put("foo2", "bar");
        map.put("herp", list);
        map.put("Foo", new Float(6.7));
        map.put("yy", new TreeMap<Object, Object>());

        String encoded = BEncoding.encode(map);
        Assert.assertEquals("d3:Foo3:6.74:foo23:bar4:herpli654e7:hwhergk3:74xe7:testingi5e2:yydee", encoded);

        @SuppressWarnings("unchecked")
        Map<String, Object> decoded = (Map<String, Object>) BEncoding.decode(encoded);
        Assert.assertEquals(5, decoded.size());
        Assert.assertEquals(5L, decoded.get("testing"));
        Assert.assertEquals("bar", decoded.get("foo2"));
        Assert.assertEquals(list.toString(), decoded.get("herp").toString());
        Assert.assertEquals("6.7", decoded.get("Foo"));
        Assert.assertEquals(new TreeMap<Object, Object>(), decoded.get("yy"));
    }

}
