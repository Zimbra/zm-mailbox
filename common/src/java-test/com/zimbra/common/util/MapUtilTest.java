/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.LoadingCache;

import static org.junit.Assert.*;

import org.junit.Test;

public class MapUtilTest {

    @Test
    public void newValueListMap() throws ExecutionException {
        LoadingCache<Integer, List<String>> map = MapUtil.newValueListMap();
        map.get(1).add("a");
        map.get(1).add("b");
        map.get(2).add("c");
        
        assertEquals(2, map.size());
        
        List<String> list = map.get(1);
        assertEquals(2, list.size());
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        
        list = map.get(2);
        assertEquals(1, list.size());
        assertTrue(list.contains("c"));
    }
    
    @Test
    public void newValueSetMap() throws ExecutionException {
        LoadingCache<Integer, Set<String>> map = MapUtil.newValueSetMap();
        map.get(1).add("a");
        map.get(1).add("b");
        map.get(2).add("c");
        
        assertEquals(2, map.size());
        
        Set<String> set = map.get(1);
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        
        set = map.get(2);
        assertEquals(1, set.size());
        assertTrue(set.contains("c"));
    }
}
