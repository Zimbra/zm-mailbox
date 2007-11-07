/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

package com.zimbra.cs.index;

/**
 * A QueryTarget is something we run a query against, 
 * ie a mailbox
 */
class QueryTarget {

	public static QueryTarget UNSPECIFIED = new QueryTarget();
	public static QueryTarget LOCAL = new QueryTarget();
	
	// pseudo-target, must be optimized out before we try to execute!
	public static QueryTarget IS_REMOTE = new QueryTarget(); 
	
	private QueryTarget() { mTarget = null; }
	public QueryTarget(String target) {
		mTarget = target;
	}
	
	private String mTarget;
	
	public boolean isCompatibleLocal() {
	    return (this == UNSPECIFIED || this == LOCAL);
	}
	
	public boolean isCompatible(String targetAcctId) {
	    if (isCompatibleLocal())
	        return false;
	    if (this == IS_REMOTE)
	        return false;
	    return mTarget.equals(targetAcctId);
	}
	
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