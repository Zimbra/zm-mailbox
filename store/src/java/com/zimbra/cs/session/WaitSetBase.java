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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.mail.WaitSetRequest;
import com.zimbra.soap.admin.type.AccountsAttrib;
import com.zimbra.soap.admin.type.WaitSetInfo;
import com.zimbra.soap.type.IdAndType;

/**
 * The base class defines shared functions, as well as any APIs which should be
 * package-private
 */
public abstract class WaitSetBase implements IWaitSet {
    abstract Map<String, WaitSetAccount> destroy();
    abstract int countSessions();
    abstract protected boolean cbSeqIsCurrent();
    abstract protected String toNextSeqNo();


    public long getLastAccessedTime() {
        return mLastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        mLastAccessedTime = lastAccessedTime;
    }

    @Override
    public Set<MailItem.Type> getDefaultInterest() {
        return defaultInterest;
    }

    @Override
    public String getOwnerAccountId() {
        return mOwnerAccountId;
    }

    @Override
    public String getWaitSetId() {
        return mWaitSetId;
    }

    synchronized WaitSetCallback getCb() { return mCb; }

    /**
     * Cancel any existing callback
     */
    protected synchronized void cancelExistingCB() {
        if (mCb != null) {
            // cancel the existing waiter
            mCb.dataReadySetCanceled(this, "");
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }

    @Override
    public synchronized void doneWaiting() {
        mCb = null;
        mLastAccessedTime = System.currentTimeMillis();
    }


    protected WaitSetBase(String ownerAccountId, String waitSetId, Set<MailItem.Type> defaultInterest) {
        mOwnerAccountId = ownerAccountId;
        mWaitSetId = waitSetId;
        this.defaultInterest = defaultInterest;
    }

    protected synchronized void trySendData() {
        boolean trace = ZimbraLog.session.isTraceEnabled();
        if (trace) ZimbraLog.session.trace("WaitSetBase.trySendData 1");

        if (mCb == null) {
            return;
        }

        if (trace) ZimbraLog.session.trace("WaitSetBase.trySendData 2");
        boolean cbIsCurrent = cbSeqIsCurrent();

        if (cbIsCurrent) {
            mSentSignalledSessions.clear();
            sentKnownChangedFolderIds.clear();
            sentPendingModifications.clear();
            mSentErrors.clear();
        }

        /////////////////////
        // Cases:
        //
        // CB up to date
        //   AND Current empty --> WAIT
        //   AND Current NOT empty --> SEND
        //
        // CB not up to date
        //   AND Current empty AND Sent empty --> WAIT
        //   AND (Current NOT empty OR Sent NOT empty) --> SEND BOTH
        //
        // ...simplifies to:
        //        send if Current NOT empty OR
        //                (CB not up to date AND Sent not empty)
        //
        if ((mCurrentSignalledSessions.size() > 0 || mCurrentErrors.size() > 0) ||
                        (!cbIsCurrent && (mSentSignalledSessions.size() > 0 || mSentErrors.size() > 0))) {
            // if sent is empty, then just swap sent,current instead of copying
            if (mSentSignalledSessions.size() == 0) {
                if (trace) ZimbraLog.session.trace("WaitSetBase.trySendData 3a");
                // SWAP sent <->current
                HashSet<String> temp = mCurrentSignalledSessions;
                mCurrentSignalledSessions = mSentSignalledSessions;
                mSentSignalledSessions = temp;
                Map<String /*accountId*/, Set<Integer /* folderId */>> tempFids = currentKnownChangedFolderIds;
                currentKnownChangedFolderIds = sentKnownChangedFolderIds;
                sentKnownChangedFolderIds = tempFids;
                Map<String, PendingModifications> tempNotifications = currentPendingModifications;
                currentPendingModifications = sentPendingModifications;
                sentPendingModifications = tempNotifications;
            } else {
                if (trace) ZimbraLog.session.trace("WaitSetBase.trySendData 3b");
                assert(!cbIsCurrent);
                mSentSignalledSessions.addAll(mCurrentSignalledSessions);
                mCurrentSignalledSessions.clear();
                sentKnownChangedFolderIds.putAll(currentKnownChangedFolderIds);
                currentKnownChangedFolderIds.clear();;
                sentPendingModifications.putAll(currentPendingModifications);
                currentPendingModifications.clear();
            }

            // error list
            mSentErrors.addAll(mCurrentErrors);
            mCurrentErrors.clear();

            assert(mSentSignalledSessions.size() > 0  || mSentErrors.size() > 0);
            if (trace) {
                ZimbraLog.session.trace("WaitSetBase.trySendData 4");
            }
            mCb.dataReady(this, toNextSeqNo(), false, mSentErrors, mSentSignalledSessions, sentKnownChangedFolderIds, sentPendingModifications);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
        if (trace) {
            ZimbraLog.session.trace("WaitSetBase.trySendData done");
        }
    }

    @Override
    public synchronized WaitSetInfo handleQuery() {
        WaitSetInfo info = WaitSetInfo.createForWaitSetIdOwnerInterestsLastAccessDate(mWaitSetId, mOwnerAccountId,
                WaitSetRequest.expandInterestStr(defaultInterest), mLastAccessedTime);

        if (mCurrentErrors.size() > 0) {
            for (WaitSetError error : mCurrentErrors) {
                info.addError(new IdAndType(error.accountId, error.error.name()));
            }
        }

        // signaled accounts
        if (mCurrentSignalledSessions.size() > 0) {
            StringBuilder signaledStr = new StringBuilder();
            for (String accountId : mCurrentSignalledSessions) {
                if (signaledStr.length() > 0)
                    signaledStr.append(",");
                signaledStr.append(accountId);
            }
            info.setSignalledAccounts(new AccountsAttrib(signaledStr.toString()));
        }
        return info;
    }

    protected synchronized void signalError(WaitSetError err) {
        mCurrentErrors.add(err);
        trySendData();
    }

    protected synchronized void addChangeFolderIds(Map<String, Set<Integer>> folderIdsMap,
            String acctId, Set<Integer> changedFolderIds) {
        Set<Integer> fids = folderIdsMap.get(acctId);
        if (fids == null) {
            fids = Sets.newHashSet();
            folderIdsMap.put(acctId, fids);
        }
        fids.addAll(changedFolderIds);
    }

    protected synchronized void addMods(Map<String, PendingModifications> mods, String acctId, PendingModifications mod) {
        mods.put(acctId, mod);
    }
    protected final String mWaitSetId;
    protected final String mOwnerAccountId;
    protected final Set<MailItem.Type> defaultInterest;

    protected long mLastAccessedTime = -1;
    protected WaitSetCallback mCb = null;

    /**
     * List of errors (right now, only mailbox deletion notifications) to be sent
     */
    protected List<WaitSetError> mCurrentErrors = new ArrayList<WaitSetError>();
    protected List<WaitSetError> mSentErrors = new ArrayList<WaitSetError>();

    /** this is the signalled set data that is new (has never been sent) */
    protected HashSet<String /*accountId*/> mCurrentSignalledSessions = Sets.newHashSet();
    protected Map<String /*accountId*/, Set<Integer /* folderId */>> currentKnownChangedFolderIds = Maps.newHashMap();
    protected Map<String /*accountId*/, PendingModifications> currentPendingModifications = Maps.newHashMap();

    /** this is the signalled set data that we've already sent, it just hasn't been acked yet */
    protected HashSet<String /*accountId*/> mSentSignalledSessions = Sets.newHashSet();
    protected Map<String /*accountId*/, Set<Integer /* folderId */>> sentKnownChangedFolderIds = Maps.newHashMap();
    protected Map<String /*accountId*/, PendingModifications> sentPendingModifications = Maps.newHashMap();
}
