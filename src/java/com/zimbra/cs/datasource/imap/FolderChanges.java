/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
