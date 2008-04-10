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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;

/**
 * An implementation of IWaitSet that listens across all accounts on the server
 */
public final class AllAccountsWaitSet extends WaitSetBase {
    
    private static Map<AllAccountsWaitSet, String> sAllAccountsWaitSets = new ConcurrentHashMap<AllAccountsWaitSet, String>();
    private static volatile int sInterestMask = 0;
    
    /** Callback from the Mailbox object when a transaction has completed in some Mailbox */
    public static final void mailboxChangeCommitted(String commitIdStr, String accountId, int changedTypesMask) {
        if ((changedTypesMask & sInterestMask) != 0) {
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets.keySet()) {
                ws.onMailboxChangeCommitted(commitIdStr, accountId, changedTypesMask);
            }
        }
    }
    
    public static final boolean isCallbackNecessary(int changeMask) {
        return (changeMask & sInterestMask) != 0;
    }
    
    /**
     * Used for creating a brand new AllAccountsWaitSet
     * 
     * @param ownerAccountId
     * @param id
     * @param defaultInterest
     * @return
     */
    static AllAccountsWaitSet create(String ownerAccountId, String id, int defaultInterest) {
        return new AllAccountsWaitSet(ownerAccountId, id, defaultInterest, false);
    }
    
    /**
     * Used for creating an AllAccountsWaitSet when we've got a seqno -- basically this happens when the client's WaitSet
     * has gone away and the client needs to re-sync
     * 
     * @param ownerAccountId
     * @param id
     * @param defaultInterest
     * @param lastKnownSeqNo
     * @return
     * @throws ServiceException
     */
    static AllAccountsWaitSet createWithSeqNo(String ownerAccountId, String id, int defaultInterest, String lastKnownSeqNo) throws ServiceException {
        AllAccountsWaitSet ws = new AllAccountsWaitSet(ownerAccountId, id, defaultInterest, true);
        try {
            // get us up to date
            ws.syncToCommitId(lastKnownSeqNo);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException when syncing waitset to specified commit ID", e);
        }
        return ws;
    }
    
    /** private constructor */
    private AllAccountsWaitSet(String ownerAccountId, String id, int defaultInterest, boolean bufferCommitsAtCreate) {
        super(ownerAccountId, id, defaultInterest);
        mCurrentSeqNo = "0";
        mCbSeqNo = "0";
        if (bufferCommitsAtCreate) {
            mBufferedCommits = new LinkedList<Pair<String,String>>();
        } else {
            mBufferedCommits = null;
        }
        
        // add us to the global set of AllAccounts waitsets, update the global interest mask
        synchronized(sAllAccountsWaitSets) {
            sAllAccountsWaitSets.put(this, "");
            
            // update the static interest mask
            int newMask = 0;
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets.keySet()) {
                newMask |= ws.getDefaultInterest();
            }
            sInterestMask = newMask;
        }
    }
    
    /* @see com.zimbra.cs.session.IWaitSet#doWait(com.zimbra.cs.session.WaitSetCallback, java.lang.String, boolean, java.util.List, java.util.List, java.util.List) */
    public List<WaitSetError> doWait(WaitSetCallback cb, String lastKnownSeqNo, 
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts, List<String> removeAccounts)
        throws ServiceException {
        
        cancelExistingCB();
        
        // figure out if there is already data here
        mCb = cb;
        mCbSeqNo = lastKnownSeqNo;
        trySendData();
        
        return new ArrayList<WaitSetError>();
    }
    
    private synchronized void onMailboxChangeCommitted(String commitIdStr, String accountId, int changedTypesMask) {
        if ((changedTypesMask & mDefaultInterest) != 0) {
            if (mBufferedCommits != null) {
                mBufferedCommits.add(new Pair<String/*acctId*/, String/*commitId*/>(accountId, commitIdStr));
            } else {
                mNextSeqNo = commitIdStr;
                mCurrentSignalledSessions.add(accountId);
                trySendData();
            }
        }
    }

    /**
     * Given a CommitId, bring this waitset into sync using the RedoLog system  
     * 
     * @param commitIdStr
     * @throws ServiceException
     * @throws IOException
     */
    private void syncToCommitId(String commitIdStr) throws ServiceException, IOException {
        assert(mBufferedCommits != null);
        assert(!Thread.holdsLock(this));
        
        //
        // Step one, go through the redo logs and get the redo logs set of mailboxes
        //
        RedoLogManager rmgr = RedoLogProvider.getInstance().getRedoLogManager();
        
        CommitId cid = CommitId.decodeFromString(commitIdStr);
        
        Pair<Set<Integer>, CommitId> changes = rmgr.getChangedMailboxesSince(cid);
        if (changes == null) {
            throw ServiceException.FAILURE("Unable to sync to commit id "+commitIdStr, null);
        }
        
        Set<Integer> mailboxes = changes.getFirst();
        
        for (Integer id : mailboxes) {
            try {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
                if (mbox != null) {
                    String accountId = mbox.getAccountId();
                    synchronized(this) {
                        mCurrentSignalledSessions.add(accountId);
                    }
                }
            } catch (ServiceException e) {
                ZimbraLog.session.warn("AllAccountsWaitSet skipping notification of mailbox id %d because we could not fetch it",
                    id, e);
            }
        }

        mNextSeqNo = changes.getSecond().encodeToString();
        
        
        //
        // Step two, process any buffered changes that have happened since we were created 
        //
        synchronized(this) {
            for (Pair<String/*acctid*/,String/*commitId*/> p : mBufferedCommits) {
                mCurrentSignalledSessions.add(p.getFirst());
                mNextSeqNo = p.getSecond();
            }
            
            // no more buffering!
            mBufferedCommits = null;
            
            if (mCurrentSignalledSessions.size() > 0) {
                trySendData();
            }
        }
    }
    
    protected boolean cbSeqIsCurrent() {
        return (mCurrentSeqNo.equals(mCbSeqNo));        
    }
    
    
    protected String toNextSeqNo() {
        mCurrentSeqNo = mNextSeqNo;
        assert(mCurrentSeqNo != null);
        return mCurrentSeqNo;
    }
    
    @Override
    int countSessions() {
        return 1;
    }
    
    @Override
    HashMap<String, WaitSetAccount> destroy() {
        synchronized(sAllAccountsWaitSets) {
            sAllAccountsWaitSets.remove(this);
            
            // update the static interest mask
            int newMask = 0;
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets.keySet()) {
                newMask |= ws.getDefaultInterest();
            }
            sInterestMask = newMask;
        }
        return null;
    }
    
    /** If non-null, then we're buffering the commits during creation */
    private List<Pair<String/*AccountId*/, String/*CommitId*/>> mBufferedCommits;
    
    private String mCbSeqNo; // seqno returned by the most recent callback
    private String mCurrentSeqNo;
    private String mNextSeqNo; // set to the commitId of the most recently signalled event....we use this to update mCurrentSeqNo when we send data.. 
}
