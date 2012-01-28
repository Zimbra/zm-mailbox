/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
