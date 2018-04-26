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
import com.google.common.collect.Sets;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
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
    /** The IDs of folders we are interested in changes for. null or empty means interested in all folders */
    Set<Integer> folderInterest;
    int mHighestChangeId;
    SyncToken mSyncToken;

    WaitSetSession(SomeAccountsWaitSet ws, String accountId, Set<MailItem.Type> interest, Set<Integer> folderInterests,
            SyncToken lastKnownSyncToken) {
        super(accountId, Session.Type.WAITSET);
        mWs = ws;
        this.interest = interest;
        this.folderInterest = folderInterests;
        mSyncToken = lastKnownSyncToken;
        ZimbraLog.session.trace("Created %s", this);
    }

    @Override
    protected boolean isMailboxListener() {
        return true;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return false;
    }

    void update(Set<MailItem.Type> interests, Set<Integer> folderInterests, SyncToken lastKnownSyncToken) {
        this.interest = interests;
        this.folderInterest = folderInterests;
        mSyncToken = lastKnownSyncToken;
        if (mSyncToken != null) {
            // if the sync token is non-null, then we want
            // to signal IFF the passed-in changeId is after the current
            // synctoken...and we want to cancel the existing signalling
            // if the new synctoken is up to date with the mailbox
            Mailbox mbox = getMailboxOrNull();
            if (null == mbox) {
                throw new UnsupportedOperationException(String.format(
                            "WaitSetSession must have an associated MailboxStore of class '%s' before calling update",
                            Mailbox.class.getName()));
            }
            int mboxHighestChange = mbox.getLastChangeID();
            if (mboxHighestChange > mHighestChangeId)
                mHighestChangeId = mboxHighestChange;
            if (mSyncToken.after(mHighestChangeId))
                mWs.unsignalDataReady(this);
            else
                mWs.signalDataReady(this);
        }
        ZimbraLog.session.trace("Updated %s", this);
    }

    @Override
    protected void cleanup() {
        mWs.cleanupSession(this);
    }

    @Override
    protected long getSessionIdleLifetime() { return 0; }

    @Override
    public void notifyPendingChanges(PendingModifications pns, int changeId, SourceSessionInfo source) {
        boolean trace = ZimbraLog.session.isTraceEnabled();
        if (trace) {
            ZimbraLog.session.trace("Notifying WaitSetSession %s: change id=%s changedFolders=%s changesTypes='%s'",
                    this, changeId, pns.getAllChangedFolders(), pns.changedTypes);
        }
        if (changeId > mHighestChangeId) {
            mHighestChangeId = changeId;
        }
        if (mSyncToken != null && mSyncToken.after(mHighestChangeId)) {
            if (trace) {
                ZimbraLog.session.trace("Not signaling waitset; sync token '%s' is later than highest change id '%s'",
                        mSyncToken, mHighestChangeId);
            }
            return; // don't signal, sync token stopped us
        }
        if (Sets.intersection(interest, pns.changedTypes).isEmpty()) {
            if (trace) {
                ZimbraLog.session.trace(
                        "Not signaling waitset; waitset is not interested in change type. interest=[%s] changes=[%s]",
                        interest, pns.changedTypes);
            }
            return;
        }
        if ((folderInterest != null) && !folderInterest.isEmpty() &&
         Sets.intersection(folderInterest, pns.getAllChangedFolders()).isEmpty()) {
            if (trace) {
                ZimbraLog.session.trace(
                        "Not signaling waitset; changes not in folders waitset is interested in %s changed=%s",
                        this.folderInterest, pns.getAllChangedFolders());
            }
            return;
        }
        if (source != null && source.getWaitSetId() != null) {
            String curWaitSetID = source.getWaitSetId();
            if (curWaitSetID != null && curWaitSetID.equals(mWs.getWaitSetId())) {
                if (trace) {
                    ZimbraLog.session.trace("Not signaling waitset; changes will be returned in SOAP headers");
                }
                return;
            }
        }
        if (trace) {
            ZimbraLog.session.trace("Signaling waitset");
        }
        mWs.signalDataReady(this, pns);
        if (trace) {
            ZimbraLog.session.trace("WaitSetSession.notifyPendingChanges done");
        }
    }

    public Set<Integer> getFolderInterest() {
        return folderInterest;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("syncToken", mSyncToken)
                .add("highestChangeId", mHighestChangeId)
                .add("interests", interest)
                .add("folderInterests", folderInterest)
                .add("mWs", mWs)
                .add("hashCode()", hashCode())
                .toString();
    }
}
