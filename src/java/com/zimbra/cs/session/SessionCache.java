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
import com.zimbra.cs.util.Zimbra;



/**
 * @author tim
 * 
 * Unless you really care about the internals of the LRU Session Cache, 
 * you probably just want to add fields and get/set methods to Session.java  
 *
 * A Simple LRU Cache with timeout intended to store arbitrary state for a "session"
 * 
 * A Session is identified by an (MailboxId, SessionID) pair.  A single account may have
 * multiple active sessions simultaneously.
 * 
 */
public final class SessionCache 
{ 
    public static void notifyPendingChanges(String accountId, PendingModifications pns) {
        Iterator iter = getInstance().getSessionIteratorForAccount(accountId);
        while (iter != null && iter.hasNext())
            try {
                Session session = (Session) iter.next();
                session.notifyPendingChanges(pns);
            } catch (ConcurrentModificationException cme) {
                break;
            } catch (Exception e) {
                mLog.warn("ignoring exception during notification", e);
            }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Usable APIs here...
    ///////////////////////////////////////////////////////////////////////////

    public static final int SESSION_SOAP = 1;
    public static final int SESSION_IMAP = 2;

    /**
     * @param sessionType the type of session (SOAP, IMAP, etc.) we need
     * @return a brand-new session for this account.  
     */
    public Session getNewSession(String accountId, int sessionType) {
        if (mShutdown)
            return null;
        Long sessionId = getNextSessionId();
        Session session = null;
        switch (sessionType) {
            default:
            case SESSION_SOAP:  session = new SoapSession(accountId, sessionId);  break;
            case SESSION_IMAP:  session = new ImapSession(accountId, sessionId);  break;
        }
        updateInCache(sessionId, session);
        return session;
    }

    /**
     * Implicitly updates the LRU timestamp
     * 
     * @param sessionId
     * @return
     */
    public Session lookup(Object sessionId, String accountId) {
        if (mShutdown) { return null; }
        synchronized(mLRUMap) {
            Session session = (Session)mLRUMap.get(sessionId);
            if (null != session) {
                if (session.validateAccountId(accountId)) {
                    updateInCache(sessionId, session);
                    return session;
                } else {
                    mLog.info("Account ID Mismatch in session cache.  Requested " + accountId + ":" + sessionId +
                              ", found " + session.getAccountId());
                }
            }
            return null;
        }
    }

    /**
     * Immediately remove this session from the session cache 
     * 
     * @param sessionId
     * @param accountId
     */
    public void clearSession(Object sessionId, String accountId) {
        if (!mShutdown) {
            synchronized(mLRUMap) {
                Session session = (Session) mLRUMap.remove(sessionId);
                if (session != null)
                    session.doCleanup();
            }
        }
    }
    
    public static SessionCache getInstance() {
        return sInstance;
    }
    
    public void shutdown() {
        synchronized(mLRUMap) {
            mLog.info("Shutdown: Clearing SessionCache");
            
            // empty the lru cache
            Iterator iter = mLRUMap.values().iterator();
            while (iter.hasNext()) {
                Session session = (Session) iter.next();
                iter.remove();
                session.doCleanup();
                assert(mLRUMap.get(session.getSessionId()) == null);
            }
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////
    // Internals below here... 
    //////////////////////////////////////////////////////////////////////////////

    // TODO make me configurable!
    private static final long SESSION_CACHE_TIMEOUT_MSEC = 10 * 60 * 1000;
    private static final long SESSION_SWEEP_INTERVAL_MSEC = 60 * 1000;  // every minute
    
    static Log mLog = LogFactory.getLog(SessionCache.class);

    /**
     * 
     */
    private SessionCache() {
        super();
        Zimbra.sTimer.schedule(new SweepMapTimerTask(), 30000, SESSION_SWEEP_INTERVAL_MSEC);
    }
    
    private boolean mShutdown = false;
    
    synchronized private Long getNextSessionId() {
        return new Long(sContextSeqNo++);
    }
    
    static private long sContextSeqNo = 1;
    
    private static SessionCache sInstance = new SessionCache();
    
    
    LinkedHashMap /* Object SessionId -> Session */ mLRUMap = new LinkedHashMap(500);
    
    HashMap /* String AccountId -> ArrayList of Sessions */ mAcctToSessionMap = new HashMap(500);
    
    private final class SweepMapTimerTask extends TimerTask {
        public void run() {
            int active, removed;
            long start = System.currentTimeMillis();
            synchronized(SessionCache.this.mLRUMap) {
                int sizeBefore = SessionCache.this.mLRUMap.size();
                long cutoffTime = new Date().getTime() - SESSION_CACHE_TIMEOUT_MSEC;
                Iterator iter = SessionCache.this.mLRUMap.values().iterator();
                while (iter.hasNext()) {
                    Session imp = (Session) iter.next();
                    if (!imp.accessedAfter(cutoffTime)) {
                        mLog.debug("Removing cached session: " + imp.toString());
                        iter.remove();
                        imp.doCleanup();
                        assert(SessionCache.this.mLRUMap.get(imp.getSessionId()) == null);
                    } else {
                        // mLRUMap iterates by last access order, most recently
                        // accessed element last.  We can break here because
                        // all later sessions are guaranteed not to be expired.
                        break;
                    }
                }
                active = SessionCache.this.mLRUMap.size();
                removed = sizeBefore - active;
            }
            if (removed > 0 || active > 0) {
                long elapsed = System.currentTimeMillis() - start;
                mLog.info("idle session sweep: removed=" + removed + ", active=" + active + " (" + elapsed + "ms)");
            }
        }
    }

    private void updateInCache(Object sessionId, Session session) {
        synchronized(mLRUMap) {
            mLRUMap.remove(sessionId);
            session.updateAccessTime();
            mLRUMap.put(sessionId, session);
        }
    }
    
    void mapAccountToSession(String accountId, Session session) {
        synchronized(mLRUMap) {
            ArrayList list = (ArrayList) mAcctToSessionMap.get(accountId);
            if (list == null) {
                list = new ArrayList();
                mAcctToSessionMap.put(accountId, list);
            }
            assert(!list.contains(session));
            list.add(session);
        }
    }
    
    void removeSessionFromAccountMap(String accountId, Session session) {
        synchronized(mLRUMap) {
            ArrayList list = (ArrayList) mAcctToSessionMap.get(accountId);
            assert(list != null);
            if (list != null) {
                list.remove(session);
                if (list.size() == 0)
                    mAcctToSessionMap.remove(accountId);
            }
        }
    }
    
    private Iterator getSessionIteratorForAccount(String accountId) {
        synchronized(mLRUMap) {
            ArrayList list = (ArrayList) mAcctToSessionMap.get(accountId);
            if (list != null)
                return list.iterator();
        }
        return null;
    }
}
