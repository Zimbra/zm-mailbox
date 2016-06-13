/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
