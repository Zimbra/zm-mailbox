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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxManager.FetchMode;
import com.zimbra.cs.service.mail.WaitSetRequest;
import com.zimbra.soap.admin.type.SessionForWaitSet;
import com.zimbra.soap.admin.type.WaitSetInfo;
import com.zimbra.soap.admin.type.WaitSetSessionInfo;

/**
 * SomeAccountsWaitSet: an implementation of IWaitSet that works by listening over one or more Accounts
 *
 * External APIs:
 *     WaitSet.doWait()              // primary wait API
 *     WaitSet.getDefaultInterest()  // accessor
 */
public final class SomeAccountsWaitSet extends WaitSetBase implements MailboxManager.Listener {

    private long mCbSeqNo = 0; // seqno passed in by the current waiting callback
    private long mCurrentSeqNo; // current sequence number

    /** these are the accounts we are listening to.  Stores EITHER a WaitSetSession or an AccountID  */
    private HashMap<String, WaitSetAccount> mSessions = new HashMap<String, WaitSetAccount>();

    /** Constructor */
    public SomeAccountsWaitSet() {
        // do nothing
    }
    SomeAccountsWaitSet(String ownerAccountId, String id, Set<MailItem.Type> defaultInterest) {
        super(ownerAccountId, id, defaultInterest);
        mCurrentSeqNo = 1;
    }

    /**
     * @return the mCbSeqNo
     */
    public long getmCbSeqNo() {
        return mCbSeqNo;
    }
    /**
     * @param mCbSeqNo the mCbSeqNo to set
     */
    public void setmCbSeqNo(long mCbSeqNo) {
        this.mCbSeqNo = mCbSeqNo;
    }
    /**
     * @return the mCurrentSeqNo
     */
    public long getmCurrentSeqNo() {
        return mCurrentSeqNo;
    }
    /**
     * @param mCurrentSeqNo the mCurrentSeqNo to set
     */
    public void setmCurrentSeqNo(long mCurrentSeqNo) {
        this.mCurrentSeqNo = mCurrentSeqNo;
    }
    /**
     * @return the mSessions
     */
    public HashMap<String, WaitSetAccount> getmSessions() {
        return mSessions;
    }
    /**
     * @param mSessions the mSessions to set
     */
    public void setmSessions(HashMap<String, WaitSetAccount> mSessions) {
        this.mSessions = mSessions;
    }

    @Override
    public List<WaitSetError> removeAccounts(List<String> accts) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();

