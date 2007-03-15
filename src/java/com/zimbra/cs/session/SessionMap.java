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
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

/**
 * Complex data structure used by the @link{SessionCache} for tracking active sessions. It supports three basic things:
 * 
 *   - Fast lookup/add/remove of session by {AccountID,SessionID} pair
 *   
 *   - Fast mapping AccountId-->{set of active sessions for that account}
 *   
 *   - Fast pruning of session based on last-access-time and session-specific timeout value
 * 
 */
final class SessionMap {
    private final Session.Type mType;

    SessionMap(Session.Type type) { 
        mType = type; 
        mAcctSessionMap = new LinkedHashMap<String, AccountSessionMap>(100);
        mSessionAccessSet = new HashMap<Long, HashSet<Session>>();
    }
    
    public Session.Type getType() { return mType; }
    
    /** @return the number of unique accounts with active sessions */
    public synchronized int totalActiveAccounts() { return mAcctSessionMap.size(); }
    
    /** @return total number of sessions in the cache */
    public synchronized int totalActiveSessions() { return mTotalActiveSessions; }

    public synchronized Collection<AccountSessionMap> activeAccounts() { return Collections.unmodifiableCollection(mAcctSessionMap.values()); }
    
    public synchronized Session get(String accountId, String sessionId) {
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        if (acctMap != null) {
            Session toRet = acctMap.get(sessionId);
            if (toRet != null) {
                updateAccessTime(toRet);
            }
            return toRet;
        } else {
            return null;
        }
    }
    
    public synchronized int countActiveSessions(String accountId) {
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        if (acctMap != null) {
            return mAcctSessionMap.size();
        } else {
            return 0;
        }
    }
    
    public synchronized Session remove(String accountId, String sessionId) {
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        if (acctMap != null) {
            Session removed = acctMap.remove(sessionId);
            if (removed != null) {
                long removedTimeout = removed.getSessionIdleLifetime();
                HashSet<Session> set = getSessionAccessSet(removedTimeout);
                assert(set.contains(removed));
                set.remove(removed);
                mTotalActiveSessions--;
                if (set.isEmpty()) 
                    mSessionAccessSet.remove(removedTimeout);
                if (acctMap.isEmpty())
                    mAcctSessionMap.remove(accountId);
            }
            return removed;
        } else
            return null;
    }
    
    public synchronized Session putAndPrune(String accountId, String sessionId, Session session, int maxSessionsPerAcct) {
        Session oldValue = put(accountId, sessionId, session);
        assert(session!=null);
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        while (acctMap.size() > maxSessionsPerAcct) {
            long leastRecent = Long.MAX_VALUE;
            String leastRecentId = null;
            
            for (Session s : acctMap.values()) {
                if (s.getLastAccessTime() < leastRecent) {
                    leastRecent = s.getLastAccessTime();
                    leastRecentId = s.getSessionId();
                }
            }
            assert(leastRecentId != null);
            int prevSize = acctMap.size();
            remove(accountId, leastRecentId);
            assert(acctMap.size() < prevSize);
        }
        return oldValue;
    }
    
    public synchronized Session put(String accountId, String sessionId, Session session) {
        assert(session!=null);
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        if (acctMap == null) {
            acctMap = new AccountSessionMap();
            mAcctSessionMap.put(accountId, acctMap);
        }
        assert(!acctMap.containsKey(sessionId));
        mTotalActiveSessions++; // new session!
        Session oldValue = acctMap.put(sessionId, session);
        updateAccessTime(session);
        return oldValue;
    }
    
    synchronized private void updateAccessTime(Session session) {
        HashSet<Session> set = getSessionAccessSet(session.getSessionIdleLifetime());
        set.remove(session);
        session.updateAccessTime();
        set.add(session);
    }
    
