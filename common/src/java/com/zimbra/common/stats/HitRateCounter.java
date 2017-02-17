/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
