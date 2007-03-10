package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.cs.util.Zimbra;

/**
 * WaitSet: scalable mechanism for listening for changes to many accounts
 * 
 * External APIs:
 *     static WaitSet.create()       // WaitSet lifetime management
 *     static WaitSet.lookup()   
 *     static WaitSet.destroy()
 *     
 *     WaitSet.getDefaultInterest()  // accessor
 *     WaitSet.doWait()              // primary wait API
 */
public class WaitSet {
    /**
     * Simple struct used to define the parameters of an account during an add or update
     */
    public static class WaitSetAccount {
        public String accountId;
        public SyncToken lastKnownSyncToken;
        public int interests;
        
        public WaitSetAccount(String id, SyncToken sync, int interest) {
            this.accountId = id;
            this.lastKnownSyncToken = sync;
            this.interests = interest;
        }
    }
    
    /**
     * Simple struct used to communicate error codes for individual accounts during a wait 
     */
    public static class WaitSetError {
        public static enum Type {
            ALREADY_IN_SET_DURING_ADD,
            NOT_IN_SET_DURING_UPDATE,
            NOT_IN_SET_DURING_REMOVE,
            ;
        }
        
        public final String accountId;
        public final Type error;
        
        public WaitSetError(String accountId, Type error) {
            this.accountId = accountId;
            this.error = error;
        }
    }
    
    /**
     * Create a new WaitSet, optionally specifying an initial set of accounts
     * to start listening wait on
     * 
     * WaitSets are stored in a serverwide cache and are stamped with a last-accessed-time,
     * therefore callers should *not* cache the WaitSet pointer beyond a few seconds they
     * should instead use the lookup() API to fetch WaitSets between calls
     * 
     * @param defaultInterest
     * @param add
     * @return
     * @throws ServiceException
     */
    public static Object[] create(int defaultInterest, List<WaitSetAccount> add) throws ServiceException {
        synchronized(sWaitSets) {
            String id = "WaitSet"+sWaitSetNumber;
            sWaitSetNumber++;
            WaitSet ws = new WaitSet(id, defaultInterest);
            ws.mLastAccessedTime = System.currentTimeMillis();
            sWaitSets.put(id, ws);
            List<WaitSetError> errors = ws.addAccounts(add);
            
            Object[] toRet = new Object[2];
            toRet[0] = id;
            toRet[1] = errors;
            return toRet;
        }
    }
    
    /**
     * Find an active waitset.
     * 
     * WaitSets are stored in a serverwide cache and are stamped with a last-accessed-time,
     * therefore callers should *not* cache the WaitSet pointer beyond a few seconds they
     * should instead use the lookup() API to fetch WaitSets between calls
     *   
     * @param id
     * @return
     */
    public static WaitSet lookup(String id) {
        synchronized(sWaitSets) {
            WaitSet toRet = sWaitSets.get(id);
            if (toRet != null) {
                assert(!Thread.holdsLock(toRet));
                synchronized(toRet) { 
                    toRet.mLastAccessedTime = System.currentTimeMillis();
                }
            }
            return toRet;
        }
    }
    
    /**
     * Destroy the referenced WaitSet.  
     * 
     * @param id
     * @throws ServiceException
     */
    public static void destroy(String id) throws ServiceException {
        synchronized(sWaitSets) {
            WaitSet ws = lookup(id);
            if (ws == null)
                throw MailServiceException.NO_SUCH_WAITSET(id);
            assert(!Thread.holdsLock(ws));
            sWaitSets.remove(id);
            ws.destroy();
        }
    }
    
    /**
     * Called by timer in order to timeout unused WaitSets
     */
    private static void sweep() {
        int activeSets = 0;
        int activeSessions = 0;
        int removed = 0;
        int withCallback = 0;
        synchronized(sWaitSets) {
            long cutoffTime = System.currentTimeMillis() - WAITSET_TIMEOUT;
            
            for (Iterator<WaitSet> iter = sWaitSets.values().iterator(); iter.hasNext();) {
                WaitSet ws = iter.next();
                assert(!Thread.holdsLock(ws)); // must never lock WS before sWaitSets or deadlock
                synchronized(ws) {
                    // only timeout if no cb AND if not accessed for a timeout
                    if (ws.mCb == null && ws.mLastAccessedTime < cutoffTime) {
                        iter.remove();
                        ws.destroy();
                        removed++;
                    } else {
                        if (ws.mCb != null)
                            withCallback++;
                        activeSets++;
                        activeSessions+=ws.mNumActiveSessions;
                    }
                }
            }
        }
        if (removed > 0) {
            ZimbraLog.session.info("WaitSet sweeper timing out %d WaitSets due to inactivity", removed);
        }
        
        if (activeSets > 0) {
            ZimbraLog.session.info("WaitSet sweeper: %d active WaitSets (%d accounts) - %d sets with blocked callbacks",
                activeSets, activeSessions, withCallback);
        }
    }

