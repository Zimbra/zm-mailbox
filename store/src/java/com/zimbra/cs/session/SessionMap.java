/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.util.ZimbraLog;

/**
 * Complex data structure used by the {@link SessionCache} for tracking active sessions. It supports three basic things:
 * <ul>
 *  <li>Fast lookup/add/remove of session by {AccountID,SessionID} pair
 *  <li>Fast mapping AccountId-->{set of active sessions for that account}
 *  <li>Fast pruning of session based on last-access-time and session-specific timeout value
 * </ul>
 */
final class SessionMap {
    private static final ExecutorService SWEEPER = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("SessionSweeper").setDaemon(true).build());

    private final Session.Type type;
    private final Map<String /*accountId*/, AccountSessionMap> accountSessionMap =
        new LinkedHashMap<String, AccountSessionMap>(100);

    /**
     * Because different types of sessions can have different timeout values, we can't just store
     * all the sessions in a big access-ordered list for quick timeout -- we'd have to iterate
     * all active sessions, which could potentially be bad.  Instead, we store sessions in a separate
     * cache depending on their timeout length.
     */
    private final Map<Long, Set<Session>> sessionAccessSet = new HashMap<Long, Set<Session>>();
    private int totalActiveSessions = 0;


    SessionMap(Session.Type type) {
        this.type = type;
    }

    public Session.Type getType() {
        return type;
    }

    /** @return the number of unique accounts with active sessions */
    public synchronized int totalActiveAccounts() {
        return accountSessionMap.size();
    }

    /** @return total number of sessions in the cache */
    public synchronized int totalActiveSessions() {
        return totalActiveSessions;
    }

    /** Returns the number of {@link Session}s in the cache for the
     *  given user. */
    public synchronized int countActiveSessions(String accountId) {
        AccountSessionMap acctMap = accountSessionMap.get(accountId);
        if (acctMap == null) {
            ZimbraLog.session.trace("SessionMap(%s).countActiveSessions(%s) NONE", type, accountId);
            return 0;
        }
        ZimbraLog.session.trace("SessionMap(%s).countActiveSessions(%s). size=%s",
                type, accountId, acctMap.size());
        return acctMap.size();
    }

    public synchronized Collection<AccountSessionMap> activeAccounts() {
        return Collections.unmodifiableCollection(accountSessionMap.values());
    }

    public synchronized Collection<Session> get(String accountId) {
        AccountSessionMap m = accountSessionMap.get(accountId);
        return (m == null) ? null : Collections.unmodifiableCollection(m.values());
    }

    /** Fetches a {@link Session} from the cache by owner and session ID and
     *  returns it.  Returns <tt>null</tt> if no matching <tt>Session</tt> is
     *  found.  As a side-effect, updates the last access time on the returned
     *  <tt>Session</tt>. */
    public synchronized Session get(String accountId, String sessionId) {
        AccountSessionMap acctMap = accountSessionMap.get(accountId);
        if (acctMap != null) {
            Session session = acctMap.get(sessionId);
            if (session != null) {
                updateAccessTime(session);
            }
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
        ZimbraLog.session.trace("SessionMap(%s).remove(accountId=%s, sessionId=%s)",
                type, accountId, sessionId);
        AccountSessionMap acctMap = accountSessionMap.get(accountId);
        if (acctMap != null) {
            Session removed = acctMap.remove(sessionId);
            if (removed != null) {
                long removedTimeout = removed.getSessionIdleLifetime();
                Set<Session> set = getSessionAccessSet(removedTimeout);
                assert(set.contains(removed));
                set.remove(removed);
                totalActiveSessions--;
                if (set.isEmpty()) {
                    sessionAccessSet.remove(removedTimeout);
                }
                if (acctMap.isEmpty()) {
                    accountSessionMap.remove(accountId);
                }
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
        synchronized (this) {
            assert(session != null);
            put(accountId, sessionId, session);

            AccountSessionMap acctMap = accountSessionMap.get(accountId);
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
                final Session removed = remove(accountId, leastRecentId);
                if (removed != null) {
                    ZimbraLog.session.info("Too many %s sessions (%d > %d), closing %s", session.getType(), prevSize, maxSessionsPerAcct, removed);
                    // clean up the sessions asynchronously outside of the synchronized block or the mailbox lock
                    SWEEPER.submit(new Runnable() {
                        @Override
                        public void run() {
                            removed.doCleanup();
                        }
                    });
                }

                // note that remove() may have nulled out accountSessionMap[accountId]
                acctMap = accountSessionMap.get(accountId);

                assert(acctMap == null || acctMap.size() < prevSize);
                if (acctMap.size() > maxSessionsPerAcct || acctMap.size() >= prevSize) {
                    ZimbraLog.session.warn("Problem in SessionMap.putAndPrune(%d): accountId: %s session: %s  maxPerAcct: %d prevSize: %d finishSize: %d leastRecentTime: %d leastRecentId: %s removed: %s",
                            iterations, accountId, sessionId, maxSessionsPerAcct, prevSize, acctMap.size(), leastRecent, leastRecentId, removed);
                    StringBuilder sb = new StringBuilder("SessionMap for account ");
                    sb.append(accountId).append(" contains: ");
                    for (Map.Entry<String, Session> entry : acctMap.entrySet()) {
                        sb.append("(").append(entry.getKey()).append(",").append(entry.getValue().toString());
                        sb.append(" time=").append(entry.getValue().getLastAccessTime()).append(") ");
                    }
                    ZimbraLog.session.warn(sb.toString());
                }
            }
        }
    }

    /** Adds a {@link Session} to the cache.  As a side effect, updates the
     *  <tt>Session</tt>'s last access time.
     *
     * @return any already-cached <tt>Session</tt> with the same owner and
     *         session ID */
    @VisibleForTesting
    synchronized Session put(String accountId, String sessionId, Session session) {
        ZimbraLog.session.trace("SessionMap(%s).put(accountId=%s, sessionId=%s, %s)",
                type, accountId, sessionId, session);
        assert(session != null);
        AccountSessionMap acctMap = accountSessionMap.get(accountId);
        if (acctMap == null) {
            acctMap = new AccountSessionMap();
            accountSessionMap.put(accountId, acctMap);
        }
        assert(!acctMap.containsKey(sessionId));
        totalActiveSessions++; // new session!
        Session oldValue = acctMap.put(sessionId, session);
        updateAccessTime(session);
        return oldValue;
    }

    synchronized private void updateAccessTime(Session session) {
        Set<Session> set = getSessionAccessSet(session.getSessionIdleLifetime());
        set.remove(session);
        session.sessionCacheSetLastAccessTime();
        set.add(session);
    }

    /**
     * Returns a shallow-copy of the list of sessions.
     */
    synchronized List<Session> copySessionList() {
        List<Session> toRet = new ArrayList<Session>(totalActiveSessions);
        for (Set<Session> set : sessionAccessSet.values()) {
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

        for (Map.Entry<Long, Set<Session>> entry : sessionAccessSet.entrySet()) {
            long cutoff = cutoffIn;
            if (all) {
                cutoff = Long.MAX_VALUE;
            } else {
                if (cutoff == -1) {
                    cutoff = now - entry.getKey();
                }
            }

            for (Iterator<Session> iter = entry.getValue().iterator(); iter.hasNext();) {
                Session s = iter.next();
                if (!s.accessedAfter(cutoff)) {
                    toRet.add(s);
                    iter.remove();
                    totalActiveSessions--;
                    AccountSessionMap acctMap = accountSessionMap.get(s.getAuthenticatedAccountId());
                    if (acctMap != null) {
                        Session removed = acctMap.remove(s.getSessionId());
                        if (removed != null) {
                            if (acctMap.isEmpty()) {
                                accountSessionMap.remove(s.getAuthenticatedAccountId());
                            }
                        }
                    }
                } else {
                    // iter is last-access order, so we know we can quit now
                    break;
                }
            }
        }
        return toRet;
    }

    /**
     * Return the set of sessions for this session Type.
     *
     * @param timeout This session's timeout
     */
    synchronized private Set<Session> getSessionAccessSet(Long timeout) {
        Set<Session> toRet = sessionAccessSet.get(timeout);
        if (toRet == null) {
            toRet = new LinkedHashSet<Session>();
            sessionAccessSet.put(timeout, toRet);
        }
        return toRet;
    }

    /** All the sessions for a given account */
    static final class AccountSessionMap extends HashMap<String, Session> {
        private static final long serialVersionUID = -8141746787729464753L;

        AccountSessionMap() {
            super(4);
        }
    }

}
