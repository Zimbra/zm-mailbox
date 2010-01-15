/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jjzhuang
 */
public class SetFolderDefaultView extends RedoableOp {

    private int mFolderId;
    private byte mDefaultView;

    public SetFolderDefaultView() {
        mFolderId = Mailbox.ID_AUTO_INCREMENT;
        mDefaultView = MailItem.TYPE_UNKNOWN;
    }

    public SetFolderDefaultView(int mailboxId, int folderId, byte view) {
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mDefaultView = view;
    }

    public int getOpCode() {
        return OP_SET_DEFAULT_VIEW;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", view=").append(mDefaultView);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mFolderId);
        out.writeByte(mDefaultView);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mFolderId = in.readInt();
        mDefaultView = in.readByte();
    }

    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setFolderDefaultView(getOperationContext(), mFolderId, mDefaultView);
    }
}