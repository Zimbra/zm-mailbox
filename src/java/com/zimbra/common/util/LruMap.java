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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * {@code Map} implementation that retains a maximum number of entries.  Entries
 * are aged out by access order. 
 *
 * @param <K> key
 * @param <V> value
 */
@SuppressWarnings("serial")
public class LruMap<K, V>
extends LinkedHashMap<K, V> {
    
    private int maxSize;
    
    public LruMap(int maxSize) {
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        boolean willRemove = size() > maxSize;
        if (willRemove) {
            willRemove(eldest.getKey(), eldest.getValue());
        }
        return willRemove;
    }

    /**
     * Override to handle an entry that will be removed from the map.
     */
    protected void willRemove(K key, V value) {
    }
}