/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Sep 15, 2004
 */
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.SessionDataProvider.NotificationQueue;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Zimbra;

/** A Simple Cache with timeout (based on last-accessed time) for a {@link Session}. objects<p>
 *
 *  Not all Session subclasses are stored in this cache currently, though all user-available
 *  sessions should eventually be stored in this cache
 **/
public final class SessionCache {

    private static final String sRunIdentifier = (System.currentTimeMillis() / 1000) + "." + new java.util.Random().nextInt(100);

    /** The frequency at which we sweep the cache to delete idle sessions. */
    private static final long SESSION_SWEEP_INTERVAL_MSEC = 1 * Constants.MILLIS_PER_MINUTE;

    private static Log sLog = LogFactory.getLog(SessionCache.class);

    private static final SessionMap[] sSessionMaps;
        static {
            sSessionMaps = new SessionMap[Session.Type.values().length];
            for (Session.Type type : Session.Type.values()) {
                sSessionMaps[type.getIndex()] = new SessionMap(type);
            }
        }

    /** Whether we've received a {@link #shutdown()} call to kill the cache. */
    private static boolean sShutdown = false;

    private static SessionDataProvider dataProvider = SessionDataProvider.getFactory().getIdProvider();

    /** Adds a {@link Session} to the cache and assigns it a session ID if it
     *  doesn't already have one.  When a reigistered <code>Session</code> ages
     *  out of the cache due to extended idle time, its {@link Session#doCleanup()}
     *  method is invoked and its session ID is unset.
     *
     * @param session  The <code>Session</code> to add to the cache
     * @return the session ID assigned to the <code>Session</code>
     * @see Session#getSessionIdleLifetime() */
    protected static String registerSession(Session session) {
        if (sShutdown || session == null)
            return null;

        Session.Type sessionType = session.getSessionType();
        String sessionId = session.getSessionId();
        assert(sessionId != null);

        getSessionMap(sessionType).putAndPrune(session.getAuthenticatedAccountId(), sessionId, session, sessionType.getMaxPerAccount());
        return sessionId;
    }

    /** Fetches a {@link Session} from the cache.  Checks to make sure that
     *  the provided account ID matches the one on the session.  Implicitly
     *  updates the LRU expiry timestamp on the session.
     *
     * @param sessionId  The identifier for the requested Session.
     * @param accountId  The owner of the requested Session.
     * @return The matching cached Session, or <tt>null</tt> if no session
     *         exists with the specified ID and owner. */
    public static Session lookup(String sessionId, String accountId) {
        if (sShutdown)
            return null;

        Session.Type type = getSessionTypeFromId(sessionId);
        Session s = getSessionMap(type).get(accountId, sessionId);
        if (s == null && ZimbraLog.session.isDebugEnabled()) {
            ZimbraLog.session.debug("no session with id " + sessionId + " found (accountId: " + accountId + ")");
        }
        return s;
    }

    /**
     * Returns all SOAP sessions registered for this account.
     *
     * @param accountId
     * @return Unmodifiable Collection of Sessions for the
     *         requested account, or <tt>null</tt> if no
     *         SOAP session exists for the account.
     */
    public static Collection<Session> getSoapSessions(String accountId) {
        if (sShutdown)
            return null;
        return getSessionMap(Session.Type.SOAP).get(accountId);
    }

    public static int countActiveSessionsForAccount(String accountId, Session.Type type) {
        SessionMap sessionMap = getSessionMap(type);
        if (sessionMap == null) {
            return 0;
        }
        int num = sessionMap.countActiveSessions(accountId);
        ZimbraLog.session.trace("SessionCache.countActiveSessionsForAccount(%s,%s)=%d",
                accountId, type, num);
        return num;
    }

    public static Collection<Session> getAllSessions(String accountId) {
        if (sShutdown)
            return null;
        Collection<Session> ret = new ArrayList<Session>();
        for (Session.Type type : Session.Type.values()) {
            Collection<Session> sessions = getSessionMap(type).get(accountId);
            if (sessions != null) {
                ret.addAll(sessions);
            }
        }
        return ret;
    }