        for (String id : accts) {
            WaitSetSession session = null;
            synchronized(this) {
                WaitSetAccount wsa = mSessions.get(id);
                if (wsa != null) {
                    session = wsa.getSession();
                    mSessions.remove(id);
                } else {
                    errors.add(new WaitSetError(id, WaitSetError.Type.NOT_IN_SET_DURING_REMOVE));
                }
            }
            if (session != null) {
                assert(!Thread.holdsLock(this));
                session.doCleanup();
            }
        }
        return errors;
    }

    @Override
    public synchronized List<WaitSetError> doWait(WaitSetCallback cb, String lastKnownSeqNo,
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts) throws ServiceException {

        cancelExistingCB();

        List<WaitSetError> errors = new LinkedList<WaitSetError>();

        if (addAccounts != null) {
            errors.addAll(addAccountErrors(addAccounts));
        }
        if (updateAccounts != null) {
            errors.addAll(updateAccountErrors(updateAccounts));
        }
        // figure out if there is already data here
        ZimbraLog.session.trace("SomeAccountsWaitSet.doWait - setting mCb was=%s new=%s", mCb, cb);
        mCb = cb;
        mCbSeqNo = Long.parseLong(lastKnownSeqNo);
        trySendData();

        return errors;
    }

    @Override
    public void mailboxAvailable(Mailbox mbox) {
        this.mailboxLoaded(mbox);
    }

    @Override
    public void mailboxCreated(Mailbox mbox) {
        this.mailboxLoaded(mbox);
    }

    @Override
    public synchronized void mailboxLoaded(Mailbox mbox) {
        WaitSetAccount wsa = mSessions.get(mbox.getAccountId());
        if (wsa != null) {
            // create a new session...
            WaitSetError error = initializeWaitSetSession(wsa, mbox);
            if (error != null) {
                mSessions.remove(wsa.getAccountId());
                signalError(error);
            }
        }
    }

    @Override
    public synchronized void mailboxDeleted(String accountId) {
        WaitSetAccount wsa = mSessions.get(accountId);
        if (wsa != null) {
            mSessions.remove(accountId);
            signalError(new WaitSetError(accountId, WaitSetError.Type.MAILBOX_DELETED));
        }
    }

    private synchronized WaitSetError initializeWaitSetSession(WaitSetAccount wsa, Mailbox mbox) {
        //
        // check to see if the session is already initialized and valid, if not, re-initialize it.
        //
        WaitSetSession session = wsa.getSession();
        if (session != null && session.getMailbox() != mbox) {
            ZimbraLog.session.warn("SESSION BEING LEAKED? WaitSetSession points to old version of mailbox...possibly leaking this session:", session);
            wsa.cleanupSession();
            session = null; // re-initialize it below
        }

        if (session == null) {
            return wsa.createSession(mbox, this);
        } else {
            return null;
        }
    }

    @Override
    protected boolean cbSeqIsCurrent() {
        return (mCbSeqNo == mCurrentSeqNo);
    }

    @Override
    protected String toNextSeqNo() {
        mCurrentSeqNo++;
        return Long.toString(mCurrentSeqNo);
    }

    private synchronized List<WaitSetError> updateAccountErrors(List<WaitSetAccount> updates) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();

        for (WaitSetAccount update : updates) {
            WaitSetAccount existing = mSessions.get(update.getAccountId());
            if (existing != null) {
                ZimbraLog.session.trace("SomeAccountsWaitSet updating %s with details from %s", existing, update);
                existing.setInterests(update.getInterests());
                existing.setLastKnownSyncToken(update.getLastKnownSyncToken());
                WaitSetSession session = existing.getSession();
                if (session != null) {
                    session.update(update.getInterests(), update.getFolderInterests(), update.getLastKnownSyncToken());
                    // update it!
                }
            } else {
                ZimbraLog.session.trace("SomeAccountsWaitSet no pre-existing session when updating %s", update);
                errors.add(new WaitSetError(update.getAccountId(), WaitSetError.Type.NOT_IN_SET_DURING_UPDATE));
            }
        }
        return errors;
    }

    synchronized protected List<WaitSetError> addAccountErrors(List<WaitSetAccount> wsas) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();

        for (WaitSetAccount wsa : wsas) {
            if (!mSessions.containsKey(wsa.getAccountId())) {
                // add the account to our session list
                mSessions.put(wsa.getAccountId(), wsa);

                // create the Session, if necessary, to listen to the requested mailbox
                try {
                    // if there is a sync token, then we need to check to see if the
                    // token is up-to-date...which means we have to fetch the mailbox.  Otherwise,
                    // we don't have to fetch the mailbox.
                    MailboxManager.FetchMode fetchMode = MailboxManager.FetchMode.AUTOCREATE;
                    if (wsa.getLastKnownSyncToken() == null)
                        fetchMode = MailboxManager.FetchMode.ONLY_IF_CACHED;

                    //
                    // THIS CALL MIGHT REGISTER THE SESSION (via the MailboxManager notification --> mailboxLoaded() callback!
                    //
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(wsa.getAccountId(), fetchMode);
                    if (mbox != null) {
                        WaitSetError error = initializeWaitSetSession(wsa, mbox);
                        if (error != null) {
                            errors.add(error);
                        }
                    }
                } catch (ServiceException e) {
                    if (e.getCode() == AccountServiceException.NO_SUCH_ACCOUNT) {
                        errors.add(new WaitSetError(wsa.getAccountId(), WaitSetError.Type.NO_SUCH_ACCOUNT));
                    } else if (e.getCode() == ServiceException.WRONG_HOST) {
                        errors.add(new WaitSetError(wsa.getAccountId(), WaitSetError.Type.WRONG_HOST_FOR_ACCOUNT));
                    } else {
                        errors.add(new WaitSetError(wsa.getAccountId(), WaitSetError.Type.ERROR_LOADING_MAILBOX));
                    }
                    mSessions.remove(wsa);
                }

            } else {
                errors.add(new WaitSetError(wsa.getAccountId(), WaitSetError.Type.ALREADY_IN_SET_DURING_ADD));
            }
        }
        return errors;
    }

    synchronized protected void cleanupSession(WaitSetSession session) {
        WaitSetAccount acct = mSessions.get(session.getTargetAccountId());
        if (acct != null) {
            acct.cleanupSession();
        }
    }

    @Override
    protected int countSessions() {
        return mSessions.size();
    }

    /**
     * Cleanup and remove all the sessions referenced by this WaitSet
     */
    @Override
    synchronized protected HashMap<String, WaitSetAccount> destroy() {
        try {
            MailboxManager.getInstance().removeListener(this);
        } catch (ServiceException e) {
            ZimbraLog.session.warn("Caught unexpected ServiceException while destroying WaitSet: "+e, e);
        }
        cancelExistingCB();
        HashMap<String, WaitSetAccount> toRet = mSessions;
        mSessions = new HashMap<String, WaitSetAccount>();
        mCurrentSignalledSessions.clear();
        mSentSignalledSessions.clear();
        mSentSignalledAccounts.clear();
        mCurrentSignalledAccounts.clear();
        mCurrentSeqNo = Long.MAX_VALUE;
        return toRet;
   }

    @Override
    protected WaitSetCallback getCb() {
        return mCb;
    }

    /**
     * Called by the WaitSetSession to revoke a signal...this happens when we get a
     * sync token <update> with a higher sync token for this account than the account's
     * current highest
     *
     * @param session
     */
    synchronized protected void unsignalDataReady(WaitSetSession session) {
        if (mSessions.containsKey(session.getTargetAccountId())) { // ...false if waitset is shutting down...
            mCurrentSignalledAccounts.remove(session.getTargetAccountId());
            mCurrentSignalledSessions.remove(session.getTargetAccountId());
        }
    }

    /**
     * Called by the WaitSetSession when there is data to be signalled by this session
     * @param session
     */
    synchronized protected void signalDataReady(WaitSetSession session) {
        signalDataReady(session, null);
    }

    /**
     * Called by the WaitSetSession when there is data to be signalled by this session
     * @param session
     */
    synchronized protected void signalDataReady(WaitSetSession session, PendingModifications pms) {
        boolean trace = ZimbraLog.session.isTraceEnabled();
        if (trace) ZimbraLog.session.trace("SomeAccountsWaitSet.signalDataReady 1");
        String targetAccId = session.getTargetAccountId();
        if (mSessions.containsKey(targetAccId)) { // ...false if waitset is shutting down...
            if (trace) ZimbraLog.session.trace("SomeAccountsWaitSet.signalDataReady 2");
            if (mCurrentSignalledAccounts.add(targetAccId)) {
                mCurrentSignalledSessions.add(session);
                if (trace) ZimbraLog.session.trace("SomeAccountsWaitSet.signalDataReady 3");
                addMods(currentPendingModifications, targetAccId, pms);
                trySendData();
            }
        }
        if (trace) ZimbraLog.session.trace("SomeAccountsWaitSet.signalDataReady done");
    }

    @Override
    public synchronized WaitSetInfo handleQuery() {
        WaitSetInfo info = super.handleQuery();
        info.setCbSeqNo(Long.toString(mCbSeqNo));
        info.setCurrentSeqNo(Long.toString(mCurrentSeqNo));

        for (Map.Entry<String, WaitSetAccount> entry : mSessions.entrySet()) {
            String acctId = entry.getKey();
            WaitSetAccount wsa = entry.getValue();
            SessionForWaitSet sess = new SessionForWaitSet(acctId, WaitSetRequest.expandInterestStr(wsa.getInterests()));

            if (!wsa.getAccountId().equals(acctId)) {
                sess.setAcctIdError(wsa.getAccountId());
            }

            if (wsa.getLastKnownSyncToken() != null) {
                sess.setToken(wsa.getLastKnownSyncToken().toString());
            }

            if (wsa.getLastKnownSyncToken() != null) {
                try {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acctId, FetchMode.ONLY_IF_CACHED);
                    if (mbox != null) {
                        int mboxLastChange = mbox.getLastChangeID();
                        sess.setMboxSyncToken(mboxLastChange);
                        sess.setMboxSyncTokenDiff(mboxLastChange-wsa.getLastKnownSyncToken().getChangeId());
                    }
                } catch (Exception e) {
                    ZimbraLog.session.warn(
                        "Caught exception from MailboxManager in SomeAccountsWaitSet.handleQuery() for accountId %s",
                                           acctId, e);
                }
            }

            WaitSetSession wss = wsa.getSession();
            if (wss != null) {
                WaitSetSessionInfo waitSetSession = new WaitSetSessionInfo(WaitSetRequest.interestToStr(wss.interest),
                        wss.mHighestChangeId, wss.getLastAccessTime(), wss.getCreationTime(), wss.getSessionId());

                waitSetSession.setFolderInterests(wss.folderInterest);
                if (wss.mSyncToken != null) {
                    waitSetSession.setToken(wss.mSyncToken.toString());
                }
                sess.setWaitSetSession(waitSetSession);
            }
            info.addSession(sess);
        }
        return info;
    }

    /**
     * Keeping this for possible future use.  Currently it is not reliable as WaitSets aren't necessarily
     * cleaned up immediately, resulting in false positives.
     */
    public synchronized boolean isMonitoringFolder(String accountId, int folderId) {
        WaitSetInfo info = super.handleQuery();
        info.setCbSeqNo(Long.toString(mCbSeqNo));
        info.setCurrentSeqNo(Long.toString(mCurrentSeqNo));

        for (Map.Entry<String, WaitSetAccount> entry : mSessions.entrySet()) {
            String acctId = entry.getKey();
            if (!accountId.equals(acctId)) {
                continue;
            }
            WaitSetAccount wsa = entry.getValue();
            Set<Integer> folderInterests = wsa.getFolderInterests();
            if ((folderInterests != null) && folderInterests.contains(folderId)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public long getCurrentSeqNo() {
        return mCurrentSeqNo;
    }
}
