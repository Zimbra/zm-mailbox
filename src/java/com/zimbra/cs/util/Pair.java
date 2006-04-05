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

import java.util.HashMap;
import java.util.Map;

public class Pair<F,S> implements Comparable {
	private F mFirst;
	private S mSecond;
	
	private static Map sPairMap = new HashMap();
	
	public static <F,S> Pair<F,S> get(F first, S second) {
		if (first == null || second == null)
			throw new IllegalArgumentException("null parameter not allowed");
		Map<S,Pair<F,S>> sub = (Map<S,Pair<F,S>>)(sPairMap.get(first));
		if (sub == null) {
			sub = new HashMap<S,Pair<F,S>>();
			sPairMap.put(first, sub);
		}
		Pair<F,S> val = sub.get(second);
		if (val == null) {
			val = new Pair<F,S>(first, second);
			sub.put(second, val);
		}
		return val;
	}
	
	private Pair(F first, S second) {
		assert(first != null && second != null);
		mFirst = first;
		mSecond = second;
	}
	
	public F getFirst() {
		return mFirst;
	}
	public S getSecond() {
		return mSecond;
	}
	public F car() {
		return getFirst();
	}
	public S cdr() {
		return getSecond();
	}
	public int compareTo(Object obj) {
		if (!(obj instanceof Pair)) {
			return 1;
		}
		Pair that = (Pair) obj;
		if (mFirst.equals(that.mFirst) &&
			mSecond.equals(that.mSecond)) {
			return 0;
		}
		return 1;
	}
}
