/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.imap.ImapSession;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ValueCounter;
import com.zimbra.cs.util.Zimbra;



/** A Simple LRU Cache with timeout intended to store arbitrary state for a "session".<p>
 * 
 *  A {@link Session} is identified by an (accountId, sessionID) pair.  A single
 *  account may have multiple active sessions simultaneously.<p>
 * 
 *  Unless you really care about the internals of the LRU Session Cache,
 *  you probably just want to add fields and get/set methods to Session.
 *
 * @author tim */
public final class SessionCache {

    ///////////////////////////////////////////////////////////////////////////
    // Public APIs here...
    ///////////////////////////////////////////////////////////////////////////

    public static final int SESSION_SOAP  = 1;
    public static final int SESSION_IMAP  = 2;
    public static final int SESSION_ADMIN = 3;

    /** Creates a new {@link Session} of the specified type and starts its
     *  expiry timeout.
     * 
     * @param accountId    The owner for the new Session.
     * @param sessionType  The type of session (SOAP, IMAP, etc.) we need.
     * @return A brand-new session for this account, or <code>null</code>
     *         if an error occurred. */
    public static Session getNewSession(String accountId, int sessionType) {
        if (sShutdown || accountId == null || accountId.trim().equals(""))
            return null;

        String sessionId = getNextSessionId();
        Session session = null;
        try {
            switch (sessionType) {
                case SESSION_IMAP:   session = new ImapSession(accountId, sessionId);   break;
                case SESSION_ADMIN:  session = new AdminSession(accountId, sessionId);  break;
                default:
                case SESSION_SOAP:   session = new SoapSession(accountId, sessionId);   break;
            }
        } catch (ServiceException e) {
            return null;
        }

        if (sLog.isDebugEnabled())
            sLog.debug("Created " + session);
        updateInCache(sessionId, session);
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

        synchronized(sLRUMap) {
            Session session = (Session) sLRUMap.get(sessionId);
            if (session != null) {
                if (session.validateAccountId(accountId)) {
                    updateInCache(sessionId, session);
                    return session;
                } else
                    sLog.warn("account ID mismatch in session cache.  Requested " + accountId + ":" + sessionId +
                              ", found " + session.getAccountId());
            } else if (sLog.isDebugEnabled())
                sLog.debug("no session with id " + sessionId + " found (accountId: " + accountId + ")");
            return null;
        }
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

        if (sLog.isDebugEnabled())
            sLog.debug("Clearing session " + sessionId);
        Session session = null;
        synchronized (sLRUMap) {
            session = (Session) sLRUMap.remove(sessionId);
        }
        if (session != null)
            session.doCleanup();
    }

    /** Empties the session cache and cleans up any existing {@link Session}s.
     * 
     * @see Session#doCleanup() */
    public static void shutdown() {
        synchronized(sLRUMap) {
            sLog.info("shutdown: clearing SessionCache");

            // empty the lru cache
            Iterator iter = sLRUMap.values().iterator();
            while (iter.hasNext()) {
                Session session = (Session) iter.next();
                iter.remove();
                session.doCleanup();
                assert(sLRUMap.get(session.getSessionId()) == null);
            }
            sShutdown = true;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////
    // Internals below here... 
    //////////////////////////////////////////////////////////////////////////////

    // TODO make me configurable!
    /** The frequency at which we sweep the cache to delete idle sessions. */
    private static final long SESSION_SWEEP_INTERVAL_MSEC = 1 * Constants.MILLIS_PER_MINUTE;

    static Log sLog = LogFactory.getLog(SessionCache.class);

    /** The cache of all active {@link Session}s.  The keys of the Map are session IDs
     *  and the values are the Sessions themselves. */
    static LinkedHashMap sLRUMap = new LinkedHashMap(500);

    /** Whether we've received a {@link #shutdown()} call to kill the cache. */
    private static boolean sShutdown = false;
    /** The ID for the next generated {@link Session}. */
    private static long sContextSeqNo = 1;


    private SessionCache() {
        Zimbra.sTimer.schedule(new SweepMapTimerTask(), 30000, SESSION_SWEEP_INTERVAL_MSEC);
    }

    private synchronized static String getNextSessionId() {
        return Long.toString(sContextSeqNo++);
    }

    private static void updateInCache(String sessionId, Session session) {
        if (sLog.isDebugEnabled())
            sLog.debug("updating session " + sessionId);
        synchronized(sLRUMap) {
            sLRUMap.remove(sessionId);
            session.updateAccessTime();
            sLRUMap.put(sessionId, session);
        }
    }

    private final class SweepMapTimerTask extends TimerTask {
        public void run() {
            if (sLog.isDebugEnabled())
                logActiveSessions();
            
            // keep track of the count of each session type that's removed
            ValueCounter removedSessionTypes = new ValueCounter();
            int numActive = 0;
            
            long now = System.currentTimeMillis();
            synchronized (sLRUMap) {
                Iterator iter = sLRUMap.values().iterator();
                while (iter.hasNext()) {
                    Session s = (Session) iter.next();
                    if (s == null)
                        continue;
                    long cutoffTime = now - s.getSessionIdleLifetime();
                    if (!s.accessedAfter(cutoffTime)) {
                        if (sLog.isDebugEnabled())
                            sLog.debug("Removing cached session: " + s);
                        if (sLog.isInfoEnabled())
                            removedSessionTypes.increment(StringUtil.getSimpleClassName(s));
                        iter.remove();
                        s.doCleanup();
                        assert(sLRUMap.get(s.getSessionId()) == null);
                    } else {
                        // mLRUMap iterates by last access order, most recently
                        // accessed element last.  We can break here because
                        // all later sessions are guaranteed not to be expired.
                        break;
                    }
                }
                numActive = sLRUMap.size();
            }

            if (sLog.isInfoEnabled() && removedSessionTypes.size() > 0) {
                sLog.info("Removed " + removedSessionTypes.getTotal() + " idle sessions (" +
                          removedSessionTypes + ").  " + numActive + " active sessions remain.");
            }
        }
        
        private void logActiveSessions() {
            // Count distinct accountId's
            ValueCounter accountCounter = new ValueCounter();
            ValueCounter sessionTypeCounter = new ValueCounter();
            synchronized (sLRUMap) {
                Iterator i = sLRUMap.values().iterator();
                while (i.hasNext()) {
                    Session session = (Session) i.next();
                    accountCounter.increment(session.getAccountId());
                    String className = StringUtil.getSimpleClassName(session);
                    sessionTypeCounter.increment(className);
                }
            }

            // Format account list
            StringBuffer accountList = new StringBuffer();
            StringBuffer manySessionsList = new StringBuffer();
            
            Iterator i = accountCounter.iterator();
            while (i.hasNext()) {
                if (accountList.length() > 0)
                    accountList.append(", ");
                String accountId = (String) i.next();
                int count = accountCounter.getCount(accountId);
                accountList.append(accountId);
                if (count > 1)
                    accountList.append("(" + count + ")");
                if (count >= 10) {
                    if (manySessionsList.length() > 0)
                        manySessionsList.append(", ");
                    manySessionsList.append(accountId + "(" + count + ")");
                }
            }

            sLog.debug("Detected " + accountCounter.getTotal() + " active sessions.  " +
                       sessionTypeCounter + ".  Accounts: " + accountList);
            if (manySessionsList.length() > 0) {
                sLog.info("Found accounts that have a large number of sessions: " +
                          manySessionsList);
            }
        }
    }
}
