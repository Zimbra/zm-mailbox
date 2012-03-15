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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

import org.junit.Test;

public class MapUtilTest {

    @Test
    public void newValueListMap() {
        Map<Integer, List<String>> map = MapUtil.newValueListMap();
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
    public void newValueSetMap() {
        Map<Integer, Set<String>> map = MapUtil.newValueSetMap();
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
