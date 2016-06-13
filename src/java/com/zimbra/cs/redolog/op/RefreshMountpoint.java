/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
