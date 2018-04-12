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

import com.google.common.base.MoreObjects;
import com.zimbra.client.ZFolder;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;

public class RemoteImapFolderStore implements ImapFolderStore {
    private transient ZFolder folder;

    public RemoteImapFolderStore(ZFolder folder) {
        this.folder = folder;
    }

    @Override
    public String getFolderIdAsString() {
        return (folder == null) ? null : folder.getFolderIdAsString();
    }

    @Override
    public FolderStore getFolderStore() {
        return folder;
    }

    @Override
    public boolean isUserRootFolder() {
        try {
            return (new ItemId(folder.getId(), (String) null).getId() == Mailbox.ID_FOLDER_USER_ROOT);
        } catch (ServiceException e) {
            return true;  // Shouldn't happen but assume the worst if it does
        }
    }

    @Override
    public boolean isIMAPDeleted() {
        return folder.isIMAPDeleted();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("folder", folder).toString();
    }
}
