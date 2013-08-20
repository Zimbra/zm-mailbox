/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.op;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

import java.io.IOException;

public class SetWebOfflineSyncDays extends RedoableOp {

    private int folderId;
    private int days;

    public SetWebOfflineSyncDays() {
        super(MailboxOperation.SetWebOfflineSyncDays);
        folderId = Mailbox.ID_AUTO_INCREMENT;
        days = 0;
    }

    public SetWebOfflineSyncDays(int mailboxId, int folderId, int days) {
        this();
        setMailboxId(mailboxId);
        this.folderId = folderId;
        this.days = days;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("id=").append(folderId);
        sb.append(", webofflinesyncdays=").append(days);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(folderId);
        out.writeInt(days);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        folderId = in.readInt();
        days = in.readInt();
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.setFolderWebOfflineSyncDays(getOperationContext(), folderId, days);
    }
}
