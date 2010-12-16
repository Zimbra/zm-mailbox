/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class LockItem extends RedoableOp {

    protected int id;
    protected MailItem.Type type;
    protected String accountId;

    public LockItem() {
    }

    public LockItem(int mailboxId, int id, MailItem.Type type, String accountId) {
        setMailboxId(mailboxId);
        this.id = id;
        this.type = type;
        this.accountId = accountId;
    }

    @Override
    public int getOpCode() {
        return OP_LOCK_ITEM;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(id);
        sb.append(", type=").append(type);
        sb.append(", accountId=").append(accountId);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeByte(type.toByte());
        out.writeUTF(accountId);
        out.writeInt(id);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        type = MailItem.Type.of(in.readByte());
        accountId = in.readUTF();
        id = in.readInt();
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.lock(getOperationContext(), id, type, accountId);
    }
}
