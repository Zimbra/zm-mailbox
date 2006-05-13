/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
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
