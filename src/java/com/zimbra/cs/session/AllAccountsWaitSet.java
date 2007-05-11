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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
    
    private static HashSet<AllAccountsWaitSet> sAllAccountsWaitSets = new LinkedHashSet<AllAccountsWaitSet>();
    private static int sInterestMask = 0;
    
    /** Callback from the Mailbox object when a transaction has completed in some Mailbox */
    public static final void mailboxChangeCommitted(String commitIdStr, String accountId, int changedTypesMask) {
        synchronized(sAllAccountsWaitSets) {
            if (sAllAccountsWaitSets.size() > 0 && ((changedTypesMask & sInterestMask) != 0)) {
                for (AllAccountsWaitSet ws : sAllAccountsWaitSets) {
                    ws.onMailboxChangeCommitted(commitIdStr, accountId, changedTypesMask);
                }
            }
        }
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
        synchronized(sAllAccountsWaitSets) {
            sAllAccountsWaitSets.add(this);
            
            // update the static interest mask
            sInterestMask = 0;
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets) {
                sInterestMask |= ws.getDefaultInterest();
            }
        }
        mCurrentSeqNo = "0";
        mCbSeqNo = "0";
        if (bufferCommitsAtCreate) {
            mBufferedCommits = new LinkedList<Pair<String,String>>();
        } else {
            mBufferedCommits = null;
        }
           
    }
    
    /* @see com.zimbra.cs.session.IWaitSet#doWait(com.zimbra.cs.session.WaitSetCallback, java.lang.String, boolean, java.util.List, java.util.List, java.util.List) */
    public List<WaitSetError> doWait(WaitSetCallback cb, String lastKnownSeqNo, boolean block,
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts, List<String> removeAccounts)
        throws ServiceException {
        
        cancelExistingCB();
        
        // figure out if there is already data here
        mCb = cb;
        mCbSeqNo = lastKnownSeqNo;
        trySendData();
        
        return new ArrayList<WaitSetError>();
    }
    
    /* @see com.zimbra.cs.session.IWaitSet#isIncludeAllAccounts() */
    public boolean isIncludeAllAccounts() { 
        return true;
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
    
    
    private synchronized final void trySendData() {
        if (mCb == null) {
            return;
        }
        
        boolean cbIsCurrent = (mCurrentSeqNo.equals(mCbSeqNo));
        
        if (cbIsCurrent) {
            mSentSignalledSessions.clear();
        }
        
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
                // SWAP mSent,mCurrent!
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
                toRet[i++] = accountId;
            }
            mCurrentSeqNo = mNextSeqNo;
            assert(mCurrentSeqNo != null);
            mCb.dataReady(this, mCurrentSeqNo, false, toRet);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
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
            sInterestMask = 0;
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets) {
                sInterestMask |= ws.getDefaultInterest();
            }
        }
        return null;
    }
    
    /** If non-null, then we're buffering the commits during creation */
    private List<Pair<String/*AccountId*/, String/*CommitId*/>> mBufferedCommits;
    
    private String mCbSeqNo; // seqno returned by the most recent callback
    private String mCurrentSeqNo;
    private String mNextSeqNo; // set to the commitId of the most recently signalled event....we use this to update mCurrentSeqNo when we send data.. 
}
