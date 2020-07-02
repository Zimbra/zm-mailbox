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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
@JsonTypeInfo(
          use = JsonTypeInfo.Id.NAME, 
          include = JsonTypeInfo.As.PROPERTY, 
          property = "type")
@JsonSubTypes({ 
          @Type(value = SomeAccountsWaitSet.class, name = "someAccountWaitSet"), 
          @Type(value = AllAccountsWaitSet.class, name = "allAccountsWaitSet") 
        })
public abstract class WaitSetBase implements IWaitSet {
    private static final long serialVersionUID = 1L;

    public WaitSetBase() {
        super();
    }

    protected String mWaitSetId;
    protected String mOwnerAccountId;
    protected Set<MailItem.Type> defaultInterest;

    protected long mLastAccessedTime = -1;
    protected WaitSetCallback mCb = null;

    /**
     * List of errors (right now, only mailbox deletion notifications) to be sent
     */
    protected List<WaitSetError> mCurrentErrors = new ArrayList<WaitSetError>();
    protected List<WaitSetError> mSentErrors = new ArrayList<WaitSetError>();

    /** this is the signalled set data that is new (has never been sent) */
    protected HashSet<String /*accountId*/> mCurrentSignalledAccounts = Sets.newHashSet();
    protected HashSet<WaitSetSession> mCurrentSignalledSessions = Sets.newHashSet();
    protected Map<String /*accountId*/, PendingModifications> currentPendingModifications = Maps.newHashMap();

    /** this is the signalled set data that we've already sent, it just hasn't been acked yet */
    protected HashSet<String /*accountId*/> mSentSignalledAccounts = Sets.newHashSet();
    protected HashSet<WaitSetSession /*accountId*/> mSentSignalledSessions = Sets.newHashSet();
    protected Map<String /*accountId*/, PendingModifications> sentPendingModifications = Maps.newHashMap();

    abstract protected Map<String, WaitSetAccount> destroy();
    abstract protected int countSessions();
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
    public void setDefaultInterest(Set<MailItem.Type> defaultInterest) {
        this.defaultInterest = defaultInterest;
    }
    @JsonIgnore
    @Override
    public String getOwnerAccountId() {
        return mOwnerAccountId;
    }
    public String getmOwnerAccountId() {
        return mOwnerAccountId;
    }
    public void setmOwnerAccountId(String mOwnerAccountId) {
        this.mOwnerAccountId = mOwnerAccountId;
    }
    @JsonIgnore
    @Override
    public String getWaitSetId() {
        return mWaitSetId;
    }
    public String getmWaitSetId() {
        return mWaitSetId;
    }
    public void setmWaitSetId(String mWaitSetId) {
        this.mWaitSetId = mWaitSetId;
    }
    /**
     * @return the currentPendingModifications
     */
    public Map<String, PendingModifications> getCurrentPendingModifications() {
        return currentPendingModifications;
    }
    /**
     * @param currentPendingModifications the currentPendingModifications to set
     */
    public void setCurrentPendingModifications(Map<String, PendingModifications> currentPendingModifications) {
        this.currentPendingModifications = currentPendingModifications;
    }
    /**
     * @return the sentPendingModifications
     */
    public Map<String, PendingModifications> getSentPendingModifications() {
        return sentPendingModifications;
    }
    /**
     * @param sentPendingModifications the sentPendingModifications to set
     */
    public void setSentPendingModifications(Map<String, PendingModifications> sentPendingModifications) {
        this.sentPendingModifications = sentPendingModifications;
    }
    protected synchronized WaitSetCallback getCb() { return mCb; }
    /**
     * @return the mCb
     */
    public WaitSetCallback getmCb() {
        return mCb;
    }
    /**
     * @param mCb the mCb to set
     */
    public void setmCb(WaitSetCallback mCb) {
        this.mCb = mCb;
    }
    /**
     * @return the mCurrentErrors
     */
    public List<WaitSetError> getmCurrentErrors() {
        return mCurrentErrors;
    }
    /**
     * @param mCurrentErrors the mCurrentErrors to set
     */
    public void setmCurrentErrors(List<WaitSetError> mCurrentErrors) {
        this.mCurrentErrors = mCurrentErrors;
    }
    /**
     * @return the mSentErrors
     */
    public List<WaitSetError> getmSentErrors() {
        return mSentErrors;
    }
    /**
     * @param mSentErrors the mSentErrors to set
     */
    public void setmSentErrors(List<WaitSetError> mSentErrors) {
        this.mSentErrors = mSentErrors;
    }
    /**
     * @return the mCurrentSignalledAccounts
     */
    public HashSet<String> getmCurrentSignalledAccounts() {
        return mCurrentSignalledAccounts;
    }
    /**
     * @param mCurrentSignalledAccounts the mCurrentSignalledAccounts to set
     */
    public void setmCurrentSignalledAccounts(HashSet<String> mCurrentSignalledAccounts) {
        this.mCurrentSignalledAccounts = mCurrentSignalledAccounts;
    }
    /**
     * @return the mCurrentSignalledSessions
     */
    public HashSet<WaitSetSession> getmCurrentSignalledSessions() {
        return mCurrentSignalledSessions;
    }
    /**
     * @param mCurrentSignalledSessions the mCurrentSignalledSessions to set
     */
    public void setmCurrentSignalledSessions(HashSet<WaitSetSession> mCurrentSignalledSessions) {
        this.mCurrentSignalledSessions = mCurrentSignalledSessions;
    }
    /**
     * @return the mSentSignalledAccounts
     */
    public HashSet<String> getmSentSignalledAccounts() {
        return mSentSignalledAccounts;
    }
    /**
     * @param mSentSignalledAccounts the mSentSignalledAccounts to set
     */
    public void setmSentSignalledAccounts(HashSet<String> mSentSignalledAccounts) {
        this.mSentSignalledAccounts = mSentSignalledAccounts;
    }
    /**
     * @return the mSentSignalledSessions
     */
    public HashSet<WaitSetSession> getmSentSignalledSessions() {
        return mSentSignalledSessions;
    }
    /**
     * @param mSentSignalledSessions the mSentSignalledSessions to set
     */
    public void setmSentSignalledSessions(HashSet<WaitSetSession> mSentSignalledSessions) {
        this.mSentSignalledSessions = mSentSignalledSessions;
    }