    /** Immediately removes the {@link Session} from the session cache and
     *  runs its {@link Session#doCleanup()} method.
     *
     * @param session  The Session to be removed. */
    public static void clearSession(Session session) {
        Session target = unregisterSession(session);
        if (target == null) {
            return;
        }
        Session.Type type = target.getSessionType();
        assert(!Thread.holdsLock(getSessionMap(type)));
        target.doCleanup();
        dataProvider.cleanup(target.getSessionId());
    }

    /** Removes the <tt>Session</tt> from the cache and unsets its session ID.
     *  Unlike {@link #clearSession(Session)}, the <tt>Session</tt>'s
     *  {@link Session#doCleanup()} method is not automatically invoked.
     *
     * @param session  The <tt>Session</tt> to remove from the cache
     * @return the <tt>Session</tt> removed from the cache, or <tt>null</tt>
     *         if the given Session was not cached
     * @see #clearSession(Session)
     * @see #registerSession(Session) */
    public static Session unregisterSession(Session session) {
        if (sShutdown || session == null || !session.isAddedToSessionCache())
            return null;

        if (ZimbraLog.session.isDebugEnabled())
            ZimbraLog.session.debug("Unregistering session " + session.getSessionId());

        Session.Type type = session.getSessionType();
        return getSessionMap(type).remove(session.getAuthenticatedAccountId(), session.getSessionId());
    }

    /** Initializes the session cache and starts the sweeper timer. */
    public static void startup() {
        Zimbra.sTimer.schedule(new SweepMapTimerTask(), 30000, SESSION_SWEEP_INTERVAL_MSEC);
        ZimbraPerf.addStatsCallback(new StatsCallback());
    }

    /** Empties the session cache and cleans up any existing {@link Session}s.
     *
     * @see Session#doCleanup() */
    public static void shutdown() {
        sShutdown = true;

        for (SessionMap sessionMap : sSessionMaps) {
            List<Session> list = sessionMap.pruneSessionsByTime(Long.MAX_VALUE);

            // IMPORTANT: Clean up sessions *after* releasing lock on Session Map
            // If Session.doCleanup() is called with the SessionMap locked, it can lead
            // to deadlock. (bug 7866)
            for (Session s : list) {
                assert(!Thread.holdsLock(sessionMap));
                s.doCleanup();
            }
        }
    }

    protected static String qualifySessionId(String sessionId) {
        return sRunIdentifier + "." + sessionId;
    }

    private static final Session.Type getSessionTypeFromId(String sessionId) {
        if (sessionId == null || sessionId.length() < 2)
            return Session.Type.NULL; // invalid session id

        return Session.Type.values()[Character.digit(sessionId.charAt(0),10)];
    }

    private static final SessionMap getSessionMap(Session.Type type) {
        return sSessionMaps[type.getIndex()];
    }

    protected static String getNextSessionId(Session.Type type) {
        return dataProvider.getNextSessionIdSequence(type);
    }

    private static void logActiveSessions() {
        StringBuilder accountList = new StringBuilder();
        StringBuilder manySessionsList = new StringBuilder();
        int totalSessions = 0;

        int sessionTypeCounter[] = new int[Session.Type.values().length];

        for (SessionMap sessionMap: sSessionMaps) {
            synchronized(sessionMap) {
                for (SessionMap.AccountSessionMap activeAcct : sessionMap.activeAccounts()) {
                    String accountId = null;
                    int count = 0;
                    for (Session session : activeAcct.values()) {
                        accountId = session.getAuthenticatedAccountId();
                        totalSessions++;
                        count++;
                        sessionTypeCounter[sessionMap.getType().getIndex()]++;
                    }
                    assert(count>0);
                    if (count > 0) {
                        if (accountList.length()>0)
                            accountList.append(',');
                        accountList.append(accountId).append('(').append(count).append(')');
                        if (count > 9) {
                            if (manySessionsList.length() > 0)
                                manySessionsList.append(',');
                            manySessionsList.append(accountId).append('(').append(count).append(')');
                        }
                    }
                }
            }
        }

        if (sLog.isDebugEnabled() && totalSessions > 0) {
            sLog.debug("Detected " + totalSessions + " active sessions.  " +
                sessionTypeCounter + ".  Accounts: " + accountList);
        }
        if (manySessionsList.length() > 0) {
            ZimbraLog.session.info("Found accounts that have a large number of sessions: " + manySessionsList);
        }
    }

