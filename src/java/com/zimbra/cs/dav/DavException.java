/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav;

@SuppressWarnings("serial")
public class DavException extends Exception {
	private boolean mStatusIsSet;
	private int mStatus;
	
	public DavException(String msg, Throwable cause) {
		super(msg, cause);
		mStatusIsSet = false;
	}
	
	public DavException(String msg, int status, Throwable cause) {
		super(msg, cause);
		mStatus = status;
		mStatusIsSet = true;
	}

	public boolean isStatusSet() {
		return mStatusIsSet;
	}
	
	public int getStatus() {
		return mStatus;
	}
}
