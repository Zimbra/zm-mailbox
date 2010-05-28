/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class SyncState {
    private final Mailbox mbox;
    private final Folder inboxFolder;
    private final Map<Integer, FolderSyncState> folders;
    private boolean hasRemoteInboxChanges;

    SyncState(Mailbox mbox) throws ServiceException {
        this.mbox = mbox;
        inboxFolder = mbox.getFolderById(Mailbox.ID_FOLDER_INBOX);
        folders = Collections.synchronizedMap(new HashMap<Integer, FolderSyncState>());
    }

    public FolderSyncState getFolderSyncState(int folderId) {
        return folders.get(folderId);
    }

    public FolderSyncState putFolderSyncState(int folderId, FolderSyncState folderSyncState) {
        return folders.put(folderId, folderSyncState);
    }

    public FolderSyncState removeFolderSyncState(int folderId) {
        return folders.remove(folderId);
    }

    public void setHasRemoteInboxChanges(boolean hasChanges) {
        hasRemoteInboxChanges = hasChanges;
    }

    public boolean hasInboxChanges() throws ServiceException {
        if (hasRemoteInboxChanges) return true;
        FolderSyncState fss = getFolderSyncState(Mailbox.ID_FOLDER_INBOX);
        return fss != null && inboxFolder.getImapMODSEQ() != fss.getLastModSeq();
    }
}