    /**
     * Constructor 
     * 
     * @param id
     * @param defaultInterest
     * @throws ServiceException
     */
    private WaitSet(String id, int defaultInterest) throws ServiceException {
        mWaitSetId = id;
        mDefaultInterest = defaultInterest;
    }
    
    /**
     * Cleanup and remove all the sessions referenced by this WaitSet
     */
    private synchronized void destroy() {
        cancelExistingCB();
        for (WaitSetSession session : mWaitSets.values()) {
            session.doCleanup();
        }
        mWaitSets.clear();
        mNumActiveSessions = 0;
        mCurrentSignalledSets.clear();
        mSentSignalledSets.clear();
        mCurrentSeqNo = Integer.MAX_VALUE;
    }
    
    /**
     * Just a helper: the 'default interest' is set when the WaitSet is created,
     * and subsequent requests can access it when creating/updating WaitSetAccounts
     * if the client didn't specify one with the update.
     * 
     * @return
     */
    public int getDefaultInterest() { return mDefaultInterest; }

    
    /**
     * User-supplied callback which is set by doWait() and which is called when one 
     * or more of the waiting sessions has new data.
     */
    public static interface WaitSetCallback {
        void dataReady(WaitSet ws, int seqNo, boolean cancelled, String[] signalledAccounts);
    }

    
    /**
     * Primary API
     * 
     * WaitMultipleAccounts:  optionally modifies the wait set and checks
     * for any notifications.  If block=1 and there are no notificatins, then
     * this API will BLOCK until there is data.
     *
     * Client should always set 'seq' to be the highest known value it has
     * received from the server.  The server will use this information to
     * retransmit lost data.
     *
     * If the client sends a last known sync token then the notification is
     * calculated by comparing the accounts current token with the client's
     * last known.
     *
     * If the client does not send a last known sync token, then notification
     * is based on change since last Wait (or change since <add> if this
     * is the first time Wait has been called with the account)
     * 
     * @param cb
     * @param lastKnownSeqNo
     * @param block
     * @param addAccounts
     * @param updateAccounts
     * @param removeAccounts
     * @return
     * @throws ServiceException
     */
    public synchronized List<WaitSetError> doWait(WaitSetCallback cb, int lastKnownSeqNo, boolean block,   
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts, 
        List<String> removeAccounts) throws ServiceException {
        
        cancelExistingCB();
        
        if (lastKnownSeqNo >= mCurrentSeqNo) {
            throw ServiceException.INVALID_REQUEST("Sequence number too high: current is "+mCurrentSeqNo+
                " client claimed last-known was "+lastKnownSeqNo, null);
        }
        
        List<WaitSetError> errors = addAccounts(addAccounts);
        errors.addAll(updateAccounts(updateAccounts));
        errors.addAll(removeAccounts(removeAccounts));
        
        // figure out if there is already data here
        mCb = cb;
        mCbSeqNo = lastKnownSeqNo;
        trySendData();
        
        return errors;
    }
    
    /**
     * Called by the WaitSetSession when there is data to be signalled by this session
     * 
     * @param session
     */
    synchronized void signalDataReady(WaitSetSession session) {
        assert(mWaitSets.containsValue(session));
        if (mCurrentSignalledSets.add(session)) {
            trySendData();
        }
    }
    
