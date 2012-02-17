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
