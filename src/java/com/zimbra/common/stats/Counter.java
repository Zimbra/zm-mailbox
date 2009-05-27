/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
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
package com.zimbra.common.stats;

/**
 * Tracks a total and count (number of calls to {@link #increment}).
 */
public class Counter {

    private volatile long mCount = 0;
    private volatile long mTotal = 0;
    
    public long getCount() {
        return mCount;
    }
    
    public long getTotal() { 
        return mTotal;
    }

    /**
     * Returns the average since the last
     * call to {@link #reset}.
     */
    public synchronized double getAverage() {
        if (mCount == 0) {
            return 0.0;
        } else {
            return (double) mTotal / (double) mCount;
        }
    }

    /**
     * Increments the total by the specified value.  Increments the count by 1.
     */
    public synchronized void increment(long value) {
        mCount++;
        mTotal += value;
    }

    /**
     * Increments the count and total by 1.  
     */
    public synchronized void increment() {
        increment(1);
    }
    
    public synchronized void reset() {
        mCount = 0;
        mTotal = 0;
    }
}
