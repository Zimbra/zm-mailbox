/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.util.TreeMap;

import com.zimbra.client.ZBaseItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingRemoteModifications;

public class ImapRemoteSession extends ImapListener {

    protected class PagedRemoteFolderData extends ImapListener.PagedFolderData {

        PagedRemoteFolderData(String cachekey, ImapFolder i4folder) {
            super(cachekey, i4folder);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected PendingModifications getQueuedNotifications(int changeId) {
            if (queuedChanges == null) {
                queuedChanges = new TreeMap<Integer, PendingModifications>();
            }
            PendingModifications pns = queuedChanges.get(changeId);
            if (pns == null) {
                queuedChanges.put(changeId, pns = new PendingRemoteModifications());
            }
            return pns;
        }

        private PendingRemoteModifications getQueuedRemoteNotifications(int changeId) {
            return (PendingRemoteModifications) getQueuedNotifications(changeId);
        }

        @Override
        protected synchronized void queueCreate(int changeId, MailItem item) {
            ZimbraLog.imap.warn("Unexpected call to queueCreate %s", ZimbraLog.getStackTrace(20));
        }

        @Override
        protected synchronized void queueCreate(int changeId, ZBaseItem item) {
            getQueuedRemoteNotifications(changeId).recordCreated(item);
        }

        @Override
        protected synchronized void queueModify(int changeId, Change chg) {
            getQueuedRemoteNotifications(changeId).recordModified((ZBaseItem) chg.what, chg.why, (ZBaseItem) chg.preModifyObj);
        }
    }

    protected ImapRemoteSession(ImapMailboxStore imapStore, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(imapStore, i4folder, handler);
    }

    @Override
    protected boolean isMailboxListener() {
        return false;
    }

    @Override
    protected boolean isRegisteredInCache() {
        return true;
    }

    @Override
    public void updateAccessTime() {
        /** Assuming we are using active and inactive session caches, similar to those in ImapSessionManager,
         *  should update the access time in those */
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    public void signalAccountChange() {
        //this should gather information about what has actually changed using ZMailbox::noOp and then call notifyPendingChanges
        ZimbraLog.imap.warn("Unexpected call to signalAccountChange %s", ZimbraLog.getStackTrace(20));
    }

    @Override
    public void notifyPendingChanges(PendingModifications pns, int changeId, Session source) {
        ZimbraLog.imap.warn("Unexpected call to notifyPendingChanges %s", ZimbraLog.getStackTrace(20));
    }

    @Override
    protected void inactivate() {
        mFolder.endSelect();
        // removes this session from the global SessionCache, *not* from ImapSessionManager
        removeFromSessionCache();
        handler = null;
        /* Above is some of the things that happen in ImapSession, but NOT all.
         * TODO: Check what else needs doing.
         */
        throw new UnsupportedOperationException("ImapRemoteSession method not FULLY supported yet");
    }

    @Override
    protected ImapFolder reload() throws ImapSessionClosedException {
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    protected boolean requiresReload() {
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    protected PagedFolderData createPagedFolderData(boolean active, ImapFolder folder) throws ServiceException {
        return new PagedRemoteFolderData(serialize(active), folder);
    }

}
