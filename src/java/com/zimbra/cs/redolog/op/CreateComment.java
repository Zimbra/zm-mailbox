/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

public class CreateComment extends RedoableOp {

    private int mItemId;
    private String mUuid;
    private int mParentId;
    private String mCreatorId;
    private String mText;

    public CreateComment() {
        super(MailboxOperation.CreateComment);
    }

    public CreateComment(int mailboxId, int parentId, String text, String creatorId) {
        this();
        setMailboxId(mailboxId);
        mParentId = parentId;
        mText = text;
        mCreatorId = creatorId;
    }

    public int getItemId() {
        return mItemId;
    }

    public String getUuid() {
        return mUuid;
    }

    public void setItemIdAndUuid(int itemId, String uuid) {
        mItemId = itemId;
        mUuid = uuid;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(mItemId);
        sb.append(", uuid=").append(mUuid);
        sb.append(", creator=").append(mCreatorId);
        sb.append(", text=").append(mText).append(", parentId=").append(mParentId);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mItemId);
        if (getVersion().atLeast(1, 37)) {
            out.writeUTF(mUuid);
        }
        out.writeInt(mParentId);
        out.writeUTF(mCreatorId);
        out.writeUTF(mText);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mItemId = in.readInt();
        if (getVersion().atLeast(1, 37)) {
            mUuid = in.readUTF();
        }
        mParentId = in.readInt();
        mCreatorId = in.readUTF();
        mText = in.readUTF();
    }

    @Override public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mbox.createComment(getOperationContext(), mParentId, mText, mCreatorId);
    }
}
