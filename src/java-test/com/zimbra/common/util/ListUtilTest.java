/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class ListUtilTest {

    @Test
    public void newArrayList() {
        Function<Integer, String> intToString = new Function<Integer, String>() {
            public String apply(Integer i) {
                return i.toString();
            }
        };

        List<Integer> intList = Lists.newArrayList(1, 2);
        List<String> stringList = ListUtil.newArrayList(intList, intToString);

        // Check transformed list.
        assertEquals(2, stringList.size());
        assertEquals("1", stringList.get(0));
        assertEquals("2", stringList.get(1));

        // Make changes to the transformed list and make sure the original list isn't affected.
        stringList.remove(0);
        stringList.set(0, "3");
        assertEquals(2, intList.size());
        assertEquals(1, (int) intList.get(0));
        assertEquals(2, (int) intList.get(1));
    }

    @Test
    public void nullToEmpty() {
        Collection<Integer> c = null;
        assertEquals(0, ListUtil.nullToEmpty(c).size());

        List<Integer> l = Lists.newArrayList(1, 2, 3);
        assertEquals(l, ListUtil.nullToEmpty(l));
    }
}
