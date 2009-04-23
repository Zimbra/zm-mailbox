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

import java.util.HashMap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.LdapUtil;

public class GalSyncToken {
	public GalSyncToken(String token) {
		parse(token);
	}

	public GalSyncToken(String ldapTs, String accountId, int changeId) {
		mLdapTimestamp = ldapTs;
		mChangeIdMap = new HashMap<String,String>();
		mChangeIdMap.put(accountId, "" + changeId);
	}
	
	private String mLdapTimestamp;
	private HashMap<String,String> mChangeIdMap;
	
	private void parse(String token) {
		mChangeIdMap = new HashMap<String,String>();
		int pos = token.indexOf(':');
		if (pos == -1) {
			// old style LDAP timestamp token
			mLdapTimestamp = token;
			return;
		}
		mLdapTimestamp = token.substring(0, pos);
		boolean finished = false;
		while (!finished) {
			token = token.substring(pos+1);
			int sep = token.indexOf(':');
			if (sep == -1)
				return;
			String key = token.substring(0, sep);
			String value = null;
			pos = token.indexOf(':', sep+1);
			if (pos == -1) {
				finished = true;
				value = token.substring(sep+1);
			} else {
				value = token.substring(sep+1, pos);
			}
			mChangeIdMap.put(key, value);
		}
	}
	
	public String getLdapTimestamp() {
		return mLdapTimestamp;
	}
	
	public int getChangeId(String accountId) {
		String cid = mChangeIdMap.get(accountId);
		if (cid != null)
			return Integer.parseInt(cid);
		return 0;
	}
	
	public boolean doMailboxSync() {
		return mChangeIdMap.size() > 0;
	}
	
	public void merge(GalSyncToken that) {
		ZimbraLog.gal.debug("merging token "+this+" with "+that);
		mLdapTimestamp = LdapUtil.getEarlierTimestamp(this.mLdapTimestamp, that.mLdapTimestamp);
		for (String aid : that.mChangeIdMap.keySet())
			mChangeIdMap.put(aid, that.mChangeIdMap.get(aid));
		ZimbraLog.gal.debug("result: "+this);
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder(mLdapTimestamp);
		for (String aid : mChangeIdMap.keySet())
			buf.append(":").append(aid).append(":").append(mChangeIdMap.get(aid));
		return buf.toString();
	}
}
