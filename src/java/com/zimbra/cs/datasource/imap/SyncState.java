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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class SyncState {
    private final Mailbox mbox;
    private final Folder inboxFolder;
    private final Map<Integer, FolderSyncState> folders;
    private boolean hasRemoteInboxChanges;
    private int lastChangeId;
    private MessageChanges inboxChanges;

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

    public void resetHasChanges() {
        hasRemoteInboxChanges = false;
        lastChangeId = mbox.getLastChangeID();
    }

    public boolean checkAndResetHasChanges(DataSource ds) throws ServiceException {
        //ZimbraLog.datasource.debug("checkAndResetHasChanges: lastChangeId = %d, changeId = %d",
        //    lastChangeId, mbox.getLastChangeID());
        // Always true if there are remote INBOX changes or this is the
        // first time we've checked
        if (hasRemoteInboxChanges || lastChangeId <= 0) {
            ZimbraLog.datasource.debug("Forcing sync due to remote INBOX changes");
            hasRemoteInboxChanges = false;
            return true;
        }
        // No changes if last change id hasn't changed
        if (mbox.getLastChangeID() == lastChangeId) {
            return false;
        }
        // Otherwise check if there have been any folder changes or
        // changes to INBOX since the last time we checked
        FolderChanges fc = FolderChanges.getChanges(ds, lastChangeId);
        if (fc.hasChanges()) {
            ZimbraLog.datasource.debug("Forcing sync due to local folder changes: " + fc);
            lastChangeId = fc.getLastChangeId();
            return true;
        }
        inboxChanges = MessageChanges.getChanges(ds, inboxFolder, lastChangeId);
        lastChangeId = Math.min(inboxChanges.getLastChangeId(), fc.getLastChangeId());
        if (inboxChanges.hasChanges()) {
            ZimbraLog.datasource.debug("Forcing sync due to local INBOX changes: " + inboxChanges);
            return true;
        }
        return false;
    }

    public MessageChanges getInboxChanges() {
        return inboxChanges;
    }
}
