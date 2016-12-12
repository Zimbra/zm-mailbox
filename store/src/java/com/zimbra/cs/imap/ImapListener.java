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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.imap.ImapSession.ImapFolderData;
import com.zimbra.cs.session.Session;

public abstract class ImapListener extends Session {
    protected static final ImapSessionManager MANAGER = ImapSessionManager.getInstance();

    final ImapPath mPath;
    final int      mFolderId;
    final boolean  mIsVirtual;
    ImapFolderData mFolder;
    ImapHandler handler;
    private final ImapMailboxStore imapMboxStore;

    private final Map<Integer, Integer> renumberCount = new ConcurrentHashMap<Integer, Integer>();

    protected ImapListener(ImapMailboxStore store, ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(i4folder.getCredentials().getAccountId(), i4folder.getPath().getOwnerAccountId(), Session.Type.IMAP);
        this.imapMboxStore = store;
        mPath      = i4folder.getPath();
        mFolderId  = i4folder.getId();
        mIsVirtual = i4folder.isVirtual();
        mFolder    = i4folder;
        this.handler = handler;

        i4folder.setSession(this);
    }

    ImapMailboxStore getImapMboxStore() {
        return imapMboxStore;
    }

    public ImapFolder getImapFolder() throws ImapSessionClosedException {
        MANAGER.recordAccess(this);
        return reload();
    }

    public void closeFolder(boolean isUnregistering) {
        MANAGER.closeFolder(this, false);
    }



    public boolean hasNotifications() {
        return mFolder.hasNotifications();
    }

    ImapHandler getHandler() {
        return handler;
    }

    public ImapPath getPath() {
        return mPath;
    }

    boolean isInteractive() {
        return handler != null;
    }

    public boolean isWritable() {
        return isInteractive() && mFolder.isWritable();
    }

    boolean isVirtual() {
        return mIsVirtual;
    }

    public int getFolderId() {
        return mFolderId;
    }

    @Override
    protected long getSessionIdleLifetime() {
        return handler.getConfig().getAuthenticatedMaxIdleTime() * 1000;
    }

    int getRenumberCount(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        return (count == null ? 0 : count);
    }

    public void incrementRenumber(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        count = (count != null ? count + 1 : 1);
        renumberCount.put(msg.msgId, count);
    }

    void resetRenumber() {
        renumberCount.clear();
    }

    boolean hasFailedRenumber() {
        //check if any id has been repeatedly renumbered
        for (Integer count : renumberCount.values()) {
            if (count > 5) {
                return true;
            }
        }
        return false;
    }

    public boolean isFailedRenumber(ImapMessage msg) {
        Integer count = renumberCount.get(msg.msgId);
        return (count == null ? false : isFailed(count));
    }

    private boolean isFailed(Integer count) {
        return count > 5;
    }

    protected abstract ImapListener detach();
    protected abstract void unload(boolean active) throws ServiceException;
    protected abstract boolean requiresReload();

    protected boolean hasExpunges() {
        return mFolder.hasExpunges();
    }

    protected int getEstimatedSize() {
        return mFolder.getSize();
    }

    protected abstract ImapFolder reload() throws ImapSessionClosedException;
    @Override
    protected abstract void cleanup();
    protected abstract void inactivate();
    protected abstract boolean isSerialized();
}
