package com.zimbra.cs.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtil {

	
	/**
	 * Out gets the intersection of elements in lhs and rhs
	 * 
	 * @param out
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	static public HashSet intersect(Set<? extends Object> lhs, Set<? extends Object> rhs) 
	{
		HashSet out = new HashSet();
		
		for (Object o : lhs) {
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
	static public Set intersect(Set out, Set<? extends Object> lhs, Set<? extends Object> rhs) {
		
		for (Object o : lhs) {
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
	static public Set union (Set out, Set<? extends Object> lhs, Set<? extends Object> rhs) {
		
		for (Object o : lhs) {
			out.add(o);
		}
		for (Object o : rhs) {
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
	static public Set union (Set lhs, Set<? extends Object> rhs) 
	{
		for (Object o : rhs) {
			lhs.add(o);
		}
		return lhs;
	}
	
	
	
}
