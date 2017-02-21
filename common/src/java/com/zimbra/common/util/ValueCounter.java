/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
            count = Integer.valueOf(delta);
        } else {
            count = Integer.valueOf(count.intValue() + delta);
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

    @Override
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
