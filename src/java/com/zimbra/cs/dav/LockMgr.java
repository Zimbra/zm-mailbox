/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav;

import com.zimbra.common.util.MapUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RFC 2518bis section 6.
 * 
 * We don't support locking with depth infinity.  All the locks are
 * implemented as advisory, with relatively short timeout of 10 mins.
 * The server keeps track of the most recent 100 locks only.
 * 
 * @author jylee
 *
 */
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
	
	// map of resource path to list of tokens
	private HashMap<String,List<String>> mLockedResources;
	
	// map of token to lock
	private Map mLocks;
	
	private LockMgr() {
		mLockedResources = new HashMap<String,List<String>>();
		mLocks = MapUtil.newLruMap(100);
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
		public Lock(LockType t, LockScope s, String d, String o) {
			type = t; scope = s; depth = d; owner = o;
			expiration = System.currentTimeMillis() + sDEFAULTTIMEOUT;
		}
		public LockType type;
		public LockScope scope;
		public String depth;
		public String owner;
		public long expiration;
		public String token;
		public boolean isExpired() {
			return expiration < System.currentTimeMillis();
		}
		public String getTimeoutStr() {
			long timeoutInSec = (expiration - System.currentTimeMillis()) / 1000;
			if (timeoutInSec < 0)
				return sTIMEOUTINFINITE;
			return sTIMEOUTSEC + timeoutInSec;
		}
	}
	
	public synchronized List<Lock> getLocks(String path) {
		List<Lock> locks = new ArrayList<Lock>();
		List<String> lockTokens = mLockedResources.get(path);
		if (lockTokens != null) {
			for (String token : lockTokens) {
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
	
	private synchronized Lock hasLock(String owner, String path, LockType type, LockScope scope) throws DavException {
		for (Lock l : getLocks(path)) {
			if (l == null)
				continue;
			else if (l.owner.compareTo(owner) == 0)
				return l;
			else if (scope == LockScope.exclusive)
				throw new DavException("already locked "+path, DavProtocol.STATUS_LOCKED);
			else if (scope == LockScope.shared && l.scope == LockScope.exclusive)
				throw new DavException("shared lock exists "+path, DavProtocol.STATUS_LOCKED);
		}
		return null;
	}
	
	public synchronized Lock createLock(DavContext ctxt, String owner, String path, LockType type, LockScope scope, String depth) throws DavException {
		Lock l = hasLock(owner, path, type, scope);
		if (l != null)
			return l;
		l = new Lock(type, scope, depth, owner);
		l.token = sTOKEN_PREFIX + UUID.randomUUID().toString();
		
		List<String> locks = mLockedResources.get(path);
		if (locks == null) {
			locks = new ArrayList<String>();
			mLockedResources.put(path, locks);
		}
		locks.add(l.token);
		mLocks.put(l.token, l);
		return l;
	}
	
	public synchronized void deleteLock(DavContext ctxt, String path, String token) {
		List<String> locks = mLockedResources.get(path);
		if (locks == null)
			return;
		if (!locks.contains(token))
			return;
		if (mLocks.containsKey(token)) {
            mLocks.remove(token);
            locks.remove(token);
		}
	}
}
