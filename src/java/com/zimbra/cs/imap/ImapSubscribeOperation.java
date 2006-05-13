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

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapSubscribeOperation extends Operation {
    private static int LOAD = 25;
        static {
            Operation.Config c = loadConfig(ImapSubscribeOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    private String mFolderName;

    ImapSubscribeOperation(ImapSession session, OperationContext oc, Mailbox mbox, String folderName) {
        super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
        mFolderName = folderName;
    }   

    protected void callback() throws ServiceException {
        synchronized (mMailbox) {
            Folder folder = mMailbox.getFolderByPath(this.getOpCtxt(), mFolderName);
            if (!ImapFolder.isFolderVisible(folder, (ImapSession) mSession))
                throw ImapServiceException.FOLDER_NOT_VISIBLE(mFolderName);
            if (!folder.isTagged(mMailbox.mSubscribeFlag))
                mMailbox.alterTag(mOpCtxt, folder.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SUBSCRIBED, true);
        }
    }
}
