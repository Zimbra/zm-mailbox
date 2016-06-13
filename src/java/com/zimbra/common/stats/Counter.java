/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
