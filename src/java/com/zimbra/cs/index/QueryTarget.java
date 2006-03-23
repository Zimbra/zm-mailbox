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

package com.zimbra.cs.index;

/**
 * A QueryTarget is something we run a query against, 
 * ie a mailbox
 */
class QueryTarget {

	public static QueryTarget UNSPECIFIED = new QueryTarget();
	public static QueryTarget LOCAL = new QueryTarget();
	
	private QueryTarget() { mTarget = null; }
	public QueryTarget(String target) {
		mTarget = target;
	}
	
	private String mTarget;
	
	public String toString() {
		if (this == UNSPECIFIED)
			return "UNSPECIFIED";
		else if (this == LOCAL) 
			return "LOCAL";
		else 
			return mTarget;
	}
	
	public boolean equals(Object other) {
		if (other == this)
			return true;
			
		if (other != null && other.getClass() == this.getClass()) {
			QueryTarget o = (QueryTarget)other;

			// one of us is a "special" instance, so just normal equals
			if (mTarget == null || o.mTarget == null)
				return this==other;
			
			// compare folders
			return mTarget.equals(o.mTarget);
		}
		return false;
	}
	
	public int hashCode() {
		if (mTarget != null)
			return mTarget.hashCode();
		else
			return 0;
	}
	
}