/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
