/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.gal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ldap.LdapUtil;

public class GalSyncToken {
	public GalSyncToken(String token) {
		parse(token);
	}

	public GalSyncToken(String ldapTs, String accountId, int changeId) {
		mLdapTimestamp = ldapTs;
		mChangeIdMap = new HashMap<String,String>();
		mChangeIdMap.put(accountId, "" + changeId);
	}
	
	static final String LDAP_GENERALIZED_TIME_FORMAT = DateUtil.ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT;
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
	
	public String getLdapTimestamp(String format) throws ServiceException {
	    // mLdapTimestamp should be always in this format
	    SimpleDateFormat standardFormat = new SimpleDateFormat(DateUtil.ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT);
	    SimpleDateFormat fmt = new SimpleDateFormat(format);
	    try {
		Date ts = standardFormat.parse(mLdapTimestamp);
		return fmt.format(ts); 
	    } catch (ParseException e) {
		// throw ServiceException.INVALID_REQUEST("invalid sync token:" + mLdapTimestamp, e);
		// can't parse, just return the original
		return mLdapTimestamp;
	    }
	}
	
	public int getChangeId(String accountId) {
		String cid = mChangeIdMap.get(accountId);
		if (cid != null)
			return Integer.parseInt(cid);
		return 0;
	}
	
	public boolean doMailboxSync() {
		return mLdapTimestamp.length() == 0 || mChangeIdMap.size() > 0;
	}
	
	public boolean isEmpty() {
	    return mLdapTimestamp.length() == 0 && mChangeIdMap.size() == 0;
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
