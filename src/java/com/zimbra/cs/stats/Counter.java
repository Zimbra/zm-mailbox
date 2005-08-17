package com.zimbra.cs.stats;

public class 
Counter extends Accumulator 
{
    private String mUnit;
    
    private long mCount = 0;
    
    public synchronized void increment() {
        mCount++;
    }

    public synchronized void increment(int size) {
        mCount += size;
    }

    protected String getLabel(int column) {
        return getName() + ":total:" + mUnit;
    }
    
    protected String getData(int column) {
        return Long.toString(mCount);
    }
    
    protected int numColumns() {
        return 1;
    }
    
    Counter() {
    }
    
    public static Counter getInstance(String module, String unit) {
        Counter c = (Counter)getInstance(module, Counter.class);
        c.mUnit = unit;
        return c;
    }

    public static Counter getInstance(String module) {
        return getInstance(module, "count");
    }
}
