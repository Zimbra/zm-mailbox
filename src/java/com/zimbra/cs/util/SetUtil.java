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
