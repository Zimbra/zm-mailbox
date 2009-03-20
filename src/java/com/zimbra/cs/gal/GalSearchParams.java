/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.cs.gal;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;

public class GalSearchParams {
	private GalSearchConfig mConfig;
	private Provisioning.GAL_SEARCH_TYPE mType;
	private int mLimit;
	private int mPageSize;
	private String mToken;
	private SearchGalResult mResult;
	
	public GalSearchConfig getConfig() {
		return mConfig;
	}
	
	public Provisioning.GAL_SEARCH_TYPE getType() {
		return mType;
	}

	public int getLimit() {
		return mLimit;
	}

	public int getPageSize() {
		return mPageSize;
	}
	
	public String getSyncToken() {
		return mToken;
	}
	
	public SearchGalResult getResult() {
		return mResult;
	}
}
