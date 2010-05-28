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
import java.util.Set;

final class FolderChanges {
    private final DataSource ds;
    private final Folder folder;
    private final Mailbox mbox;
    private List<MessageChange> messageChanges;
    private Set<Integer> changedFolderIds;
    private int lastChangeId;
    private int lastModSeq;

    public static FolderChanges getChanges(DataSource ds, Folder folder, int changeId)
        throws ServiceException {
        return new FolderChanges(ds, folder).findChanges(changeId);
    }
    
    private FolderChanges(DataSource ds, Folder folder) {
        this.ds = ds;
        this.folder = folder;
        mbox = folder.getMailbox();
    }

    private FolderChanges findChanges(int changeId) throws ServiceException {
        // Find tombstones and modified items since specified change id
        List<Integer> tombstones;
        List<Integer> modifiedItems;
        
        synchronized (mbox) {
            lastChangeId = mbox.getLastChangeID();
            lastModSeq = folder.getImapMODSEQ();
            if (lastChangeId <= changeId) {
                return this; // No changes
            }
            tombstones = mbox.getTombstones(changeId).getIds(MailItem.TYPE_MESSAGE);
            modifiedItems = mbox.getModifiedItems(null, changeId, MailItem.TYPE_MESSAGE).getFirst();
        }
        if ((tombstones == null || tombstones.isEmpty()) && modifiedItems.isEmpty()) {
            return this; // No changes
        }

        messageChanges = new ArrayList<MessageChange>();
        changedFolderIds = new HashSet<Integer>();
        
        // Find messages deleted from this folder
        if (tombstones != null) {
            for (int id : tombstones) {
                ImapMessage im = getTracker(id);
                if (im != null && im.getFolderId() == folder.getId()) {
                    messageChanges.add(MessageChange.deleted(id, im));
                }
            }
        }

        // Find modified messages for this folder
        int folderId = folder.getId();
        for (int id : modifiedItems) {
            Message msg = getMessage(id);
            if (msg != null) {
                changedFolderIds.add(msg.getFolderId());
                ImapMessage tracker = getTracker(id);
                if (tracker != null) {
                    changedFolderIds.add(tracker.getFolderId());
                    if (msg.getFolderId() == tracker.getFolderId()) {
                        if (tracker.getFolderId() == folderId &&
                            tracker.getFlags() != msg.getFlagBitmask()) {
                            // Message flags updated
                            messageChanges.add(MessageChange.updated(msg, tracker));
                        } else {
                            // Message moved to this folder from another.
                            // Let this case be handled when the other folder
                            // is synchronized.
                        }
                    } else if (tracker.getFolderId() == folderId) {
                        // Message moved to another folder
                        messageChanges.add(MessageChange.moved(msg, tracker));
                    }
                } else if (msg.getFolderId() == folderId) {
                    // Message added to this folder
                    messageChanges.add(MessageChange.added(msg));
                }
            }
        }
        
        return this;
    }

    public boolean hasChanges() {
        return messageChanges != null && !messageChanges.isEmpty();
    }
    
    public Collection<MessageChange> getMessageChanges() {
        if (messageChanges == null) {
            messageChanges = new ArrayList<MessageChange>();
        }
        return messageChanges;
    }

    public int getLastChangeId() {
        return lastChangeId;
    }

    public int getLastModSeq() {
        return lastModSeq;
    }

    /*
     * Returns set of ids for all folders that have been effected by changes.
     */
    public Collection<Integer> getChangedFolderIds() {
        if (changedFolderIds == null) {
            changedFolderIds = new HashSet<Integer>();
        }
        return changedFolderIds;
    }

    private Message getMessage(int itemId) throws ServiceException {
        try {
            return mbox.getMessageById(null, itemId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }
    
    private ImapMessage getTracker(int itemId) throws ServiceException {
        try {
            return new ImapMessage(ds, itemId);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        int added = 0, updated = 0, moved = 0, deleted = 0;
        for (MessageChange change : getMessageChanges()) {
            switch (change.getType()) {
            case ADDED:   added++; break;
            case UPDATED: updated++; break;
            case MOVED:   moved++; break;
            case DELETED: deleted++; break;
            }
        }
        return String.format(
            "{changeId=%d,added=%d,updated=%d,moved=%d,deleted=%d,folders=%s}",
            lastChangeId, added, updated, moved, deleted, getChangedFolderIds());
    }

}
