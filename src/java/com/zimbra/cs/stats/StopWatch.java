package com.liquidsys.coco.stats;

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