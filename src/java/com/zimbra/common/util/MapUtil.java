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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

public class MapUtil {

    public static <K, V> Map<K, V> newLruMap(int maxSize) {
        return new LruMap<K, V>(maxSize);
    }
    
    /**
     * Returns a new {@code Map} that maps a key to a {@code List} of values.
     * When {@code get()} is called on a key that does not exist in the map,
     * the map implicitly creates a new key that maps to an empty {@code List}. 
     */
    private static <K, V> Map<K, List<V>> newValueListMap() {
        Function<K, List<V>> listCreator = new Function<K, List<V>>() {
            @Override
            public List<V> apply(K from) {
                return new ArrayList<V>();
            }
        };
        return new MapMaker().makeComputingMap(listCreator);
    }
    
    /**
     * Converts a Guava {@code Multimap} to a {@code Map} that maps a key to a
     * {@code List} of values.
     */
    public static <K, V> Map<K, List<V>> multimapToMapOfLists(Multimap<K, V> multimap) {
        Map<K, List<V>> map = newValueListMap();
        if (multimap != null) {
            for (Map.Entry<K, V> entry : multimap.entries()) {
                map.get(entry.getKey()).add(entry.getValue());
            }
        }
        return map;
    }
}
