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

import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;

public class ImapRemoteListener extends ImapListener {

    ImapRemoteListener(ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(i4folder, handler);
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
    public String getAuthenticatedAccountId() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public String getTargetAccountId() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public MailboxStore getMailbox() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public ImapFolder getImapFolder() throws ImapSessionClosedException {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public ImapPath getPath() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public boolean hasNotifications() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    /**
     * TODO - do we really need this?
     */
    @Override
    public void updateAccessTime() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public void closeFolder(boolean isUnregistering) {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public boolean isWritable() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public int getFolderId() {
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public void incrementRenumber(ImapMessage msg) {
        // TODO: Would be nice to be able to share code with ImapSession for this
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public boolean isFailedRenumber(ImapMessage msg) {
        // TODO: Would be nice to be able to share code with ImapSession for this
        throw new UnsupportedOperationException("ImapRemoteListener method not supported yet");
    }

    @Override
    public void notifyPendingChanges(PendingModifications pns, int changeId, Session source) {
        ZimbraLog.imap.warn("Unexpected call to notifyPendingChanges %s", ZimbraLog.getStackTrace(20));
    }

    @Override
    protected void cleanup() {
        ImapHandler i4handler = handler;
        if (i4handler != null) {
            ZimbraLog.imap.debug("dropping connection because Session is closing %s", this);
            i4handler.close();
        }
    }
}
