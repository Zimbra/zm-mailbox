package com.zimbra.cs.datasource.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

final class MessageChanges {
    private final DataSource ds;
    private final int folderId;
    private final Mailbox mbox;
    private final List<MessageChange> changes;
    private int modSeq;

    public static MessageChanges getChanges(DataSource ds, Folder folder, int lastModSeq)
        throws ServiceException {
        return new MessageChanges(ds, folder).findChanges(lastModSeq);
    }
    
    private MessageChanges(DataSource ds, Folder folder) {
        this.ds = ds;
        this.folderId = folder.getId();
        mbox = folder.getMailbox();
        changes = new ArrayList<MessageChange>();
    }

    private MessageChanges findChanges(int lastModSeq) throws ServiceException {
        List<Integer> tombstones;
        List<Integer> modifiedItems;
        synchronized (mbox) {
            modSeq = mbox.getLastChangeID();
            if (modSeq <= lastModSeq) {
                return this;
            }
            tombstones = mbox.getTombstones(lastModSeq).getIds(MailItem.TYPE_MESSAGE);
            modifiedItems = mbox.getModifiedItems(null, lastModSeq, MailItem.TYPE_MESSAGE).getFirst();
        }
        // Find deleted messages for this folder
        if (tombstones != null) {
            for (int id : tombstones) {
                ImapMessage im = getTracker(id);
                if (im != null && im.getFolderId() == folderId) {
                    changes.add(MessageChange.deleted(id, im));
                }
            }
        }
        // Find updated, added, or moved messages for this folder
        for (int id : modifiedItems) {
            Message msg = getMessage(id);
            if (msg != null) {
                ImapMessage tracker = getTracker(id);
                if (tracker != null) {
                    if (msg.getFolderId() == tracker.getFolderId()) {
                        if (tracker.getFolderId() == folderId) {
                            changes.add(MessageChange.updated(msg, tracker));
                        }
                    } else if (tracker.getFolderId() == folderId) {
                        changes.add(MessageChange.moved(msg, tracker));
                    }
                } else if (msg.getFolderId() == folderId) {
                    changes.add(MessageChange.added(msg));
                }
            }
        }
        return this;
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }
    
    public List<MessageChange> getChanges() {
        return changes;
    }

    public int getModSeq() {
        return modSeq;
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
        for (MessageChange change : changes) {
            switch (change.getType()) {
            case ADDED:   added++; break;
            case UPDATED: updated++; break;
            case MOVED:   moved++; break;
            case DELETED: deleted++; break;
            }
        }
        return String.format(
            "{modSeq=%d,added=%d,updated=%d,moved=%d,deleted=%d}",
            modSeq, added, updated, moved, deleted);
    }

}
