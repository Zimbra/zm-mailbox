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
import java.util.Arrays;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class DeleteItemFromDumpster extends RedoableOp {

    private int[] mIds;

    public DeleteItemFromDumpster() {
        super(MailboxOperation.DeleteItemFromDumpster);
    }

    public DeleteItemFromDumpster(int mailboxId, int[] ids) {
        this();
        setMailboxId(mailboxId);
        mIds = ids;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("ids=");
        sb.append(Arrays.toString(mIds));
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mIds.length);
        for (int id : mIds)
            out.writeInt(id);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mIds = new int[in.readInt()];
        for (int i = 0; i < mIds.length; i++)
            mIds[i] = in.readInt();
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());

        try {
            mbox.deleteFromDumpster(getOperationContext(), mIds);
        } catch (MailServiceException.NoSuchItemException e) {
            if (mLog.isInfoEnabled())
                mLog.info("Some of the items being deleted were already deleted from dumpster " + getMailboxId());
        }
    }

    @Override public boolean isDeleteOp() {
        return true;
    }
}
