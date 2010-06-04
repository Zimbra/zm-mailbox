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
import com.zimbra.cs.mailbox.Message;

import java.util.ArrayList;
import java.util.Collection;

import java.util.HashSet;
import java.util.List;

class MessageChanges {
    private final DataSource ds;
    private final Folder folder;
    private final Mailbox mbox;
    private List<MessageChange> changes;
    private int lastChangeId;

    public static MessageChanges getChanges(DataSource ds, Folder folder, int changeId)
        throws ServiceException {
        return new MessageChanges(ds, folder).findChanges(changeId);
    }

    private MessageChanges(DataSource ds, Folder folder) {
        this.ds = ds;
        this.folder = folder;
        mbox = folder.getMailbox();
    }

    private MessageChanges findChanges(int changeId) throws ServiceException {
        // Find tombstones and modified items since specified change id
        List<Integer> tombstones;
        List<Integer> modifiedItems;
        
        synchronized (mbox) {
            lastChangeId = mbox.getLastChangeID();
            if (lastChangeId <= changeId) {
                return this; // No changes
            }
            tombstones = mbox.getTombstones(changeId).getIds(MailItem.TYPE_MESSAGE);
            modifiedItems = mbox.getModifiedItems(null, changeId, MailItem.TYPE_MESSAGE).getFirst();
        }
        if ((tombstones == null || tombstones.isEmpty()) && modifiedItems.isEmpty()) {
            return this; // No changes
        }

        changes = new ArrayList<MessageChange>();
        
        // Find messages deleted from this folder
        if (tombstones != null) {
            for (int id : tombstones) {
                ImapMessage tracker = getTracker(id);
                if (tracker != null && tracker.getFolderId() == folder.getId()) {
                    changes.add(MessageChange.deleted(id, tracker));
                }
            }
        }

        // Find modified messages for this folder
        for (int id : modifiedItems) {
            MessageChange change = getChange(id);
            if (change != null) {
                changes.add(change);
            }
        }
        
        return this;
    }

    private MessageChange getChange(int msgId) throws ServiceException {
        Message msg = getMessage(msgId);
        if (msg != null) {
            ImapMessage tracker = getTracker(msgId);
            if (tracker != null) {
                if (msg.getFolderId() == folder.getId()) {
                    if (tracker.getFolderId() != folder.getId()) {
                        // Message moved to this folder from another
                        return MessageChange.moved(msg, tracker);
                    }
                    if (tracker.getFlags() != msg.getFlagBitmask()) {
                        // Message flags updated
                        return MessageChange.updated(msg, tracker);
                    }
                } else if (tracker.getFolderId() == folder.getId()) {
                    // Message moved from this folder to another
                    return MessageChange.moved(msg, tracker);
                }
            } else if (msg.getFolderId() == folder.getId()) {
                // Message added to this folder
                return MessageChange.added(msg);
            }
        }
        return null;
    }

    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }
    
    public Collection<MessageChange> getChanges() {
        if (changes == null) {
            changes = new ArrayList<MessageChange>();
        }
        return changes;
    }

    public int getLastChangeId() {
        return lastChangeId;
    }

    public Collection<Integer> getFolderIdsToSync() {
        HashSet<Integer> folderIds = new HashSet<Integer>();
        for (MessageChange change : changes) {
            if (change.isAdded() || change.isUpdated() || change.isMoved()) {
                folderIds.add(change.getMessage().getFolderId());
            }
            if (change.isUpdated() || change.isMoved() || change.isDeleted()) {
                folderIds.add(change.getTracker().getFolderId());
            }
        }
        return folderIds;
    }

    private Message getMessage(int msgId) throws ServiceException {
        try {
            return mbox.getMessageById(null, msgId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }
    
    private ImapMessage getTracker(int msgId) throws ServiceException {
        try {
            return new ImapMessage(ds, msgId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        int added = 0, updated = 0, moved = 0, deleted = 0;
        for (MessageChange change : getChanges()) {
            switch (change.getType()) {
            case ADDED:   added++; break;
            case UPDATED: updated++; break;
            case MOVED:   moved++; break;
            case DELETED: deleted++; break;
            }
        }
        return String.format(
            "{changeId=%d,added=%d,updated=%d,moved=%d,deleted=%d}",
            lastChangeId, added, updated, moved, deleted);
    }

}
