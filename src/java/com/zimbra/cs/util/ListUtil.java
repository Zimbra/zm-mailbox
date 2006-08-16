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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.util.*;

public class ListUtil {
	
	public static boolean isEmpty(List l) {
		return (l == null || l.size() == 0);
	}
	
	public static boolean isEmpty(Collection c) {
		return (c == null || c.size() == 0);
	}
	
    
    /**
     * Merge two sorted Lists
     * 
     * I could have used the Java Set collection here...unfortunately the only ordered set
     * is their TreeSet, and set union/subtraction therefore take NlogN with a pretty big
     * constant overhead.  
     * 
     * @param dest
     * @param src
     */
    public static void mergeSortedLists(List dest, List[] /* Comparable */ src, boolean removeDuplicates)
    {
        int numSrc = 0;
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                numSrc++;
            }
        }
        
        if (numSrc == 1) {
            for (int i = 0; i < src.length; i++) {
                if (src[i] != null) {
                    dest.addAll(src[i]);
                    return;
                }
            }
        }
        
        Iterator iter[] = new Iterator[numSrc];
        int iterOffset = 0;
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                iter[iterOffset++] = src[i].iterator();
            }
        }
        
        
        
        int numItersActive = src.length;
        
        // holds the next values of each iterator
        Comparable nextValue[] = new Comparable[src.length];
        
        Comparable lowestValue = null;
        int lowestValueOffset = -1;
        
        Comparable lastAdded = null;

        // prime the pump
        for (int i = 0; i < iter.length; i++) {
            if (iter[i].hasNext()) {
                nextValue[i] = (Comparable)(iter[i].next());
                
                if (lowestValue == null || (lowestValue.compareTo(nextValue[i]) > 0)) {
                    lowestValue = nextValue[i];
                    lowestValueOffset = i;
                }
            } else {
                iter[i] = null;
                numItersActive--;
            }
        }
        
        while (numItersActive > 0) {
            // grab lowest value from the src list, put it on the return list
            if ((!removeDuplicates) || (lastAdded == null)) {
                dest.add(nextValue[lowestValueOffset]);
                lastAdded = nextValue[lowestValueOffset];
                nextValue[lowestValueOffset] = null;
            } else {
                if (!lastAdded.equals(nextValue[lowestValueOffset])) {
                    dest.add(nextValue[lowestValueOffset]);
                    lastAdded = nextValue[lowestValueOffset];
                }
                nextValue[lowestValueOffset] = null;
            }
            
            // iterate the proper src list
            if (iter[lowestValueOffset].hasNext()) {
                nextValue[lowestValueOffset] = (Comparable)(iter[lowestValueOffset].next());
            } else {
                iter[lowestValueOffset] = null;
                numItersActive--;
            }
            
            // find the next-lowest-value
            lowestValue = null;
            lowestValueOffset = -1;
            for (int i = 0; i < src.length; i++) {
                if (lowestValue == null || 
                        ((nextValue[i] != null) && (lowestValue.compareTo(nextValue[i]) > 0))) {
                    lowestValue = nextValue[i];
                    lowestValueOffset = i;
                }
            }
        }
    }
    
    /**
     * @param a
     * @param b
     * 
     * Subtract two sorted lists
     * 
     * returns (a-b);
     */
    public static List subtractSortedLists(List /* Comparable */ a, List /* Comparable */ b) {
        List toRet = new ArrayList();
        
        Iterator aIter = a.iterator();
        Iterator bIter = b.iterator();
        
        Comparable aVal=null;
        if (aIter.hasNext()) {
            aVal = (Comparable)aIter.next();
        }
        Comparable bVal=null;
        if (bIter.hasNext()) {
            bVal = (Comparable)bIter.next();
        }
        
        while (aVal != null) {
            if (bVal == null) {
                toRet.add(aVal);
            } else {
                int comp = aVal.compareTo(bVal);
                if (comp < 0) {
                    // a < b
                    toRet.add(aVal);
                } else if (comp > 0) {
                    // a > b
                    if (bIter.hasNext()) {
                        bVal = (Comparable)bIter.next();
                    } else {
                        bVal = null;
                    }
                    continue; // DON'T move A fwd...
                } else {
                    // a==b, so skip A!
                }
            }
            
            if (aIter.hasNext()) {
                aVal = (Comparable)aIter.next();
            } else {
                aVal = null;
            }
        }
        
        return toRet;
    }
    
    static private void testListUtil() {
        List<Integer>[] in = (ArrayList<Integer>[]) new List[5];

        int i = 0;
        
        in[i] = new ArrayList<Integer>();
        in[i].add(new Integer(1));
        in[i].add(new Integer(3));
        in[i].add(new Integer(5));
        in[i].add(new Integer(7));
        in[i].add(new Integer(9));
        
        i = 1;
        in[i] = new ArrayList<Integer>();
        in[i].add(new Integer(1));
        in[i].add(new Integer(7));
        in[i].add(new Integer(12));
        in[i].add(new Integer(13));
        in[i].add(new Integer(13));

        i = 2;
        in[i] = new ArrayList<Integer>();
        in[i].add(new Integer(1));
        in[i].add(new Integer(2));
        in[i].add(new Integer(3));
        in[i].add(new Integer(4));
        in[i].add(new Integer(5));
        

        i = 3;
        in[i] = new ArrayList<Integer>();
        in[i].add(new Integer(5));
        in[i].add(new Integer(6));
        in[i].add(new Integer(7));
        in[i].add(new Integer(8));
        in[i].add(new Integer(9));

        
        i = 4;
        in[i] = new ArrayList<Integer>();
        in[i].add(new Integer(100));
        in[i].add(new Integer(101));
        in[i].add(new Integer(102));
        in[i].add(new Integer(103));
        in[i].add(new Integer(104));
        
        
        List test;
        
        
        test = new ArrayList();
        mergeSortedLists(test, in, false);
        System.out.print("DUPES_NOT_REMOVED: ");
        for (Iterator iter = test.iterator(); iter.hasNext();) {
            Integer cur = (Integer)iter.next();
            System.out.print(cur+", ");
        }
        System.out.println();

        
        test = new ArrayList();
        mergeSortedLists(test, in, true);
        System.out.print("DUPES_REMOVED: ");
        for (Iterator iter = test.iterator(); iter.hasNext();) {
            Integer cur = (Integer)iter.next();
            System.out.print(cur+", ");
        }
        System.out.println();
        
        test = subtractSortedLists(in[2], in[0]);
        System.out.print("(1,2,3,4,5) - (1,3,5,7,9): ");
        for (Iterator iter = test.iterator(); iter.hasNext();) {
            Integer cur = (Integer)iter.next();
            System.out.print(cur+", ");
        }
        System.out.println();

        test = subtractSortedLists(in[0], in[1]);
        System.out.print("(1,3,5,7,9) - (1,7,12,13,13): ");
        for (Iterator iter = test.iterator(); iter.hasNext();) {
            Integer cur = (Integer)iter.next();
            System.out.print(cur+", ");
        }
        System.out.println();
        
    }
    
    
    

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        testListUtil();
    }

}