    /**
     * Return a copy of the session's current list of active sessions
     *
     * @param type
     * @return
     */
    public static List<Session> getActiveSessions(Session.Type type) {
        return getSessionMap(type).copySessionList();
    }

    /**
     * Return active session counts in an array { activeAccounts, activeSessions }
     *
     * @param type
     * @return
     */
    public static int[] countActive(Session.Type type) {
        int[] toRet = new int[2];

        SessionMap sessionMap = getSessionMap(type);

        synchronized(sessionMap) {
            toRet[0] = sessionMap.totalActiveAccounts();
            toRet[1] = sessionMap.totalActiveSessions();
        }
        return toRet;
    }

    public static int getNextSoapSequence(String soapSessionId) {
        return dataProvider.getNextSoapSessionSequence(soapSessionId);
    }

    public static int getCurrentSoapSequence(String soapSessionId) {
        return dataProvider.getCurrentSoapSessionSequence(soapSessionId);
    }

    public static boolean soapSessionExists(String soapSessionId) {
        return dataProvider.soapSessionExists(soapSessionId);
    }

    public static NotificationQueue getSoapNotificationQueue(SoapSession session) {
        return dataProvider.getSoapNotifications(session);
    }

    private static final class StatsCallback implements RealtimeStatsCallback {
        StatsCallback()  { }

        /* @see com.zimbra.common.stats.RealtimeStatsCallback#getStatData() */
        @Override
        public Map<String, Object> getStatData() {
            Map<String, Object> data = new HashMap<String, Object>();
            SessionMap soapMap = getSessionMap(Session.Type.SOAP);
            data.put(ZimbraPerf.RTS_SOAP_SESSIONS, soapMap.totalActiveSessions());
            return data;
        }
    }

    private static final class SweepMapTimerTask extends TimerTask {
        SweepMapTimerTask()  { }

        @Override public void run() {
            try {
                if (sLog.isDebugEnabled())
                    SessionCache.logActiveSessions();

                int removedByType[] = new int[Session.Type.values().length];
                int totalActive = 0;

                for (SessionMap sessionMap : sSessionMaps) {
                    List<Session> toReap = sessionMap.pruneIdleSessions();
                    totalActive += sessionMap.totalActiveSessions();

                    // keep track of the count of each session type that's removed
                    removedByType[sessionMap.getType().getIndex()]+=toReap.size();

                    for (Session s : toReap) {
                        if (ZimbraLog.session.isDebugEnabled()) {
                            ZimbraLog.session.debug("Removing cached session: %s", s);
                        }
                        assert(!Thread.holdsLock(sessionMap));
                        // IMPORTANT: Clean up sessions *after* releasing lock on Session Map
                        // If Session.doCleanup() is called with sMap locked, it can lead
                        // to deadlock. (bug 7866)
                        try {
                            s.doCleanup();
                            dataProvider.cleanup(s.getSessionId());
                        } catch (Exception e) {
                            //continue to cleanup other sessions that have already been pruned
                            ZimbraLog.session.warn("cleanup failed for session %s", s);
                        }
                    }
                }

                int totalRemoved = 0;
                for (int r : removedByType) {
                    totalRemoved += r;
                }

                if (sLog.isInfoEnabled() && totalRemoved > 0) {
                    StringBuilder sb = new StringBuilder("Removed ").append(totalRemoved).append(" idle sessions (");
                    StringBuilder typeStr = new StringBuilder();
                    for (int i = 1; i < removedByType.length; i++) {
                        if (removedByType[i]>0) {
                            if (typeStr.length() > 0) {
                                typeStr.append(", ");
                            }
                            typeStr.append(Session.Type.values()[i].name());
                        }
                    }
                    sb.append(typeStr).append("). ").append(totalActive).append(" active sessions remain.");
                    sLog.info(sb.toString());
                }
            } catch (OutOfMemoryError e) {
                Zimbra.halt("Caught out of memory error (in SessionCache timer)", e);
            } catch (Throwable e) { //don't let exceptions kill the timer
                ZimbraLog.session.warn("Caught exception in SessionCache timer", e);
            }

        }
    }

}
