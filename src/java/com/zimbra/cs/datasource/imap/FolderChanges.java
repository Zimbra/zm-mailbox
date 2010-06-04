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
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FolderChanges {
    private final DataSource ds;
    private final Mailbox mbox;
    private List<FolderChange> changes;
    private int lastChangeId;

    public static FolderChanges getChanges(DataSource ds, int lastSync)
        throws ServiceException {
        return new FolderChanges(ds, ds.getMailbox()).findChanges(lastSync);
    }

    private FolderChanges(DataSource ds, Mailbox mbox) {
        this.ds = ds;
        this.mbox = mbox;
    }

    private FolderChanges findChanges(int lastSync) throws ServiceException {
        List<Integer> tombstones;
        List<Folder> modifiedFolders;

        synchronized (mbox) {
            lastChangeId = mbox.getLastChangeID();
            if (lastChangeId <= lastSync) {
                return this; // No changes
            }
            tombstones = mbox.getTombstones(lastSync).getIds(MailItem.TYPE_FOLDER);
            modifiedFolders = mbox.getModifiedFolders(lastSync);
        }
        if ((tombstones == null || tombstones.isEmpty()) && modifiedFolders.isEmpty()) {
            return this; // No changes
        }
        changes = new ArrayList<FolderChange>();

        // Find deleted folders
        if (tombstones != null) {
            for (int id : tombstones) {
                ImapFolder tracker = getTracker(id);
                if (tracker != null) {
                    changes.add(FolderChange.deleted(id, tracker));
                }
            }
        }

        // Find added and moved folders
        for (Folder folder : modifiedFolders) {
            FolderChange change = getChange(folder);
            if (change != null) {
                changes.add(change);
            }
        }

        return this;
    }

    private FolderChange getChange(Folder folder) throws ServiceException {
        ImapFolder tracker = getTracker(folder.getId());
        if (tracker != null) {
            if (!folder.getPath().equals(tracker.getLocalPath())) {
                return FolderChange.moved(folder, tracker);
            }
        } else {
            return FolderChange.added(folder);
        }
        return null;
    }

    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    public Collection<FolderChange> getChanges() {
        if (changes == null) {
            changes = new ArrayList<FolderChange>();
        }
        return changes;
    }

    public int getLastChangeId() {
        return lastChangeId;
    }

    private ImapFolder getTracker(int folderId) throws ServiceException {
        try {
            return new ImapFolder(ds, folderId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        int added = 0, moved = 0, deleted = 0;
        for (FolderChange change : getChanges()) {
            switch (change.getType()) {
            case ADDED: added++; break;
            case MOVED: moved++; break;
            case DELETED: deleted++; break;
            }
        }
        return String.format(
            "{changeId=%d,added=%d,moved=%d,deleted=%d",
            lastChangeId, added, moved, deleted);
    }


}
