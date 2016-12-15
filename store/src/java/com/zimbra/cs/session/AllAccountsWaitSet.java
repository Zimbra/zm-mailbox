/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.soap.admin.type.BufferedCommitInfo;
import com.zimbra.soap.admin.type.WaitSetInfo;

/**
 * An implementation of IWaitSet that listens across all accounts on the server
 */
public final class AllAccountsWaitSet extends WaitSetBase {

    private static Map<AllAccountsWaitSet, String> sAllAccountsWaitSets = new ConcurrentHashMap<AllAccountsWaitSet, String>();
    private static volatile Set<MailItem.Type> interestTypes = EnumSet.noneOf(MailItem.Type.class);

    /** Callback from the Mailbox object when a transaction has completed in some Mailbox */
    public static final void mailboxChangeCommitted(String commitIdStr, String accountId,
            Set<MailItem.Type> changedTypes) {
        if (!Collections.disjoint(changedTypes, interestTypes)) {
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets.keySet()) {
                ws.onMailboxChangeCommitted(commitIdStr, accountId, changedTypes);
            }
        }
    }

    public static final boolean isCallbackNecessary(Set<MailItem.Type> types) {
        return !Collections.disjoint(types, interestTypes);
    }

    /**
     * Used for creating a brand new AllAccountsWaitSet
     */
    static AllAccountsWaitSet create(String ownerAccountId, String id, Set<MailItem.Type> defaultInterest) {
        return new AllAccountsWaitSet(ownerAccountId, id, defaultInterest, false);
    }

    /**
     * Used for creating an AllAccountsWaitSet when we've got a seqno -- basically this happens when the client's WaitSet
     * has gone away and the client needs to re-sync.
     */
    static AllAccountsWaitSet createWithSeqNo(String ownerAccountId, String id, Set<MailItem.Type> defaultInterest,
            String lastKnownSeqNo) throws ServiceException {
        AllAccountsWaitSet ws = new AllAccountsWaitSet(ownerAccountId, id, defaultInterest, true);
        boolean success = false;
        try {
            // get us up to date
            ws.syncToCommitId(lastKnownSeqNo);
            success = true;
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException when syncing waitset to specified commit ID", e);
        } finally {
            if (!success) {
                ws.destroy();  // Remove from sAllAccountsWaitSets map to prevent memory leak.
                ws = null;
            }
        }
        return ws;
    }

    /** private constructor */
    private AllAccountsWaitSet(String ownerAccountId, String id, Set<MailItem.Type> defaultInterest,
            boolean bufferCommitsAtCreate) {
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
            if (ZimbraLog.session.isDebugEnabled()) {
                ZimbraLog.session.debug("added: sAllAccountsWaitSets.size() = " + sAllAccountsWaitSets.size());
            }

            // update the static interest mask
            Set<MailItem.Type> types = EnumSet.noneOf(MailItem.Type.class);
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets.keySet()) {
                types.addAll(ws.getDefaultInterest());
            }
            interestTypes = types;
        }
    }

    @Override
    public List<WaitSetError> removeAccounts(List<String> removeAccounts) {
        // do nothing
        return new ArrayList<WaitSetError>();
    }

    @Override
    public synchronized List<WaitSetError> doWait(WaitSetCallback cb, String lastKnownSeqNo,
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts)
        throws ServiceException {

        cancelExistingCB();

        // figure out if there is already data here
        mCb = cb;
        mCbSeqNo = lastKnownSeqNo;
        trySendData();

        return new ArrayList<WaitSetError>();
    }

    private synchronized void onMailboxChangeCommitted(String commitIdStr, String accountId,
            Set<MailItem.Type> changedTypes) {
        if (!Collections.disjoint(changedTypes, defaultInterest)) {
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

    @Override
    protected boolean cbSeqIsCurrent() {
        return (mCurrentSeqNo.equals(mCbSeqNo));
    }


    @Override
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
    Map<String, WaitSetAccount> destroy() {
        synchronized(sAllAccountsWaitSets) {
            sAllAccountsWaitSets.remove(this);
            if (ZimbraLog.session.isDebugEnabled()) {
                ZimbraLog.session.debug("removed: sAllAccountsWaitSets.size() = " + sAllAccountsWaitSets.size());
            }

            // update the static interest mask
            Set<MailItem.Type> types = EnumSet.noneOf(MailItem.Type.class);
            for (AllAccountsWaitSet ws : sAllAccountsWaitSets.keySet()) {
                types.addAll(ws.getDefaultInterest());
            }
            interestTypes = types;
        }
        return null;
    }

    @Override
    public synchronized WaitSetInfo handleQuery() {
        WaitSetInfo info = super.handleQuery();
        info.setCbSeqNo(mCbSeqNo);
        info.setCurrentSeqNo(mCurrentSeqNo);
        info.setNextSeqNo(mNextSeqNo);

        if (mBufferedCommits != null) {
            for (Pair<String, String> p : mBufferedCommits) {
                info.addBufferedCommit(new BufferedCommitInfo(p.getFirst(), p.getSecond()));
            }
        }
        return info;
    }

    /** If non-null, then we're buffering the commits during creation */
    private List<Pair<String/*AccountId*/, String/*CommitId*/>> mBufferedCommits;

    private String mCbSeqNo; // seqno returned by the most recent callback
    private String mCurrentSeqNo;
    private String mNextSeqNo; // set to the commitId of the most recently signalled event....we use this to update mCurrentSeqNo when we send data..
}
