package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.datasource.ImapFolder;
import com.zimbra.cs.db.DbImapFolder;
import com.zimbra.common.service.ServiceException;

import java.util.List;
import java.util.ArrayList;

public class FolderChanges {
    private final ImapImport imapImport;
    private final int lastSync;
    private List<Change> changes;

    public FolderChanges(ImapImport imapImport, int lastSync) {
        this.imapImport = imapImport;
        this.lastSync = lastSync;
        changes = new ArrayList<Change>();
    }

    private void loadChanges() throws ServiceException {
        Mailbox mb = imapImport.getMailbox();
        for (Folder folder : mb.getModifiedFolders(lastSync)) {
            Change change = new Change(folder, getImapFolder(folder.getId()));
            if (change.isChanged()) {
                changes.add(change);
            }
        }
        List<Integer> deletedIds = mb.getTombstones(lastSync).getIds(MailItem.TYPE_FOLDER);
        if (deletedIds != null) {
            for (int id : deletedIds) {
                Change change = new Change(null, getImapFolder(id));
                if (change.isChanged()) {
                    changes.add(change);
                }
            }
        }
    }

    private ImapFolder getImapFolder(int id) throws ServiceException {
        return DbImapFolder.getImapFolder(
            imapImport.getMailbox(), imapImport.getDataSource(), id);
    }
    
    private class Change {
        final Folder folder;
        final ImapFolder imapFolder;

        Change(Folder folder, ImapFolder imapFolder) {
            this.folder = folder;
            this.imapFolder = imapFolder;
        }

        public boolean isChanged() {
            return isCreated() || isDeleted() || isRenamed();
        }

        public boolean isCreated() {
            return folder != null && imapFolder == null;
        }

        public boolean isDeleted() {
            return folder == null && imapFolder != null;
        }

        public boolean isRenamed() {
            return folder != null && imapFolder != null &&
                !folder.getPath().equals(imapFolder.getLocalPath());
        }
    }
}
