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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 15, 2004
 */
package com.zimbra.cs.session;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.cs.imap.ImapSession;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ValueCounter;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.util.ZimbraLog;

/** A Simple Cache with timeout (based on last-accessed time) for a {@link Session}. objects<p>
 * 
 *  Not all Session subclasses are stored in this cache currently, though all user-available
 *  sessions should eventually be stored in this cache
 **/
public final class SessionCache {

    /** Creates a new {@link Session} of the specified type and starts its
     *  expiry timeout.
     * 
     * @param accountId    The owner for the new Session.
     * @param sessionType  The type of session (SOAP, IMAP, etc.) we need.
     * @return A brand-new session for this account, or <code>null</code>
     *         if an error occurred. */
    public static Session getNewSession(String accountId, Session.Type sessionType) {
        if (sShutdown || accountId == null || accountId.trim().equals(""))
            return null;

        String sessionId = getNextSessionId(sessionType);
        Session session = null;
        try {
            switch (sessionType) {
                case IMAP:   session = new ImapSession(accountId, sessionId);   break;
                case ADMIN:  session = new AdminSession(accountId, sessionId);  break;
                case SYNCLISTENER: throw ServiceException.FAILURE("SYNCLISTENER sessions should not be created by the session cache", null);
                case WAITSET: throw ServiceException.FAILURE("WAITSET sessions should not be created by the session cache", null);
                default:
                case SOAP:   session = new SoapSession(accountId, sessionId);   break;
            }
        } catch (ServiceException e) {
            ZimbraLog.session.warn("failed to create session", e);
            return null;
        }
        
        if (ZimbraLog.session.isDebugEnabled())
            ZimbraLog.session.debug("Created " + session);
        
        getSessionMap(sessionType).putAndPrune(accountId, sessionId, session, sessionType.getMaxPerAccount());
        return session;
    }

    /** Fetches a {@link Session} from the cache.  Checks to make sure that
     *  the provided account ID matches the one on the session.  Implicitly
     *  updates the LRU expiry timestamp on the session.
     * 
     * @param sessionId  The identifier for the requested Session.
     * @param accountId  The owner of the requested Session.
     * @return The matching cached Session, or <code>null</code> if no session
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

    /** Immediately removes this {@link Session} from the session cache.
     * 
     * @param session The Session to be removed. */
    public static void clearSession(Session session) {
        if (session != null)
            clearSession(session.getSessionId(), session.getAccountId());
    }

    /** Immediately removes this session from the session cache.  Checks to
     *  make sure that the provided account ID matches the one on the cached
     *  {@link Session}.
     * 
     * @param sessionId  The identifier for the requested Session.
     * @param accountId  The owner of the requested Session. */
    public static void clearSession(String sessionId, String accountId) {
        if (sShutdown)
            return;

        if (ZimbraLog.session.isDebugEnabled())
            ZimbraLog.session.debug("Clearing session " + sessionId);
        
        Session.Type type = getSessionTypeFromId(sessionId);
        Session session = getSessionMap(type).remove(accountId, sessionId);
        if (session != null) {
            assert(!Thread.holdsLock(getSessionMap(type)));
            session.doCleanup();
        }
    }
    
    /**
     * Inintializes the session cache, starts the sweeper timer
     */
    public static void startup() {
        Zimbra.sTimer.schedule(new SweepMapTimerTask(), 30000, SESSION_SWEEP_INTERVAL_MSEC);
    }

    /** Empties the session cache and cleans up any existing {@link Session}s.
     * 
     * @see Session#doCleanup() */
    public static void shutdown() {
        sShutdown = true;
        
        for (SessionMap sessionMap : sSessionMaps) {
            List<Session> list = sessionMap.pruneSessionsByTime(Long.MAX_VALUE);
            
            // Call deadlock-prone Session.doCleanup() after releasing lock on sLRUMap.
            for (Session s: list) {
                assert(!Thread.holdsLock(sessionMap));
                s.doCleanup();
            }
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////
    // Internals below here... 
    //////////////////////////////////////////////////////////////////////////////

    /** The frequency at which we sweep the cache to delete idle sessions. */
    private static final long SESSION_SWEEP_INTERVAL_MSEC = 1 * Constants.MILLIS_PER_MINUTE;

    static Log sLog = LogFactory.getLog(SessionCache.class);

    /** The cache of all active {@link Session}s.  The keys of the Map are session IDs
     *  and the values are the Sessions themselves. */
//    private static LinkedHashMap<String, Session> sLRUMap = new LinkedHashMap<String, Session>(500);
    
    private static final Session.Type getSessionTypeFromId(String sessionId) {
        if (sessionId.length() < 2)
            return Session.Type.NULL; // invalid session id
        
        return Session.Type.values()[Character.digit(sessionId.charAt(0),10)];
    }
    
    private static final SessionMap getSessionMap(Session.Type type) {
        return sSessionMaps[type.getIndex()];
    }
    private static final SessionMap[] sSessionMaps;
    static {
        sSessionMaps = new SessionMap[Session.Type.values().length];
        for (Session.Type type : Session.Type.values()) {
            sSessionMaps[type.getIndex()] = new SessionMap(type);
        }
    }

    /** Whether we've received a {@link #shutdown()} call to kill the cache. */
    private static boolean sShutdown = false;
    
    /** The ID for the next generated {@link Session}. */
    private static long sContextSeqNo = 1;

    private synchronized static String getNextSessionId(Session.Type type) {
        return Integer.toString(type.getIndex())+Long.toString(sContextSeqNo++);
    }

    private static void logActiveSessions() {
        StringBuilder accountList = new StringBuilder();
        StringBuilder manySessionsList = new StringBuilder();
        int totalSessions = 0;
        int totalAccounts = 0;
        
        int sessionTypeCounter[] = new int[Session.Type.values().length];
        
        for (SessionMap sessionMap: sSessionMaps) {
            synchronized(sessionMap) {
                for (SessionMap.AccountSessionMap activeAcct : sessionMap.activeAccounts()) {
                    String accountId = null;
                    totalAccounts++;
                    int count = 0;
                    for (Session session : activeAcct.values()) {
                        accountId = session.getAccountId();
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
    
    private static final class SweepMapTimerTask extends TimerTask {
        public void run() {
            if (sLog.isDebugEnabled())
                SessionCache.logActiveSessions();

            int removedByType[] = new int[Session.Type.values().length];
            int totalActive = 0;
            
            for (SessionMap sessionMap : sSessionMaps) {
                List<Session> toReap = sessionMap.pruneIdleSessions();
                totalActive += sessionMap.totalActiveSessions();
                
                // keep track of the count of each session type that's removed
                removedByType[sessionMap.getType().getIndex()]+=toReap.size();
                
                for (Session s: toReap) {
                    if (ZimbraLog.session.isDebugEnabled())
                        ZimbraLog.session.debug("Removing cached session: " + s);
                    assert(!Thread.holdsLock(sessionMap));
                    // IMPORTANT: Clean up sessions *after* releasing lock on sLRUMap.
                    // If Session.doCleanup() is called with sLRUMap locked, it can lead
                    // to deadlock. (bug 7866)
                    s.doCleanup();
                }
            }
            
            int totalRemoved = 0;
            for (int r : removedByType) {
                totalRemoved+=r;
            }
            
            if (sLog.isInfoEnabled() && totalRemoved>0) {
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
            }
        }
    }
    
}
