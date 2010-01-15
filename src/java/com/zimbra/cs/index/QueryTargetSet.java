/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.HashSet;

class QueryTargetSet extends HashSet<QueryTarget> 
{
	public QueryTargetSet() {
	}
	
	public QueryTargetSet(int size) {
		super(size);
	}
	
	/**
	 * Used in the optimize() pathway, count the number of explicit
	 * QueryTarget's (ie don't count "unspecified") 
	 * @return
	 */
	int countExplicitTargets() {
		int toRet = 0;
		for (QueryTarget q : this) {
			if (q != QueryTarget.UNSPECIFIED)
				toRet++;
		}
		return toRet;		
	}
	
	boolean isSubset(QueryTargetSet other) {
		for (QueryTarget t : this) {
			if (!other.contains(t))
				return false;
		}
		return true;
	}

	boolean hasExternalTargets() {
		for (QueryTarget t : this) {
			if (t != QueryTarget.UNSPECIFIED && t != QueryTarget.LOCAL)
				return true;
		}
		return false;
	}
	
}
