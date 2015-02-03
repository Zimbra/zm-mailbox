/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014 Zimbra, Inc.
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
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeMap;

/*
 * can't use legacy MultiMap interface together with generic java 1.5 interfaces.
 * 
import org.apache.commons.collections.MultiMap;
 */

@SuppressWarnings("serial")
public class MultiTreeMap<K,V> extends TreeMap<K,Collection<V>> /* implements MultiMap */ {

	public MultiTreeMap(Comparator<? super K> c) {
		super(c);
	}
	
	public boolean containsValue(Object value) {
		if (super.containsValue(value))
			return true;
		for (Collection<V> v : values())
			if (v.contains(value))
				return true;

		return false;
	}
	
	public V add(K key, V value) {
		Collection<V> v = super.get(key);
		if (v == null) {
			v = new ArrayList<V>();
			super.put(key, v);
		}
		v.add(value);
		return value;
	}
	
	public V getFirst(K key) {
		Collection<V> v = get(key);
		if (v != null && v.size() > 0)
			return v.iterator().next();
		return null;
	}
	
	public void remove(K key, V value) {
		Collection<V> v = super.get(key);
		if (v != null)
			v.remove(value);
	}
}
