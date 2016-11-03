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
import com.zimbra.cs.imap.ImapSession.ImapFolderData;
import com.zimbra.cs.session.Session;

public abstract class ImapListener extends Session {

    final ImapPath mPath;
    final int      mFolderId;
    final boolean  mIsVirtual;
    ImapFolderData mFolder;
    ImapHandler handler;

    ImapListener(ImapFolder i4folder, ImapHandler handler) throws ServiceException {
        super(i4folder.getCredentials().getAccountId(), i4folder.getPath().getOwnerAccountId(), Session.Type.IMAP);
        mPath      = i4folder.getPath();
        mFolderId  = i4folder.getId();
        mIsVirtual = i4folder.isVirtual();
        mFolder    = i4folder;
        this.handler = handler;

        i4folder.setFolderListener(this);
    }

    public abstract ImapFolder getImapFolder() throws ImapSessionClosedException;
    public abstract boolean hasNotifications();
    public abstract void closeFolder(boolean isUnregistering);
    public abstract void incrementRenumber(ImapMessage msg);
    public abstract boolean isFailedRenumber(ImapMessage msg);

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
}
