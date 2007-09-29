/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class RenameFolder extends RedoableOp {

    private int mId;
    private int mParentId;
    private String mName;

    public RenameFolder() {
        mId = mParentId = UNKNOWN_ID;
    }

    public RenameFolder(int mailboxId, int id, int parentId, String name) {
        setMailboxId(mailboxId);
        mId = id;
        mParentId = parentId;
        mName = name != null ? name : "";
    }

    public int getOpCode() {
        return OP_RENAME_FOLDER;
    }

    protected String getPrintableData() {
        return "id=" + mId + ", name=" + mName + ",parent=" + mParentId;
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mParentId);
        out.writeUTF(mName);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mParentId = in.readInt();
        mName = in.readUTF();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.renameFolder(getOperationContext(), mId, mParentId, mName);
    }
}
