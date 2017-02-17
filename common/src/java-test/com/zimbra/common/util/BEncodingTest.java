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
