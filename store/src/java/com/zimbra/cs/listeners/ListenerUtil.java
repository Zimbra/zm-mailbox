/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.listeners;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ListenerUtil {

    // listeners priority
    public enum Priority {
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN;
    }

    // Method for sorting the listener map based on priorities
    public static <K, V extends Comparable<V>> Map<K, V> sortByPriority(final Map<K, V> map) {
        Comparator<K> valueComparator = new Comparator<K>() {

            public int compare(K k1, K k2) {
                int compare = map.get(k1).compareTo(map.get(k2));
                if (compare == 0)
                    return 1;
                else
                    return compare;
            }
        };

        Map<K, V> sortedByPriorities = new TreeMap<K, V>(valueComparator);
        sortedByPriorities.putAll(map);
        return sortedByPriorities;
    }
}
