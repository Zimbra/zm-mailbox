/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
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

import org.apache.commons.collections.map.LRUMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.zimbra.cs.dav.resource.DavResource;

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
	
	private synchronized boolean canLock(DavResource rs, LockType type, LockScope scope) {
		List<String> locks = mLockedResources.get(rs);
		if (locks != null) {
			for (String token : locks) {
				Lock l = (Lock)mLocks.get(token);
				if (l == null)
					continue;
				else if (scope == LockScope.exclusive)
					return false;
				else if (scope == LockScope.shared && l.scope == LockScope.exclusive)
					return false;
			}
		}
		return true;
	}
	
	public synchronized Lock createLock(DavContext ctxt, DavResource rs, LockType type, LockScope scope, int depth) throws DavException {
		if (!canLock(rs, type, scope))
			throw new DavException("can't lock the resource "+rs.getUri(), DavProtocol.STATUS_LOCKED);
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
	
	public synchronized void deleteLock(DavContext ctxt, DavResource rs, String token) {
		List<String> locks = mLockedResources.get(rs);
		if (locks == null)
			return;
		if (!locks.contains(token))
			return;
		if (mLocks.containsKey(token)) {
			Lock l = (Lock)mLocks.get(token);
			if (l.owner.equals(ctxt.getAuthAccount().getName())) {
				mLocks.remove(token);
				locks.remove(token);
			}
		}
	}
}
