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

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class DeleteConfig extends RedoableOp {

    private String sectionPart;

    protected DeleteConfig() {
        super(MailboxOperation.DeleteConfig);
    }

    public DeleteConfig(int mboxId, String section) {
        this();
        setMailboxId(mboxId);
        this.sectionPart = section;
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mbox.deleteConfig(getOperationContext(), this.sectionPart);
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("sectionPart=").append(this.sectionPart);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(this.sectionPart);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        this.sectionPart = in.readUTF();
    }
}
