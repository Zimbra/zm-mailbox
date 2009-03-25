/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtil {

    
    /**
     * Out gets intersection of elements in lhs and rhs
     * 
     * @param out
     * @param lhs
     * @param rhs
     * @return
     */
    public static <T> Set<T> subtract(Set<T> lhs, Set<T> rhs) {
        HashSet<T> out = new HashSet<T>();
        
        for (T o : lhs) {
            if (!rhs.contains(o))
                out.add(o);
        }
        return out;
    }
    
    /**
     * Out gets the intersection of elements in lhs and rhs
     * 
     * @param out
     * @param lhs
     * @param rhs
     * @return
     */
    public static <T> Set<T> intersect(Set<T> lhs, Set<T> rhs) {
        HashSet<T> out = new HashSet<T>();
        
        for (T o : lhs) {
            if (rhs.contains(o))
                out.add(o);
        }
        return out;
    }
	
	/**
	 * Out gets the intersection of elements in lhs and rhs
	 * 
	 * @param out
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static <T> Set<T> intersect(Set<T> out, Set<T> lhs, Set<T> rhs) {
		
		for (T o : lhs) {
			if (rhs.contains(o))
				out.add(o);
		}
		return out;
	}
	
	/**
	 * Out gets the union of elements in lhs and rhs
	 * 
	 * @param out
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static <T> Set<T> union (Set<T> out, Set<T> lhs, Set<T> rhs) {
		
		for (T o : lhs) {
			out.add(o);
		}
		for (T o : rhs) {
			out.add(o);
		}
		return out;
	}
	
	/**
	 * Union into lhs 
	 * 
	 * @param out
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static <T> Set<T> union (Set<T> lhs, Set<T> rhs) {
		for (T o : rhs) {
			lhs.add(o);
		}
		return lhs;
	}
	
	
	
}
