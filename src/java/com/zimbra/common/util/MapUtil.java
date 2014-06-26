/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

public class MapUtil {

    public static <K, V> TimeoutMap<K, V> newTimeoutMap(long timeoutMillis) {
        return new TimeoutMap<K, V>(timeoutMillis);
    }
    
    public static <K, V> LruMap<K, V> newLruMap(int maxSize) {
        return new LruMap<K, V>(maxSize);
    }

    /**
     * Returns a new {@code Map} that maps a key to a {@code List} of values.
     * When {@code get()} is called on a key that does not exist in the map,
     * the map implicitly creates a new key that maps to an empty {@code List}. 
     */
    public static <K, V> ConcurrentMap<K, List<V>> newValueListMap() {
        Function<K, List<V>> listCreator = new Function<K, List<V>>() {
            @Override
            public List<V> apply(K from) {
                return new ArrayList<V>();
            }
        };
        return new MapMaker().makeComputingMap(listCreator);
    }
    
    /**
     * Returns a new {@code Map} that maps a key to a {@code Set} of values.
     * When {@code get()} is called on a key that does not exist in the map,
     * the map implicitly creates a new key that maps to an empty {@code Set}. 
     */
    public static <K, V> ConcurrentMap<K, Set<V>> newValueSetMap() {
        Function<K, Set<V>> setCreator = new Function<K, Set<V>>() {
            @Override
            public Set<V> apply(K from) {
                return new HashSet<V>();
            }
        };
        return new MapMaker().makeComputingMap(setCreator);
    }
    
    /**
     * Converts a Guava {@code Multimap} to a {@code Map} that maps a key to a
     * {@code List} of values.
     */
    public static <K, V> ConcurrentMap<K, List<V>> multimapToMapOfLists(Multimap<K, V> multimap) {
        ConcurrentMap<K, List<V>> map = newValueListMap();
        if (multimap != null) {
            for (Map.Entry<K, V> entry : multimap.entries()) {
                map.get(entry.getKey()).add(entry.getValue());
            }
        }
        return map;
    }
}
