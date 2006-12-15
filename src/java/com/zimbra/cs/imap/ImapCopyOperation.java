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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.CopyOperation;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.session.Session;
import com.zimbra.common.service.ServiceException;

public class ImapCopyOperation extends Operation {

    private static int LOAD = 15;
        static {
            Operation.Config c = loadConfig(CopyOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    static final int SUGGESTED_BATCH_SIZE = 50;

    private int[] mItemIds;
    private byte mType;
    private int mFolderId;

    private List<MailItem> mCopies;


    public ImapCopyOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                List<Integer> itemIds, byte type, int folderId)
    {
        super(session, oc, mbox, req, LOAD * itemIds.size());
        mFolderId = folderId;
        mType = type;
        mItemIds = new int[itemIds.size()];
        int i = 0;
        for (int id : itemIds)
            mItemIds[i++] = id;
    }

    public ImapCopyOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                Collection<ImapMessage> i4set, int folderId)
    {
        super(session, oc, mbox, req, LOAD * i4set.size());

        mFolderId = folderId;
        mType = MailItem.TYPE_UNKNOWN;
        mItemIds = new int[i4set.size()];
        int i = 0;
        for (ImapMessage i4msg : i4set) {
            mItemIds[i++] = i4msg.msgId;
            if (i == 1)
                mType = i4msg.getType();
            else if (i4msg.getType() != mType)
                mType = MailItem.TYPE_UNKNOWN;
        }
    }

    protected void callback() throws ServiceException {
        try {
            mCopies = getMailbox().imapCopy(this.getOpCtxt(), mItemIds, mType, mFolderId);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException execiting " + this, e);
        }
    }

    public List<MailItem> getMessages()  { return mCopies; }

    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        toRet.append(" id=").append(Arrays.toString(mItemIds)).append(" type=").append(mType).append(" target=").append(mFolderId);
        return toRet.toString();
    }
}
