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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;

public class ImapRemoteSession extends ImapListener {

    ImapRemoteSession(ImapFolder i4folder, ImapHandler handler) throws ServiceException {
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
    public ImapFolder getImapFolder() throws ImapSessionClosedException {
        // ImapSession does:
        //     MANAGER.recordAccess(this);
        //     return reload();
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    public void closeFolder(boolean isUnregistering) {
        // ImapSession does:
        //     ImapSessionManager.getInstance().closeFolder(this, false);
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    public void updateAccessTime() {
        /** Assuming we are using active and inactive session caches, similar to those in ImapSessionManager,
         *  should update the access time in those */
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    public void notifyPendingChanges(PendingModifications pns, int changeId, Session source) {
        ZimbraLog.imap.warn("Unexpected call to notifyPendingChanges %s", ZimbraLog.getStackTrace(20));
    }

    @Override
    protected int getEstimatedSize() {
        return mFolder.getSize();
    }

    @Override
    public boolean hasExpunges() {
        return mFolder.hasExpunges();
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
    protected void cleanup() {
        ImapHandler i4handler = handler;
        if (i4handler != null) {
            ZimbraLog.imap.debug("dropping connection because Session is closing %s", this);
            i4handler.close();
        }
    }

    @Override
    protected ImapListener detach() {
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    protected boolean isSerialized() {
        /* ImapSession does:
         *     return mFolder instanceof PagedFolderData;
         */
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    protected void unload(boolean active) throws ServiceException {
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    protected ImapFolder reload() throws ImapSessionClosedException {
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }

    @Override
    protected boolean requiresReload() {
        throw new UnsupportedOperationException("ImapRemoteSession method not supported yet");
    }
}
