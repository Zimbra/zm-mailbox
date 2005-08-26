package com.zimbra.cs.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A simple counter that maintains the count of unique values passed into the
 * {@link #increment} and {@link #decrement} methods.
 * 
 * @author bburtin
 */
public class ValueCounter {

    private Map /* <Object, Integer> */ mValues = new HashMap();
    
    public void increment(Object value) {
        increment(value, 1);
    }
    
    public void decrement(Object value) {
        increment(value, -1);
    }
    
    public void increment(Object value, int delta) {
        Integer count = (Integer) mValues.get(value);
        if (count == null) {
            count = new Integer(delta);
        } else {
            count = new Integer(count.intValue() + delta);
        }
        mValues.put(value, count);
    }
    
    public int getCount(Object value) {
        Integer count = (Integer) mValues.get(value);
        if (count == null) {
            return 0;
        }
        return count.intValue();
    }
    
    public Iterator iterator() {
        return mValues.keySet().iterator();
    }
    
    public int size() {
        return mValues.size();
    }
    
    public int getTotal() {
        int total = 0;
        Iterator i = iterator();
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
        Iterator i = iterator();
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
