/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A simple counter that maintains the count of unique values passed into the
 * {@link #increment} and {@link #decrement} methods.
 * 
 * @author bburtin
 */
public class ValueCounter<E> {

    private Map<E, Integer> mValues = new HashMap<E, Integer>();
    
    public void increment(E value) {
        increment(value, 1);
    }
    
    public void decrement(E value) {
        increment(value, -1);
    }
    
    public void increment(E value, int delta) {
        Integer count = mValues.get(value);
        if (count == null) {
            count = new Integer(delta);
        } else {
            count = new Integer(count.intValue() + delta);
        }
        mValues.put(value, count);
    }
    
    public int getCount(Object value) {
        Integer count = mValues.get(value);
        if (count == null) {
            return 0;
        }
        return count.intValue();
    }
    
    public Iterator<E> iterator() {
        return mValues.keySet().iterator();
    }
    
    public int size() {
        return mValues.size();
    }
    
    public int getTotal() {
        int total = 0;
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            total = total + getCount(i.next());
        }
        return total;
    }
    
    public void clear() {
        mValues.clear();
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator<E> i = iterator();
        while (i.hasNext()) {
            if (buf.length() != 0) {
                buf.append(", ");
            }
            Object value = i.next();
            buf.append(value + ": " + getCount(value));
        }
        return buf.toString();
    }
}
