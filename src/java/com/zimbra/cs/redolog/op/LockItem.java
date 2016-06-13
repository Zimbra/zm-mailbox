/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class LockItem extends RedoableOp {

    protected int id;
    protected MailItem.Type type;
    protected String accountId;

    public LockItem() {
        super(MailboxOperation.LockItem);
    }

    public LockItem(int mailboxId, int id, MailItem.Type type, String accountId) {
        this();
        setMailboxId(mailboxId);
        this.id = id;
        this.type = type;
        this.accountId = accountId;
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