    /**
     * @return
     */
    synchronized List<Session> copySessionList() {
        // return a shallow-copy of the list of sessions
        List<Session> toRet = new ArrayList<Session>(mTotalActiveSessions);
        
        for (HashSet<Session> set : mSessionAccessSet.values()) {
            toRet.addAll(set);
        }
        return toRet;
    }

    
    /**
     * Prune sessions in the cache based on their last access time, ignoring
     * the session timeout.  This API is used for testing.
     * 
     * @return A list of sessions which have been removed from the cache,
     *         caller is responsible for calling session.doCleanup() on all 
     *         the listed sessions
     */
    synchronized List<Session> pruneSessionsByTime(long cutoffTime) {
        return pruneSessionsInternal(false, cutoffTime);
    }
    
    /**
     * Prune ALLS sessions in the cache.
     * This API is used for shutdown
     * 
     * @return A list of sessions which have been removed from the cache,
     *         caller is responsible for calling session.doCleanup() on all 
     *         the listed sessions
     */
    synchronized List<Session> pruneAllSessions() {
        return pruneSessionsInternal(true, -1);
    }
    
    /**
     * Prune idle sessions from the cache, returning a list of them for cleanup.
     * This API is used for shutdown
     * 
     * @return A list of sessions which have been removed from the cache,
     *         caller is responsible for calling session.doCleanup() on all 
     *         the listed sessions
     */
    synchronized List<Session> pruneIdleSessions() {
        return pruneSessionsInternal(false, -1);
    }
    
    synchronized private List<Session> pruneSessionsInternal(boolean all, long cutoffIn) {
        List<Session> toRet = new ArrayList<Session>();
        long now = System.currentTimeMillis();
        
        for (Map.Entry<Long, HashSet<Session>> entry : mSessionAccessSet.entrySet()) {
            long cutoff = cutoffIn;
            if (all) {
                cutoff= Long.MAX_VALUE;
            } else {
                if (cutoff == -1)
                    cutoff = now - entry.getKey();
            }
            
            for (Iterator<Session> iter = entry.getValue().iterator(); iter.hasNext();) {
                Session s = iter.next();
                if (!s.accessedAfter(cutoff)) {
                    toRet.add(s);
                    iter.remove();
                    mTotalActiveSessions--;
                    AccountSessionMap acctMap = mAcctSessionMap.get(s.getAccountId());
                    Session removed = acctMap.remove(s.getSessionId());
                    assert(removed == s);
                    if (removed != null) {
                        if (acctMap.isEmpty())
                            mAcctSessionMap.remove(s.getAccountId());
                    }
                } else {
                    // iter is last-access order, so we know we can quit now
                    break;
                }
            }
        }
        return toRet;
    }
    
    private HashMap<String /*accountId*/, AccountSessionMap> mAcctSessionMap;
    
    /**
     * Because different types of sessions can have different timeout values, we can't just store 
     * all the sessions in a big access-ordered list for quick timeout -- we'd have to iterate
     * all active sessions, which could potentially be bad.  Instead, we store sessions in a separate
     * cache depending on their timeout length.
     */
    private HashMap<Long, HashSet<Session>> mSessionAccessSet;
    private int mTotalActiveSessions = 0;
    
    /**
     * Return the LinkedHashSet of sessions for this session Type
     * @param timeout This session's timeout
     * @return
     */
    synchronized private HashSet<Session> getSessionAccessSet(Long timeout) {
        HashSet<Session> toRet = mSessionAccessSet.get(timeout);
        if (toRet == null) {
            toRet = new LinkedHashSet<Session>();
            mSessionAccessSet.put(timeout, toRet);
        }
        return toRet;
    }
    
