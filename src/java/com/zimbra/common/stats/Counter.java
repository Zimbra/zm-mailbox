/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks a total and count (number of calls to {@link #increment}).
 */
public class Counter {

    private AtomicLong mCount = new AtomicLong();
    private AtomicLong mTotal = new AtomicLong();
    
    public long getCount() {
        return mCount.longValue();
    }
    
    public long getTotal() { 
        return mTotal.longValue();
    }

    /**
     * Returns the average since the last
     * call to {@link #reset}.
     */
    public synchronized double getAverage() {
        if (mCount.longValue() == 0) {
            return 0.0;
        } else {
            return (double) mTotal.longValue() / (double) mCount.longValue();
        }
    }

    /**
     * Increments the total by the specified value.  Increments the count by 1.
     */
    public void increment(long value) {
        mCount.getAndIncrement();
        mTotal.getAndAdd(value);
    }

    /**
     * Increments the count and total by 1.  
     */
    public void increment() {
        increment(1);
    }
    
    public synchronized void reset() {
        mCount.set(0);
        mTotal.set(0);
    }
}
