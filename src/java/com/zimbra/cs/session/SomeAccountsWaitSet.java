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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * SomeAccountsWaitSet: an implementation of IWaitSet that works by listening over one or more Accounts
 * 
 * External APIs:
 *     WaitSet.doWait()              // primary wait API
 *     WaitSet.getDefaultInterest()  // accessor
 */
public final class SomeAccountsWaitSet extends WaitSetBase implements MailboxManager.Listener {

    /** Constructor */
    SomeAccountsWaitSet(String ownerAccountId, String id, int defaultInterest) {
        super(ownerAccountId, id, defaultInterest);
        mCurrentSeqNo = 1;
    }
    
    public List<WaitSetError> removeAccounts(List<String> accts) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (String id : accts) {
            WaitSetSession session = null;
            synchronized(this) {
                WaitSetAccount wsa = mSessions.get(id);
                if (wsa != null) {
                    session = wsa.getSession();
                }
            }
            if (session != null) {
                assert(!Thread.holdsLock(this));
                session.doCleanup();
            } else {
                errors.add(new WaitSetError(id, WaitSetError.Type.NOT_IN_SET_DURING_REMOVE));
            }
        }
        return errors;
    }
    
    /* @see com.zimbra.cs.session.IWaitSet#doWait(com.zimbra.cs.session.WaitSetCallback, java.lang.String, boolean, java.util.List, java.util.List, java.util.List) */
    public synchronized List<WaitSetError> doWait(WaitSetCallback cb, String lastKnownSeqNo,    
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts) throws ServiceException {
        
        cancelExistingCB();
        
        List<WaitSetError> errors = new LinkedList<WaitSetError>();
        
        if (addAccounts != null) {
            errors.addAll(addAccounts(addAccounts));
        }
        if (updateAccounts != null) {
            errors.addAll(updateAccounts(updateAccounts));
        }
        // figure out if there is already data here
        mCb = cb;
        mCbSeqNo = Long.parseLong(lastKnownSeqNo);
        trySendData();
        
        return errors;
    }

    /* @see com.zimbra.cs.mailbox.MailboxManager.Listener#mailboxAvailable(com.zimbra.cs.mailbox.Mailbox) */
    public void mailboxAvailable(Mailbox mbox) {
        this.mailboxLoaded(mbox);
    }
    
    /* @see com.zimbra.cs.mailbox.MailboxManager.Listener#mailboxCreated(com.zimbra.cs.mailbox.Mailbox) */
    public void mailboxCreated(Mailbox mbox) {
        this.mailboxLoaded(mbox);
    }
    
    /* @see com.zimbra.cs.mailbox.MailboxManager.Listener#mailboxLoaded(com.zimbra.cs.mailbox.Mailbox) */
    public synchronized void mailboxLoaded(Mailbox mbox) {
        WaitSetAccount wsa = mSessions.get(mbox.getAccountId());
        if (wsa != null) {
            WaitSetSession session = wsa.getSession();
            if (session == null) {
                // create a new session... 
                initializeWaitSetSession(wsa);
            }
        } 
    }
    
    private synchronized WaitSetError initializeWaitSetSession(WaitSetAccount wsa) {
        WaitSetSession wsSession = new WaitSetSession(this, wsa.accountId, wsa.interests, wsa.lastKnownSyncToken);
        try {
            wsSession.register();
            wsa.ref = new SoftReference<WaitSetSession>(wsSession);
            // must force update here so that initial sync token is checked against current mbox state
            wsSession.update(wsa.interests, wsa.lastKnownSyncToken);
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
    
    protected boolean cbSeqIsCurrent() {
        return (mCbSeqNo == mCurrentSeqNo);
    }
    
    protected String toNextSeqNo() {
        mCurrentSeqNo++;
        return Long.toString(mCurrentSeqNo);
    }
    
    private synchronized List<WaitSetError> updateAccounts(List<WaitSetAccount> updates) {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();
        
        for (WaitSetAccount update : updates) {
            WaitSetAccount existing = mSessions.get(update.accountId);
            if (existing != null) {
                existing.interests = update.interests;
                existing.lastKnownSyncToken = update.lastKnownSyncToken;
                WaitSetSession session = existing.getSession();
                if (session != null) {
                    session.update(existing.interests, existing.lastKnownSyncToken);
                    // update it!
                }
            } else {
                errors.add(new WaitSetError(update.accountId, WaitSetError.Type.NOT_IN_SET_DURING_UPDATE));
            }
        }
        return errors;
    }
    
    synchronized List<WaitSetError> addAccounts(List<WaitSetAccount> wsas) throws ServiceException {
        List<WaitSetError> errors = new ArrayList<WaitSetError>();

        for (WaitSetAccount wsa : wsas) {
            if (!mSessions.containsKey(wsa.accountId)) {
                // add the account to our session list  
                mSessions.put(wsa.accountId, wsa);
                
                // create the Session, if necessary, to listen to the requested mailbox
                try {
                    // if there is a sync token, then we need to check to see if the 
                    // token is up-to-date...which means we have to fetch the mailbox.  Otherwise,
                    // we don't have to fetch the mailbox.
                    MailboxManager.FetchMode fetchMode = MailboxManager.FetchMode.AUTOCREATE;
                    if (wsa.lastKnownSyncToken == null)
                        fetchMode = MailboxManager.FetchMode.ONLY_IF_CACHED;
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(wsa.accountId, fetchMode);
                    if (mbox != null) {
                        WaitSetError error = initializeWaitSetSession(wsa);
                        if (error != null) { 
                            errors.add(error);
                        }
                    }
                } catch (ServiceException e) {
                    if (e.getCode() == AccountServiceException.NO_SUCH_ACCOUNT) {
                        errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.NO_SUCH_ACCOUNT));
                    } else if (e.getCode() == ServiceException.WRONG_HOST) {
                        errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.WRONG_HOST_FOR_ACCOUNT));
                    } else {
                        errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX));
                    }
                    mSessions.remove(wsa);
                }                
                
            } else {
                errors.add(new WaitSetError(wsa.accountId, WaitSetError.Type.ALREADY_IN_SET_DURING_ADD));
            }
        }
        return errors;
    }
    
    synchronized void cleanupSession(WaitSetSession session) {
        WaitSetAccount acct = mSessions.get(session.getAuthenticatedAccountId());
        if (acct != null && acct.ref != null) {
            WaitSetSession existing = acct.getSession();
            if (existing == session) {
                acct.ref = null;
            }
        }
    }
    
    @Override
    int countSessions() {
        return mSessions.size();
    }
    
    /**
     * Cleanup and remove all the sessions referenced by this WaitSet
     */
    @Override
    synchronized HashMap<String, WaitSetAccount> destroy() {
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
        mCurrentSeqNo = Long.MAX_VALUE;
        return toRet;
   }
    
    @Override
    WaitSetCallback getCb() { 
        return mCb;
    }
    
    /**
     * Called by the WaitSetSession to revoke a signal...this happens when we get a 
     * sync token <update> with a higher sync token for this account than the account's
     * current highest
     * 
     * @param session
     */
    synchronized void unsignalDataReady(WaitSetSession session) {
        if (mSessions.containsKey(session.getAuthenticatedAccountId())) { // ...false if waitset is shutting down...
            mCurrentSignalledSessions.remove(session.getAuthenticatedAccountId());
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
    
    private long mCbSeqNo = 0; // seqno passed in by the current waiting callback
    private long mCurrentSeqNo; // current sequence number 
    
    /** these are the accounts we are listening to.  Stores EITHER a WaitSetSession or an AccountID  */
    private HashMap<String, WaitSetAccount> mSessions = new HashMap<String, WaitSetAccount>();
}
