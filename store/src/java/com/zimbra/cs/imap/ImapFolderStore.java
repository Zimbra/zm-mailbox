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

import com.zimbra.client.ZFolder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;

public interface ImapFolderStore {

    String getId();
    int getUIDValidity();

    public static ImapFolderStore get(Object folder) throws ServiceException {
        if (folder == null) {
            return null;
        }
        if (folder instanceof Folder) {
            return new LocalImapFolderStore((Folder) folder);
        }
        if (folder instanceof ZFolder) {
            return new RemoteImapFolderStore((ZFolder) folder);
        }
        return null; // TODO or throw an exception?
    }
}
