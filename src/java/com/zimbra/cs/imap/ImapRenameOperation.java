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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.session.Session;

public class ImapRenameOperation extends Operation {
    private static int LOAD = 10;
        static {
            Operation.Config c = loadConfig(ImapRenameOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    private String mOldName;
    private String mNewName;

    ImapRenameOperation(Session session, OperationContext oc, Mailbox mbox, String oldName, String newName) {
        super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
        mOldName = oldName;
        // the new folder name must begin with '/' to trigger the appropriate server behavior
        mNewName = (newName.startsWith("/") ? newName : '/' + newName);
    }

    protected void callback() throws ServiceException {
        synchronized (mMailbox) {
            int folderId = mMailbox.getFolderByPath(getOpCtxt(), mOldName).getId();
            if (folderId != Mailbox.ID_FOLDER_INBOX) {
                mMailbox.renameFolder(getOpCtxt(), folderId, mNewName);
            } else {
                throw ImapServiceException.CANT_RENAME_INBOX();
            }
        }
    }
}
