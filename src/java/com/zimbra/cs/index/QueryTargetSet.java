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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
