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

import java.util.Set;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.util.SyncToken;

/**
 * Simple struct used to define the parameters of an account during an add or update
 */
public class WaitSetAccount {

    public WaitSetAccount(String id, SyncToken sync, Set<MailItem.Type> interest, Set<Integer> folderInterests) {
        this.setAccountId(id);
        this.lastKnownSyncToken = sync;
        this.interests = interest;
        this.folderInterests = folderInterests;
        ZimbraLog.session.debug("Created %s", toString());
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
        // The session is not already initialized....therefore it's OK to lock in the reverse order
        // (waitset then mailbox) because we know the session isn't added as a listener and therefore
        // we won't get an upcall from the Mailbox
        //
        // See bug 31666 for more info
        //
        WaitSetSession session = new WaitSetSession(ws, accountId, interests, folderInterests, lastKnownSyncToken);
        ZimbraLog.session.debug("Created WaitSetSession %s for waitset %s on account %s", session.getSessionId(), ws.getWaitSetId(), accountId);
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            session.register();
            sessionId = session.getSessionId();
            // must force update here so that initial sync token is checked against current mbox state
            session.update(interests, folderInterests, lastKnownSyncToken);
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
            if (e.getCode() == AccountServiceException.NO_SUCH_ACCOUNT) {
                return new WaitSetError(accountId, WaitSetError.Type.NO_SUCH_ACCOUNT);
            } else if (e.getCode() == ServiceException.WRONG_HOST) {
                return new WaitSetError(accountId, WaitSetError.Type.WRONG_HOST_FOR_ACCOUNT);
            } else {
                return new WaitSetError(accountId, WaitSetError.Type.ERROR_LOADING_MAILBOX);
            }
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("account", accountId)
                .add("sessionId", sessionId)
                .add("lastKnownSyncToken", lastKnownSyncToken)
                .add("interests", interests)
                .add("folderInterests", folderInterests)
                .add("hashCode()", hashCode())
                .toString();
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

    void setInterests(Set<MailItem.Type> interests) {
        this.interests = interests;
    }

    void setFolderInterests(Set<Integer> fInterests) {
        this.folderInterests = fInterests;
    }

    public Set<MailItem.Type> getInterests() {
        return interests;
    }

    public Set<Integer> getFolderInterests() {
        return folderInterests;
    }

    private String accountId;
    private Set<MailItem.Type> interests;
    private Set<Integer> folderInterests;
    private SyncToken lastKnownSyncToken;
    private String sessionId;
}
