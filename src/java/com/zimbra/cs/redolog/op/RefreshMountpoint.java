/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class RefreshMountpoint extends RedoableOp {

    private int mId;          // item id of the mountpoint
    private String mOwnerId;  // account id of the remote folder owner
    private int mRemoteId;    // item id of the remote folder

    public RefreshMountpoint() {
        super(MailboxOperation.RefreshMountpoint);
        mId = UNKNOWN_ID;
    }

    public RefreshMountpoint(int mailboxId, int mptId, String ownerId, int remoteId) {
        this();
        setMailboxId(mailboxId);
        mId = mptId;
        mOwnerId = ownerId;
        mRemoteId = remoteId;
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.refreshMountpoint(getOperationContext(), mId, mOwnerId, mRemoteId);
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(mId);
        sb.append(", owner=").append(mOwnerId).append(", remoteId=").append(mRemoteId);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mOwnerId);
        out.writeInt(mRemoteId);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mOwnerId = in.readUTF();
        mRemoteId = in.readInt();
    }
}
