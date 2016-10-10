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

import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.util.SyncToken;

/**
 * A session subclass which is used when an external entity wants to listen on a
 * particular account for changes to the mailbox, but doesn't care what exactly
 * those changes are: presumably because the caller is going to use some sort
 * of other channel (e.g. IMAP) for synchronizing with the Mailbox.
 */
public class WaitSetSession extends Session {
    SomeAccountsWaitSet mWs = null;
    Set<MailItem.Type> interest;
    int mHighestChangeId;
    SyncToken mSyncToken;

    WaitSetSession(SomeAccountsWaitSet ws, String accountId, Set<MailItem.Type> interest, SyncToken lastKnownSyncToken) {
        super(accountId, Session.Type.WAITSET);
        mWs = ws;
        this.interest = interest;
        mSyncToken = lastKnownSyncToken;
    }

    @Override
    protected boolean isMailboxListener() {
        return true;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return false;
    }

    void update(Set<MailItem.Type> interest, SyncToken lastKnownSyncToken) {
        this.interest = interest;
        mSyncToken = lastKnownSyncToken;
        if (mSyncToken != null) {
            // if the sync token is non-null, then we want
            // to signal IFF the passed-in changeId is after the current
            // synctoken...and we want to cancel the existing signalling
            // if the new synctoken is up to date with the mailbox
            int mboxHighestChange = (getMailboxOrNull()).getLastChangeID();
            if (mboxHighestChange > mHighestChangeId)
                mHighestChangeId = mboxHighestChange;
            if (mSyncToken.after(mHighestChangeId))
                mWs.unsignalDataReady(this);
            else
                mWs.signalDataReady(this);
        }
    }

    @Override
    protected void cleanup() {
        mWs.cleanupSession(this);
    }

    @Override
    protected long getSessionIdleLifetime() { return 0; }

    @Override
    public void notifyPendingChanges(PendingModifications pns, int changeId, Session source) {
        boolean trace = ZimbraLog.session.isTraceEnabled();
        if (trace)
            ZimbraLog.session.trace("Notifying WaitSetSession: change id=" + changeId +
                    ", highest change id=" + mHighestChangeId + ", sync token=" + mSyncToken);
        if (changeId > mHighestChangeId) {
            mHighestChangeId = changeId;
        }
        if (mSyncToken != null && mSyncToken.after(mHighestChangeId)) {
            if (trace) ZimbraLog.session.trace("Not signaling waitset; sync token is later than highest change id");
            return; // don't signal, sync token stopped us
        }
        if (!Sets.intersection(interest, pns.changedTypes).isEmpty()) {
            if (trace) ZimbraLog.session.trace("Signaling waitset");
            mWs.signalDataReady(this);
        } else {
            if (trace) ZimbraLog.session.trace("Not signaling waitset; waitset is not interested in change type");
        }
        if (trace) ZimbraLog.session.trace("WaitSetSession.notifyPendingChanges done");
    }
}
