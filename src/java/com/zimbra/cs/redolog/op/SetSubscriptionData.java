/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
