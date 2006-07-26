/* ***** BEGIN LICENSE BLOCK *****
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
package com.zimbra.cs.operation;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class GetItemOperation extends Operation {
    private static int LOAD = 1;
        static {
            Operation.Config c = loadConfig(GetItemOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    enum ItemBy { ID, PATH, IMAP_ID };

    ItemBy mMethod;
    String mPath;
    byte mType;
    int[] mIds;
    int mFolderId;

    MailItem[] mItems;

    public GetItemOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, int id, byte type) {
        super(session, oc, mbox, req, LOAD);
        mIds = new int[] { id };
        mType = type;
        mMethod = ItemBy.ID;
    }

    public GetItemOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, int[] ids, byte type) {
        super(session, oc, mbox, req, LOAD);
        mIds = ids;
        mType = type;
        mMethod = ItemBy.ID;
    }

    public GetItemOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, String path, byte type) {
        super(session, oc, mbox, req, LOAD);
        mType = type;
        mPath = path;
        mMethod = ItemBy.PATH;
    }

    public GetItemOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, int imapId, int folderId) {
        super(session, oc, mbox, req, LOAD);
        mIds = new int[] { imapId };
        mFolderId = folderId;
        mMethod = ItemBy.IMAP_ID;
    }


    protected void callback() throws ServiceException {
        if (mMethod == ItemBy.ID && mIds.length > 1) {
            mItems = getMailbox().getItemById(getOpCtxt(), mIds, mType);
            return;
        }

        mItems = new MailItem[1];
        if (mMethod == ItemBy.PATH) {
            mItems[0] = getMailbox().getItemByPath(getOpCtxt(), mPath, Mailbox.ID_FOLDER_USER_ROOT, false);
        } else if (mMethod == ItemBy.IMAP_ID) {
            mItems[0] = getMailbox().getItemByImapId(getOpCtxt(), mIds[0], mFolderId);
        } else {
            mItems[0] = getMailbox().getItemById(getOpCtxt(), mIds[0], mType);
        }
    }
    
    public MailItem getItem() { return mItems[0]; }
    public MailItem[] getItems() { return mItems; }
    public Folder getFolder() { return (Folder)mItems[0]; }
    
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (mPath != null) {
            sb.append(" path=").append(mPath);
        } else {
            sb.append(" id=(");
            boolean first = true;
            for (int id : mIds) {
                if (!first) {
                    sb.append(",");
                    first = false;
                }
                sb.append(id);
            }
        }
        sb.append(") type=").append(mType);
        return sb.toString();
    }
    
}
