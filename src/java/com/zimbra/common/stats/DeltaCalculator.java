/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates changes to the total, count, and average
 * for the wrapped <tt>Counter</tt> between subsequent calls to {@link #reset}.
 */
public class DeltaCalculator
implements Accumulator {

    private Counter mCounter;
    private long mLastCount = 0;
    private long mSecondToLastCount = 0;
    private long mLastTotal = 0;
    private long mSecondToLastTotal = 0;
    
    private String mCountName;
    private String mTotalName;
    private String mAverageName;
    private String mRealtimeAverageName;
    
    private List<String> mNames;
    
    public DeltaCalculator(Counter counter) {
        mCounter = counter;
    }

    /**
     * If non-null, the given name will be returned by {@link #getNames()}
     * and the count value will be returned by {@link #getData()}.
     */
    public DeltaCalculator setCountName(String name) {
        mCountName = name;
        updateNames();
        return this;
    }
    
    /**
     * If non-null, the given name will be returned by {@link #getNames()}
     * and the total value will be returned by {@link #getData()}.
     */
    public DeltaCalculator setTotalName(String name) {
        mTotalName = name;
        updateNames();
        return this;
    }
    
    /**
     * If non-null, the given name will be returned by {@link #getNames()}
     * and the average value will be returned by {@link #getData()}.
     */
    public DeltaCalculator setAverageName(String name) {
        mAverageName = name;
        updateNames();
        return this;
    }
    
    /**
     * If non-null, the given name will be returned by {@link #getNames()}
     * and the realtime average value will be returned by {@link #getData()}.
     */
    public DeltaCalculator setRealtimeAverageName(String name) {
        mRealtimeAverageName = name;
        updateNames();
        return this;
    }
    
    private void updateNames() {
        ArrayList<String> names = new ArrayList<String>();
        if (mCountName != null) {
            names.add(mCountName);
        }
        if (mTotalName != null) {
            names.add(mTotalName);
        }
        if (mAverageName != null) {
            names.add(mAverageName);
        }
        if (mRealtimeAverageName != null) {
            names.add(mRealtimeAverageName);
        }
        mNames = Collections.unmodifiableList(names);
    }

    public void reset() {
        synchronized (mCounter) {
            mSecondToLastCount = mLastCount;
            mSecondToLastTotal = mLastTotal;
            mLastCount = mCounter.getCount();
            mLastTotal = mCounter.getTotal();
        }
    }

    /**
     * Returns the total since the last call to {@link #reset}.
     */
    public long getTotal() {
        synchronized (mCounter) {
            return mCounter.getTotal() - mLastTotal;
        }
    }

    /**
     * Returns the count since the last call to {@link #reset}.
     */
    public long getCount() {
        synchronized (mCounter) {
            return mCounter.getCount() - mLastCount;
        }
    }
    
    /**
     * Returns the average since the last call to {@link #reset}.
     */
    public double getAverage() {
        long count = 0;
        long total = 0;
        
        synchronized (mCounter) {
            count = mCounter.getCount() - mLastCount;
            if (count == 0) {
                return 0;
            }
            total = mCounter.getTotal() - mLastTotal;
        }

        return (double) total / (double) count; 
    }
    
    /**
     * Returns the average since the second-to-last call to
     * {@link #reset}.  We use the second-to-last call in order
     * to avoid spikes at the beginning of the interval.
     */
    public double getRealtimeAverage() {
        long count = 0;
        long total = 0;
        
        synchronized (mCounter) {
            count = mCounter.getCount() - mSecondToLastCount;
            if (count == 0) {
                return 0;
            }
            total = mCounter.getTotal() - mSecondToLastTotal;
        }

        return (double) total / (double) count; 
    }

    ////////////// Accumulator implementation ///////////////////
    
    public List<Object> getData() {
        if (mNames == null) {
            return Collections.emptyList();
        }
        
        List<Object> data = new ArrayList<Object>(mNames.size());
        synchronized (mCounter) {
            if (mCountName != null) {
                data.add(getCount());
            }
            if (mTotalName != null) {
                data.add(getTotal());
            }
            if (mAverageName != null) {
                data.add(getAverage());
            }
        }
        return data;
    }

    public List<String> getNames() {
        return mNames;
    }
}
