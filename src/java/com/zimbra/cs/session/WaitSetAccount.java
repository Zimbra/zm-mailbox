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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.util.SyncToken;

/**
 * Simple struct used to define the parameters of an account during an add or update
 */
public class WaitSetAccount {
    public WaitSetAccount(String id, SyncToken sync, int interest) {
        this.setAccountId(id);
        this.lastKnownSyncToken = sync;
        this.interests = interest;
    }
    public WaitSetSession getSession() {
        if (sessionId != null) {
            try {
                Mailbox mbox = getMailboxIfLoaded();
                if (mbox != null) 
                    return (WaitSetSession)mbox.getListener(sessionId);
            } catch (ServiceException e) {
                ZimbraLog.session.info("Caught exception fetching mailbox in WaitSetAccount.getSession()", e);
            }
        }
        return null;
    }
    
    /**
     * The mailbox is loaded and in memory -- so create a {@link WaitSetSession} and add it 
     * as a listener to the mailbox
     * <p> 
     * @param mbox 
     * @param ws
     * @return
     */
    public WaitSetError createSession(Mailbox mbox, SomeAccountsWaitSet ws) {
        assert(sessionId == null);
        // The session is not already initialized....therefore it's OK to lock in the reverse order 
        // (waitset then mailbox) because we know the session isn't added as a listener and therefore 
        // we won't get an upcall from the Mailbox
        //
        // See bug 31666 for more info
        //
        WaitSetSession session = new WaitSetSession(ws, accountId, interests, lastKnownSyncToken);
        
        try {
            synchronized(mbox) { // this is OK, see above comment
                session.register();
                sessionId = session.getSessionId();
                // must force update here so that initial sync token is checked against current mbox state
                session.update(interests, lastKnownSyncToken);
            }
        } catch (MailServiceException e) {
            sessionId = null;
            if (e.getCode().equals(MailServiceException.MAINTENANCE)) {
                //wsa.ref = null; // will get re-set when mailboxAvailable() is called
                //wsa.setRef(null);
                ZimbraLog.session.debug("Maintenance mode trying to initialize WaitSetSession for accountId "+accountId); 
            } else {
                ZimbraLog.session.warn("Error initializing WaitSetSession for accountId "+accountId+" -- MailServiceException", e); 
                return new WaitSetError(accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX);
            }
        } catch (ServiceException e) {
            sessionId = null;
            ZimbraLog.session.warn("Error initializing WaitSetSession for accountId "+accountId+" -- ServiceException", e); 
            return new WaitSetError(accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX);
        }
        return null;
    }
    
    /**
     * Remove the session as a listener from the mailbox, clean up our references to it.
     */
    public void cleanupSession() {
        WaitSetSession session = getSession();
        if (session!= null) {
            sessionId = null; // must set this first to avoid recursion
            session.doCleanup();
        }
    }
    
    public String toString() {
        return "WaitSetAccount("+accountId+")";
    }
    
    private Mailbox getMailboxIfLoaded() throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId,
                                                                          MailboxManager.FetchMode.ONLY_IF_CACHED);
        return mbox;
    }
    
    void setLastKnownSyncToken(SyncToken lastKnownSyncToken) {
        this.lastKnownSyncToken = lastKnownSyncToken;
    }
    
    public SyncToken getLastKnownSyncToken() {
        return lastKnownSyncToken;
    }

    void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountId() {
        return accountId;
    }

    void setInterests(int interests) {
        this.interests = interests;
    }
    
    public int getInterests() {
        return interests;
    }

    private String accountId;
    private int interests;
    private SyncToken lastKnownSyncToken;
    private String sessionId;
}