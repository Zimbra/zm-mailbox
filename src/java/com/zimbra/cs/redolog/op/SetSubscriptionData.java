/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
/*
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetSubscriptionData extends RedoableOp {

    private int mFolderId;
    private long mLastItemDate;
    private String mLastItemGuid;

    public SetSubscriptionData() {
        super(MailboxOperation.SetSubscriptionData);
        mFolderId = Mailbox.ID_AUTO_INCREMENT;
        mLastItemGuid = "";
    }

    public SetSubscriptionData(int mailboxId, int folderId, long date, String guid) {
        this();
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mLastItemDate = date;
        mLastItemGuid = guid == null ? "" : guid;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", date=").append(mLastItemDate);
        sb.append(", guid=").append(mLastItemGuid);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mFolderId);
        out.writeLong(mLastItemDate);
        out.writeUTF(mLastItemGuid);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mFolderId = in.readInt();
        mLastItemDate = in.readLong();
        mLastItemGuid = in.readUTF();
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setSubscriptionData(getOperationContext(), mFolderId, mLastItemDate, mLastItemGuid);
    }
}
