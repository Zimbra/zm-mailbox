/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

/*
 * can't use legacy MultiMap interface together with generic java 1.5 interfaces.
 * 
import org.apache.commons.collections.MultiMap;
 */

@SuppressWarnings("serial")
public class MultiTreeMap<K,V> extends TreeMap<K,Collection<V>> /* implements MultiMap */ {

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
		if (v != null)
			return v.iterator().next();
		return null;
	}
	
	public void remove(K key, V value) {
		Collection<V> v = super.get(key);
		if (v != null)
			v.remove(value);
	}
}
