package com.zimbra.common.stats;

public class HitRateCounter extends Counter {

	private long mLastCount = 0;
   	private long mLastTotal = 0;
    
   	/*
	 * return cache hit reflects real time hit rate in given interval, non-cumulative.see bug 85833.  
	 * assume the callers are always from ZimbraPerf periodical dump stats.  
	 */
    public double getAverage() {
        long deltaCount = 0; 
        long deltaTotal  = 0; 
        synchronized(this){
	     	long curCount = getCount();
	     	long curTotal = getTotal();
	     	deltaCount = curCount - mLastCount;
	     	deltaTotal = curTotal - mLastTotal;
	     	mLastCount = curCount;
	     	mLastTotal = curTotal;
        }
  
         if (deltaCount == 0) {
             return 0.0;
         } else {
             return (double) deltaTotal / (double) deltaCount;
         }
     }
}
