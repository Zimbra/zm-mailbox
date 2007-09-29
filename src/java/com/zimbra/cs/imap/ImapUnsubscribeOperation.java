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
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;

public class ImapUnsubscribeOperation extends Operation {
    private static int LOAD = 25;
        static {
            Operation.Config c = loadConfig(ImapUnsubscribeOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    private String mFolderName;

    ImapUnsubscribeOperation(ImapSession session, OperationContext oc, Mailbox mbox, String folderName) {
        super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
        mFolderName = folderName;
    }   

    protected void callback() throws ServiceException {
        synchronized (mMailbox) {
            Folder folder = mMailbox.getFolderByPath(mOpCtxt, mFolderName);
            if (folder.isTagged(mMailbox.mSubscribeFlag))
                mMailbox.alterTag(mOpCtxt, folder.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SUBSCRIBED, false);
        }
    }
}
