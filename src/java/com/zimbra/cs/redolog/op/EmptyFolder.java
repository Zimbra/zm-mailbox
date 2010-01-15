/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 6, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class EmptyFolder extends RedoableOp {

    private int mId;
    private boolean mSubfolders;


    public EmptyFolder() {
        mId = UNKNOWN_ID;
        mSubfolders = false;
    }

    public EmptyFolder(long mailboxId, int id, boolean subfolders) {
        setMailboxId(mailboxId);
        mId = id;
        mSubfolders = subfolders;
    }

    @Override public int getOpCode() {
        return OP_EMPTY_FOLDER;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", subfolders=").append(mSubfolders);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeBoolean(mSubfolders);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mSubfolders = in.readBoolean();
    }

    @Override public boolean isDeleteOp() {
        return true;
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.emptyFolder(getOperationContext(), mId, mSubfolders);
    }
}
