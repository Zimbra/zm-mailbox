package com.zimbra.cs.session;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.util.SyncToken;

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
public class WaitSet implements MailboxManager.Listener {

    /**
     * Simple struct used to define the parameters of an account during an add or update
     */
    public static class WaitSetAccount {
        public WaitSetAccount(String id, SyncToken sync, int interest) {
            this.accountId = id;
            this.lastKnownSyncToken = sync;
            this.interests = interest;
            this.ref = null;
        }
        public WaitSetSession getSession() {
            if (ref != null) {
                WaitSetSession toRet = ref.get();
                if (toRet == null)
                    ref = null;
                return toRet;
            } else
                return null;
        }
        public String accountId;
        public int interests;
        
        public SyncToken lastKnownSyncToken;
        
        public SoftReference<WaitSetSession> ref;
    }
    
    /**
     * User-supplied callback which is set by doWait() and which is called when one 
     * or more of the waiting sessions has new data.
     */
    public static interface WaitSetCallback {
        void dataReady(WaitSet ws, long seqNo, boolean cancelled, String[] signalledAccounts);
    }

    /**
     * Simple struct used to communicate error codes for individual accounts during a wait 
     */
    public static class WaitSetError {
        public static enum Type {
            ALREADY_IN_SET_DURING_ADD,
            ERROR_LOADING_MAILBOX,
            MAINTENANCE_MODE,
            NO_SUCH_ACCOUNT,
            WRONG_HOST_FOR_ACCOUNT,
            NOT_IN_SET_DURING_REMOVE,
            NOT_IN_SET_DURING_UPDATE,
            ;
        }

        public WaitSetError(String accountId, Type error) {
            this.accountId = accountId;
            this.error = error;
        }
        public final String accountId;

        public final Type error;
    }

    public WaitSetCallback getCb() { 
        return mCb;
    }

    public boolean isIncludeAllAccounts() {
        return mIncludeAllAccounts;
    }

    public long getLastAccessedTime() {
        return mLastAccessedTime;
    }

    HashMap<String, WaitSetAccount> getSessions() {
        return mSessions;
    }

    void setCb(WaitSetCallback cb) {
        mCb = cb;
    }

    void setIncludeAllAccounts(boolean includeAllAccounts) {
        mIncludeAllAccounts = includeAllAccounts;
    }
    
    void setLastAccessedTime(long lastAccessedTime) {
        mLastAccessedTime = lastAccessedTime;
    }