    /**
     * @return TRUE if data sent, FALSE otherwise
     */
    private synchronized final void trySendData() {
        if (mCb == null)
            return;
        boolean cbIsCurrent = (mCbSeqNo == mCurrentSeqNo-1);
        
        if (cbIsCurrent)
            mSentSignalledSets.clear();
        
        /////////////////////
        // Cases:
        //
        // CB up to date 
        //   AND CurrentSignalled empty --> WAIT
        //   AND CurrentSignalled NOT empty --> SEND
        //
        // CB not up to date
        //   AND CurrentSignalled empty AND SendSignalled empty --> WAIT
        //   AND CurrentSignalled NOT empty OR SentSignalled NOT empty --> SEND BOTH
        //
        // ...simplifies to:
        //        send if CurrentSignalled NOT empty OR
        //                (CB not up to date AND SentSignalled not empty)
        //
        if (mCurrentSignalledSets.size() > 0 || (!cbIsCurrent && mSentSignalledSets.size() > 0)) {
            // if sent empty, then just swap sent,current instead of copying
            if (mSentSignalledSets.size() == 0) {
                // SWAP mSent,mCurrent! save an allocation
                HashSet<WaitSetSession> temp = mCurrentSignalledSets;
                mCurrentSignalledSets = mSentSignalledSets;
                mSentSignalledSets = temp;
            } else {
                assert(!cbIsCurrent);
                mSentSignalledSets.addAll(mCurrentSignalledSets);
                mCurrentSignalledSets.clear();
            }
            // at this point, mSentSignalled is everything we're supposed to send...lets
            // make an array of the account ID's and signal them up!
            assert(mSentSignalledSets.size() > 0);
            String[] toRet = new String[mSentSignalledSets.size()];
            int i = 0;
            for (WaitSetSession session : mSentSignalledSets) {
                toRet[i] = session.getAccountId();
            }
            mCb.dataReady(this, mCurrentSeqNo, false, toRet);
            mCurrentSeqNo++;
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Cancel any existing callback
     */
    private void cancelExistingCB() {
        assert(Thread.holdsLock(this));
        if (mCb != null) {
            // cancel the existing waiter
            mCb.dataReady(this, -1, true, null);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    private List<WaitSetError> addAccounts(List<WaitSetAccount> accts) throws ServiceException {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (WaitSetAccount acct : accts) {
            if (!mWaitSets.containsKey(acct.accountId)) {
                String sessionId = acct.accountId+"-"+mWaitSetId;
                WaitSetSession ws = new WaitSetSession(this, acct.accountId, sessionId, acct.interests, acct.lastKnownSyncToken);
                mWaitSets.put(acct.accountId, ws);
                mNumActiveSessions++;
            } else {
                errors.add(new WaitSetError(acct.accountId, WaitSetError.Type.ALREADY_IN_SET_DURING_ADD));
            }
        }
        return errors;
    }
    
    private List<WaitSetError> updateAccounts(List<WaitSetAccount> accts) throws ServiceException {
        assert(Thread.holdsLock(this));
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (WaitSetAccount acct : accts) {
            WaitSetSession set = mWaitSets.get(acct.accountId);
            if (set != null) {
                set.update(acct.interests, acct.lastKnownSyncToken);
                // update it!
            } else {
                errors.add(new WaitSetError(acct.accountId, WaitSetError.Type.NOT_IN_SET_DURING_UPDATE));
            }
        }
        return errors;
    }
    
    private List<WaitSetError> removeAccounts(List<String> accts) throws ServiceException {
        assert(Thread.holdsLock(this));
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (String id : accts) {
            WaitSetSession session = mWaitSets.remove(id); 
            if (session != null) {
                session.doCleanup();
                mNumActiveSessions--;
            } else {
                errors.add(new WaitSetError(id, WaitSetError.Type.NOT_IN_SET_DURING_REMOVE));
            }
        }
        return errors;
    }
    
    private static final TimerTask sSweeper = new TimerTask() { 
        public void run() { 
            WaitSet.sweep();
        }
    };
    
    public static void startup() {
        Zimbra.sTimer.schedule(sSweeper, WAITSET_SWEEP_DELAY, WAITSET_SWEEP_DELAY);
    }
    public static void shutdown() {
        sSweeper.cancel();
    }
    
    /** these are the accounts we are listening to... */
    private HashMap<String, WaitSetSession> mWaitSets = new HashMap<String, WaitSetSession>();

    /** this is the signalled set data that we've already sent, it just hasn't been acked yet */
    private HashSet<WaitSetSession> mSentSignalledSets = new HashSet<WaitSetSession>();
    
    /** this is the signalled set data that is new (has never been sent) */
    private HashSet<WaitSetSession> mCurrentSignalledSets = new HashSet<WaitSetSession>();
    
    private int mCurrentSeqNo = 1;
    private String mWaitSetId;
    private final int mDefaultInterest;
    private WaitSetCallback mCb = null;
    private int mCbSeqNo = 0;
    private long mLastAccessedTime = -1;
    private long mNumActiveSessions = 0;
    private static int sWaitSetNumber = 1;
    private static final HashMap<String, WaitSet> sWaitSets = new HashMap<String, WaitSet>();

    private static final int WAITSET_TIMEOUT = 1000 * 60 * 5; // 5min
    private static final int WAITSET_SWEEP_DELAY = 1000 * 60; // once every minute
}
