/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

public class 
StopWatch extends Accumulator
{
    public static final StopWatch getInstance(String module) {
        StopWatch ta = (StopWatch)getInstance(module, StopWatch.class);
        return ta;
    }

    private long mNumTimes = 0;
    private long mTotalTime = 0;
    private long mPrevNumTimes = 0;
    private long mPrevClock = 0;

    public long start() { 
        return System.currentTimeMillis();
    }
    
    public synchronized void stop(long startTime) {
        mNumTimes++;
        mTotalTime += System.currentTimeMillis() - startTime;
    }
    
    protected String getLabel(int column) {
        StringBuffer ret = new StringBuffer(getName());
        switch (column) {
        case 0:
            ret.append(":total:count");
            break;
        case 1:
            ret.append(":total:millis");
            break;
        case 2:
            ret.append(":interval:ops/sec");
            break;
        default:
            throw new IllegalArgumentException();
        }
        return ret.toString();
    }
    
    protected synchronized String getData(int column) {
        switch (column) {
        case 0:
            return Long.toString(mNumTimes);
        case 1:
            return Long.toString(mTotalTime);
        case 2:
            long now = System.currentTimeMillis();
            long rateInLastInterval = 0;
            if ((now - mPrevClock) != 0) {
                rateInLastInterval = (1000 * (mNumTimes - mPrevNumTimes)) / (now - mPrevClock);
            }
            mPrevNumTimes = mNumTimes;
            mPrevClock = now;
            return Long.toString(rateInLastInterval);
        default:
            throw new IllegalArgumentException();
        }
    }
    
    protected int numColumns() {
        return 3;
    }
}