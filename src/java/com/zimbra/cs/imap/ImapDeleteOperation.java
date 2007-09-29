/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.session.Session;

class ImapDeleteOperation extends Operation {
    private static int LOAD = 25;
        static {
            Operation.Config c = loadConfig(ImapDeleteOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    private String mFolderName;
    private int mFolderId;

    ImapDeleteOperation(Session session, OperationContext oc, Mailbox mbox, String folderName) {
        super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
        mFolderName = folderName;
    }   

    protected void callback() throws ServiceException {
        synchronized(mMailbox) {
            Folder folder = mMailbox.getFolderByPath(getOpCtxt(), mFolderName);
            if (!ImapFolder.isFolderVisible(folder, (ImapSession) mSession)) {
                throw ImapServiceException.FOLDER_NOT_VISIBLE(folder.getPath());
            } else if (!folder.isDeletable()) {
                throw ImapServiceException.CANNOT_DELETE_SYSTEM_FOLDER(folder.getPath());
            }

            mFolderId = folder.getId();
            if (!folder.hasSubfolders())
                mMailbox.delete(this.getOpCtxt(), mFolderId, MailItem.TYPE_FOLDER);
            else {
                // 6.3.4: "It is permitted to delete a name that has inferior hierarchical
                //         names and does not have the \Noselect mailbox name attribute.
                //         In this case, all messages in that mailbox are removed, and the
                //         name will acquire the \Noselect mailbox name attribute."
                mMailbox.emptyFolder(this.getOpCtxt(), mFolderId, false);
                // FIXME: add \Deleted flag to folder
            }
        }
    }

    int getFolderId() { return mFolderId; }
}
