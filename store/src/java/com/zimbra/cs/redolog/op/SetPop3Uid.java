/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetPop3Uid extends RedoableOp {

    private int itemId;
    private MailItem.Type type;
    private String uid;

    public SetPop3Uid() {
        super(MailboxOperation.SetPop3Uid);
    }

    public SetPop3Uid(int mailboxId, int id, MailItem.Type type, String uid) {
        this();
        setMailboxId(mailboxId);
        this.itemId = id;
        this.type = type;
        this.uid = uid;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(itemId).append(", pop3uid=").append(uid);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(itemId);
        out.writeByte(type.toByte());
        out.writeUTF(uid);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        this.itemId = in.readInt();
        this.type = MailItem.Type.of(in.readByte());
        this.uid = in.readUTF();
    }

    @Override
    public void redo() throws Exception {
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mailbox.setPop3Uid(getOperationContext(), itemId, type, uid);
    }
}