    void setSessions(HashMap<String, WaitSetAccount> sessions) {
        mSessions = sessions;
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
    public synchronized List<WaitSetError> doWait(WaitSetCallback cb, long lastKnownSeqNo, boolean block,   
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts, 
        List<String> removeAccounts) throws ServiceException {
        
        cancelExistingCB();
        
        if (lastKnownSeqNo > mCurrentSeqNo) {
            throw ServiceException.INVALID_REQUEST("Sequence number too high: current is "+mCurrentSeqNo+
                " client claimed last-known was "+lastKnownSeqNo, null);
        }
        
        List<WaitSetError> errors = new LinkedList<WaitSetError>();
        
        if (addAccounts != null)
            errors.addAll(addAccounts(addAccounts));
        if (updateAccounts != null)
            errors.addAll(updateAccounts(updateAccounts));
        if (removeAccounts != null)
            errors.addAll(removeAccounts(removeAccounts));
        
        // figure out if there is already data here
        mCb = cb;
        mCbSeqNo = lastKnownSeqNo;
        trySendData();
        
        return errors;
    }
    
    /**
     * Just a helper: the 'default interest' is set when the WaitSet is created,
     * and subsequent requests can access it when creating/updating WaitSetAccounts
     * if the client didn't specify one with the update.
     * 
     * @return
     */
    public int getDefaultInterest() { return mDefaultInterest; }
    
    public String getWaitSetId() { return mWaitSetId; }
    
    /* @see com.zimbra.cs.mailbox.MailboxManager.Listener#mailboxAvailable(com.zimbra.cs.mailbox.Mailbox) */
    public synchronized void mailboxAvailable(Mailbox mbox) {
        this.mailboxLoaded(mbox);
    }
    
    /* @see com.zimbra.cs.mailbox.MailboxManager.Listener#mailboxCreated(com.zimbra.cs.mailbox.Mailbox) */
    public synchronized void mailboxCreated(Mailbox mbox) {
        this.mailboxLoaded(mbox);
    }
    
    /* @see com.zimbra.cs.mailbox.MailboxManager.Listener#mailboxLoaded(com.zimbra.cs.mailbox.Mailbox) */
    public synchronized void mailboxLoaded(Mailbox mbox) {
        WaitSetAccount wsa = mSessions.get(mbox.getAccountId());
        if (wsa == null && mIncludeAllAccounts) {
            wsa = new WaitSetAccount(mbox.getAccountId(), null, mDefaultInterest);
            mSessions.put(mbox.getAccountId(), wsa);
        }
        if (wsa != null) {
            WaitSetSession session = wsa.getSession();
            if (session == null) {
                // create a new session... 
                initializeWaitSetSession(wsa);
            }
        } 
    }

    synchronized List<WaitSetError> addAccounts(List<WaitSetAccount> wsas) throws ServiceException {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();

        for (WaitSetAccount wsa : wsas) {
            if (!mSessions.containsKey(wsa.accountId)) {
                // add the account to our session list  
                mSessions.put(wsa.accountId, wsa);
                
                try {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(wsa.accountId, MailboxManager.FetchMode.ONLY_IF_CACHED);
                    if (mbox != null) {
                        WaitSetError error = initializeWaitSetSession(wsa);
                        if (error != null) { 
                            errors.add(error);
                        }
                    }
                } catch (ServiceException e) {
                    if (e.getCode() == AccountServiceException.NO_SUCH_ACCOUNT)
                        errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.NO_SUCH_ACCOUNT));
                    else if (e.getCode() == ServiceException.WRONG_HOST)
                        errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.WRONG_HOST_FOR_ACCOUNT));
                    else 
                        errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX));
                    mSessions.remove(wsa);
                }                
                
            } else {
                errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.ALREADY_IN_SET_DURING_ADD));
            }
        }
        return errors;
    }
    
    synchronized List<WaitSetError> addAllAccounts() throws ServiceException {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        List<Mailbox> mboxes = MailboxManager.getInstance().getAllLoadedMailboxes();
        for (Mailbox mbox : mboxes) {
            WaitSetAccount wsa = new WaitSetAccount(mbox.getAccountId(), null, mDefaultInterest);
            mSessions.put(mbox.getAccountId(), wsa);
            WaitSetError error = initializeWaitSetSession(wsa);
            if (error != null) { 
                errors.add(error);
            }
        }
        
        mboxes.clear();
        return errors;
    }
    
    /**
     * Cancel any existing callback
     */
    private synchronized void cancelExistingCB() {
        if (mCb != null) {
            // cancel the existing waiter
            mCb.dataReady(this, -1, true, null);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Cleanup and remove all the sessions referenced by this WaitSet
     */
    synchronized HashMap<String, WaitSetAccount> destroy() {
        cancelExistingCB();
        HashMap<String, WaitSetAccount> toRet = mSessions;
        mSessions = new HashMap<String, WaitSetAccount>();
        mCurrentSignalledSessions.clear();
        mSentSignalledSessions.clear();
        mCurrentSeqNo = Integer.MAX_VALUE;
        return toRet;
   }
    
    synchronized WaitSetError initializeWaitSetSession(WaitSetAccount wsa) {
        WaitSetSession ws = new WaitSetSession(this, wsa.accountId, wsa.interests, wsa.lastKnownSyncToken);
        try {
            ws.register();
            wsa.ref = new SoftReference<WaitSetSession>(ws);
        } catch (MailServiceException e) {
            if (e.getCode().equals(MailServiceException.MAINTENANCE)) {
                return new WaitSetError(wsa.accountId, WaitSetError.Type.MAINTENANCE_MODE);
            } else {
                return new WaitSetError(wsa.accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX);
            }
        } catch (ServiceException e) {
            return new WaitSetError(wsa.accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX);
        }
        return null;
    }
    
    synchronized List<WaitSetError> removeAccounts(List<String> accts) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (String id : accts) {
            WaitSetAccount wsa = mSessions.get(id);
            if (wsa != null) {
                WaitSetSession session = wsa.getSession();
                if (session != null) {
                    session.doCleanup();
                }
            } else {
                errors.add(new WaitSetError(id, WaitSetError.Type.NOT_IN_SET_DURING_REMOVE));
            }
        }
        return errors;
    }
    /**
     * @return TRUE if data sent, FALSE otherwise
     */
    private synchronized final void trySendData() {
        if (mCb == null)
            return;
        boolean cbIsCurrent = (mCbSeqNo == mCurrentSeqNo);
        
        if (cbIsCurrent)
            mSentSignalledSessions.clear();
        
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
        if (mCurrentSignalledSessions.size() > 0 || (!cbIsCurrent && mSentSignalledSessions.size() > 0)) {
            // if sent empty, then just swap sent,current instead of copying
            if (mSentSignalledSessions.size() == 0) {
                // SWAP mSent,mCurrent! save an allocation
                HashSet<String> temp = mCurrentSignalledSessions;
                mCurrentSignalledSessions = mSentSignalledSessions;
                mSentSignalledSessions = temp;
            } else {
                assert(!cbIsCurrent);
                mSentSignalledSessions.addAll(mCurrentSignalledSessions);
                mCurrentSignalledSessions.clear();
            }
            // at this point, mSentSignalled is everything we're supposed to send...lets
            // make an array of the account IDs and signal them up!
            assert(mSentSignalledSessions.size() > 0);
            String[] toRet = new String[mSentSignalledSessions.size()];
            int i = 0;
            for (String accountId : mSentSignalledSessions) {
                toRet[i] = accountId;
            }
            mCurrentSeqNo++;
            mCb.dataReady(this, mCurrentSeqNo, false, toRet);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    private synchronized List<WaitSetError> updateAccounts(List<WaitSetAccount> wsas) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (WaitSetAccount wsa : wsas) {
            WaitSetAccount existing = mSessions.get(wsa.accountId);
            if (existing != null) {
                existing.interests = wsa.interests;
                existing.lastKnownSyncToken = existing.lastKnownSyncToken;
                WaitSetSession session = existing.getSession();
                if (session != null) {
                    session.update(existing.interests, existing.lastKnownSyncToken);
                    // update it!
                }
            } else {
                errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.NOT_IN_SET_DURING_UPDATE));
            }
        }
        return errors;
    }
    
    synchronized void cleanupSession(WaitSetSession session) {
        WaitSetAccount acct = mSessions.get(session.getAuthenticatedAccountId());
        if (acct != null && acct.ref != null) {
            WaitSetSession existing = acct.getSession();
            if (existing == session)
                acct.ref = null;
        }
    }
    
    /**
     * Called by the WaitSetSession when there is data to be signalled by this session
     * 
     * @param session
     */
    synchronized void signalDataReady(WaitSetSession session) {
        if (mSessions.containsKey(session.getAuthenticatedAccountId())) { // ...false if waitset is shutting down...
            if (mCurrentSignalledSessions.add(session.getAuthenticatedAccountId())) {
                trySendData();
            }
        }
    }
    
    /**
     * Constructor 
     * 
     * @param id
     * @param defaultInterest
     * @throws ServiceException
     */
    WaitSet(String id, int defaultInterest) {
        mWaitSetId = id;
        mDefaultInterest = defaultInterest;
        mCurrentSeqNo = 1;
    }
    
    /**
     * Constructor 
     * 
     * @param id
     * @param defaultInterest
     * @throws ServiceException
     */
    WaitSet(String id, int defaultInterest, boolean allAccounts, long startingSeqNo) {
        mWaitSetId = id;
        if (allAccounts != true)
            throw new IllegalArgumentException("AllAccounts must be true for this constructor");
        if (!id.startsWith(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX))
            throw new IllegalArgumentException("Invalid ID for all accounts WaitSet");
            
        mDefaultInterest = defaultInterest;
        mCurrentSeqNo = startingSeqNo;
    }
    
    
    private WaitSetCallback mCb = null;
    private long mCbSeqNo = 0; // seqno passed in by the current waiting callback
    private long mCurrentSeqNo; // current sequence number 
    
    /** this is the signalled set data that is new (has never been sent) */
    private HashSet<String /*accountId*/> mCurrentSignalledSessions = new HashSet<String>();
    
    private final int mDefaultInterest;
    private boolean mIncludeAllAccounts = false;
    private long mLastAccessedTime = -1;

    /** this is the signalled set data that we've already sent, it just hasn't been acked yet */
    private HashSet<String /*accountId*/> mSentSignalledSessions = new HashSet<String>();

    /** these are the accounts we are listening to.  Stores EITHER a WaitSetSession or an AccountID  */
    private HashMap<String, WaitSetAccount> mSessions = new HashMap<String, WaitSetAccount>();
    private String mWaitSetId;
}
