/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
import com.zimbra.common.util.ZimbraLog;

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
    public synchronized int totalActiveAccounts() {
        return mAcctSessionMap.size();
    }
    
    /** @return total number of sessions in the cache */
    public synchronized int totalActiveSessions() {
        return mTotalActiveSessions;
    }

    /** Returns the number of {@link Session}s in the cache for the
     *  given user. */
    public synchronized int countActiveSessions(String accountId) {
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        if (acctMap != null) {
            return mAcctSessionMap.size();
        } else {
            return 0;
        }
    }

    public synchronized Collection<AccountSessionMap> activeAccounts() { return Collections.unmodifiableCollection(mAcctSessionMap.values()); }

    /** Fetches a {@link Session} from the cache by owner and session ID and
     *  returns it.  Returns <tt>null</tt> if no matching <tt>Session</tt> is
     *  found.  As a side-effect, updates the last access time on the returned
     *  <tt>Session</tt>. */
    public synchronized Session get(String accountId, String sessionId) {
        AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
        if (acctMap != null) {
            Session session = acctMap.get(sessionId);
            if (session != null)
                updateAccessTime(session);
            return session;
        } else {
            return null;
        }
    }

    /** Looks up a {@link Session} by owner and session ID, removes it from
     *  the cache, and returns it.  If no such <tt>Session</tt> is found,
     *  returns <tt>null</tt>.  As a side effect, unsets the session ID on
     *  the removed <tt>Session</tt>. */
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
        } else {
            return null;
        }
    }

    /** Adds a {@link Session} to the cache, making sure that this doesn't
     *  cause the owner to exceed their maximum permitted number of that type
     *  of <tt>Session</tt>.  If it would cause them to go over the limit, the
     *  owner's least recently accessed <tt>Session</tt> of that type is
     *  removed from the cache and its {@link Session#doCleanup()} method is
     *  called.  As a side effect, all removed <tt>Session</tt>s have their
     *  session ID unset. */
    public void putAndPrune(String accountId, String sessionId, Session session, int maxSessionsPerAcct) {
        //ZimbraLog.session.debug("PutAndPrune(%s, %s, %s)", accountId, sessionId, session.toString());

        List<Session> dequeued = null;
        synchronized (this) {
            assert(session != null);
            put(accountId, sessionId, session);
    
            AccountSessionMap acctMap = mAcctSessionMap.get(accountId);
            int iterations = 0; // debugging info looking for bug 17324
            while (acctMap != null && acctMap.size() > maxSessionsPerAcct) {
                iterations++;
                long leastRecent = Long.MAX_VALUE;
                String leastRecentId = null;
                
                for (Map.Entry<String, Session> entry : acctMap.entrySet()) {
                    Session s = entry.getValue();
                    if (s.getLastAccessTime() < leastRecent) {
                        leastRecent = s.getLastAccessTime();
                        leastRecentId = entry.getKey();
                    }
                }
                assert(leastRecentId != null);

                int prevSize = acctMap.size();
                Session removed = remove(accountId, leastRecentId);
                if (removed != null) {
                    if (dequeued == null)
                        dequeued = new ArrayList<Session>(3);
                    dequeued.add(removed);
                }

                // note that remove() may have nulled out mAcctSessionMap[accountId]
                acctMap = mAcctSessionMap.get(accountId);

                assert(acctMap == null || acctMap.size() < prevSize);
                if (acctMap.size() > maxSessionsPerAcct || acctMap.size() >= prevSize) {
                    ZimbraLog.session.warn("Problem in SessionMap.putAndPrune(%d): accountId: %s session: %s  maxPerAcct: %d prevSize: %d finishSize: %d leastRecentTime: %d leastRecentId: %s removed: %s",
                        iterations, accountId, sessionId, maxSessionsPerAcct, prevSize, acctMap.size(), leastRecent, leastRecentId, removed);
                    StringBuilder sb = new StringBuilder("SessionMap for account ");
                    sb.append(accountId).append(" contains: ");
                    for (Map.Entry<String, Session> entry : acctMap.entrySet())
                        sb.append("(").append(entry.getKey()).append(",").append(entry.getValue().toString()).append(" time=").append(entry.getValue().getLastAccessTime()).append(") ");
                    ZimbraLog.session.warn(sb.toString());
                }
            }
        }

        // clean up the sessions outside of the synchronized block
        if (dequeued != null) {
            for (Session removed : dequeued) {
                ZimbraLog.session.debug("Account: %s has too many sessions open of type %s, forcing session %s to close",
                    accountId, session.getType(), removed);
                removed.doCleanup();
            }
        }
    }

    /** Adds a {@link Session} to the cache.  As a side effect, updates the
     *  <tt>Session</tt>'s last access time.
     *  
     * @return any already-cached <tt>Session</tt> with the same owner and
     *         session ID */
    synchronized private Session put(String accountId, String sessionId, Session session) {
        assert(session != null);
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
        session.sessionCacheSetLastAccessTime();
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
                cutoff = Long.MAX_VALUE;
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
                    AccountSessionMap acctMap = mAcctSessionMap.get(s.getAuthenticatedAccountId());
                    Session removed = acctMap.remove(s.getSessionId());
                    assert(removed == s);
                    if (removed != null) {
                        if (acctMap.isEmpty())
                            mAcctSessionMap.remove(s.getAuthenticatedAccountId());
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

            Session s1 =   new AdminSession("a1").testSetSessionId("s1");
            Session s1_2 = new AdminSession("a1").testSetSessionId("s2");
            Session s2 =   new AdminSession("a2").testSetSessionId("s1");
            Session s3 =   new AdminSession("a3").testSetSessionId("s1");
            Session s4 =   new AdminSession("a4").testSetSessionId("s1");
            Session s5 =   new AdminSession("a5").testSetSessionId("s1");
            Session s5_2 = new AdminSession("a5").testSetSessionId("s2");
            
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
                if (s.getAuthenticatedAccountId().equals("a1")) {
                    if (s.getSessionId().equals("s1"))
                        hasA1S1 = true;
                    if (s.getSessionId().equals("s2"))
                        hasA1S2 = true;
                }
                if (s.getAuthenticatedAccountId().equals("a2"))
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
                System.out.println("\t"+s.getAuthenticatedAccountId()+"_"+s.getSessionId());
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
        private static final long serialVersionUID = -8141746787729464753L;

        AccountSessionMap()  { super(4); }
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