    static boolean unitTest() {
        try {
            SessionMap map = new SessionMap(Session.Type.NULL);
            
            Session s1 =   new AdminSession("a1", "s1");
            Session s1_2 = new AdminSession("a1", "s2");
            Session s2 =   new AdminSession("a2", "s1");
            Session s3 =   new AdminSession("a3", "s1");
            Session s4 =   new AdminSession("a4", "s1");
            Session s5 =   new AdminSession("a5", "s1");
            Session s5_2 = new AdminSession("a5", "s2");
            
            map.put("a1", "s1", s1);
            map.put("a1", "s2", s1_2);
            // sleep ensures the system clock goes fwd, important since
            // we're testing things that work based on last access time
            try { Thread.sleep(10);} catch (Exception e) {};
            map.put("a2", "s1", s2);
            try { Thread.sleep(10);} catch (Exception e) {};
            long afterA2 = System.currentTimeMillis();
            try { Thread.sleep(10);} catch (Exception e) {};
            map.put("a3", "s1", s3);
            map.put("a4", "s1", s4);
            map.put("a5", "s1", s5);
            map.put("a5", "s2", s5_2);
            try { Thread.sleep(10);} catch (Exception e) {};
            try { Thread.sleep(10);} catch (Exception e) {};

            if (map.totalActiveAccounts() != 5) { throw ServiceException.FAILURE("Wrong # accounts active", null); };
            if (map.totalActiveSessions() != 7) { throw ServiceException.FAILURE("Wrong # active sessions", null); };
            
            Session check = map.get("a2", "s1");
            if (check == null) { throw ServiceException.FAILURE("Couldn't find a2_s1 in map", null);};
            
            if (map.totalActiveAccounts() != 5) { throw ServiceException.FAILURE("Wrong # accounts active", null); };
            if (map.totalActiveSessions() != 7) { throw ServiceException.FAILURE("Wrong # active sessions", null); };

            // map should be (access-order): a1_s2, s1_s2, a3_s1, ... a2_s1
            List<Session> removed = map.pruneSessionsByTime(afterA2);
            if (removed.isEmpty()) { throw ServiceException.FAILURE("Didn't get anything removing afterA2", null);};
            boolean hasA1S1 = false;
            boolean hasA1S2 = false;
            boolean hasA2 = false;
            for (Session s : removed) {
                if (s.getAccountId().equals("a1")) {
                    if (s.getSessionId().equals("s1"))
                        hasA1S1 = true;
                    if (s.getSessionId().equals("s2"))
                        hasA1S2 = true;
                }
                if (s.getAccountId().equals("a2"))
                    hasA2 = true;
            }
            if (!hasA1S1)
                throw ServiceException.FAILURE("Missing a1_s1!", null);
            if (!hasA1S2)
                throw ServiceException.FAILURE("Missing a1_s2!", null);
            if (hasA2)
                throw ServiceException.FAILURE("Found a2 removing afterA2", null);                        
            
            if (map.totalActiveAccounts() != 4) { throw ServiceException.FAILURE("Wrong # accounts active", null); };
            if (map.totalActiveSessions() != 5) { throw ServiceException.FAILURE("Wrong # active sessions", null); };
            map.remove("a5", "s1"); 
            if (map.totalActiveAccounts() != 4) { throw ServiceException.FAILURE("Wrong # accounts active", null); };
            if (map.totalActiveSessions() != 4) { throw ServiceException.FAILURE("Wrong # active sessions", null); };

            map.remove("a5", "s2"); 
            if (map.totalActiveAccounts() != 3) { throw ServiceException.FAILURE("Wrong # accounts active", null); };
            if (map.totalActiveSessions() != 3) { throw ServiceException.FAILURE("Wrong # active sessions", null); };
            
            System.out.println("Final session list:");
            for (Session s : map.copySessionList()) {
                System.out.println("\t"+s.getAccountId()+"_"+s.getSessionId());
            }
            return true;
        } catch (ServiceException e) {
            System.err.println("Unit Test Failed: Caught exception: "+e);
            e.printStackTrace();
            return false;
        }
        
    }
    
    /** All the sessions for a given account */
    static final class AccountSessionMap extends HashMap<String, Session> {
        AccountSessionMap() {
            super(4);
        }
    }
    
    
    /**
     * Run unit tests
     * 
     * @param args
     */
    public static void main(String[] args) {
        if (SessionMap.unitTest()) {
            System.out.println("Unit tests succeeded!");
        } else {
            System.out.println("Unit tests FAILED!");
        }
    }
    
}