package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.zimbra.common.service.ServiceException;

public class WaitSet {
    
    // these are the accounts we are listening to...
    private HashMap<String, WaitSetSession> mWaitSets = new HashMap<String, WaitSetSession>();

    // this is the signalled set data that we've already sent, it just hasn't been acked yet
    private HashSet<WaitSetSession> mSentSignalledSets = new HashSet<WaitSetSession>();
    
    // this is the signalled set data that is new (has never been sent)
    private HashSet<WaitSetSession> mCurrentSignalledSets = new HashSet<WaitSetSession>();
    
    private int mCurrentSeqNo = 1;
    private String mWaitSetId;
    private final int mDefaultInterest;
    private WaitSetCallback mCb = null;
    private int mCbSeqNo = 0;
    
    public static final int INTEREST_MESSAGES       = 0x0001;
    public static final int INTEREST_CONTACTS       = 0x0002;
    public static final int INTEREST_APPOINTMENTS   = 0x0004;
    public static final int INTEREST_ALL            = 0xFFFF;
    
    public static interface WaitSetCallback {
        void dataReady(WaitSet ws, int seqNo, boolean cancelled, String[] signalledAccounts);
    }
    
    public static class WaitSetAccount {
        public String accountId;
        public int lastKnownSyncToken;
        public int interests;
        
        public WaitSetAccount(String id, int sync, int interest) {
            this.accountId = id;
            this.lastKnownSyncToken = sync;
            this.interests = interest;
        }
    }
    
    
    public static class WaitSetError {
        public static enum Type {
            ALREADY_IN_SET_DURING_ADD,
            NOT_IN_SET_DURING_UPDATE,
            NOT_IN_SET_DURING_REMOVE,
            ;
        }
        
        public String accountId;
        public Type error;
        
        public WaitSetError(String accountId, Type error) {
            this.accountId = accountId;
            this.error = error;
        }
    }
    
    private static int sWaitSetNumber = 1;
    private static final HashMap<String, WaitSet> sWaitSets = new HashMap<String, WaitSet>();
    
    public static Object[] create(int defaultInterest, List<WaitSetAccount> add) throws ServiceException {
        synchronized(sWaitSets) {
            String id = "WaitSet"+sWaitSetNumber;
            sWaitSetNumber++;
            WaitSet ws = new WaitSet(id, defaultInterest);
            sWaitSets.put(id, ws);
            List<WaitSetError> errors = ws.addAccounts(add);
            
            Object[] toRet = new Object[2];
            toRet[0] = id;
            toRet[1] = errors;
            return toRet;
        }
    }
    
    public static WaitSet lookup(String id) {
        synchronized(sWaitSets) {
            return sWaitSets.get(id);
        }
    }
    
    public static void destroy(String id) throws ServiceException {
        synchronized(sWaitSets) {
            WaitSet ws = lookup(id);
            if (ws == null)
                throw ServiceException.INVALID_REQUEST("Could not find WaitSet for id: " +id, null);
            sWaitSets.remove(id);
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
    
    private synchronized void destroy() {
        cancelExistingCB();
        for (WaitSetSession session : mWaitSets.values()) {
            session.doCleanup();
        }
        mWaitSets.clear();
        mCurrentSignalledSets.clear();
        mSentSignalledSets.clear();
        mCurrentSeqNo = Integer.MAX_VALUE;
    }
    
    public int getDefaultInterest() { return mDefaultInterest; }
    
    public synchronized List<WaitSetError> doWait(WaitSetCallback cb, int lastKnownSeqNo, boolean block,   
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts, 
        List<String> removeAccounts) throws ServiceException {
        
        assert(mCbSeqNo <= lastKnownSeqNo) : "CbSeqNo="+mCbSeqNo+" lastKnownSeqno="+lastKnownSeqNo;
        cancelExistingCB();
        
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
     * Cancel any existing callback
     */
    private void cancelExistingCB() {
        assert(Thread.holdsLock(this));
        if (mCb != null) {
            // cancel the existing waiter
            mCb.dataReady(this, -1, true, null);
            mCb = null;
        }
    }
    
    private List<WaitSetError> addAccounts(List<WaitSetAccount> accts) throws ServiceException {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (WaitSetAccount acct : accts) {
            if (!mWaitSets.containsKey(acct.accountId)) {
                WaitSetSession ws = new WaitSetSession(this, acct.accountId, acct.accountId+"-"+mWaitSetId);
                mWaitSets.put(acct.accountId, ws);
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
            } else {
                errors.add(new WaitSetError(id, WaitSetError.Type.NOT_IN_SET_DURING_REMOVE));
            }
        }
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
        }
    }
    
}