    /**
     * Cancel any existing callback
     */
    protected synchronized void cancelExistingCB() {
        if (mCb != null) {
            // cancel the existing waiter
            mCb.dataReadySetCanceled(this, "");
            ZimbraLog.session.trace("WaitSetBase.cancelExistingCB - setting mCb null");
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }

    /**
     * Called to signal that the supplied WaitSetCallback should not be notified of any more changes
     * @param myCb - the callback that will no longer accept change notifications
     */
    @Override
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public synchronized void doneWaiting(WaitSetCallback myCb) {
        mLastAccessedTime = System.currentTimeMillis();
        boolean sameObject = (mCb == null);
        if (sameObject) {
            return;
        }
        if ((myCb == null) || (mCb == myCb)) {
            ZimbraLog.session.debug("WaitSetBase.doneWaiting - setting mCb null");
            mCb = null;
        } else {
            // This happens when the callers request has been canceled by a newer request
            ZimbraLog.session.debug("WaitSetBase.doneWaiting - saved callback NOT ours so NOT making null");
        }
    }

    protected WaitSetBase(String ownerAccountId, String waitSetId, Set<MailItem.Type> defaultInterest) {
        mOwnerAccountId = ownerAccountId;
        mWaitSetId = waitSetId;
        this.defaultInterest = defaultInterest;
    }

    protected synchronized void trySendData() {
        if (mCb == null) {
            ZimbraLog.session.trace("WaitSetBase.trySendData - no callback listening");
            return;
        }

        ZimbraLog.session.trace("WaitSetBase.trySendData 1 cb=%s", mCb);
        boolean cbIsCurrent = cbSeqIsCurrent();

        if (cbIsCurrent) {
            mSentSignalledAccounts.clear();
            mSentSignalledSessions.clear();
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
        if ((mCurrentSignalledAccounts.size() > 0 || mCurrentErrors.size() > 0) ||
                        (!cbIsCurrent && (mSentSignalledSessions.size() > 0 || mSentErrors.size() > 0 || mSentSignalledAccounts.size() > 0))) {
            // if sent is empty, then just swap sent,current instead of copying
            if (mSentSignalledAccounts.size() == 0) {
                ZimbraLog.session.trace("WaitSetBase.trySendData 2a");
                // SWAP sent <->current
                HashSet<String> tempAccounts = mCurrentSignalledAccounts;
                mCurrentSignalledAccounts = mSentSignalledAccounts;
                mSentSignalledAccounts = tempAccounts;
                HashSet<WaitSetSession> tempSessions = mCurrentSignalledSessions;
                mCurrentSignalledSessions = mSentSignalledSessions;
                mSentSignalledSessions = tempSessions;
                Map<String, PendingModifications> tempNotifications = currentPendingModifications;
                currentPendingModifications = sentPendingModifications;
                sentPendingModifications = tempNotifications;
            } else {
                ZimbraLog.session.trace("WaitSetBase.trySendData 2b");
                assert(!cbIsCurrent);
                mSentSignalledAccounts.addAll(mCurrentSignalledAccounts);
                mCurrentSignalledAccounts.clear();
                mSentSignalledSessions.addAll(mCurrentSignalledSessions);
                mCurrentSignalledSessions.clear();
                sentPendingModifications.putAll(currentPendingModifications);
                currentPendingModifications.clear();
            }

            // error list
            mSentErrors.addAll(mCurrentErrors);
            mCurrentErrors.clear();

            assert(mSentSignalledAccounts.size() > 0 || mSentErrors.size() > 0);
            ZimbraLog.session.trace("WaitSetBase.trySendData 3");
            mCb.dataReady(this, toNextSeqNo(), false, mSentErrors, mSentSignalledSessions, mSentSignalledAccounts, sentPendingModifications);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
        ZimbraLog.session.trace("WaitSetBase.trySendData done");
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
        if (mCurrentSignalledAccounts.size() > 0) {
            StringBuilder signaledStr = new StringBuilder();
            for (String accountId : mCurrentSignalledAccounts) {
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
}
