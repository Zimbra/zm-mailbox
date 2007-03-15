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
 *  Not all Session subclasses are stored in this cache -- only those Session types which want 
 *  timeout caching arestored here.<p>
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

        String sessionId = getNextSessionId();
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
        
        sSessionMap.putAndPrune(accountId, sessionId, session, 10);
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

        Session s = sSessionMap.get(accountId, sessionId);
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
        
        Session session = sSessionMap.remove(accountId, sessionId);
        if (session != null) {
            assert(!Thread.holdsLock(sSessionMap));
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
        
        List<Session> list = sSessionMap.pruneSessionsByTime(Long.MAX_VALUE);
        
        // Call deadlock-prone Session.doCleanup() after releasing lock on sLRUMap.
        for (Session s: list) {
            assert(!Thread.holdsLock(sSessionMap));
            s.doCleanup();
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
    private static SessionMap sSessionMap = new SessionMap();

    /** Whether we've received a {@link #shutdown()} call to kill the cache. */
    private static boolean sShutdown = false;
    /** The ID for the next generated {@link Session}. */
    private static long sContextSeqNo = 1;


    private synchronized static String getNextSessionId() {
        return Long.toString(sContextSeqNo++);
    }

    private static void logActiveSessions() {
        ValueCounter sessionTypeCounter = new ValueCounter();
        StringBuilder accountList = new StringBuilder();
        StringBuilder manySessionsList = new StringBuilder();
        int totalSessions = 0;
        int totalAccounts = 0;

        synchronized (sSessionMap) {
            for (SessionMap.AccountSessionMap activeAcct : sSessionMap.activeAccounts()) {
                String accountId = null;
                totalAccounts++;
                int count = 0;
                for (Session session : activeAcct.values()) {
                    accountId = session.getAccountId();
                    totalSessions++;
                    count++;
                    String className = StringUtil.getSimpleClassName(session);
                    sessionTypeCounter.increment(className);
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

            assert(totalAccounts == sSessionMap.totalActiveAccounts());
            assert(totalSessions == sSessionMap.totalActiveSessions());
        }
        
        if (sLog.isDebugEnabled() && totalSessions > 0) {
            sLog.debug("Detected " + totalSessions + " active sessions.  " +
                sessionTypeCounter + ".  Accounts: " + accountList);
        }
        if (manySessionsList.length() > 0) {
            ZimbraLog.session.info("Found accounts that have a large number of sessions: " + manySessionsList);
        }
    }
    
    public static List<Session> getActiveSessions() {
        return sSessionMap.copySessionList();
    }
    
//    public static void dumpState(Writer w) {
//        List<Session>list = sSessionMap.copySessionList();
//        try { w.write("\nACTIVE SESSIONS:\n"); } catch(IOException e) {};
//        for (Session s: list) {
//            s.dumpState(w);
//            try { w.write('\n'); } catch(IOException e) {};
//        }
//        try { w.write('\n'); } catch(IOException e) {};
//    }

    private static final class SweepMapTimerTask extends TimerTask {
        public void run() {
            if (sLog.isDebugEnabled())
                SessionCache.logActiveSessions();
            
            List<Session> toReap = sSessionMap.pruneIdleSessions();
            
            // keep track of the count of each session type that's removed
            ValueCounter removedSessionTypes = new ValueCounter();
            
            for (Session s: toReap) {
                if (ZimbraLog.session.isDebugEnabled())
                    ZimbraLog.session.debug("Removing cached session: " + s);
                if (sLog.isInfoEnabled())
                    removedSessionTypes.increment(StringUtil.getSimpleClassName(s));
                assert(!Thread.holdsLock(sSessionMap));
                // IMPORTANT: Clean up sessions *after* releasing lock on sLRUMap.
                // If Session.doCleanup() is called with sLRUMap locked, it can lead
                // to deadlock. (bug 7866)
                s.doCleanup();
            }

            if (sLog.isInfoEnabled() && removedSessionTypes.size() > 0) {
                sLog.info("Removed " + removedSessionTypes.getTotal() + " idle sessions (" +
                          removedSessionTypes + ").  " + sSessionMap.totalActiveSessions() + " active sessions remain.");
            }
        }
    }
    
}
