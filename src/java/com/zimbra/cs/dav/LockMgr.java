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
package com.zimbra.cs.dav;

import org.apache.commons.collections.map.LRUMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.zimbra.cs.dav.resource.DavResource;

public class LockMgr {
	private static LockMgr sInstance;
	public static LockMgr getInstance() {
		if (sInstance == null) {
			synchronized(LockMgr.class) {
				if (sInstance == null) {
					sInstance = new LockMgr();
				}
			}
		}
		return sInstance;
	}
	
	// map of resource to list of tokens
	private HashMap<DavResource,List<String>> mLockedResources;
	
	// map of token to lock
	private LRUMap mLocks;
	
	private LockMgr() {
		mLockedResources = new HashMap<DavResource,List<String>>();
		mLocks = new LRUMap(100);
	}
	
	public enum LockType {
		write
	}
	public enum LockScope {
		exclusive, shared
	}

	private static final int sDEFAULTTIMEOUT = 10 * 60 * 1000;
	private static final String sTIMEOUTINFINITE = "Infinite";
	private static final String sTIMEOUTSEC = "Second-";
	
	public static class Lock {
		public Lock(LockType t, LockScope s, int d, String o) {
			type = t; scope = s; depth = d; owner = o;
			expiration = System.currentTimeMillis() + sDEFAULTTIMEOUT;
		}
		public LockType type;
		public LockScope scope;
		public int depth;    // 0 -> 0, 1 -> infinity
		public String owner;
		public long expiration;
		public String token;
		public boolean isExpired() {
			return expiration < System.currentTimeMillis();
		}
		public String getTimeoutStr() {
			long timeoutInSec = expiration - System.currentTimeMillis();
			if (timeoutInSec < 0)
				return sTIMEOUTINFINITE;
			return sTIMEOUTSEC + timeoutInSec;
		}
	}
	
	public synchronized List<Lock> getLocks(DavResource rs) {
		List<Lock> locks = new ArrayList<Lock>();
		List<String> lockTokens = mLockedResources.get(rs);
		if (lockTokens != null) {
			for (String token : lockTokens) {
				@SuppressWarnings("unchecked")
				Lock l = (Lock)mLocks.get(token);
				if (l == null)
					continue;
				if (l.isExpired())
					locks.remove(l);
				else
					locks.add(l);
			}
		}
		return locks;
	}
	
	private static final String sTOKEN_PREFIX = "urn:uuid:";
	
	public synchronized Lock createLock(DavContext ctxt, DavResource rs, LockType type, LockScope scope, int depth) {
		Lock l = new Lock(type, scope, depth, ctxt.getAuthAccount().getName());
		l.token = sTOKEN_PREFIX + UUID.randomUUID().toString();
		
		List<String> locks = mLockedResources.get(rs);
		if (locks == null) {
			locks = new ArrayList<String>();
			mLockedResources.put(rs, locks);
		}
		locks.add(l.token);
		mLocks.put(l.token, l);
		return l;
	}
	
	public synchronized void deleteLock(DavContext ctxt, String token) {
		if (mLocks.containsKey(token)) {
			Lock l = (Lock)mLocks.get(token);
			if (l.owner.equals(ctxt.getAuthAccount().getName()))
				mLocks.remove(token);
		}
	}
}
